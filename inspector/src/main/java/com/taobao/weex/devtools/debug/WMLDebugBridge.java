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
  public int initAppFramework(String s, String s1, WXJSObject[] wxjsObjects) {
//    while (mSession == null || (mSession != null && !mSession.isOpen())) {
//      synchronized (mLock) {
//        try {
//          Log.v(TAG, "waiting for session now");
//          mLock.wait(1000);
//        } catch (InterruptedException e) {
//          e.printStackTrace();
//        }
//      }
//    }
//
//    return sendMessage(getInitFrameworkMessage(framework, params));
    return mOriginBridge.initAppFramework(s, s1, wxjsObjects);
  }

  @Override
  public int execJsOnApp(String s, String s1, WXJSObject[] wxjsObjects) {

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
//    } else if (TextUtils.equals(function, "createInstance")) {
//      func.put(WXDebugConstants.METHOD, "createInstance");
//    } else {
//      func.put(WXDebugConstants.METHOD, WXDebugConstants.WEEX_CALL_JAVASCRIPT);
//    }
//    func.put(WXDebugConstants.ARGS, array);
//
//    Map<String, Object> map = new HashMap<>();
//    map.put(WXDebugConstants.METHOD, WXDebugConstants.METHOD_CALL_JS);
//    map.put(WXDebugConstants.PARAMS, func);
//    return sendMessage(JSON.toJSONString(map));
//
    return mOriginBridge.execJsOnApp(s, s1, wxjsObjects);
  }

  @Override
  public byte[] execJsOnAppWithResult(String s, String s1, Map<String, Object> map) {

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

    return mOriginBridge.execJsOnAppWithResult(s, s1, map);
  }

  @Override
  public int createAppContext(String s, String s1, Map<String, Object> map) {

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

    return mOriginBridge.createAppContext(s, s1, map);
  }

  @Override
  public int destoryAppContext(String s) {
    return mOriginBridge.destoryAppContext(s);
  }

  @Override
  public void postMessage(String s, byte[] bytes) {
    mOriginBridge.postMessage(s, bytes);
  }

  @Override
  public void dispatchMessage(String s, String s1, byte[] bytes, String s2) {
    mOriginBridge.dispatchMessage(s, s1, bytes, s2);
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

  private String getInitFrameworkMessage(String framework, WXParams params) {
    Map<String, Object> func = new HashMap<>();
    func.put(WXDebugConstants.PARAM_JS_SOURCE, framework);
    func.put(WXDebugConstants.PARAM_LAYOUT_SANDBOX, "true");
    if (params != null) {
      Map<String, Object> environmentMap = getEnvironmentMap(params);
      if (environmentMap != null && environmentMap.size() > 0) {
        Map<String, Object> wxEnvironment = new HashMap<>();
        wxEnvironment.put(WXDebugConstants.ENV_WX_ENVIRONMENT, environmentMap);
        func.put(WXDebugConstants.PARAM_INIT_ENV, wxEnvironment);
      }
    }

    Map<String, Object> map = new HashMap<>();
    map.put(WXDebugConstants.METHOD, WXDebugConstants.METHOD_INIT_RUNTIME);
    map.put(WXDebugConstants.PARAMS, func);

    return JSON.toJSONString(map);
  }

  private Map<String, Object> getEnvironmentMap(WXParams params) {
    Map<String, Object> environment = new HashMap<>();
    environment.put(WXDebugConstants.ENV_APP_NAME, params.getAppName());
    environment.put(WXDebugConstants.ENV_APP_VERSION, params.getAppVersion());
    environment.put(WXDebugConstants.ENV_PLATFORM, params.getPlatform());
    environment.put(WXDebugConstants.ENV_OS_VERSION, params.getOsVersion());
    environment.put(WXDebugConstants.ENV_LOG_LEVEL, params.getLogLevel());
    environment.put(WXDebugConstants.ENV_WEEX_VERSION, params.getWeexVersion());
    environment.put(WXDebugConstants.ENV_DEVICE_MODEL, params.getDeviceModel());
    environment.put(WXDebugConstants.ENV_INFO_COLLECT, params.getShouldInfoCollect());
    environment.put(WXDebugConstants.ENV_DEVICE_WIDTH, params.getDeviceWidth());
    environment.put(WXDebugConstants.ENV_DEVICE_HEIGHT, params.getDeviceHeight());
    environment.put("runtime", "devtools");

    environment.putAll(WXEnvironment.getCustomOptions());

    return environment;
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
