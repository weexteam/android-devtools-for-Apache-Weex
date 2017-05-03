package com.taobao.weex.devtools.debug;

/**
 * Created by budao on 2016/11/1.
 */
public class SocketClientFactory {
  public static SocketClient create(DebugServerProxy proxy) {
    return new WebSocketSession(proxy);
  }
}
