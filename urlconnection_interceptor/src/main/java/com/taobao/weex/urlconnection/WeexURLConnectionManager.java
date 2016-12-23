/*
 * Copyright (c) 2014-present, Facebook, Inc.
 * All rights reserved.
 *
 * This source code is licensed under the BSD-style license found in the
 * LICENSE file in the root directory of this source tree. An additional grant
 * of patent rights can be found in the PATENTS file in the same directory.
 */

package com.taobao.weex.urlconnection;

import android.support.annotation.Nullable;

import com.taobao.weex.devtools.inspector.network.NetworkEventReporter;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;



/**
 * Individual connection flow manager that aids in communicating network events to Stetho
 * via the {@link NetworkEventReporter} API.  This class is
 * stateful and should be instantiated for each individual HTTP request.
 * <p>
 * Be aware that there are caveats with inspection using {@link HttpURLConnection} on Android:
 * <ul>
 * <li>Compressed payload sizes are typically not available, even when compression was in use over
 * the wire.
 * <li>Redirects are by default handled internally, making it impossible to visualize them.
 * To visualize them, redirects must be handled manually by invoking
 * {@link HttpURLConnection#setFollowRedirects(boolean)}.
 * </ul>
 */

public class WeexURLConnectionManager {
  private static final boolean sIsInspectorPresent;

  static {
    boolean isInspectorPresent = false;
    try {
      Class.forName("com.taobao.weex.devtools.WeexInspector");
      isInspectorPresent = true;
    } catch (ClassNotFoundException e) {
    }
    sIsInspectorPresent = isInspectorPresent;
  }

  @Nullable
  private final Holder mHolder;

  // Holder hides WeexURLConnectionManagerImpl from the class verifier as per:
  // http://en.wikipedia.org/wiki/Initialization-on-demand_holder_idiom
  private static class Holder {
    private final WeexURLConnectionManagerImpl impl;

    public Holder(@Nullable String friendlyName) {
      impl = new WeexURLConnectionManagerImpl(friendlyName);
    }
  }

  public WeexURLConnectionManager(@Nullable String friendlyName) {
    if (sIsInspectorPresent) {
      mHolder = new Holder(friendlyName);
    } else {
      mHolder = null;
    }
  }

  public boolean isInspectorEnabled() {
    return mHolder != null && mHolder.impl.isInspectorActive();
  }

  /**
   * Indicates that the {@link HttpURLConnection} instance has been configured and is about
   * to be used to initiate an actual HTTP connection.  Call this method before any of the
   * active methods such as {@link HttpURLConnection#connect()},
   * {@link HttpURLConnection#getInputStream()}, or {@link HttpURLConnection#getOutputStream()}
   *
   * @param connection Connection instance configured with a method and headers.
   * @param requestEntity Represents the request body if the request method supports it.
   */
  public void preConnect(
      HttpURLConnection connection,
      @Nullable SimpleRequestEntity requestEntity) {
    if (mHolder != null) {
      mHolder.impl.preConnect(connection, requestEntity);
    }
  }

  /**
   * Indicates that the {@link HttpURLConnection} has just successfully exchanged HTTP messages
   * (request headers + body and response headers) with the server but has not yet consumed
   * the response body.
   *
   * @throws IOException May throw an exception internally due to {@link HttpURLConnection}
   *     method signatures.  The request should be considered aborted/failed if this method
   *     throws.
   */
  public void postConnect() throws IOException {
    if (mHolder != null) {
      mHolder.impl.postConnect();
    }
  }

  /**
   * Indicates that there was a non-recoverable failure during HTTP message exchange at some
   * point between {@link #preConnect} and {@link #interpretResponseStream}.
   *
   * @param ex Relay the exception that was thrown from {@link HttpURLConnection}
   */
  public void httpExchangeFailed(IOException ex) {
    if (mHolder != null) {
      mHolder.impl.httpExchangeFailed(ex);
    }
  }

  /**
   * Deliver the response stream from {@link HttpURLConnection#getInputStream()} to
   * Stetho so that it can be intercepted.  Note that compression is transparently
   * supported on modern Android systems and no special awareness is necessary for
   * gzip compression on the wire.  Unfortunately this means that it is sometimes impossible
   * to determine whether compression actually occurred and so Stetho may report inflated
   * byte counts.
   * <p>
   * If the {@code Content-Length} header is provided by the server, this will be assumed to be
   * the raw byte count on the wire.
   *
   * @param responseStream Stream as furnished by {@link HttpURLConnection#getInputStream()}.
   *
   * @return The filtering stream which is to be read after this method is called.
   */
  public InputStream interpretResponseStream(@Nullable InputStream responseStream) {
    if (mHolder != null) {
      return mHolder.impl.interpretResponseStream(responseStream);
    } else {
      return responseStream;
    }
  }

  /**
   * Convenience method to access the lower level
   * {@link NetworkEventReporter} API (must be explicitly
   * cast).
   *
   * @deprecated This should no longer be used as it could potentially break the mechanism
   *     we use to allow convenient stripping of Stetho from release builds when using this
   *     module.  If you need access to this, consider writing your own custom version of this
   *     module.
   */
  @Deprecated
  @Nullable
  public Object getReporter() {
    if (mHolder != null) {
      return mHolder.impl.getReporter();
    } else {
      return null;
    }
  }

  /**
   * Low level method to access this request's unique identifier according to
   * {@link NetworkEventReporter}.  Most callers won't
   * need this.
   */
  @Nullable
  public String getRequestId() {
    if (mHolder != null) {
      return mHolder.impl.getRequestId();
    } else {
      return null;
    }
  }
}
