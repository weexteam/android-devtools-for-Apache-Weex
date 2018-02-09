package com.taobao.weex.devtools.debug;

import android.text.TextUtils;
import android.util.Log;

import com.taobao.weex.WXEnvironment;
import com.taobao.weex.devtools.common.LogRedirector;
import com.taobao.weex.devtools.common.ReflectionUtil;
import com.taobao.weex.utils.WXLogUtils;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import android.text.TextUtils;
import android.util.Log;
import com.taobao.weex.devtools.common.LogRedirector;
import com.taobao.weex.devtools.common.ReflectionUtil;
import com.taobao.weex.utils.WXLogUtils;


public class OkHttpSocketClient extends SocketClient {

  private static final String TAG = "OkHttpSocketClient";

  private static HashMap<String, Class> sClazzMap = new HashMap<String, Class>();

  private static final String CLASS_WEBSOCKET = "com.squareup.okhttp.ws.WebSocket";
  private static final String CLASS_WEBSOCKET_LISTENER = "com.squareup.okhttp.ws.WebSocketListener";
  private static final String CLASS_WEBSOCKET_CALL = "com.squareup.okhttp.ws.WebSocketCall";
  private static final String CLASS_WEBSOCKET_PAYLOADTYPE = "com.squareup.okhttp.ws.WebSocket$PayloadType";

  private static final String CLASS_OKHTTP_CLIENT = "com.squareup.okhttp.OkHttpClient";
  private static final String CLASS_RESPONSE = "com.squareup.okhttp.Response";
  private static final String CLASS_REQUEST = "com.squareup.okhttp.Request";
  private static final String CLASS_REQUEST_BUILDER = "com.squareup.okhttp.Request$Builder";

  private static final String CLASS_BUFFER = "okio.Buffer";
  private static final String CLASS_BUFFER_SOURCE = "okio.BufferedSource";

  //okhttp 2.7.5
  private static final String CLASS_MEDIATYPE = "com.squareup.okhttp.MediaType";
  private static final String CLASS_REQUESTBODY = "com.squareup.okhttp.RequestBody";
  private static final String CLASS_RESPONSEBODY = "com.squareup.okhttp.ResponseBody";

  static {
    String[] classNames = new String[]{
        CLASS_WEBSOCKET,
        CLASS_WEBSOCKET_LISTENER,
        CLASS_WEBSOCKET_CALL,
        CLASS_WEBSOCKET_PAYLOADTYPE,
        CLASS_OKHTTP_CLIENT,
        CLASS_RESPONSE,
        CLASS_REQUEST,
        CLASS_REQUEST_BUILDER,
        CLASS_BUFFER,
        CLASS_BUFFER_SOURCE,
        CLASS_MEDIATYPE,
        CLASS_REQUESTBODY,
        CLASS_RESPONSEBODY
    };
    for (String className : classNames) {
      sClazzMap.put(className, ReflectionUtil.tryGetClassForName(className));
    }
  }

  private Class mOkHttpClientClazz = sClazzMap.get(CLASS_OKHTTP_CLIENT);
  private Class mRequestClazz = sClazzMap.get(CLASS_REQUEST);
  private Class mRequestBuilderClazz = sClazzMap.get(CLASS_REQUEST_BUILDER);
  private Class mWebSocketCallClazz = sClazzMap.get(CLASS_WEBSOCKET_CALL);
  private Class mWebSocketListenerClazz = sClazzMap.get(CLASS_WEBSOCKET_LISTENER);

  private Class mPayloadTypeClazz = sClazzMap.get(CLASS_WEBSOCKET_PAYLOADTYPE);
  private Class mWebSocketClazz = sClazzMap.get(CLASS_WEBSOCKET);

  private Class mBufferClazz = sClazzMap.get(CLASS_BUFFER);
  private Class mBufferedSourceClazz = sClazzMap.get(CLASS_BUFFER_SOURCE);

  private Class mMediaTypeClazz = sClazzMap.get(CLASS_MEDIATYPE);

  private Class mRequestBodyClazz = sClazzMap.get(CLASS_REQUESTBODY);
  private Class mResponseBodyClazz = sClazzMap.get(CLASS_RESPONSEBODY);

  public OkHttpSocketClient(DebugServerProxy proxy) {
    super(proxy);
    mInvocationHandler = new WebSocketInvocationHandler();
  }

  protected void connect(String url) {
    if (mSocketClient != null) {
      throw new IllegalStateException("OkHttpSocketClient is already initialized.");
    }
    try {
      mSocketClient = mOkHttpClientClazz.newInstance();
      Method connectTimeout = ReflectionUtil.tryGetMethod(
          mOkHttpClientClazz,
          "setConnectTimeout",
          new Class[]{long.class, TimeUnit.class});

      Method writeTimeout = ReflectionUtil.tryGetMethod(
          mOkHttpClientClazz,
          "setWriteTimeout",
          new Class[]{long.class, TimeUnit.class});

      Method readTimeout = ReflectionUtil.tryGetMethod(
          mOkHttpClientClazz,
          "setReadTimeout",
          new Class[]{long.class, TimeUnit.class});

      ReflectionUtil.tryInvokeMethod(mSocketClient, connectTimeout, 30, TimeUnit.SECONDS);
      ReflectionUtil.tryInvokeMethod(mSocketClient, writeTimeout, 30, TimeUnit.SECONDS);
      ReflectionUtil.tryInvokeMethod(mSocketClient, readTimeout, 0, TimeUnit.SECONDS);

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

  @Override
  protected void close() {
    if (mWebSocket != null) {
      Method closeMethod = ReflectionUtil.tryGetMethod(mWebSocketClazz, "close",
                                                       new Class[]{int.class, String.class});
      ReflectionUtil.tryInvokeMethod(mWebSocket, closeMethod, 1000, "End of session");
      mWebSocket = null;
      WXLogUtils.w(TAG, "Close websocket connection");
    }
  }

  @Override
  protected void sendProtocolMessage(int requestID, String message) {
    if (mWebSocket == null) {
      return;
    }
    try {
      if (mPayloadTypeClazz != null) {
        Field textField = ReflectionUtil.tryGetDeclaredField(mPayloadTypeClazz, "TEXT");
        Object textValue = ReflectionUtil.getFieldValue(textField, null);
        Method sendMessageMethod = ReflectionUtil.tryGetMethod(mWebSocketClazz,
            "sendMessage", new Class[] {mPayloadTypeClazz, mBufferClazz});

        Object buffer = mBufferClazz.newInstance();
        Method writeUtf8 = ReflectionUtil.tryGetMethod(mBufferClazz, "writeUtf8",
            new Class[] {String.class});

        ReflectionUtil.tryInvokeMethod(mWebSocket, sendMessageMethod, textValue,
            ReflectionUtil.tryInvokeMethod(buffer, writeUtf8, message));
      } else {
        //okhttp 2.7.5
        Field textField = ReflectionUtil.tryGetDeclaredField(mWebSocketClazz, "TEXT");
        Object textValue = ReflectionUtil.getFieldValue(textField, null);

        Method createMethod = ReflectionUtil.tryGetMethod(
            mRequestBodyClazz,
            "create",
            new Class[] {mMediaTypeClazz, String.class});

        Object requestBody = ReflectionUtil.tryInvokeMethod(
            mRequestBodyClazz,
            createMethod,
            textValue,
            message);
        Method sendMessageMethod = ReflectionUtil.tryGetMethod(mWebSocketClazz,
            "sendMessage", new Class[] {mRequestBodyClazz});
        ReflectionUtil.tryInvokeMethod(mWebSocket, sendMessageMethod, requestBody);
      }

      if (WXEnvironment.isApkDebugable()) {
        Log.d(TAG, "sendProtocolMessage :" + message);
      }

    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    }
  }


  private void abort(String message, Throwable cause) {
    Log.w(TAG, "Error occurred, shutting down websocket connection: " + message);
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
        abort("Websocket onFailure", (IOException) args[0]);
      } else if ("onMessage".equals(method.getName())) {
        Object bufferedSource;
        if (mPayloadTypeClazz != null) {
          bufferedSource = mBufferedSourceClazz.cast(args[0]);
        } else {
          //okhttp 2.7.5
          Object responseBody = mResponseBodyClazz.cast(args[0]);
          Method sourceMethod = ReflectionUtil.tryGetMethod(mResponseBodyClazz, "source");
          bufferedSource = ReflectionUtil.tryInvokeMethod(responseBody, sourceMethod);
        }
        Method readUtf8 = ReflectionUtil.tryGetMethod(mBufferedSourceClazz, "readUtf8");
        try {
          String message = (String)ReflectionUtil.tryInvokeMethod(bufferedSource, readUtf8);
          mProxy.handleMessage(message);
        } catch (Exception e) {
          if (LogRedirector.isLoggable(TAG, Log.VERBOSE)) {
            LogRedirector.w(TAG, "Unexpected I/O exception processing message: " + e);
          }
        } finally {
          Method closeMethod = ReflectionUtil.tryGetMethod(mBufferedSourceClazz, "close");
          ReflectionUtil.tryInvokeMethod(bufferedSource, closeMethod);
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
