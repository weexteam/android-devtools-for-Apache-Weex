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
import com.taobao.weex.WXSDKManager;
import com.taobao.weex.bridge.WXBridgeManager;
import com.taobao.weex.common.IWXBridge;
import com.taobao.weex.common.IWXDebugProxy;
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

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static android.os.Build.VERSION;
import static android.os.Build.VERSION_CODES;

public class DebugServerProxy implements IWXDebugProxy {
  private static final String TAG = "DebugServerProxy";
  private static final String DEVTOOL_VERSION = "0.10.0.5";
  private SocketClient mWebSocketClient;
  private ObjectMapper mObjectMapper = new ObjectMapper();
  private MethodDispatcher mMethodDispatcher;
  private Iterable<ChromeDevtoolsDomain> mDomainModules;
  private JsonRpcPeer mPeer;
  private String mRemoteUrl = WXEnvironment.sRemoteDebugProxyUrl;
  private WXBridgeManager mJsManager;
  private Context mContext;
  private DebugBridge mBridge;

  public DebugServerProxy(Context context, WXBridgeManager jsManager) {
    if (context == null) {
      throw new IllegalArgumentException("Context of DebugServerProxy should not be null");
    }
    mContext = context;
    mWebSocketClient = SocketClientFactory.create(this);
    mJsManager = jsManager;
    mPeer = new JsonRpcPeer(mObjectMapper, mWebSocketClient);
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
      WXEnvironment.sDebugServerConnectable = true;
      WeexInspector.initializeWithDefaults(mContext);
      mBridge = DebugBridge.getInstance();
      mBridge.setSession(mWebSocketClient);
      mBridge.setBridgeManager(mJsManager);
      mWebSocketClient.connect(mRemoteUrl, new OkHttp3SocketClient.Callback() {

        private String getShakeHandsMessage() {
          Map<String, Object> func = new HashMap<>();
          func.put("name", WXEnvironment.getApplication().getPackageName() + " : " + android.os.Process.myPid());
          func.put("model", WXEnvironment.SYS_MODEL);
          func.put("weexVersion", WXEnvironment.WXSDK_VERSION);
          func.put("devtoolVersion", DEVTOOL_VERSION);
          func.put("platform", WXEnvironment.OS);
          func.put("deviceId", getDeviceId(mContext));
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
              if (mBridge != null) {
                mBridge.onConnected();
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
              if (mJsManager != null) {
                mJsManager.initScriptsFramework(null);
              }
            }
          }
        }

        @Override
        public void onFailure(Throwable cause) {
          synchronized (DebugServerProxy.class) {
            if (mBridge != null) {
              mBridge.onDisConnected();
            }
            Log.d(TAG, "connect debugger server failure!! " + cause.toString());
          }
        }

      });
    }
  }

  //@Override
  public void stop(boolean reload) {
    synchronized (DebugServerProxy.class) {
      if (mWebSocketClient != null) {
        mWebSocketClient.close(0, null);
        mWebSocketClient = null;
      }
      mBridge = null;
      if (reload) {
        switchLocalRuntime();
      }
    }

  }

  private void switchLocalRuntime() {
    WXEnvironment.sDebugServerConnectable = false;
    WXSDKEngine.reload(WXEnvironment.getApplication(), false);
    WXEnvironment.getApplication().sendBroadcast(new Intent(IWXDebugProxy.ACTION_DEBUG_INSTANCE_REFRESH));
  }

  @Override
  public IWXBridge getWXBridge() {
    return mBridge;
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
