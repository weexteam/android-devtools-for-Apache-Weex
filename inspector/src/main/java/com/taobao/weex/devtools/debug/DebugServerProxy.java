package com.taobao.weex.devtools.debug;

import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.alibaba.fastjson.JSON;
import com.taobao.weex.WXEnvironment;
import com.taobao.weex.WXSDKEngine;
import com.taobao.weex.WXSDKInstance;
import com.taobao.weex.WXSDKManager;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.bridge.WXDebugJsBridge;
import com.taobao.weex.common.IWXBridge;
import com.taobao.weex.common.IWXDebugConfig;
import com.taobao.weex.devtools.WMLInspector;
import com.taobao.weex.devtools.WeexInspector;
import com.taobao.weex.devtools.common.LogRedirector;
import com.taobao.weex.devtools.common.Util;
import com.taobao.weex.devtools.inspector.MessageHandlingException;
import com.taobao.weex.devtools.inspector.MethodDispatcher;
import com.taobao.weex.devtools.inspector.MismatchedResponseException;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcException;
import com.taobao.weex.devtools.inspector.jsonrpc.JsonRpcPeer;
import com.taobao.weex.devtools.inspector.jsonrpc.PendingRequest;
import com.taobao.weex.devtools.inspector.jsonrpc.protocol.JsonRpcError;
import com.taobao.weex.devtools.inspector.jsonrpc.protocol.JsonRpcRequest;
import com.taobao.weex.devtools.inspector.jsonrpc.protocol.JsonRpcResponse;
import com.taobao.weex.devtools.inspector.protocol.ChromeDevtoolsDomain;
import com.taobao.weex.devtools.json.ObjectMapper;
import com.taobao.weex.utils.WXLogUtils;
import com.taobao.windmill.bridge.IWMLBridge;
import com.taobao.windmill.bridge.WMLBridgeManager;
import com.taobao.windmill.rt.util.WMLEnv;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;

public class DebugServerProxy {

    private static final String TAG = "DebugServerProxy";
    private static final String DEVTOOL_VERSION = "0.16.21";
    private SocketClient mWebSocketClient;
    private ObjectMapper mObjectMapper = new ObjectMapper();
    private MethodDispatcher mMethodDispatcher;
    private Iterable<ChromeDevtoolsDomain> mDomainModules;
    private JsonRpcPeer mPeer;
    public String mRemoteUrl = WXEnvironment.sRemoteDebugProxyUrl;
    private WXBridgeManager mWXJsManager;
    private WXDebugJsBridge mWXDebugJsBridge;
    private WXDebugBridge mWXBridge;
    private WMLDebugBridge mWMLBridge;
    private Context mContext;

    public DebugServerProxy(Context context, IWXDebugConfig wxDebugAdapter) {

        if (context == null) {
            throw new IllegalArgumentException("Context of DebugServerProxy should not be null");
        }
        mContext = context;
        mWebSocketClient = SocketClientFactory.create(this);
        mPeer = new JsonRpcPeer(mObjectMapper, mWebSocketClient);

        if (wxDebugAdapter != null) {
            if (wxDebugAdapter.getWXJSManager() != null) {
                mWXJsManager = wxDebugAdapter.getWXJSManager();
            }
            if (wxDebugAdapter.getWXDebugJsBridge() != null) {
                mWXDebugJsBridge = wxDebugAdapter.getWXDebugJsBridge();
            }
        }
    }

    private static void logDispatchException(JsonRpcException e) {
        JsonRpcError errorMessage = e.getErrorMessage();
        switch (errorMessage.code) {
            case METHOD_NOT_FOUND:
                LogRedirector.d(TAG, "Method not implemented: " + errorMessage.message);
                break;
            default:
                LogRedirector.w(TAG, "Error processing remote message", e);
                break;
        }
    }

    // here just used to flag a debugged device, result same with adb device is fine
    private String getDeviceId(Context context) {
        String deviceId = null;
        if (VERSION.SDK_INT > VERSION_CODES.FROYO) {
            deviceId = Build.SERIAL;
        }
        if (TextUtils.isEmpty(deviceId)) {
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        }
        return deviceId;
    }

    public void start() {
        synchronized (DebugServerProxy.class) {
            if (mContext == null) {
                new IllegalArgumentException("Context is null").printStackTrace();
                return;
            }
            WXEnvironment.sDebugServerConnectable = true;
            WeexInspector.initializeWithDefaults(mContext);
            mWXBridge = WXDebugBridge.getInstance();
            mWXBridge.setSession(mWebSocketClient);
            mWXBridge.setWXDebugJsBridge(mWXDebugJsBridge);
            mWebSocketClient.connect(mRemoteUrl, new SocketClient.Callback() {

                private String getShakeHandsMessage() {
                    Map<String, Object> func = new HashMap<>();
                    func.put("name", WXEnvironment.getApplication().getPackageName() + " : " + android.os.Process.myPid());
                    func.put("model", WXEnvironment.SYS_MODEL);
                    func.put("weexVersion", WXEnvironment.WXSDK_VERSION);
                    func.put("devtoolVersion", DEVTOOL_VERSION);
                    func.put("platform", WXEnvironment.OS);
                    func.put("deviceId", getDeviceId(mContext));
                    func.put("network", WXEnvironment.sDebugNetworkEventReporterEnable);
                    if (WXEnvironment.sLogLevel != null) {
                        func.put("logLevel", WXEnvironment.sLogLevel.getName());
                    }
                    func.put("remoteDebug", WXEnvironment.sRemoteDebugMode);

                    Map<String, Object> map = new HashMap<>();
                    map.put("id", "0");
                    map.put("method", "WxDebug.registerDevice");
                    map.put("params", func);
                    return JSON.toJSONString(map);
                }

                @Override
                public void onSuccess(String response) {
                    synchronized (DebugServerProxy.class) {
                        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
                            mWebSocketClient.sendText(getShakeHandsMessage());
                            if (mWXBridge != null) {
                                mWXBridge.onConnected();
                            }
                            mDomainModules = new WeexInspector.DefaultInspectorModulesBuilder(mContext).finish();
                            mMethodDispatcher = new MethodDispatcher(mObjectMapper, mDomainModules);
                            WXSDKManager.getInstance().postOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {
                                            Toast.makeText(
                                                    WXEnvironment.sApplication,
                                                    "debug server connected", Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                    }, 0);
                            Log.d(TAG, "connect debugger server success!");
                            if (mWXJsManager != null) {
                                mWXJsManager.initScriptsFramework(null);
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    synchronized (DebugServerProxy.class) {
                        if (mWXBridge != null) {
                            mWXBridge.onDisConnected();
                        }
                        Log.w(TAG, "connect debugger server failure!!");
                        cause.printStackTrace();
                    }
                }

            });
        }
    }

    public void startWMLDebug() {
        synchronized (DebugServerProxy.class) {
            if (mContext == null) {
                new IllegalArgumentException("Context is null").printStackTrace();
                return;
            }
            WMLInspector.initializeWithDefaults(mContext);
            mWMLBridge = WMLDebugBridge.getInstance();
            mWMLBridge.setSession(mWebSocketClient);
            mWebSocketClient.connect(mRemoteUrl, new SocketClient.Callback() {

                private String getShakeHandsMessage() {
                    Map<String, Object> func = new HashMap<>();
                    func.put("deviceId", getDeviceId(mContext));
                    func.put("platform", WXEnvironment.OS);
                    func.put("model", WXEnvironment.SYS_MODEL);
                    func.put("windmillVersion", WMLEnv.sWindmillVersion);
                    func.put("devtoolVersion", DEVTOOL_VERSION);
                    func.put("name", WMLEnv.sAppName);

                    func.put("remoteDebug", WMLBridgeManager.sRemoteDebugMode);

                    Map<String, Object> map = new HashMap<>();
                    map.put("method", "WMLDebug.registerDevice");
                    map.put("params", func);
                    return JSON.toJSONString(map);
                }

                @Override
                public void onSuccess(String response) {
                    synchronized (DebugServerProxy.class) {
                        if (mWebSocketClient != null && mWebSocketClient.isOpen()) {
                            mWebSocketClient.sendText(getShakeHandsMessage());
                            if (mWMLBridge != null) {
                                mWMLBridge.onConnected();
                            }
                            mDomainModules = new WMLInspector.DefaultInspectorModulesBuilder(mContext).finish();
                            mMethodDispatcher = new MethodDispatcher(mObjectMapper, mDomainModules);
                            WXSDKManager.getInstance().postOnUiThread(
                                    new Runnable() {

                                        @Override
                                        public void run() {
                                            Toast.makeText(
                                                    WXEnvironment.sApplication,
                                                    "debug server connected", Toast.LENGTH_SHORT)
                                                    .show();
                                        }
                                    }, 0);
                            Log.d(TAG, "connect debugger server success!");
//                            if (mWXJsManager != null) {
//                                mWXJsManager.initScriptsFramework(null);
//                            }
                        }
                    }
                }

                @Override
                public void onFailure(Throwable cause) {
                    synchronized (DebugServerProxy.class) {
                        if (mWMLBridge != null) {
                            mWMLBridge.onDisConnected();
                        }
                        Log.w(TAG, "connect debugger server failure!!");
                        cause.printStackTrace();
                    }
                }

            });
        }
    }

    public void stop(boolean reload) {
        synchronized (DebugServerProxy.class) {
            if (mWebSocketClient != null) {
                mWebSocketClient.close(0, null);
                mWebSocketClient = null;
            }
            mWXBridge = null;
            mWMLBridge = null;
            if (reload) {
                switchLocalRuntime();
            }
        }

    }

    private void switchLocalRuntime() {
        WXSDKEngine.reload(WXEnvironment.getApplication(), false);
        WXEnvironment.getApplication().sendBroadcast(new Intent()
                .setAction(WXSDKInstance.ACTION_DEBUG_INSTANCE_REFRESH)
                .putExtra("params", "")
        );
    }

    public IWXBridge getWXBridge() {
        if (mWXBridge == null) {
            WXLogUtils.e(TAG, "WXDebugBridge is null!");
        }
        return mWXBridge;
    }

    public IWMLBridge getWMLBridge() {
        if (mWMLBridge == null) {
            WXLogUtils.e(TAG, "WMLDebugBridge is null!");
        }
        return mWMLBridge;
    }

    public void handleMessage(String message) throws IOException {
        try {
            Util.throwIfNull(mPeer);
            handleRemoteMessage(mPeer, message);
        } catch (Exception e) {
            if (LogRedirector.isLoggable(TAG, Log.VERBOSE)) {
                LogRedirector.v(TAG, "Unexpected I/O exception processing message: " + e);
            }
        }
    }

    private void handleRemoteMessage(JsonRpcPeer peer, String message)
            throws IOException, MessageHandlingException, JSONException {
        // Parse as a generic JSONObject first since we don't know if this is a request or response.
        JSONObject messageNode = new JSONObject(message);
        if (messageNode.has("method")) {
            handleRemoteRequest(peer, messageNode);
        } else if (messageNode.has("result")) {
            handleRemoteResponse(peer, messageNode);
        } else {
            throw new MessageHandlingException("Improper JSON-RPC message: " + message);
        }
    }

    private void handleRemoteRequest(JsonRpcPeer peer, JSONObject requestNode)
            throws MessageHandlingException {
        JsonRpcRequest request;
        request = mObjectMapper.convertValue(
                requestNode,
                JsonRpcRequest.class);

        JSONObject result = null;
        JSONObject error = null;
        try {
            result = mMethodDispatcher.dispatch(peer,
                    request.method,
                    request.params);
        } catch (JsonRpcException e) {
            logDispatchException(e);
            error = mObjectMapper.convertValue(e.getErrorMessage(), JSONObject.class);
        }
        if (request.id != null) {
            JsonRpcResponse response = new JsonRpcResponse();
            response.id = request.id;
            response.result = result;
            response.error = error;
            JSONObject jsonObject = mObjectMapper.convertValue(response, JSONObject.class);
            String responseString;
            try {
                responseString = jsonObject.toString();
            } catch (OutOfMemoryError e) {
                // JSONStringer can cause an OOM when the Json to handle is too big.
                response.result = null;
                response.error = mObjectMapper.convertValue(e.getMessage(), JSONObject.class);
                jsonObject = mObjectMapper.convertValue(response, JSONObject.class);
                responseString = jsonObject.toString();
            }
            peer.getWebSocket().sendText(responseString);
        }
    }

    private void handleRemoteResponse(JsonRpcPeer peer, JSONObject responseNode)
            throws MismatchedResponseException {
        JsonRpcResponse response = mObjectMapper.convertValue(
                responseNode,
                JsonRpcResponse.class);
        PendingRequest pendingRequest = peer.getAndRemovePendingRequest(response.id);
        if (pendingRequest == null) {
            throw new MismatchedResponseException(response.id);
        }
        if (pendingRequest.callback != null) {
            pendingRequest.callback.onResponse(peer, response);
        }
    }
}