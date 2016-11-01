package com.taobao.weex.devtools.debug;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;

import com.taobao.weex.devtools.common.LogRedirector;
import com.taobao.weex.devtools.common.ReflectionUtil;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

public class OkHttp3SocketClient extends SocketClient {

  private static final String TAG = "OkHttp3SocketClient";
  private static final String KEY_MESSAGE = "web_socket_message";
  private Handler mHandler;
  private Object mWebSocket;
  private Callback mConnectCallback;
  private DebugServerProxy mProxy;
  private HandlerThread mHandlerThread;
  private Object mSocketClient;
  private Object mWebSocketListener;
  private InvocationHandler mInvocationHandler;

  private static HashMap<String, Class> sClazzMap = new HashMap<String, Class>();

  static {
    try {
      sClazzMap.put("okhttp3.ws.WebSocket", Class.forName("okhttp3.ws.WebSocket"));
      sClazzMap.put("okhttp3.ws.WebSocketListener", Class.forName("okhttp3.ws.WebSocketListener"));
      sClazzMap.put("okhttp3.Response", Class.forName("okhttp3.Response"));
      sClazzMap.put("okhttp3.Request", Class.forName("okhttp3.Request"));
      sClazzMap.put("okhttp3.Request$Builder", Class.forName("okhttp3.Request$Builder"));
      sClazzMap.put("okhttp3.ws.WebSocketCall", Class.forName("okhttp3.ws.WebSocketCall"));

      sClazzMap.put("okhttp3.MediaType", Class.forName("okhttp3.MediaType"));
      sClazzMap.put("okhttp3.OkHttpClient", Class.forName("okhttp3.OkHttpClient"));
      sClazzMap.put("okhttp3.OkHttpClient$Builder", Class.forName("okhttp3.OkHttpClient$Builder"));
      sClazzMap.put("okhttp3.RequestBody", Class.forName("okhttp3.RequestBody"));
      sClazzMap.put("okhttp3.ResponseBody", Class.forName("okhttp3.ResponseBody"));

      sClazzMap.put("okio.Buffer", Class.forName("okio.Buffer"));
      sClazzMap.put("okio.BufferedSource", Class.forName("okio.BufferedSource"));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  Class mOkHttpClientClazz = sClazzMap.get("okhttp3.OkHttpClient");
  Class mOkHttpClientBuilderClazz = sClazzMap.get("okhttp3.OkHttpClient$Builder");

  Class mRequestClazz = sClazzMap.get("okhttp3.Request");
  Class mRequestBuilderClazz = sClazzMap.get("okhttp3.Request$Builder");
  Class mWebSocketCallClazz = sClazzMap.get("okhttp3.ws.WebSocketCall");
  Class mWebSocketListenerClazz = sClazzMap.get("okhttp3.ws.WebSocketListener");

  Class mRequestBodyClazz = sClazzMap.get("okhttp3.RequestBody");
  Class mResponseBodyClazz = sClazzMap.get("okhttp3.ResponseBody");
  Class mMediaTypeClazz = sClazzMap.get("okhttp3.MediaType");
  Class mWebSocketClazz = sClazzMap.get("okhttp3.ws.WebSocket");
  Class mBufferedSourceClazz = sClazzMap.get("okio.BufferedSource");

  public OkHttp3SocketClient(DebugServerProxy proxy) {
    mProxy = proxy;
    mHandlerThread = new HandlerThread("DebugServerProxy");
    mHandlerThread.start();
    mHandler = new MessageHandler(mHandlerThread.getLooper());
    mInvocationHandler = new InvocationHandler();
  }

  public void connect(String url, Callback callback) {
    mConnectCallback = callback;
    Message message = Message.obtain();
    message.what = CONNECT_TO_WEB_SOCKET;
    Bundle data = new Bundle();
    data.putString(KEY_MESSAGE, url);
    message.setData(data);
    if (mHandlerThread != null && mHandlerThread.isAlive()) {
      mHandler.sendMessage(message);
    }
  }

  @Override
  public void sendText(String payload) {
    Message message = Message.obtain();
    message.what = SEND_MESSAGE;
    Bundle data = new Bundle();
    data.putString(KEY_MESSAGE, payload);
    message.setData(data);
    if (mHandlerThread != null && mHandlerThread.isAlive()) {
      mHandler.sendMessage(message);
    }
  }

  @Override
  public void sendBinary(byte[] payload) {
  }

  @Override
  public void close(int closeReason, String reasonPhrase) {
    if (mHandlerThread != null && mHandlerThread.isAlive()) {
      mHandler.sendEmptyMessage(CLOSE_WEB_SOCKET);
    }
  }

  @Override
  public boolean isOpen() {
    return mWebSocket != null;
  }

  private void connect(String url) {
    if (mSocketClient != null) {
      throw new IllegalStateException("OkHttp3SocketClient is already initialized.");
    }
    try {
      Object builder = mOkHttpClientBuilderClazz.newInstance();
      Method connectTimeout = mOkHttpClientBuilderClazz.getMethod("connectTimeout", new Class[]{long.class, TimeUnit.class});
      Method writeTimeout = mOkHttpClientBuilderClazz.getMethod("writeTimeout", new Class[]{long.class, TimeUnit.class});
      Method readTimeout = mOkHttpClientBuilderClazz.getMethod("readTimeout", new Class[]{long.class, TimeUnit.class});
      Method build = mOkHttpClientBuilderClazz.getMethod("build");

      builder = connectTimeout.invoke(builder, 30, TimeUnit.SECONDS);
      builder = writeTimeout.invoke(builder, 30, TimeUnit.SECONDS);
      builder = readTimeout.invoke(builder, 0, TimeUnit.MINUTES);
      mSocketClient = build.invoke(builder);

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

  private void closeQuietly() {
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

  private void sendMessage(int requestID, String message) {
    if (mWebSocket == null) {
      return;
    }
    try {
      Method createMethod = mRequestBodyClazz.getMethod("create", new Class[]{
          mMediaTypeClazz, String.class
      });
      Field textField = ReflectionUtil.tryGetDeclaredField(mWebSocketClazz, "TEXT");
      Object textValue = ReflectionUtil.getFieldValue(textField, null);

      Object requestBody = createMethod.invoke(mRequestBodyClazz, textValue, message);

      Method sendMessageMethod = mWebSocketClazz.getMethod("sendMessage", new Class[]{mRequestBodyClazz});
      sendMessageMethod.invoke(mWebSocket, requestBody);
    } catch (NoSuchMethodException e) {
      e.printStackTrace();
    } catch (IllegalAccessException e) {
      e.printStackTrace();
    } catch (InvocationTargetException e) {
      e.printStackTrace();
    }
  }


  private void abort(String message, Throwable cause) {
    Log.v(TAG, "Error occurred, shutting down websocket connection: " + message);
    closeQuietly();

    // Trigger failure callbacks
    if (mConnectCallback != null) {
      mConnectCallback.onFailure(cause);
      mConnectCallback = null;
    }
  }


  class MessageHandler extends Handler {

    MessageHandler(Looper looper) {
      super(looper);
    }

    public void handleMessage(Message msg) {
      switch (msg.what) {
        case CONNECT_TO_WEB_SOCKET:
          connect(msg.getData().getString(KEY_MESSAGE));
          break;
        case SEND_MESSAGE:
          OkHttp3SocketClient.this.sendMessage(0, msg.getData().getString(KEY_MESSAGE));
          break;
        case CLOSE_WEB_SOCKET:
          closeQuietly();
          mHandlerThread.quit();
          break;
        case DISCONNECT_LOOPER:
          closeQuietly();
          mHandlerThread.quit();
          break;
      }
    }
  }

  class InvocationHandler implements java.lang.reflect.InvocationHandler {

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
        Method source = mResponseBodyClazz.getMethod("source");
        Object payload = mResponseBodyClazz.cast(args[0]);
        Object bufferSource = source.invoke(payload);
        try {

          Method readUtf8 = mBufferedSourceClazz.getMethod("readUtf8");

          String message = (String) readUtf8.invoke(bufferSource);
          mProxy.handleMessage(message);
        } catch (Exception e) {
          if (LogRedirector.isLoggable(TAG, Log.VERBOSE)) {
            LogRedirector.v(TAG, "Unexpected I/O exception processing message: " + e);
          }
        } finally {
          Method closeMethod = mBufferedSourceClazz.getMethod("close");
          closeMethod.invoke(bufferSource);
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
