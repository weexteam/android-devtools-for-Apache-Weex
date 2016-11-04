package com.taobao.weex.devtools.debug;

import android.text.TextUtils;
import android.util.Log;

import com.taobao.weex.devtools.common.LogRedirector;
import com.taobao.weex.devtools.common.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class OkHttp3SocketClient extends SocketClient {

  private static final String TAG = "OkHttp3SocketClient";

  private static HashMap<String, Class> sClazzMap = new HashMap<String, Class>();
  private static final String CLASS_WEBSOCKET = "okhttp3.ws.WebSocket";
  private static final String CLASS_WEBSOCKET_LISTENER = "okhttp3.ws.WebSocketListener";
  private static final String CLASS_WEBSOCKET_CALL = "okhttp3.ws.WebSocketCall";
  private static final String CLASS_MEDIATYPE = "okhttp3.MediaType";

  private static final String CLASS_OKHTTP_CLIENT = "okhttp3.OkHttpClient";
  private static final String CLASS_OKHTTP_CLIENT_BUILDER = "okhttp3.OkHttpClient$Builder";
  private static final String CLASS_RESPONSE = "okhttp3.Response";
  private static final String CLASS_REQUEST = "okhttp3.Request";
  private static final String CLASS_RESPONSE_BODY = "okhttp3.ResponseBody";
  private static final String CLASS_REQUEST_BODY = "okhttp3.RequestBody";
  private static final String CLASS_REQUEST_BUILDER = "okhttp3.Request$Builder";

  private static final String CLASS_BUFFER = "okio.Buffer";
  private static final String CLASS_BUFFER_SOURCE = "okio.BufferedSource";

  static {
    String[] classNames = new String[]{
        CLASS_WEBSOCKET,
        CLASS_WEBSOCKET_LISTENER,
        CLASS_WEBSOCKET_CALL,
        CLASS_MEDIATYPE,
        CLASS_OKHTTP_CLIENT,
        CLASS_OKHTTP_CLIENT_BUILDER,
        CLASS_RESPONSE,
        CLASS_REQUEST,
        CLASS_REQUEST_BUILDER,
        CLASS_BUFFER,
        CLASS_BUFFER_SOURCE,
        CLASS_REQUEST_BODY,
        CLASS_RESPONSE_BODY
    };
    for (String className : classNames) {
      sClazzMap.put(className, ReflectionUtil.tryGetClassForName(className));
    }
  }

  private Class mOkHttpClientClazz = sClazzMap.get(CLASS_OKHTTP_CLIENT);
  private Class mOkHttpClientBuilderClazz = sClazzMap.get(CLASS_OKHTTP_CLIENT_BUILDER);

  private Class mRequestClazz = sClazzMap.get(CLASS_REQUEST);
  private Class mRequestBuilderClazz = sClazzMap.get(CLASS_REQUEST_BUILDER);
  private Class mWebSocketCallClazz = sClazzMap.get(CLASS_WEBSOCKET_CALL);
  private Class mWebSocketListenerClazz = sClazzMap.get(CLASS_WEBSOCKET_LISTENER);

  private Class mRequestBodyClazz = sClazzMap.get(CLASS_REQUEST_BODY);
  private Class mResponseBodyClazz = sClazzMap.get(CLASS_RESPONSE_BODY);
  private Class mMediaTypeClazz = sClazzMap.get(CLASS_MEDIATYPE);
  private Class mWebSocketClazz = sClazzMap.get(CLASS_WEBSOCKET);
  private Class mBufferedSourceClazz = sClazzMap.get(CLASS_BUFFER_SOURCE);

  public OkHttp3SocketClient(DebugServerProxy proxy) {
    super(proxy);
    mInvocationHandler = new WebSocketInvocationHandler();
  }

  protected void connect(String url) {
    if (mSocketClient != null) {
      throw new IllegalStateException("OkHttp3SocketClient is already initialized.");
    }
    try {
      Object builder = mOkHttpClientBuilderClazz.newInstance();
      Method connectTimeout = ReflectionUtil.tryGetMethod(
          mOkHttpClientBuilderClazz,
          "connectTimeout",
          new Class[]{long.class, TimeUnit.class});

      Method writeTimeout = ReflectionUtil.tryGetMethod(
          mOkHttpClientBuilderClazz,
          "writeTimeout",
          new Class[]{long.class, TimeUnit.class});

      Method readTimeout = ReflectionUtil.tryGetMethod(
          mOkHttpClientBuilderClazz,
          "readTimeout",
          new Class[]{long.class, TimeUnit.class});

      builder = ReflectionUtil.tryInvokeMethod(builder, connectTimeout, 30, TimeUnit.SECONDS);
      builder = ReflectionUtil.tryInvokeMethod(builder, writeTimeout, 30, TimeUnit.SECONDS);
      builder = ReflectionUtil.tryInvokeMethod(builder, readTimeout, 0, TimeUnit.SECONDS);


      Method build = ReflectionUtil.tryGetMethod(mOkHttpClientBuilderClazz, "build");

      mSocketClient = ReflectionUtil.tryInvokeMethod(builder, build);

      if (!TextUtils.isEmpty(url)) {
        Object requestBuilder = mRequestBuilderClazz.newInstance();
        Method urlMethod = ReflectionUtil.tryGetMethod(
            mRequestBuilderClazz,
            "url",
            new Class[]{String.class});
        Method buildMethod = ReflectionUtil.tryGetMethod(
            mRequestBuilderClazz,
            "build");

        requestBuilder = ReflectionUtil.tryInvokeMethod(requestBuilder, urlMethod, url);
        Object request = ReflectionUtil.tryInvokeMethod(requestBuilder, buildMethod);

        Method enqueueMethod = ReflectionUtil.tryGetMethod(
            mWebSocketCallClazz,
            "enqueue",
            mWebSocketListenerClazz);
        Method createCallMethod = ReflectionUtil.tryGetMethod(
            mWebSocketCallClazz,
            "create",
            new Class[]{mOkHttpClientClazz, mRequestClazz});

        Object call = ReflectionUtil.tryInvokeMethod(
            mWebSocketCallClazz,
            createCallMethod,
            mSocketClient,
            request);

        mWebSocketListener = Proxy.newProxyInstance(
            mWebSocketListenerClazz.getClassLoader(),
            new Class[]{mWebSocketListenerClazz},
            mInvocationHandler);
        ReflectionUtil.tryInvokeMethod(call, enqueueMethod, mWebSocketListener);
      }
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    }
  }

  protected void close() {
    if (mWebSocket != null) {
      Method closeMethod = ReflectionUtil.tryGetMethod(
          mWebSocketClazz,
          "close",
          new Class[]{int.class, String.class});

      ReflectionUtil.tryInvokeMethod(mWebSocket, closeMethod, 1000, "End of session");
      mWebSocket = null;
    }
  }

  @Override
  protected void sendProtocolMessage(int requestID, String message) {
    if (mWebSocket == null) {
      return;
    }
    Method createMethod = ReflectionUtil.tryGetMethod(
        mRequestBodyClazz,
        "create",
        new Class[]{mMediaTypeClazz, String.class}
        );

    Field textField = ReflectionUtil.tryGetDeclaredField(mWebSocketClazz, "TEXT");
    Object textValue = ReflectionUtil.getFieldValue(textField, null);

    Object requestBody = ReflectionUtil.tryInvokeMethod(
        mRequestBodyClazz,
        createMethod,
        textValue,
        message);

    Method sendMessageMethod = ReflectionUtil.tryGetMethod(
        mWebSocketClazz,
        "sendMessage",
        new Class[]{mRequestBodyClazz});

    ReflectionUtil.tryInvokeMethod(mWebSocket, sendMessageMethod, requestBody);
  }


  private void abort(String message, Throwable cause) {
    Log.v(TAG, "Error occurred, shutting down websocket connection: " + message);
    close();

    // Trigger failure callbacks
    if (mConnectCallback != null) {
      mConnectCallback.onFailure(cause);
      mConnectCallback = null;
    }
  }

  class WebSocketInvocationHandler implements InvocationHandler {

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

      if ("onOpen".equals(method.getName())) {
        mWebSocket = mWebSocketClazz.cast(args[0]);
        if (mConnectCallback != null) {
          mConnectCallback.onSuccess(null);
        }
      } else if ("onFailure".equals(method.getName())) {
        abort("Websocket exception", (IOException) args[0]);
      } else if ("onMessage".equals(method.getName())) {
        Method source = ReflectionUtil.tryGetMethod(mResponseBodyClazz, "source");
        Object payload = mResponseBodyClazz.cast(args[0]);
        Object bufferSource = ReflectionUtil.tryInvokeMethod(payload,source);
        try {
          Method readUtf8 = ReflectionUtil.tryGetMethod(mBufferedSourceClazz, "readUtf8");
          String message = (String) ReflectionUtil.tryInvokeMethod(bufferSource,readUtf8);
          mProxy.handleMessage(message);
        } catch (Exception e) {
          if (LogRedirector.isLoggable(TAG, Log.VERBOSE)) {
            LogRedirector.v(TAG, "Unexpected I/O exception processing message: " + e);
          }
        } finally {
          Method closeMethod = ReflectionUtil.tryGetMethod(mBufferedSourceClazz, "close");
          ReflectionUtil.tryInvokeMethod(bufferSource, closeMethod);
        }
      } else if ("onPong".equals(method.getName())) {

      } else if ("onClose".equals(method.getName())) {
        if (mHandlerThread != null && mHandlerThread.isAlive()) {
          mHandler.sendEmptyMessage(CLOSE_WEB_SOCKET);
        }
      }
      return null;
    }
  }
}
