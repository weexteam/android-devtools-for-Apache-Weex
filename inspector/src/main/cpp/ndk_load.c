//
// Created by zhongcang on 2018/1/29.
//

#include "jni.h"
#include "native_bridge.h"
#include <stdlib.h>


#define JNIREG_CLASS "com/taobao/weex/devtools/inspector/protocol/JsBridgeCall"


JNINativeMethod methodTable[] = {
        {"callCreateBody",      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",                                     (void *) callCreateBody},
        {"callAddElement",      "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V", (void *) callAddElement},
        {"callCreateFinish",    "(Ljava/lang/String;Ljava/lang/String;)V",                                                       (void *) callCreateFinish},
        {"callUpdateAttrs",     "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",                   (void *) callUpdateAttrs},
        {"callUpdateStyle",     "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",                   (void *) callUpdateStyle},
        {"callRemoveElement",   "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",                                     (void *) callRemoveElement},
        {"callAddEvent",        "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",                   (void *) callAddEvent},
        {"callRemoveEvent",     "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V",                   (void *) callRemoveEvent},
        {"callNativeModule",    "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[B;[B;)V",                               (void *) callNativeModule},
        {"callNativeComponent", "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;[B;[B;)V",                               (void *) callNativeComponent},
        {"setTimeoutNative",    "(Ljava/lang/String;Ljava/lang/String;)V",                                                       (void *) setTimeoutNative}
};


int registerNativeMethods(JNIEnv *env, const char *className,
                          JNINativeMethod *gMethods,
                          int numberMethods) {

    jclass clazz = (*env)->FindClass(env, className);
    if (NULL == clazz) {
        return JNI_FALSE;
    }
    if ((*env)->RegisterNatives(env, clazz, gMethods, numberMethods) < 0) {
        return JNI_FALSE;
    }
    return JNI_OK;
}


jint JNI_OnLoad(JavaVM *vm, void *reserved) {

    JNIEnv *env = NULL;
    jint result = -1;

    if ((*vm)->GetEnv(vm, (void **) &env, JNI_VERSION_1_4) != JNI_OK) {
        return result;
    }

    registerNativeMethods(env, JNIREG_CLASS,
                          methodTable,
                          (int) sizeof(methodTable) / sizeof((methodTable)[0])
    );


    return JNI_VERSION_1_4;
}