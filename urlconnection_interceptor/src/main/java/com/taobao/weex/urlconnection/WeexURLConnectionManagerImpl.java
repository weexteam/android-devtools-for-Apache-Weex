/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.taobao.weex.urlconnection;


import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.taobao.weex.devtools.inspector.network.DefaultResponseHandler;
import com.taobao.weex.devtools.inspector.network.NetworkEventReporter;
import com.taobao.weex.devtools.inspector.network.NetworkEventReporterManager;
import com.taobao.weex.devtools.inspector.network.RequestBodyHelper;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Isolated implementation class to allow us to escape the verifier if Stetho is not
 * present.  This is done for convenience so that {@link WeexURLConnectionManager} hooks can be
 * left in a release build without any significant overhead either at runtime or in the compiled
 * APK.
 */
class WeexURLConnectionManagerImpl {
  private static final AtomicInteger sSequenceNumberGenerator = new AtomicInteger(0);

  private NetworkEventReporter mEventReporter;
  private final int mRequestId;
  @Nullable
  private final String mFriendlyName;

  @Nullable private String mRequestIdString;

  private HttpURLConnection mConnection;
  @Nullable private URLConnectionInspectorRequest mInspectorRequest;
  @Nullable private RequestBodyHelper mRequestBodyHelper;

  public WeexURLConnectionManagerImpl(@Nullable String friendlyName) {
    mRequestId = sSequenceNumberGenerator.getAndIncrement();
    mFriendlyName = friendlyName;
    mEventReporter = NetworkEventReporterManager.get();
    if (mEventReporter == null) {
      mEventReporter = NetworkEventReporterManager.newEmptyReporter();
    }
  }

  public boolean isInspectorActive() {
    return mEventReporter.isEnabled();
  }

  /**
   * @see WeexURLConnectionManager#preConnect
   */
  public void preConnect(
      HttpURLConnection connection,
      @Nullable SimpleRequestEntity requestEntity) {
    throwIfConnection();
    mConnection = connection;
    if (isInspectorActive()) {
      mRequestBodyHelper = new RequestBodyHelper(mEventReporter, getRequestId());
      mInspectorRequest = new URLConnectionInspectorRequest(
          getRequestId(),
          mFriendlyName,
          connection,
          requestEntity,
          mRequestBodyHelper);
      mEventReporter.requestWillBeSent(mInspectorRequest);
    }
  }

  /**
   * @see WeexURLConnectionManager#postConnect
   */
  public void postConnect() throws IOException {
    throwIfNoConnection();
    if (isInspectorActive()) {
      if (mRequestBodyHelper != null && mRequestBodyHelper.hasBody()) {
        mRequestBodyHelper.reportDataSent();
      }
      mEventReporter.responseHeadersReceived(
          new URLConnectionInspectorResponse(
              getRequestId(),
              mConnection));
    }
  }

  /**
   * @see WeexURLConnectionManager#httpExchangeFailed
   */
  public void httpExchangeFailed(IOException ex) {
    throwIfNoConnection();
    if (isInspectorActive()) {
      mEventReporter.httpExchangeFailed(getRequestId(), ex.toString());
    }
  }

  /**
   * @see WeexURLConnectionManager#interpretResponseStream
   */
  public InputStream interpretResponseStream(@Nullable InputStream responseStream) {
    throwIfNoConnection();
    if (isInspectorActive()) {
      // Note that Content-Encoding is stripped out by HttpURLConnection on modern versions of
      // Android (fun fact, it's powered by okhttp) when decompression is handled transparently.
      // When this occurs, we will not be able to report the compressed size properly.  Callers,
      // however, can disable this behaviour which will once again give us access to the raw
      // Content-Encoding so that we can handle it properly.
      responseStream = mEventReporter.interpretResponseStream(
          getRequestId(),
          mConnection.getHeaderField("Content-Type"),
          mConnection.getHeaderField("Content-Encoding"),
          responseStream,
          new DefaultResponseHandler(mEventReporter, getRequestId()));
    }
    return responseStream;
  }

  private void throwIfNoConnection() {
    if (mConnection == null) {
      throw new IllegalStateException("Must call preConnect");
    }
  }

  private void throwIfConnection() {
    if (mConnection != null) {
      throw new IllegalStateException("Must not call preConnect twice");
    }
  }

  /**
   * Convenience method to access the lower level {@link NetworkEventReporter} API.
   */
  public NetworkEventReporter getReporter() {
    return mEventReporter;
  }

  /**
   * @see WeexURLConnectionManager#getRequestId()
   */
  @NonNull
  public String getRequestId() {
    if (mRequestIdString == null) {
      mRequestIdString = String.valueOf(mRequestId);
    }
    return mRequestIdString;
  }
}
