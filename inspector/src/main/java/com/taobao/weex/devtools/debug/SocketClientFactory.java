package com.taobao.weex.devtools.debug;

import com.taobao.weex.devtools.common.ReflectionUtil;

/**
 * Created by budao on 2016/11/1.
 */
public class SocketClientFactory {
  public static SocketClient create(DebugServerProxy proxy) {
    CustomerWSClient wsClient = new CustomerWSClient(proxy);
    if (wsClient.isAvailed()) {
      return wsClient;
    } else if (ReflectionUtil.tryGetClassForName("okhttp3.ws.WebSocketListener") != null) {
      return new OkHttp3SocketClient(proxy);
    } else if (ReflectionUtil.tryGetClassForName("com.squareup.okhttp.ws.WebSocketListener") != null) {
      return new OkHttpSocketClient(proxy);
    } else {
      new RuntimeException("No suitable websocket client found, trying to using WeexInspector.overrideWebSocketClient() to setting one").printStackTrace();
    }
    return null;
  }
}
