package com.taobao.weex.devtools.inspector.protocol.module;

import android.content.Context;
import android.content.Intent;

import com.alibaba.fastjson.JSON;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.common.IWXDebugProxy;
import com.taobao.weex.devtools.debug.DebugBridge;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcPeer;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcResult;
import com.taobao.weex.devtools.inspector.network.NetworkEventReporterImpl;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsDomain;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsMethod;
import com.taobao.weex.devtools.json.ObjectMapper;
import com.taobao.weex.devtools.json.annotation.JsonProperty;
import com.taobao.weex.utils.LogLevel;

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
    Context context = WXEnvironment.getApplication();
    if (context != null) {
      WXSDKEngine.reload(context, true);
      context.sendBroadcast(new Intent(IWXDebugProxy.ACTION_DEBUG_INSTANCE_REFRESH));
    }
  }

  @ChromeDevtoolsMethod
  public void disable(JsonRpcPeer peer, JSONObject params) {
    Context context = WXEnvironment.getApplication();
    if (context != null) {
      WXSDKEngine.reload(context, false);
      context.sendBroadcast(new Intent(IWXDebugProxy.ACTION_DEBUG_INSTANCE_REFRESH));
    }
  }

  @ChromeDevtoolsMethod
  public void setLogLevel(JsonRpcPeer peer, JSONObject params) {
    if (params != null) {
      LogLevel logLevel = sLevelMap.get(params.optString("logLevel"));
      if (logLevel != null) {
        WXEnvironment.sLogLevel = logLevel;
      }
    }
  }

  @ChromeDevtoolsMethod
  public void setElementMode(JsonRpcPeer peer, JSONObject params) {
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
    if (params != null) {
      DebugBridge.getInstance().callNative(
          params.optString("instance"),
          params.optString("tasks"),
          params.optString("callback"));
    }
    // another way to handle call native
//        CallNative callNative = mObjectMapper.convertValue(params, CallNative.class);
//        if (callNative != null) {
//            try {
//                String tasks = mObjectMapper.convertListToJsonArray(callNative.tasks).toString();
//                Log.v(TAG, "WxDebug.callNative tasks " + tasks);
//                DebugBridge.getInstance().callNative(callNative.instance,
//                        tasks,
//                        callNative.callback);
//            } catch (InvocationTargetException e) {
//                e.printStackTrace();
//            } catch (IllegalAccessException e) {
//                e.printStackTrace();
//            }
//        }
  }

  @ChromeDevtoolsMethod
  public void callAddElement(JsonRpcPeer peer, JSONObject params) {
    if (params != null) {
      DebugBridge.getInstance().callAddElement(
          params.optString("instance"),
          params.optString("ref"),
          params.optString("dom"),
          params.optString("index"),
          params.optString("callback"));
    }
  }

  @ChromeDevtoolsMethod
  public void reload(JsonRpcPeer peer, JSONObject params) {
    WXSDKEngine.reload();
    Context context = WXEnvironment.getApplication();
    if (context != null) {
      context.sendBroadcast(new Intent(IWXDebugProxy.ACTION_DEBUG_INSTANCE_REFRESH));
    }
  }

  @ChromeDevtoolsMethod
  public void refresh(JsonRpcPeer peer, JSONObject params) {
    Context context = WXEnvironment.getApplication();
    if (context != null) {
      context.sendBroadcast(new Intent(IWXDebugProxy.ACTION_DEBUG_INSTANCE_REFRESH));
    }
  }

  @ChromeDevtoolsMethod
  public void network(JsonRpcPeer peer, JSONObject params) {
    try {
      boolean enabled = params.getBoolean("enable");
      NetworkEventReporterImpl.setEnabled(enabled);
    } catch (JSONException e) {
      e.printStackTrace();
    }
  }

  @ChromeDevtoolsMethod
  public SyncCallResponse syncCall(JsonRpcPeer peer, JSONObject params) {
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
      result = DebugBridge.getInstance().callNativeModule(
          instanceId,
          domain,
          method,
          arguments,
          options);
    } else if ("callNativeComponent".equals(syncMethod)) {
      DebugBridge.getInstance().callNativeComponent(
          instanceId,
          domain,
          method,
          arguments,
          options);
    }

    response.method = "WxDebug.syncReturn";
    SyncCallResponseParams param = new SyncCallResponseParams();
    param.syncId = syncId;
    param.ret = JSON.toJSON(result);
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
