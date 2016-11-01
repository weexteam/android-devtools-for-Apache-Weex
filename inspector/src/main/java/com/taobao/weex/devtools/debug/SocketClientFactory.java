package com.taobao.weex.devtools.debug;

import com.taobao.weex.devtools.common.ReflectionUtil;

/**
 * Created by budao on 2016/11/1.
 */
public class SocketClientFactory {
  public static SocketClient create(DebugServerProxy proxy) {
    if (ReflectionUtil.tryGetClassForName("okhttp3.ws.WebSocketListener") != null) {
      return new OkHttp3SocketClient(proxy);
    } else if (ReflectionUtil.tryGetClassForName("com.squareup.okhttp.ws.WebSocketListener") != null) {
      return new OkHttpSocketClient(proxy);
    }
    return null;
  }
}
