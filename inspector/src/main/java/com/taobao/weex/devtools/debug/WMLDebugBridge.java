package com.taobao.weex.devtools.debug;

import android.text.TextUtils;
import android.util.Log;
import com.alibaba.fastjson.JSON;
import com.squareup.okhttp.OkHttpClient;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.bridge.WXJSObject;
import com.taobao.weex.devtools.websocket.SimpleSession;
import com.taobao.weex.utils.WXWsonJSONSwitch;
import com.taobao.windmill.bridge.IWMLBridge;
import com.taobao.windmill.bridge.WMLBridge;
import com.taobao.windmill.bundle.container.common.WMLConstants;
import com.taobao.windmill.bundle.container.utils.WMLLogUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class WMLDebugBridge implements IWMLBridge {

  private static final String TAG = "WMLDebugBridge";
  private static volatile WMLDebugBridge sInstance;
  private final Object mLock = new Object();
  private volatile SimpleSession mSession;
  private IWMLBridge mOriginBridge;
  private final OkHttpClient mHttpclient = new OkHttpClient();
  private static WMLLogUtils.WMLDevtoolLogWatcher logWatcher;
  private static ArrayList<String> aliveApp = new ArrayList<>();

  private WMLDebugBridge() {
    mOriginBridge = new WMLBridge();
    WMLLogUtils.setWMLDevtoolLogWatcher(new WMLLogUtils.WMLDevtoolLogWatcher() {
      @Override
      public void onLog(int logLevel, String tag, String log) {

        ArrayList args = new ArrayList();
        HashMap<String, Object> args_map = new HashMap();
        args_map.put("type", "string");
        args_map.put("value", log);
        args.add(args_map);

        Map<String, Object> params = new HashMap<>();
        switch (logLevel) {
          case Log.VERBOSE:
            params.put("type", "verbose");
            break;
          case Log.DEBUG:
            params.put("type", "debug");
            break;
          case Log.INFO:
            params.put("type", "info");
            break;
          case Log.WARN:
            params.put("type", "warn");
            break;
          case Log.ERROR:
            params.put("type", "error");
            break;
          case Log.ASSERT:
            params.put("type", "assert");
            break;
        }
        params.put("args", args);
        params.put("timestamp", System.currentTimeMillis());

        Map<String, Object> map = new HashMap<>();
        map.put("method", "WMLDebug.runtimeConsoleAPICalled");
        map.put("params", params);
        sendMessage(JSON.toJSONString(map));
      }
    });
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

    Map<String, Object> env = new HashMap<>();
    int argsCount = args == null ? 0 : args.length;
    for (int i = 0; i < argsCount; i++) {
      if (args[i] != null) {
          env.put(args[i].key, args[i].data);
      }
    }

    Map<String, Object> params = new HashMap<>();
    params.put("appId", appId);
    params.put("source", framework);
    params.put("bundleUrl", frameworkName);
    params.put("env", env);

    Map<String, Object> map = new HashMap<>();
    if (TextUtils.equals(frameworkName, WMLConstants.NAME_WINDMILL_WORKER_JS)) {
      map.put("method", "WMLDebug.initRuntimeWorker");
    } else {
      map.put("method", "WMLDebug.initAppFrameworkWorker");
    }
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
    func.put("appId", appId);
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

    aliveApp.add(appId);
    Map<String, Object> debugParams = new HashMap<>();
    debugParams.put("appId", appId);
    debugParams.put("source", template);
    debugParams.put("bundleUrl", "app.js");

    Map<String, Object> map = new HashMap<>();
    map.put("method", "WMLDebug.importAppJS");
    map.put("params", debugParams);
    return sendMessage(JSON.toJSONString(map));
  }

  @Override
  public int destoryAppContext(String appId) {

    aliveApp.remove(appId);
    Map<String, Object> params = new HashMap<>();
    params.put("appId", appId);

    Map<String, Object> map = new HashMap<>();
    map.put("method", "WMLDebug.destoryAppContext");
    map.put("params", params);
    return sendMessage(JSON.toJSONString(map));
  }

  public void destoryAllDebugAppContext() {
    for (String appId : aliveApp) {
      Map<String, Object> params = new HashMap<>();
      params.put("appId", appId);

      Map<String, Object> map = new HashMap<>();
      map.put("method", "WMLDebug.destoryAppContext");
      map.put("params", params);
      sendMessage(JSON.toJSONString(map));
    }

    aliveApp.clear();
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
