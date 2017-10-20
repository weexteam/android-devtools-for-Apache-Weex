package com.taobao.weex.devtools.debug;

import com.taobao.weex.devtools.WeexInspector;

import java.io.IOException;

/**
 * Created by moxun on 2017/6/12.
 */

public class CustomerWSClient extends SocketClient {

    private IWebSocketClient webSocketClient;

    public CustomerWSClient(DebugServerProxy proxy) {
        super(proxy);
        webSocketClient = WeexInspector.getCustomerWSClient();
    }

    @Override
    protected void connect(String url) {
        if (webSocketClient != null) {
            webSocketClient.connect(url, new IWebSocketClient.WSListener() {
                @Override
                public void onOpen() {
                    if (mConnectCallback != null) {
                        mConnectCallback.onSuccess(null);
                    }
                }

                @Override
                public void onMessage(String message) {
                    try {
                        mProxy.handleMessage(message);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onClose() {
                    if (mHandlerThread != null && mHandlerThread.isAlive()) {
                        mHandler.sendEmptyMessage(CLOSE_WEB_SOCKET);
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    if (mConnectCallback != null) {
                        mConnectCallback.onFailure(cause);
                        mConnectCallback = null;
                    }
                }
            });
        }
    }

    @Override
    protected void close() {
        if (webSocketClient != null) {
            webSocketClient.close();
        }
    }

    @Override
    protected void sendProtocolMessage(int requestID, String message) {
        if (webSocketClient != null) {
            webSocketClient.sendMessage(requestID, message);
        }
    }

    @Override
    public boolean isOpen() {
        return webSocketClient != null && webSocketClient.isOpen();
    }

    public boolean isAvailed() {
        return webSocketClient != null;
    }
}
