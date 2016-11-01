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


public class OkHttpSocketClient extends SocketClient {

  private static final String TAG = "OkHttpSocketClient";
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
      sClazzMap.put("com.squareup.okhttp.ws.WebSocket", Class.forName("com.squareup.okhttp.ws.WebSocket"));
      sClazzMap.put("com.squareup.okhttp.ws.WebSocketListener", Class.forName("com.squareup.okhttp.ws.WebSocketListener"));
      sClazzMap.put("com.squareup.okhttp.Response", Class.forName("com.squareup.okhttp.Response"));
      sClazzMap.put("com.squareup.okhttp.Request", Class.forName("com.squareup.okhttp.Request"));
      sClazzMap.put("com.squareup.okhttp.Request$Builder", Class.forName("com.squareup.okhttp.Request$Builder"));
      sClazzMap.put("com.squareup.okhttp.ws.WebSocketCall", Class.forName("com.squareup.okhttp.ws.WebSocketCall"));

      sClazzMap.put("com.squareup.okhttp.ws.WebSocket$PayloadType", Class.forName("com.squareup.okhttp.ws.WebSocket$PayloadType"));
      sClazzMap.put("com.squareup.okhttp.OkHttpClient", Class.forName("com.squareup.okhttp.OkHttpClient"));

      sClazzMap.put("okio.Buffer", Class.forName("okio.Buffer"));
      sClazzMap.put("okio.BufferedSource", Class.forName("okio.BufferedSource"));
    } catch (ClassNotFoundException e) {
      e.printStackTrace();
    }
  }

  Class mOkHttpClientClazz = sClazzMap.get("com.squareup.okhttp.OkHttpClient");
  Class mRequestClazz = sClazzMap.get("com.squareup.okhttp.Request");
  Class mRequestBuilderClazz = sClazzMap.get("com.squareup.okhttp.Request$Builder");
  Class mWebSocketCallClazz = sClazzMap.get("com.squareup.okhttp.ws.WebSocketCall");
  Class mWebSocketListenerClazz = sClazzMap.get("com.squareup.okhttp.ws.WebSocketListener");

  Class mMediaTypeClazz = sClazzMap.get("com.squareup.okhttp.ws.WebSocket$PayloadType");
  Class mWebSocketClazz = sClazzMap.get("com.squareup.okhttp.ws.WebSocket");

  Class mBufferClazz = sClazzMap.get("okio.Buffer");
  Class mBufferedSourceClazz = sClazzMap.get("okio.BufferedSource");

  public OkHttpSocketClient(DebugServerProxy proxy) {
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
          OkHttpSocketClient.this.sendMessage(0, msg.getData().getString(KEY_MESSAGE));
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
