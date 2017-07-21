package com.taobao.weex.devtools.debug;

/**
 * Created by moxun on 2017/6/12.
 */

public interface IWebSocketClient {
    boolean isOpen();
    void connect(String wsAddress, WSListener listener);
    void close();
    void sendMessage(int requestId, String message);

    interface WSListener {
        void onOpen();
        void onMessage(String message);
        void onClose();
        void onFailure(Throwable cause);
    }
}
