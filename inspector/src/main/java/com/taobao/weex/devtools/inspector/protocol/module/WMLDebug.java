package com.taobao.weex.devtools.inspector.protocol.module;

import com.taobao.weex.devtools.common.LogUtil;
import com.taobao.weex.devtools.debug.WMLDebugBridge;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcPeer;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsDomain;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsMethod;
import com.taobao.weex.devtools.json.ObjectMapper;
import com.taobao.weex.utils.LogLevel;
import com.taobao.windmill.bridge.WMLBridgeManager;

import org.json.JSONObject;

import java.nio.charset.Charset;
import java.util.HashMap;

public class WMLDebug implements ChromeDevtoolsDomain {

    private static final String TAG = "WMLDebug";
    private static final HashMap<String, LogLevel> sLevelMap = new HashMap<String, LogLevel>(6);

    static {
        sLevelMap.put("all", LogLevel.ALL);
        sLevelMap.put("verbose", LogLevel.VERBOSE);
        sLevelMap.put("info", LogLevel.INFO);
        sLevelMap.put("debug", LogLevel.DEBUG);
        sLevelMap.put("warn", LogLevel.WARN);
        sLevelMap.put("error", LogLevel.ERROR);
    }

    private final ObjectMapper mObjectMapper = new ObjectMapper();

    public WMLDebug() {

    }

    @ChromeDevtoolsMethod
    public void enable(JsonRpcPeer peer, JSONObject params) {
        LogUtil.e("WMLDebug-new >>>> enable=" + params);
    }

    @ChromeDevtoolsMethod
    public void disable(JsonRpcPeer peer, JSONObject params) {
        LogUtil.e("WMLDebug-new >>>> disable=" + params);
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
}
