package com.taobao.weex.devtools.debug;

import com.taobao.weex.devtools.websocket.SimpleSession;

/**
 * Created by budao on 2016/11/1.
 */
public abstract class SocketClient implements SimpleSession {
  protected static final int CONNECT_TO_WEB_SOCKET = 1;
  protected static final int SEND_MESSAGE = 2;
  protected static final int CLOSE_WEB_SOCKET = 3;
  protected static final int DISCONNECT_LOOPER = 4;

  abstract void connect(String url, Callback callback);

  public interface Callback {
    void onSuccess(String response);

    void onFailure(Throwable cause);
  }
}
