package com.taobao.weex.devtools.inspector.protocol.module;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.taobao.weex.devtools.debug.SocketClient;
import com.taobao.weex.devtools.debug.WMLDebugBridge;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcPeer;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcResult;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsDomain;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsMethod;
import com.taobao.weex.devtools.json.ObjectMapper;
import com.taobao.weex.devtools.json.annotation.JsonProperty;
import com.taobao.weex.utils.LogLevel;
import com.taobao.windmill.bridge.WMLBridgeManager;
import com.taobao.windmill.rt.util.WMLEnv;

import org.json.JSONException;
import org.json.JSONObject;
import java.util.HashMap;

public class WMLDebug implements ChromeDevtoolsDomain {

    private static final String TAG = "WMLDebug";
    private static final HashMap<String, LogLevel> sLevelMap = new HashMap<String, LogLevel>(6);
    private SocketClient mWebSocketClient;

    static {
        sLevelMap.put("all", LogLevel.ALL);
        sLevelMap.put("verbose", LogLevel.VERBOSE);
        sLevelMap.put("info", LogLevel.INFO);
        sLevelMap.put("debug", LogLevel.DEBUG);
        sLevelMap.put("warn", LogLevel.WARN);
        sLevelMap.put("error", LogLevel.ERROR);
    }

    private final ObjectMapper mObjectMapper = new ObjectMapper();

    public WMLDebug(SocketClient webSocketClient) {
        this.mWebSocketClient = webSocketClient;
    }

    @ChromeDevtoolsMethod
    public void remoteDebug(JsonRpcPeer peer, JSONObject params) {
        Context context = WMLEnv.getApplicationContext();
        boolean remoteDebug = params.optBoolean("value");
        if (context != null) {
            WMLBridgeManager.getInstance().restart(remoteDebug, context);
            LocalBroadcastManager.getInstance(WMLEnv.getApplicationContext()).sendBroadcast(new Intent("remote_debug_windmill"));
        }
        if (!remoteDebug) {
            WMLDebugBridge.getInstance().destoryAllDebugAppContext();
        }
    }

    @ChromeDevtoolsMethod
    public void disable(JsonRpcPeer peer, JSONObject params) {

    }

    @ChromeDevtoolsMethod
    public void reload(JsonRpcPeer peer, JSONObject params) {
        String appId = params.optString("appId");
        if (!TextUtils.isEmpty(appId)) {
            Intent intent = new Intent("debug_windmill_reload");
            intent.putExtra("appId", appId);
            LocalBroadcastManager.getInstance(WMLEnv.getApplicationContext()).sendBroadcast(intent);
        }
    }

    @ChromeDevtoolsMethod
    public void postMessage(JsonRpcPeer peer, JSONObject params) {
        String appId = params.optString("appId");
        String data = params.optString("data");
        WMLBridgeManager.getInstance().postMessage(appId, data);
    }

    @ChromeDevtoolsMethod
    public void dispatchMessage(JsonRpcPeer peer, JSONObject params) {
        String appId = params.optString("appId");
        String clientId = params.optString("clientId");
        String data = params.optString("data");
        String callbackId = params.optString("callbackId");
        WMLBridgeManager.getInstance().dispatchMessage(appId, clientId, data, callbackId);
    }

    @ChromeDevtoolsMethod
    public void dispatchMessageSync(JsonRpcPeer peer, JSONObject params) {

        int id = params.optInt("syncId");

        String appId = params.optString("appId");
        String clientId = params.optString("clientId");
        String data = params.optString("data");

        byte[] result = WMLBridgeManager.getInstance().dispatchMessageSync(appId, clientId, data);

        JSONObject response = new JSONObject();
        JSONObject response_params = new JSONObject();
        try {
            response.put("method", "WMLDebug.receiveMessageSync");
            response.put("id", id);

            response_params.put("appId", appId);
            response_params.put("data", new String(result));
            response_params.put("error", "");

            response.put("params", response_params);

            if (mWebSocketClient != null) {
                mWebSocketClient.sendText(response.toString());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
}
