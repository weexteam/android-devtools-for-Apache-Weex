package com.taobao.weex.devtools.debug;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.squareup.okhttp.MediaType;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.RequestBody;
import com.squareup.okhttp.Response;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.bridge.WXJSObject;
import com.taobao.weex.bridge.WXParams;
import com.taobao.weex.common.IWXBridge;
import com.taobao.weex.devtools.websocket.SimpleSession;
import com.taobao.weex.utils.WXWsonJSONSwitch;
import com.taobao.windmill.bridge.IWMLBridge;
import com.taobao.windmill.bridge.WMLBridge;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WMLDebugBridge implements IWMLBridge {

  private static final String TAG = "WMLDebugBridge";
  private static volatile WMLDebugBridge sInstance;
  private final Object mLock = new Object();
  private volatile SimpleSession mSession;
  private IWMLBridge mOriginBridge;
  public static final MediaType MEDIA_TYPE_MARKDOWN
          = MediaType.parse("application/json; charset=utf-8");
  private final OkHttpClient client = new OkHttpClient();


  private WMLDebugBridge() {
    mOriginBridge = new WMLBridge();
  }

  public static WMLDebugBridge getInstance() {
    if (sInstance == null) {
      synchronized (WXDebugBridge.class) {
        if (sInstance == null) {
          sInstance = new WMLDebugBridge();
        }
      }
    }
    return sInstance;
  }

  @Override
  public int initAppFramework(String appId, String framework, String frameworkName, WXJSObject[] args) {
    while (mSession == null || (mSession != null && !mSession.isOpen())) {
      synchronized (mLock) {
        try {
          Log.v(TAG, "waiting for session now");
          mLock.wait(1000);
        } catch (InterruptedException e) {
          e.printStackTrace();
        }
      }
    }

    ArrayList<Object> env = new ArrayList<>();
    int argsCount = args == null ? 0 : args.length;
    for (int i = 0; i < argsCount; i++) {
      if (args[i] != null) {
        if (args[i].type != WXJSObject.String) {
          env.add(WXWsonJSONSwitch.convertWXJSObjectDataToJSON(args[i]));
        } else {
          env.add(args[i].data);
        }
      }
    }

    Map<String, Object> params = new HashMap<>();
    params.put("appId", appId);
    params.put("source", framework);
    params.put("bundleUrl", "windmill.worker.js");
    params.put("env", env);

    Map<String, Object> map = new HashMap<>();
    map.put("method", "WMLDebug.initRuntimeWoker");
    map.put("params", params);
    return sendMessage(JSON.toJSONString(map));
  }

  @Override
  public int execJsOnApp(String appId, String function, WXJSObject[] args) {

    ArrayList<Object> array = new ArrayList<>();
    int argsCount = args == null ? 0 : args.length;
    for (int i = 0; i < argsCount; i++) {
      if (args[i] != null) {
        if (args[i].type != WXJSObject.String) {
          array.add(WXWsonJSONSwitch.convertWXJSObjectDataToJSON(args[i]));
        } else {
          array.add(args[i].data);
        }
      }
    }

    Map<String, Object> func = new HashMap<>();
    func.put("method", function);
    func.put("args", array);

    Map<String, Object> map = new HashMap<>();
    map.put("method", "WMLDebug.callJS");
    map.put("params", func);
    return sendMessage(JSON.toJSONString(map));
  }

  @Override
  public byte[] execJsOnAppWithResult(String appId, String function, Map<String, Object> params) {

//    String result = "";
//
//    ArrayList<Object> array = new ArrayList<>();
//    int argsCount = args == null ? 0 : args.length;
//    for (int i = 0; i < argsCount; i++) {
//      if (args[i] != null) {
//        if (args[i].type != WXJSObject.String) {
//          array.add(WXWsonJSONSwitch.convertWXJSObjectDataToJSON(args[i]));
//        } else {
//          array.add(args[i].data);
//        }
//      }
//    }
//
//    Map<String, Object> func = new HashMap<>();
//    if (TextUtils.equals(function, "registerComponents") || TextUtils.equals(function, "registerModules") || TextUtils.equals(function, "destroyInstance")) {
//      func.put(WXDebugConstants.METHOD, function);
//    } else {
//      func.put(WXDebugConstants.METHOD, WXDebugConstants.WEEX_CALL_JAVASCRIPT);
//    }
//    func.put(WXDebugConstants.ARGS, array);
//
//    Map<String, Object> map = new HashMap<>();
//    map.put(WXDebugConstants.METHOD, WXDebugConstants.METHOD_CALL_JS);
//    map.put(WXDebugConstants.PARAMS, func);
//
//    if (TextUtils.isEmpty(syncCallJSURL))
//      return new byte[0];
//
//    Request request = new Request.Builder()
//            .url(syncCallJSURL)
//            .post(RequestBody.create(MEDIA_TYPE_MARKDOWN, JSON.toJSONString(map)))
//            .build();
//
//    Response response = null;
//    try {
//      response = client.newCall(request).execute();
//      if (response.isSuccessful()) {
//        result = response.body().string();
//      }
//    } catch (IOException e) {
//      e.printStackTrace();
//    }
//
//    return WXWsonJSONSwitch.convertJSONToWsonIfUseWson(result.getBytes());

    return mOriginBridge.execJsOnAppWithResult(appId, function, params);
  }

  @Override
  public int createAppContext(String appId, String template, Map<String, Object> params) {

//    WXJSObject wxjsObjects1[] = new WXJSObject[4];
//    WXJSObject wxjsObjects2[] = new WXJSObject[3];
//
//    WXJSObject instanceId = wxjsObjects[0];
//    WXJSObject code = wxjsObjects[1];
//    WXJSObject bundleUrl = wxjsObjects[2];
//    WXJSObject options = wxjsObjects[3];
//    WXJSObject raxApi = wxjsObjects[4];
//
//    wxjsObjects1[0] = instanceId;
//    wxjsObjects1[1] = bundleUrl;
//    wxjsObjects1[2] = options;
//    wxjsObjects1[3] = raxApi;
//    doCreateInstanceContext(s, s1, "createInstanceContext", wxjsObjects1);
//
//    wxjsObjects2[0] = instanceId;
//    wxjsObjects2[1] = code;
//    wxjsObjects2[2] = bundleUrl;
//    return doImportScript(s, s1, "importScript", wxjsObjects2);

    return mOriginBridge.createAppContext(appId, template, params);
  }

  @Override
  public int destoryAppContext(String appId) {
    return mOriginBridge.destoryAppContext(appId);
  }

  @Override
  public void postMessage(String appId, byte[] data) {
    mOriginBridge.postMessage(appId, data);
  }

  @Override
  public void dispatchMessage(String clientId, String appId, byte[] params, String callbackId) {
    mOriginBridge.dispatchMessage(clientId, appId, params, callbackId);
  }

  public void setSession(SimpleSession session) {
    mSession = session;
  }

  public void onConnected() {
    Log.v(TAG, "connect to debug server success");
    synchronized (mLock) {
      mLock.notify();
    }
  }

  public void onDisConnected() {
    Log.w(TAG, "WebSocket disconnected");
    synchronized (mLock) {
      mSession = null;
      mLock.notify();
    }
  }

  private int sendMessage(String message) {
    if (mSession != null && mSession.isOpen()) {
      mSession.sendText(message);
      return 1;
    } else {
      // session error, we need stop debug mode and switch to local runtime
      WXBridgeManager.getInstance().stopRemoteDebug();
      return 0;
    }
  }
}
