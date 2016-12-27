/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */


package com.taobao.weex.devtools.inspector.network;

import android.support.annotation.Nullable;

import java.io.ByteArrayOutputStream;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterOutputStream;


/**
 * Helper which manages provides computed request sizes as well as transparent decompression.
 * Note that request compression is not officially part of the HTTP standard however it is
 * commonly in use and can be conveniently supported here.
 * <p>
 * To use, invoke {@link #createBodySink} to prepare an output stream where the raw body can be
 * written.  Then invoke {@link #getDisplayBody()} to retrieve the possibly decoded body.
 * Finally, {@link #reportDataSent()} can be called to report to WeexInspector the raw and decompressed
 * payload sizes.
 */
public class RequestBodyHelper {

  protected static final String GZIP_ENCODING = "gzip";
  protected static final String DEFLATE_ENCODING = "deflate";

  private final NetworkEventReporter mEventReporter;
  private final String mRequestId;

  private ByteArrayOutputStream mDeflatedOutput;
  private CountingOutputStream mDeflatingOutput;

  public RequestBodyHelper(NetworkEventReporter eventReporter, String requestId) {
    mEventReporter = eventReporter;
    mRequestId = requestId;
  }

  public OutputStream createBodySink(@Nullable String contentEncoding) throws IOException {
    OutputStream deflatingOutput;
    ByteArrayOutputStream deflatedOutput = new ByteArrayOutputStream();
    if (GZIP_ENCODING.equals(contentEncoding)) {
      deflatingOutput = GZipOutputStream.create(deflatedOutput);
    } else if (DEFLATE_ENCODING.equals(contentEncoding)) {
      deflatingOutput = new InflaterOutputStream(deflatedOutput);
    } else {
      deflatingOutput = deflatedOutput;
    }

    mDeflatingOutput = new CountingOutputStream(deflatingOutput);
    mDeflatedOutput = deflatedOutput;

    return mDeflatingOutput;
  }

  public byte[] getDisplayBody() {
    throwIfNoBody();
    return mDeflatedOutput.toByteArray();
  }

  public boolean hasBody() {
    return mDeflatedOutput != null;
  }

  public void reportDataSent() {
    throwIfNoBody();
    mEventReporter.dataSent(
        mRequestId,
        mDeflatedOutput.size(),
        (int)mDeflatingOutput.getCount());
  }

  private void throwIfNoBody() {
    if (!hasBody()) {
      throw new IllegalStateException("No body found; has createBodySink been called?");
    }
  }

  private class CountingOutputStream extends FilterOutputStream {
    private long mCount;

    public CountingOutputStream(OutputStream out) {
      super(out);
    }

    public long getCount() {
      return mCount;
    }

    @Override
    public void write(int oneByte) throws IOException {
      out.write(oneByte);
      mCount++;
    }

    @Override
    public void write(byte[] buffer) throws IOException {
      write(buffer, 0, buffer.length);
    }

    @Override
    public void write(byte[] buffer, int offset, int length) throws IOException {
      out.write(buffer, offset, length);
      mCount += length;
    }
  }

  /**
   * An {@link OutputStream} filter which decompresses gzip data before it is written to the
   * specified destination output stream.  This is functionally equivalent to
   * {@link InflaterOutputStream} but provides gzip header awareness.  The
   * implementation however is very different to avoid actually interpreting the gzip header.
   */
  static class GZipOutputStream extends FilterOutputStream {
    private final Future<Void> mCopyFuture;

    private static final ExecutorService sExecutor = Executors.newCachedThreadPool();

    public static GZipOutputStream create(OutputStream finalOut) throws IOException {
      PipedInputStream pipeIn = new PipedInputStream();
      PipedOutputStream pipeOut = new PipedOutputStream(pipeIn);

      Future<Void> copyFuture = sExecutor.submit(
              new GunzippingCallable(pipeIn, finalOut));

      return new GZipOutputStream(pipeOut, copyFuture);
    }

    private GZipOutputStream(OutputStream out, Future<Void> copyFuture) throws IOException {
      super(out);
      mCopyFuture = copyFuture;
    }

    @Override
    public void close() throws IOException {
      boolean success = false;
      try {
        super.close();
        success = true;
      } finally {
        try {
          getAndRethrow(mCopyFuture);
        } catch (IOException e) {
          if (success) {
            throw e;
          }
        }
      }
    }

    private static <T> T getAndRethrow(Future<T> future) throws IOException {
      while (true) {
        try {
          return future.get();
        } catch (InterruptedException e) {
          // Continue...
        } catch (ExecutionException e) {
          Throwable cause = e.getCause();
          propagateIfInstanceOf(cause, IOException.class);
          propagate(cause);
        }
      }
    }

    private static class GunzippingCallable implements Callable<Void> {
      private final InputStream mIn;
      private final OutputStream mOut;

      public GunzippingCallable(InputStream in, OutputStream out) {
        mIn = in;
        mOut = out;
      }

      @Override
      public Void call() throws IOException {
        GZIPInputStream in = new GZIPInputStream(mIn);
        try {
          copy(in, mOut, new byte[1024]);
        } finally {
          in.close();
          mOut.close();
        }
        return null;
      }
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void propagateIfInstanceOf(Throwable t, Class<T> type)
            throws T {
      if (type.isInstance(t)) {
        throw (T)t;
      }
    }

    private static RuntimeException propagate(Throwable t) {
      propagateIfInstanceOf(t, Error.class);
      propagateIfInstanceOf(t, RuntimeException.class);
      throw new RuntimeException(t);
    }

    private static void copy(InputStream input, OutputStream output, byte[] buffer)
            throws IOException {
      int n;
      while ((n = input.read(buffer)) != -1) {
        output.write(buffer, 0, n);
      }
    }
  }
}
