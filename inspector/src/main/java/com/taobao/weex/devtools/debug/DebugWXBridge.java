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
import com.taobao.weex.bridge.WXBridge;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.bridge.WXJSObject;
import com.taobao.weex.bridge.WXParams;
import com.taobao.weex.common.IWXBridge;
import com.taobao.weex.common.IWXDebugJsBridge;
import com.taobao.weex.devtools.common.LogUtil;
import com.taobao.weex.devtools.websocket.SimpleSession;
import com.taobao.weex.dom.CSSShorthand;
import com.taobao.weex.layout.ContentBoxMeasurement;
import com.taobao.weex.utils.WXWsonJSONSwitch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by budao on 16/6/25
 * .
 */
public class DebugWXBridge implements IWXBridge {

    private static final String TAG = "DebugWXBridge";
    private static volatile DebugWXBridge sInstance;
    private final Object mLock = new Object();
    private WXBridgeManager mJsManager;
    private volatile SimpleSession mSession;
    private IWXBridge mOriginBridge;
    private IWXDebugJsBridge mWXDebugJsBridge;
    public static final MediaType MEDIA_TYPE_MARKDOWN
            = MediaType.parse("application/json; charset=utf-8");

    private final OkHttpClient client = new OkHttpClient();
    private String syncCallJSURL = "";


    private DebugWXBridge() {
        //TODO params
        mOriginBridge = new WXBridge();
    }

    public static DebugWXBridge getInstance() {
        if (sInstance == null) {
            synchronized (DebugWXBridge.class) {
                if (sInstance == null) {
                    sInstance = new DebugWXBridge();
                }
            }
        }

        return sInstance;
    }

    @Override
    public int initFramework(String framework, WXParams params) {
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

        return sendMessage(getInitFrameworkMessage(framework, params));
    }

    @Override
    public int initFrameworkEnv(String framework, WXParams wxParams, String cacheDir, boolean pieSupport) {
        return initFramework(framework, wxParams);
    }

    @Override
    public void refreshInstance(String instanceId, String namespace, String function, WXJSObject[] args) {
//        mOriginBridge.refreshInstance(instanceId, namespace, function, args);
    }

    @Override
    public int execJS(String instanceId, String namespace, String function, WXJSObject[] args) {
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
        if (TextUtils.equals(function, "registerComponents") || TextUtils.equals(function, "registerModules") || TextUtils.equals(function, "destroyInstance")) {
          func.put(WXDebugConstants.METHOD, function);
        } else if (TextUtils.equals(function, "createInstance")) {
          func.put(WXDebugConstants.METHOD, "createInstance");
        } else {
          func.put(WXDebugConstants.METHOD, WXDebugConstants.WEEX_CALL_JAVASCRIPT);
        }
        func.put(WXDebugConstants.ARGS, array);

        Map<String, Object> map = new HashMap<>();
        map.put(WXDebugConstants.METHOD, WXDebugConstants.METHOD_CALL_JS);
        map.put(WXDebugConstants.PARAMS, func);
        return sendMessage(JSON.toJSONString(map));
    }

    @Override
    public byte[] execJSWithResult(String instanceId, String namespace, String function, WXJSObject[] args) {

        String result = "";

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
        if (TextUtils.equals(function, "registerComponents") || TextUtils.equals(function, "registerModules") || TextUtils.equals(function, "destroyInstance")) {
            func.put(WXDebugConstants.METHOD, function);
        } else {
            func.put(WXDebugConstants.METHOD, WXDebugConstants.WEEX_CALL_JAVASCRIPT);
        }
        func.put(WXDebugConstants.ARGS, array);

        Map<String, Object> map = new HashMap<>();
        map.put(WXDebugConstants.METHOD, WXDebugConstants.METHOD_CALL_JS);
        map.put(WXDebugConstants.PARAMS, func);

        if (TextUtils.isEmpty(syncCallJSURL))
            return new byte[0];

        Request request = new Request.Builder()
                .url(syncCallJSURL)
                .post(RequestBody.create(MEDIA_TYPE_MARKDOWN, JSON.toJSONString(map)))
                .build();

        Response response = null;
        try {
            response = client.newCall(request).execute();
            if (response.isSuccessful()) {
                result = response.body().string();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return WXWsonJSONSwitch.convertJSONToWsonIfUseWson(result.getBytes());
    }

    @Override
    public int execJSService(String javascript) {
        if (!TextUtils.isEmpty(javascript)) {
            Map<String, Object> params = new HashMap<>();
            params.put(WXDebugConstants.PARAM_JS_SOURCE, javascript);

            Map<String, Object> map = new HashMap<>();
            map.put(WXDebugConstants.METHOD, WXDebugConstants.METHOD_IMPORT_JS);
            map.put(WXDebugConstants.PARAMS, params);
            return sendMessage(JSON.toJSONString(map));
        }
        return 0;
    }

    public void onVsync(String instanceId) {

    }

    @Override
    public void takeHeapSnapshot(String filename) {
        LogUtil.log("warning", "Ignore invoke takeSnapshot: " + filename);
    }

    @Override
    public int createInstanceContext(String s, String s1, String s2, WXJSObject[] wxjsObjects) {

        WXJSObject wxjsObjects1[] = new WXJSObject[4];
        WXJSObject wxjsObjects2[] = new WXJSObject[3];

        WXJSObject instanceId = wxjsObjects[0];
        WXJSObject code = wxjsObjects[1];
        WXJSObject bundleUrl = wxjsObjects[2];
        WXJSObject options = wxjsObjects[3];
        WXJSObject raxApi = wxjsObjects[4];

        wxjsObjects1[0] = instanceId;
        wxjsObjects1[1] = bundleUrl;
        wxjsObjects1[2] = options;
        wxjsObjects1[3] = raxApi;
        doCreateInstanceContext(s, s1, "createInstanceContext", wxjsObjects1);

        wxjsObjects2[0] = instanceId;
        wxjsObjects2[1] = code;
        wxjsObjects2[2] = bundleUrl;
        return doImportScript(s, s1, "importScript", wxjsObjects2);
    }

    private int doCreateInstanceContext(String instanceId, String namespace, String function, WXJSObject[] args) {
        ArrayList<Object> array = new ArrayList<>();
        int argsCount = args == null ? 0 : args.length;
        for (int i = 0; i < argsCount; i++) {
            if (args[i].type != WXJSObject.String) {
                array.add(WXWsonJSONSwitch.convertWXJSObjectDataToJSON(args[i]));
            } else {
                array.add(args[i].data);
            }
        }

        Map<String, Object> map = new HashMap<>();
        map.put(WXDebugConstants.METHOD, WXDebugConstants.METHOD_CALL_JS);

        Map<String, Object> func = new HashMap<>();
        func.put(WXDebugConstants.METHOD, function);
        func.put(WXDebugConstants.ARGS, array);

        map.put(WXDebugConstants.PARAMS, func);
        return sendMessage(JSON.toJSONString(map));
    }

    private int doImportScript(String instanceId, String namespace, String function, WXJSObject[] args) {
        ArrayList<Object> array = new ArrayList<>();
        int argsCount = args == null ? 0 : args.length;
        for (int i = 0; i < argsCount; i++) {
            if (args[i].type != WXJSObject.String) {
                array.add(WXWsonJSONSwitch.convertWXJSObjectDataToJSON(args[i]));
            } else {
                array.add(args[i].data);
            }
        }

        Map<String, Object> func = new HashMap<>();
        func.put(WXDebugConstants.METHOD, function);
        func.put(WXDebugConstants.ARGS, array);

        Map<String, Object> map = new HashMap<>();
        map.put(WXDebugConstants.METHOD, WXDebugConstants.METHOD_CALL_JS);
        map.put(WXDebugConstants.PARAMS, func);
        return sendMessage(JSON.toJSONString(map));
    }

    @Override
    public int destoryInstance(String s, String s1, String s2, WXJSObject[] wxjsObjects) {
        return execJS(s, s1, s2, wxjsObjects);
    }

    @Override
    public String execJSOnInstance(String s, String s1, int i) {
        return mOriginBridge.execJSOnInstance(s, s1, i);
    }

    @Override
    public int callNative(String instanceId, byte[] tasks, String callback) {
        return callNative(instanceId, new String(tasks), callback);
    }

    /**
     * js call native
     */
    @Override
    public int callNative(String instanceId, String tasks, String callback) {
        return mOriginBridge.callNative(instanceId, tasks, callback);
    }

    @Override
    public void reportJSException(String instanceId, String func, String exception) {
        mOriginBridge.reportJSException(instanceId, func, exception);
    }

    @Override
    public Object callNativeModule(String instanceId, String module, String method, byte[] arguments, byte[] options) {
        return mOriginBridge.callNativeModule(instanceId, module, method, arguments, options);
    }

    @Override
    public void callNativeComponent(String instanceId, String componentRef, String method, byte[] arguments, byte[] options) {
        mOriginBridge.callNativeComponent(instanceId, componentRef, method, arguments, options);
    }

    @Override
    public int callUpdateFinish(String instanceId, byte[] tasks, String callback) {
        return mOriginBridge.callUpdateFinish(instanceId, tasks, callback);
    }

    @Override
    public int callRefreshFinish(String instanceId, byte[] tasks, String callback) {
        return mOriginBridge.callRefreshFinish(instanceId, tasks, callback);
    }

    @Override
    public int callAddEvent(String instanceId, String ref, String event) {
        return mOriginBridge.callAddEvent(instanceId, ref, event);
    }

    @Override
    public int callRemoveEvent(String instanceId, String ref, String event) {
        return mOriginBridge.callRemoveEvent(instanceId, ref, event);
    }

    @Override
    public int callUpdateStyle(String instanceId, String ref, HashMap<String, Object> styles, HashMap<String, String> paddings, HashMap<String, String> margins, HashMap<String, String> borders) {
        return mOriginBridge.callUpdateStyle(instanceId, ref, styles, paddings, margins, borders);
    }

    @Override
    public int callUpdateAttrs(String instanceId, String ref, HashMap<String, String> attrs) {
        return mOriginBridge.callUpdateAttrs(instanceId, ref, attrs);
    }

    @Override
    public int callLayout(String s, String s1, int i, int i1, int i2, int i3, int i4, int i5, int i6) {
        return mOriginBridge.callLayout(s, s1, i, i1, i2, i3, i4, i5, i6);
    }


    @Override
    public int callCreateFinish(String instanceId) {
        return mOriginBridge.callCreateFinish(instanceId);
    }

    @Override
    public int callRenderSuccess(String s) {
        return -1;
    }

    @Override
    public int callAppendTreeCreateFinish(String instanceId, String ref) {
        return mOriginBridge.callAppendTreeCreateFinish(instanceId, ref);
    }

    @Override
    public void reportServerCrash(String instanceId, String crashFile) {
        LogUtil.e("ServerCrash: instanceId: " + instanceId + ", crashFile: " + crashFile);
    }

    @Override
    public int callCreateBody(String pageId, String componentType, String ref, HashMap<String, String> styles, HashMap<String, String> attributes, HashSet<String> events, float[] margins, float[] paddings, float[] borders) {
        return mOriginBridge.callCreateBody(pageId, componentType, ref, styles, attributes, events, margins, paddings, borders);
    }

    @Override
    public int callAddElement(String instanceId, String componentType, String ref, int index, String parentRef, HashMap<String, String> styles, HashMap<String, String> attributes, HashSet<String> events, float[] margins, float[] paddings, float[] borders, boolean willLayout) {
        return  mOriginBridge.callAddElement(instanceId, componentType, ref, index, parentRef, styles, attributes, events, margins, paddings, borders, willLayout);
    }


    @Override
    public int callRemoveElement(String instanceId, String ref) {
        return mOriginBridge.callRemoveElement(instanceId, ref);
    }

    @Override
    public int callMoveElement(String instanceId, String ref, String parentRef, int index) {
        return mOriginBridge.callMoveElement(instanceId, ref, parentRef, index);
    }


    @Override
    public int callHasTransitionPros(String instanceId, String ref, HashMap<String, String> styles) {
        return mOriginBridge.callHasTransitionPros(instanceId, ref, styles);
    }

    @Override
    public ContentBoxMeasurement getMeasurementFunc(String instanceId, long renderObjectPtr) {
        return mOriginBridge.getMeasurementFunc(instanceId, renderObjectPtr);
    }

    @Override
    public void bindMeasurementToRenderObject(long ptr) {
        mOriginBridge.bindMeasurementToRenderObject(ptr);
    }

    @Override
    public void setRenderContainerWrapContent(boolean b, String s) {
        mOriginBridge.setRenderContainerWrapContent(b, s);
    }

    @Override
    public long[] getFirstScreenRenderTime(String instanceId) {
        return mOriginBridge.getFirstScreenRenderTime(instanceId);
    }

    @Override
    public long[] getRenderFinishTime(String instanceId) {
        return mOriginBridge.getRenderFinishTime(instanceId);
    }

    @Override
    public void setDefaultHeightAndWidthIntoRootDom(String s, float v, float v1, boolean b, boolean b1) {
        mOriginBridge.setDefaultHeightAndWidthIntoRootDom(s, v, v1, b, b1);
    }

    @Override
    public void onInstanceClose(String s) {
        mOriginBridge.onInstanceClose(s);
    }

    @Override
    public void forceLayout(String s) {
        mOriginBridge.forceLayout(s);
    }

    @Override
    public boolean notifyLayout(String s) {
        return mOriginBridge.notifyLayout(s);
    }

    @Override
    public void setStyleWidth(String instanceId, String ref, float value) {
        mOriginBridge.setStyleWidth(instanceId, ref, value);
    }

    @Override
    public void setStyleHeight(String instanceId, String ref, float value) {
        mOriginBridge.setStyleHeight(instanceId, ref, value);
    }

    @Override
    public void setMargin(String instanceId, String ref, CSSShorthand.EDGE edge, float value) {
        mOriginBridge.setMargin(instanceId, ref, edge, value);
    }

    @Override
    public void setPadding(String instanceId, String ref, CSSShorthand.EDGE edge, float value) {
        mOriginBridge.setPadding(instanceId, ref, edge, value);
    }

    @Override
    public void setPosition(String instanceId, String ref, CSSShorthand.EDGE edge, float value) {
        mOriginBridge.setPosition(instanceId, ref, edge, value);
    }

    @Override
    public void markDirty(String instanceId, String ref, boolean dirty) {
        mOriginBridge.markDirty(instanceId, ref, dirty);
    }

    @Override
    public void registerCoreEnv(String key, String value) {
        mOriginBridge.registerCoreEnv(key, value);
    }

    @Override
    public void reportNativeInitStatus(String statusCode, String errorMsg) {
        mOriginBridge.reportNativeInitStatus(statusCode, errorMsg);
    }

    @Override
    public void setTimeoutNative(String callbackId, String time) {
        mOriginBridge.setTimeoutNative(callbackId, time);
    }

    @Override
    public void setJSFrmVersion(String version) {
        mOriginBridge.setJSFrmVersion(version);
    }

    @Override
    public void resetWXBridge(boolean remoteDebug) {
        final String className = this.getClass().getName().replace('.', '/');
        mWXDebugJsBridge.resetWXBridge(this, className);
    }

    public void setSession(SimpleSession session) {
        mSession = session;
        if (mSession instanceof SocketClient) {
            String[] temp = ((SocketClient)mSession).getUrl().split("debugProxy/native");
            if (temp.length < 2)
                return;
            syncCallJSURL = temp[0] + "syncCallJS" + temp[1];
            syncCallJSURL = "http://" + syncCallJSURL.split("://")[1];
        }
    }

    public void setBridgeManager(WXBridgeManager bridgeManager) {
        mJsManager = bridgeManager;
    }

    public void sendToRemote(String message) {
        if (mSession != null && mSession.isOpen()) {
            mSession.sendText(message);
        }
    }

    public void post(Runnable runnable) {
        if (mSession != null && mSession.isOpen()) {
            mSession.post(runnable);
        }
    }

    public boolean isSessionActive() {
        return mSession != null && mSession.isOpen();
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

    public void setWXDebugJsBridge(IWXDebugJsBridge wxDebugJsBridge) {
        this.mWXDebugJsBridge = wxDebugJsBridge;
    }

    public IWXDebugJsBridge getWXDebugJsBridge() {
        return mWXDebugJsBridge;
    }
}