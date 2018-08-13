package com.taobao.weex.devtools.debug;

import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

import com.taobao.weex.devtools.websocket.SimpleSession;

import java.lang.reflect.InvocationHandler;

/**
 * Created by budao on 2016/11/1.
 */
public abstract class SocketClient implements SimpleSession {
  protected static final int CONNECT_TO_WEB_SOCKET = 1;
  protected static final int SEND_MESSAGE = 2;
  protected static final int CLOSE_WEB_SOCKET = 3;
  protected static final int DISCONNECT_LOOPER = 4;
  private static final String KEY_MESSAGE = "web_socket_message";
  protected Handler mHandler;
  protected Object mWebSocket;
  protected Callback mConnectCallback;
  protected DebugServerProxy mProxy;
  protected HandlerThread mHandlerThread;
  protected Object mSocketClient;
  protected Object mWebSocketListener;
  protected InvocationHandler mInvocationHandler;
  private String mUrl;

  public SocketClient(DebugServerProxy proxy) {
    init(proxy);
  }

  public void connect(String url, Callback callback) {
    mUrl = url;
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

  public String getUrl() {
    return mUrl;
  }

  protected void init(DebugServerProxy proxy) {
    mUrl = proxy.mRemoteUrl;
    mProxy = proxy;
    mHandlerThread = new HandlerThread("DebugServerProxy");
    mHandlerThread.start();
    mHandler = new MessageHandler(mHandlerThread.getLooper());
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

  @Override
  public void post(Runnable runnable) {
    if (mHandler != null) {
      mHandler.post(runnable);
    }
  }

  protected abstract void connect(String url);

  protected abstract void close();

  protected abstract void sendProtocolMessage(int requestID, String message);

  public interface Callback {
    void onSuccess(String response);

    void onFailure(Throwable cause);
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
          sendProtocolMessage(0, msg.getData().getString(KEY_MESSAGE));
          break;
        case CLOSE_WEB_SOCKET:
          close();
          mHandlerThread.quit();
          break;
        case DISCONNECT_LOOPER:
          close();
          mHandlerThread.quit();
          break;
      }
    }
  }
}
