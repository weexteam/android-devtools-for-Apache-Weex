package com.taobao.weex.devtools.debug;

import android.text.TextUtils;
import android.util.Log;

import com.alibaba.fastjson.JSON;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.bridge.WXBridge;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.bridge.WXJSObject;
import com.taobao.weex.bridge.WXParams;
import com.taobao.weex.common.IWXBridge;
import com.taobao.weex.common.IWXJsFunctions;
import com.taobao.weex.devtools.common.LogUtil;
import com.taobao.weex.devtools.websocket.SimpleSession;
import com.taobao.weex.dom.CSSShorthand;
import com.taobao.weex.layout.ContentBoxMeasurement;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Created by budao on 16/6/25
 * .
 */
public class DebugBridge implements IWXBridge {

    private static final String TAG = "DebugBridge";
    private static volatile DebugBridge sInstance;
    private final Object mLock = new Object();
    private WXBridgeManager mJsManager;
    private volatile SimpleSession mSession;
    private IWXBridge mOriginBridge;
    private IWXJsFunctions jsFunctions;

    private DebugBridge() {
        //TODO params
        mOriginBridge = new WXBridge();
    }

    public static DebugBridge getInstance() {
        Log.e("listen", "DebugBridge instance");
        if (sInstance == null) {
            synchronized (DebugBridge.class) {
                if (sInstance == null) {
                    sInstance = new DebugBridge();
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

        final String className = this.getClass().getName().replace('.', '/');
        jsFunctions.initWxBridge(this, className);
        return sendMessage(getInitFrameworkMessage(framework, params));
    }

    @Override
    public int initFrameworkEnv(String framework, WXParams wxParams, String cacheDir, boolean pieSupport) {
        return initFramework(framework, wxParams);
    }

    @Override
    public int execJS(String instanceId, String namespace, String function, WXJSObject[] args) {
        ArrayList<Object> array = new ArrayList<>();
        int argsCount = args == null ? 0 : args.length;
        for (int i = 0; i < argsCount; i++) {
            if (args[i].type != WXJSObject.String) {
                array.add(JSON.parse(args[i].data.toString()));
            } else {
                array.add(args[i].data);
            }
        }

        Map<String, Object> func = new HashMap<>();
        func.put(WXDebugConstants.METHOD, function);
        func.put(WXDebugConstants.ARGS, array);

        // Log.v(TAG, "callJS: function is " + function + ", args " + array);
        Map<String, Object> map = new HashMap<>();
        map.put(WXDebugConstants.METHOD, WXDebugConstants.METHOD_CALL_JS);
        map.put(WXDebugConstants.PARAMS, func);
        return sendMessage(JSON.toJSONString(map));
    }

    @Override
    public byte[] execJSWithResult(String instanceId, String namespace, String function, WXJSObject[] args) {
        return new byte[0];
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
        return execJS(s, s1, s2, wxjsObjects);
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
    public int callAddElement(String pageId, String componentType, String ref, int index, String parentRef, HashMap<String, String> styles, HashMap<String, String> attributes, HashSet<String> events, float[] margins, float[] paddings, float[] borders) {
        return mOriginBridge.callAddElement(pageId, componentType, ref, index, parentRef, styles, attributes, events, margins, paddings, borders);
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
//    if (null != mJsManager) {
//      return mJsManager.callHasTransitionPros(instanceId, ref, styles);
//    }
//    return 0;

        return mOriginBridge.callHasTransitionPros(instanceId, ref, styles);
    }

    @Override
    public void bindMeasurementToWXCore(String instanceId, String ref, ContentBoxMeasurement contentBoxMeasurement) {
        mOriginBridge.bindMeasurementToWXCore(instanceId, ref, contentBoxMeasurement);
    }

//    @Override
//    public void bindMeasurementToWXCore(String instanceId, String ref) {
//        mOriginBridge.bindMeasurementToWXCore(instanceId, ref);
//    }
//
//    @Override
//    public ContentBoxMeasurement getMeasurementFunc(String instanceId, String ref) {
//        return mOriginBridge.getMeasurementFunc(instanceId, ref);
//    }

//    @Override
//    public void bindMeasurementToWXCore(String s, String s1, ContentBoxMeasurement contentBoxMeasurement) {
//
//        mOriginBridge.bindMeasurementToWXCore(s, s1, contentBoxMeasurement);
//    }

    @Override
    public void setRenderContainerWrapContent(boolean b, String s) {
        mOriginBridge.setRenderContainerWrapContent(b, s);
    }

    @Override
    public int printFirstScreenRenderTime(String s) {
        return mOriginBridge.printFirstScreenRenderTime(s);
    }

    @Override
    public int printRenderFinishTime(String s) {
        return 0;
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
//    if (null != mJsManager) {
//      mJsManager.setStyleWidth(instanceId, ref, value);
//    }
        mOriginBridge.setStyleWidth(instanceId, ref, value);
    }

    @Override
    public void setStyleHeight(String instanceId, String ref, float value) {
//    if (null != mJsManager) {
//      mJsManager.setStyleHeight(instanceId, ref, value);
//    }

        mOriginBridge.setStyleHeight(instanceId, ref, value);
    }

    @Override
    public void setMargin(String instanceId, String ref, CSSShorthand.EDGE edge, float value) {
//    if (null != mJsManager) {
//      mJsManager.setMargin(instanceId, ref, edge, value);
//    }
        mOriginBridge.setMargin(instanceId, ref, edge, value);
    }

    @Override
    public void setPadding(String instanceId, String ref, CSSShorthand.EDGE edge, float value) {
//    if (null != mJsManager) {
//      mJsManager.setPadding(instanceId, ref, edge, value);
//    }

        mOriginBridge.setPadding(instanceId, ref, edge, value);
    }

    @Override
    public void setPosition(String instanceId, String ref, CSSShorthand.EDGE edge, float value) {
//    if (null != mJsManager) {
//      mJsManager.setPosition(instanceId, ref, edge, value);
//    }
        mOriginBridge.setPosition(instanceId, ref, edge, value);
    }

    @Override
    public void markDirty(String instanceId, String ref, boolean dirty) {

    }

    @Override
    public void registerCoreEnv(String key, String value) {

    }

    @Override
    public void setViewPortWidth(String instanceId, float value) {

    }

    public void calculateLayout(String instanceId, String ref) {
        if (null != mJsManager) {
//      mJsManager.calculateLayout(instanceId, ref);
        }
    }


    public void setSession(SimpleSession session) {
        mSession = session;
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

    public void setJsFunctions(IWXJsFunctions jsFunctions) {
        this.jsFunctions = jsFunctions;
    }

    public IWXJsFunctions getJsFunctions() {
        return jsFunctions;
    }
}