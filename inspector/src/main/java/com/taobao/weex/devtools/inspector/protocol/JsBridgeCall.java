package com.taobao.weex.devtools.inspector.protocol;

/**
 * @author chenpeihan
 * @date 2018/1/29
 */

public class JsBridgeCall {

  public native void callNative(String instanceId, String tasks, String callback);

  public native void callCreateBody(String instanceId, String tasks, String callback);

  public native void callAddElement(String instanceId, String ref, String dom, String index,
                                    String callback);

  public native void callCreateFinish(String instanceId, String callback);

  public native void callUpdateAttrs(String instanceId, String ref, String task, String callback);

  public native void callUpdateStyle(String instanceId, String ref, String task, String callback);

  public native void callRemoveElement(String instanceId, String ref, String callback);

  public native void callAddEvent(String instanceId, String ref, String event, String callback);

  public native void callRemoveEvent(String instanceId, String ref, String event, String callback);

  public native void callNativeModule(String instanceId, String module, String method, byte[]
      arguments, byte[] options);

  public native void callNativeComponent(String instanceId, String componentRef, String method,
                                         byte[] arguments, byte[] options);

  public native void setTimeoutNative(String callbackId, String time);

}
