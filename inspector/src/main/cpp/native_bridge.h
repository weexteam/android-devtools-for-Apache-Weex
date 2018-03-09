//
// Created by 陈佩翰 on 2018/1/29.
//

#ifndef WEEX_INSPECTOR_NATIVE_BRIDGE_H_H
#define WEEX_INSPECTOR_NATIVE_BRIDGE_H_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT
void callCreateBody(jstring instanceId, jstring tasks, jstring callback);

JNIEXPORT
void callAddElement(jstring instanceId, jstring ref, jstring dom, jstring index, jstring callback);

JNIEXPORT
void callCreateFinish(jstring instanceId, jstring callback);

JNIEXPORT
void callUpdateAttrs(jstring instanceId, jstring ref, jstring task, jstring callback);

JNIEXPORT
void callUpdateStyle(jstring instanceId, jstring ref, jstring task, jstring callback);

JNIEXPORT
void callRemoveElement(jstring instanceId, jstring ref, jstring callback);

JNIEXPORT
void callAddEvent(jstring instanceId, jstring ref, jstring event, jstring callback);

JNIEXPORT
void callRemoveEvent(jstring instanceId, jstring ref, jstring event, jstring callback);

JNIEXPORT
void callNativeModule(jstring instanceId, jstring module, jstring method, jbyteArray arguments,
                      jbyteArray options);

JNIEXPORT
void callNativeComponent(jstring instanceId, jstring componentRef, jstring method,
                         jbyteArray arguments, jbyteArray options);

JNIEXPORT
void setTimeoutNative(jstring callbackId, jstring time);

#ifdef __cplusplus
}
#endif

#endif //WEEX_INSPECTOR_NATIVE_BRIDGE_H_H
