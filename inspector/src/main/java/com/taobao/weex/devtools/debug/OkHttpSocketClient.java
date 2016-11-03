package com.taobao.weex.devtools.debug;

import android.text.TextUtils;
import android.util.Log;

import com.taobao.weex.devtools.common.LogRedirector;
import com.taobao.weex.devtools.common.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;


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
        CLASS_BUFFER_SOURCE
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

  private Class mMediaTypeClazz = sClazzMap.get(CLASS_WEBSOCKET_PAYLOADTYPE);
  private Class mWebSocketClazz = sClazzMap.get(CLASS_WEBSOCKET);

  private Class mBufferClazz = sClazzMap.get(CLASS_BUFFER);
  private Class mBufferedSourceClazz = sClazzMap.get(CLASS_BUFFER_SOURCE);

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
      Method connectTimeout = mOkHttpClientClazz.getMethod("setConnectTimeout",
          new Class[]{long.class, TimeUnit.class});
      Method writeTimeout = mOkHttpClientClazz.getMethod("setWriteTimeout",
          new Class[]{long.class, TimeUnit.class});
      Method readTimeout = mOkHttpClientClazz.getMethod("setReadTimeout",
          new Class[]{long.class, TimeUnit.class});

      connectTimeout.invoke(mSocketClient, 30, TimeUnit.SECONDS);
      writeTimeout.invoke(mSocketClient, 30, TimeUnit.SECONDS);
      readTimeout.invoke(mSocketClient, 0, TimeUnit.MINUTES);

      if (!TextUtils.isEmpty(url)) {
        Object requestBuilder = mRequestBuilderClazz.newInstance();
        Method urlMethod = mRequestBuilderClazz.getMethod("url", new Class[]{String.class});
        Method buildMethod = mRequestBuilderClazz.getMethod("build");
        requestBuilder = urlMethod.invoke(requestBuilder, url);
        Object request = buildMethod.invoke(requestBuilder);


        Method enqueueMethod = mWebSocketCallClazz.getDeclaredMethod("enqueue", mWebSocketListenerClazz);
        Method createCallMethod = mWebSocketCallClazz.getDeclaredMethod("create", new
            Class[]{mOkHttpClientClazz, mRequestClazz});
        Object call = createCallMethod.invoke(mWebSocketCallClazz, mSocketClient, request);
        mWebSocketListener = Proxy.newProxyInstance(mWebSocketListenerClazz.getClassLoader(),
            new Class[]{mWebSocketListenerClazz}, mInvocationHandler);
        enqueueMethod.invoke(call, mWebSocketListener);
      }
    } catch (InstantiationException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }

  @Override
  protected void close() {
    if (mWebSocket != null) {
      try {
        Method closeMethod = mWebSocketClazz.getMethod("close", new Class[]{int.class, String.class});
        closeMethod.invoke(mWebSocket, 1000, "End of session");
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      }
      mWebSocket = null;
    }
  }

  protected void sendProtocolMessage(int requestID, String message) {
    if (mWebSocket == null) {
      return;
    }
    try {
      Field textField = ReflectionUtil.tryGetDeclaredField(mMediaTypeClazz, "TEXT");
      Object textValue = ReflectionUtil.getFieldValue(textField, null);
      Method sendMessageMethod = mWebSocketClazz.getMethod("sendMessage",
          new Class[]{mMediaTypeClazz, mBufferClazz});

      Object buffer = mBufferClazz.newInstance();
      Method writeUtf8 = mBufferClazz.getMethod("writeUtf8", new Class[]{String.class});
      sendMessageMethod.invoke(mWebSocket, textValue, writeUtf8.invoke(buffer, message));
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    } catch (InstantiationException e) {
      e.printStackTrace();
    }
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
        Object bufferedSource = mBufferedSourceClazz.cast(args[0]);
        Method readUtf8 = mBufferedSourceClazz.getMethod("readUtf8");
        try {
          String message = (String) readUtf8.invoke(bufferedSource);
          mProxy.handleMessage(message);
        } catch (Exception e) {
          if (LogRedirector.isLoggable(TAG, Log.VERBOSE)) {
            LogRedirector.v(TAG, "Unexpected I/O exception processing message: " + e);
          }
        } finally {
          Method closeMethod = mBufferedSourceClazz.getMethod("close");
          closeMethod.invoke(bufferedSource);
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
