package com.taobao.weex.devtools.debug;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.io.IOException;
import java.net.URI;

/**
 * Created by moxun on 17/4/25.
 */

public class WebSocketSession extends SocketClient {

    private WebSocketClient webSocketClient;

    public WebSocketSession(DebugServerProxy proxy) {
        super(proxy);
    }

    @Override
    protected void connect(String url) {
        webSocketClient = new WebSocketClient(URI.create(url)) {
            @Override
            public void onOpen(ServerHandshake serverHandshake) {
                if (mConnectCallback != null) {
                    mConnectCallback.onSuccess(null);
                }
            }

            @Override
            public void onMessage(String s) {
                try {
                    mProxy.handleMessage(s);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onClose(int i, String s, boolean b) {
                if (mHandlerThread != null && mHandlerThread.isAlive()) {
                    mHandler.sendEmptyMessage(CLOSE_WEB_SOCKET);
                }
            }

            @Override
            public void onError(Exception e) {
                if (webSocketClient != null) {
                    webSocketClient.close();
                }

                if (mConnectCallback != null) {
                    mConnectCallback.onFailure(e);
                    mConnectCallback = null;
                }
                e.printStackTrace();
            }
        };
        webSocketClient.connect();
    }

    @Override
    protected void close() {
        webSocketClient.close();
    }

    @Override
    protected void sendProtocolMessage(int requestID, String message) {
        webSocketClient.send(message);
    }

    @Override
    public boolean isOpen() {
        return webSocketClient != null && webSocketClient.isOpen();
    }
}
