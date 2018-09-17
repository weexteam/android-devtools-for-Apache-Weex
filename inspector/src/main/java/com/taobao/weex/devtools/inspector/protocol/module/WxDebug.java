package com.taobao.weex.devtools.inspector.protocol.module;

import android.content.Context;
import android.content.Intent;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.bridge.WXJSObject;
import com.taobao.weex.devtools.debug.WXDebugBridge;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcPeer;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcResult;
import com.taobao.weex.devtools.inspector.network.NetworkEventReporterImpl;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsDomain;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsMethod;
import com.taobao.weex.devtools.json.ObjectMapper;
import com.taobao.weex.devtools.json.annotation.JsonProperty;
import com.taobao.weex.utils.LogLevel;
import com.taobao.weex.utils.WXLogUtils;
import com.taobao.weex.utils.WXWsonJSONSwitch;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.List;

/**
 * Created by budao on 16/6/24.
 */
public class WxDebug implements ChromeDevtoolsDomain {

    private static final String TAG = "WxDebug";
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

    public WxDebug() {

    }

    @ChromeDevtoolsMethod
    public void enable(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> enable=" + params);
        Context context = WXEnvironment.getApplication();
        if (context != null) {
            WXSDKEngine.reload(context, true);
            context.sendBroadcast(new Intent()
                    .setAction(WXSDKInstance.ACTION_DEBUG_INSTANCE_REFRESH)
                    .putExtra("params", null == params ? "" : params.toString())
            );
        }
    }

    @ChromeDevtoolsMethod
    public void disable(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> disable=" + params);
        Context context = WXEnvironment.getApplication();
        if (context != null) {
            WXSDKEngine.reload(context, false);
            context.sendBroadcast(new Intent()
                    .setAction(WXSDKInstance.ACTION_DEBUG_INSTANCE_REFRESH)
                    .putExtra("params", null == params ? "" : params.toString())
            );
        }
    }

    @ChromeDevtoolsMethod
    public void setLogLevel(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> setLogLevel=" + params);
        if (params != null) {
            LogLevel logLevel = sLevelMap.get(params.optString("logLevel"));
            if (logLevel != null) {
                WXEnvironment.sLogLevel = logLevel;
            }
        }
    }

    @ChromeDevtoolsMethod
    public void setElementMode(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> setElementMode=" + params);
        if (params != null) {
            String mode = params.optString("mode");
            if ("vdom".equals(mode)) {
                DOM.setNativeMode(false);
            } else {
                DOM.setNativeMode(true);
            }
        }
    }

    @ChromeDevtoolsMethod
    public void callNative(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> callNative=" + params);
        //TODO CREATEBODY  callCreateBody

        if (params != null) {
//      WXDebugBridge.getInstance().callNative(
//              params.optString("instance"),
//              params.optString("tasks"),
//              params.optString("callback"));

            final String instance = params.optString("instance");
            final byte[] tasks = params.optString("tasks").getBytes();
            final String callback = params.optString("callback");
            WXBridgeManager.getInstance().post(new Runnable() {
                @Override
                public void run() {
                    WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallNative(instance, tasks, callback);
                }
            });
        }
    }


    @ChromeDevtoolsMethod
    public void callAddElement(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> callAddElement=" + params);
        if (null == params) {
            Log.e(TAG, "callAddElement: params==null !");
            return;
        }
        final String instanceInd = params.optString("instance");
        final String ref = params.optString("ref");
        final String index = params.optString("index");
//        final String callBack = params.optString("callback");
        final String dom = params.optString("dom");

        //      WXDebugBridge.getInstance().callAddElement(
        //          params.optString("instance"),
        //          params.optString("ref"),
        //          params.optString("dom"),
        //          params.optString("index"),
        //          params.optString("callback"));
        //   }

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallAddElement(instanceInd, ref, dom, index);
            }
        });
    }

    @ChromeDevtoolsMethod
    public void callCreateBody(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callCreateBody: params==null !");
            return;
        }

        Log.e(TAG, "callCreateBody: params==" + params.toString());

        final String instanceInd = params.optString("instance");
        final String domStr = params.optString("domStr");
        if (instanceInd == null || instanceInd.isEmpty() || domStr == null || domStr.isEmpty())
            return;
        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallCreateBody(instanceInd, domStr);
            }
        });
    }

    @ChromeDevtoolsMethod
    public void callUpdateFinish(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callUpdateFinish: params==null !");
            return;
        }


        final String instanceInd = params.optString("instance");
        final String domStr = params.optString("domStr");
        final String task = params.optString("tasks");

        if (instanceInd == null || instanceInd.isEmpty() || domStr == null || domStr.isEmpty())
            return;

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallUpdateFinish(instanceInd, task.getBytes(), domStr);
            }
        });


    }

    @ChromeDevtoolsMethod
    public void callCreateFinish(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callCreateFinish: params==null !");
            return;
        }

        final String instanceInd = params.optString("instance");

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallCreateFinish(instanceInd);
            }
        });


    }

    @ChromeDevtoolsMethod
    public void callRefreshFinish(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callRefreshFinish: params==null !");
            return;
        }


        final String instanceInd = params.optString("instance");
        final String callback = params.optString("callback");
        final String task = params.optString("tasks");
        if (instanceInd == null || instanceInd.isEmpty())
            return;

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallRefreshFinish(instanceInd, task.getBytes(), callback);
            }
        });
    }

    @ChromeDevtoolsMethod
    public void callUpdateAttrs(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callUpdateAttrs: params==null !");
            return;
        }

        final String instanceInd = params.optString("instance");
        final String ref = params.optString("ref");
        final String data = params.optString("data");

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallUpdateAttrs(instanceInd, ref, data);
            }
        });
    }

    @ChromeDevtoolsMethod
    public void callUpdateStyle(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callUpdateStyle: params==null !");
            return;
        }

        final String instanceInd = params.optString("instance");
        final String ref = params.optString("ref");
        final String data = params.optString("data");

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallUpdateStyle(instanceInd, ref, data);
            }
        });
    }

    @ChromeDevtoolsMethod
    public void callRemoveElement(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callRemoveElement: params==null !");
            return;
        }

        final String instanceInd = params.optString("instance");
        final String ref = params.optString("ref");

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallRemoveElement(instanceInd, ref);
            }
        });
    }

    @ChromeDevtoolsMethod
    public void callMoveElement(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callMoveElement: params==null !");
            return;
        }
        final String instanceInd = params.optString("instance");
        final String ref = params.optString("ref");
        final String parentRef = params.optString("parentRef");
        final String index_str = params.optString("index_str");

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallMoveElement(instanceInd, ref, parentRef, index_str);
            }
        });
    }

    @ChromeDevtoolsMethod
    public void callAddEvent(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callAddEvent: params==null !");
            return;
        }
        final String instanceInd = params.optString("instance");
        final String ref = params.optString("ref");
        final String event = params.optString("event");

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallAddEvent(instanceInd, ref, event);
            }
        });
    }

    @ChromeDevtoolsMethod
    public void callRemoveEvent(JsonRpcPeer peer, JSONObject params) {
        if (null == params) {
            Log.e(TAG, "callRemoveEvent: params==null !");
            return;
        }


        final String instanceInd = params.optString("instance");
        final String ref = params.optString("ref");
        final String event = params.optString("event");

        WXBridgeManager.getInstance().post(new Runnable() {
            @Override
            public void run() {
                WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallRemoveEvent(instanceInd, ref, event);
            }
        });
    }

    @ChromeDevtoolsMethod
    public void reload(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> reload=" + params);
        WXSDKEngine.reload();
        Context context = WXEnvironment.getApplication();
        if (context != null) {
            context.sendBroadcast(new Intent()
                    .setAction(WXSDKInstance.ACTION_DEBUG_INSTANCE_REFRESH)
                    .putExtra("params", null == params ? "" : params.toString())
            );
        }
    }

    @ChromeDevtoolsMethod
    public void refresh(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> refresh=" + params);
        Context context = WXEnvironment.getApplication();
        if (context != null) {
            context.sendBroadcast(new Intent()
                    .setAction(WXSDKInstance.ACTION_DEBUG_INSTANCE_REFRESH)
                    .putExtra("params", null == params ? "" : params.toString())
            );
        }
    }

    @ChromeDevtoolsMethod
    public void network(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> network=" + params);
        try {
            boolean enabled = params.getBoolean("enable");
            NetworkEventReporterImpl.setEnabled(enabled);
            WXEnvironment.sDebugNetworkEventReporterEnable = enabled;
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @ChromeDevtoolsMethod
    public SyncCallResponse syncCall(JsonRpcPeer peer, JSONObject params) {
        WXLogUtils.e("WxDebug-new >>>> syncCall=" + params);
        SyncCallResponse response = new SyncCallResponse();
        int syncId = params.optInt("syncId");
        String syncMethod = params.optString("method");
        org.json.JSONArray args = params.optJSONArray("args");

        Object result = null;
        byte[] arguments = null;
        byte[] options = null;
        String instanceId = args.optString(0);
        String domain = args.optString(1);
        String method = args.optString(2);
        org.json.JSONArray jsonArray = args.optJSONArray(3);
        org.json.JSONObject jsonObject = args.optJSONObject(4);
        if (jsonArray != null) {
            arguments = jsonArray.toString().getBytes();
        }
        if (jsonObject != null) {
            options = jsonObject.toString().getBytes();
        }
        if ("callNativeModule".equals(syncMethod)) {
             result = WXDebugBridge.getInstance().callNativeModule(instanceId,
                    domain,
                    method,
                    WXWsonJSONSwitch.convertJSONToWsonIfUseWson(arguments),
                    WXWsonJSONSwitch.convertJSONToWsonIfUseWson(options));
//            WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallNativeModule(
//                    instanceId,
//                    domain,
//                    method,
//                    arguments,
//                    options);
        } else if ("callNativeComponent".equals(syncMethod)) {
            WXDebugBridge.getInstance().callNativeComponent(instanceId,
                    domain,
                    method,
                    WXWsonJSONSwitch.convertJSONToWsonIfUseWson(arguments),
                    WXWsonJSONSwitch.convertJSONToWsonIfUseWson(options));
//            WXDebugBridge.getInstance().getWXDebugJsBridge().jsHandleCallNativeComponent(
//                    instanceId,
//                    domain,
//                    method,
//                    arguments,
//                    options);
        }

        response.method = "WxDebug.syncReturn";
        SyncCallResponseParams param = new SyncCallResponseParams();
        param.syncId = syncId;
        if (result instanceof WXJSObject) {
            param.ret = WXWsonJSONSwitch.fromObjectToJSONString((WXJSObject) result);
        } else {
            param.ret = JSON.toJSON(result);
        }
        response.params = param;
        return response;
    }

    public static class SyncCallResponse implements JsonRpcResult {

        @JsonProperty
        public String method;
        @JsonProperty
        public SyncCallResponseParams params;
    }

    public static class SyncCallResponseParams {

        @JsonProperty
        public Integer syncId;
        @JsonProperty
        public Object ret;
    }

    public static class CallNative {

        @JsonProperty(required = true)
        public String instance;
        @JsonProperty(required = true)
        public String callback;
        @JsonProperty(required = true)
        public List<Task> tasks;
    }

    public static class CallJS {

        @JsonProperty(required = true)
        public String method;
        @JsonProperty(required = true)
        public List<Object> args;
    }

    public static class Task {

        @JsonProperty(required = true)
        public String module;
        @JsonProperty(required = true)
        public String method;
        @JsonProperty(required = true)
        public List<String> args;
    }
}
