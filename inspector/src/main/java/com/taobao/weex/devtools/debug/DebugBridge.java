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
        Log.e("dyy", "Class Name is " + className);
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
    public int callNative(String instanceId, byte[] tasks, String callback) {
        return callNative(instanceId, new String(tasks), callback);
    }

    /**
     * js call native
     */
    @Override
    public int callNative(String instanceId, String tasks, String callback) {
//    if (mJsManager != null) {
//      return mJsManager.callNative(instanceId, tasks, callback);
//    } else {
//      return 0;
//    }
        Log.e("dyy", "callNative is running");
        return mOriginBridge.callNative(instanceId, tasks, callback);
    }

    @Override
    public void reportJSException(String instanceId, String func, String exception) {
//    if (mJsManager != null) {
//      mJsManager.reportJSException(instanceId, func, exception);
//    }
        mOriginBridge.reportJSException(instanceId, func, exception);
    }

    @Override
    public Object callNativeModule(String instanceId, String module, String method, byte[] arguments, byte[] options) {
//    if (mJsManager != null) {
//      JSONArray argArray = JSON.parseArray(new String(arguments));
//      return mJsManager.callNativeModule(instanceId, module, method, argArray, options);
//    }
//    return null;

        Log.d("dyy", "callNativeModule is running");

        return mOriginBridge.callNativeModule(instanceId, module, method, arguments, options);
    }

    @Override
    public void callNativeComponent(String instanceId, String componentRef, String method, byte[] arguments, byte[] options) {
//    JSONArray argArray = JSON.parseArray(new String(arguments));
//    WXBridgeManager.getInstance().callNativeComponent(instanceId, componentRef, method, argArray, options);
        mOriginBridge.callNativeComponent(instanceId, componentRef, method, arguments, options);
    }

    @Override
    public int callUpdateFinish(String instanceId, byte[] tasks, String callback) {
//    if (mJsManager != null) {
//      //?
//      return mJsManager.callUpdateFinish(instanceId, callback);
//    }
//    return 0;
        return mOriginBridge.callUpdateFinish(instanceId, tasks, callback);
    }

    @Override
    public int callRefreshFinish(String instanceId, byte[] tasks, String callback) {
//    if (mJsManager != null) {
//      return mJsManager.callRefreshFinish(instanceId, callback);
//    }
//    return 0;
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
    public int callLayout(String pageId, String ref, int top, int bottom, int left, int right, int height, int width) {
        return mOriginBridge.callLayout(pageId, ref, top, bottom, left, right, height, width);
    }

    @Override
    public int callCreateFinish(String instanceId) {
        return mOriginBridge.callCreateFinish(instanceId);
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

    public int callCreateBodyByWeexCore(String pageId, String componentType, String ref,
                                        HashMap<String, String> styles, HashMap<String, String> attributes, HashSet<String> events,
                                        HashMap<String, String> paddings, HashMap<String, String> margins,
                                        HashMap<String, String> borders) {
//    if (null != mJsManager) {
//      return mJsManager.callCreateBodyByWeexCore(pageId, componentType, ref, styles, attributes, events,
//                                                 paddings, margins, borders);
//    }
        return 0;
//    return mOriginBridge.callCreateBodyByWeexCore(pageId,componentType,ref,styles,attributes,
//                                                  events,paddings,margins,borders);
    }

    public int callAddElementByWeexCore(String pageId, String componentType, String ref, int index, String parentRef,
                                        HashMap<String, String> styles, HashMap<String, String> attributes, HashSet<String> events,
                                        HashMap<String, String> paddings, HashMap<String, String> margins,
                                        HashMap<String, String> borders) {
//    if (mJsManager != null) {
//      return mJsManager.callAddElementByWeexCore(pageId, componentType, ref, index, parentRef,
//                                                 styles, attributes, events, paddings, margins, borders);
//    }
        return 1;
//    return mOriginBridge.callAddElementByWeexCore(pageId,componentType,ref,index,parentRef,
//                                                  styles,attributes,events,paddings,margins,
//                                                  borders);
    }


    @Override
    public int callRemoveElement(String instanceId, String ref) {
//    if (mJsManager != null) {
//      return mJsManager.callRemoveElement(instanceId, ref);
//    }
//    return 0;
        return mOriginBridge.callRemoveElement(instanceId, ref);
    }

    @Override
    public int callMoveElement(String instanceId, String ref, String parentRef, int index) {
//    if (mJsManager != null) {
//      return mJsManager.callMoveElement(instanceId, ref, parentRef, index);
//    }
//    return 0;
        return mOriginBridge.callMoveElement(instanceId, ref, parentRef, index);
    }

    public int callUpdateStyleByWeexCore(String instanceId, String ref,
                                         HashMap<String, Object> styles,
                                         HashMap<String, String> paddings,
                                         HashMap<String, String> margins,
                                         HashMap<String, String> borders) {
//    if (null != mJsManager) {
//      return mJsManager.callUpdateStyleByWeexCore(instanceId, ref, styles, paddings, margins, borders);
//    }
        return 0;
//    return mOriginBridge.callUpdateStyleByWeexCore(instanceId,ref,styles,paddings,margins,borders);
    }


    public int callUpdateAttrsByWeexCore(String instanceId, String ref,
                                         HashMap<String, String> attrs) {
//    if (null != mJsManager) {
//      return mJsManager.callUpdateAttrsByWeexCore(instanceId, ref, attrs);
//    }
        return 0;

//    return mOriginBridge.callUpdateAttrsByWeexCore(instanceId,ref,attrs);
    }

    public int callLayoutByWeexCore(String pageId, String ref, int top, int bottom, int left, int
            right, int height, int width) {
//    if (null != mJsManager) {
//      return mJsManager.callLayoutByWeexCore(pageId, ref, top, bottom, left, right, height, width);
//    }
        return 0;
//    return mOriginBridge.callLayoutByWeexCore(pageId,ref,top,bottom,left,right,height,width);
    }

    public int callCreateFinishByWeexCore(String instanceId) {
//    if (null != mJsManager) {
//      mJsManager.callCreateFinishByWeexCore(instanceId);
//    }
        return 0;
//    return mOriginBridge.callCreateFinishByWeexCore(instanceId);
    }

    public void callLogOfFirstScreen(String message) {
        LogUtil.i("callLogOfFirstScreen :" + message);

//     mOriginBridge.callLogOfFirstScreen(message);

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