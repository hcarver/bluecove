/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
 *
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 *  @version $Id$
 */
#define CPP__FILE "common.c"

#include "common.h"
#include <stdio.h>

const char* cRuntimeException = "java/lang/RuntimeException";
const char* cIOException = "java/io/IOException";
const char* cInterruptedIOException = "java/io/InterruptedIOException";
const char* cBluetoothStateException = "javax/bluetooth/BluetoothStateException";
const char* cBluetoothConnectionException = "javax/bluetooth/BluetoothConnectionException";
const char* cServiceRegistrationException = "javax/bluetooth/ServiceRegistrationException";

// --- Debug

bool nativeDebugCallbackEnabled = false;
static jclass nativeDebugListenerClass;
static jmethodID nativeDebugMethod = NULL;

void enableNativeDebug(JNIEnv *env, jobject loggerClass, jboolean on) {
    if (on) {
        if (nativeDebugCallbackEnabled) {
            return;
        }
        nativeDebugListenerClass = (jclass)(*env)->NewGlobalRef(env, loggerClass);
        if (nativeDebugListenerClass != NULL) {
            nativeDebugMethod = (*env)->GetStaticMethodID(env, nativeDebugListenerClass, "nativeDebugCallback", "(Ljava/lang/String;ILjava/lang/String;)V");
            if (nativeDebugMethod != NULL) {
                nativeDebugCallbackEnabled = true;
                debug("nativeDebugCallback ON");
            }
        }
    } else {
        nativeDebugCallbackEnabled = false;
    }
}

void callDebugListener(JNIEnv *env, const char* fileName, int lineN, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    {
        if ((env != NULL) && (nativeDebugCallbackEnabled)) {
            char msg[1064];
            vsnprintf(msg, 1064, fmt, ap);
            (*env)->CallStaticVoidMethod(env, nativeDebugListenerClass, nativeDebugMethod, (*env)->NewStringUTF(env, fileName), lineN, (*env)->NewStringUTF(env, msg));
        }
    }
    va_end(ap);
}

void ndebug(const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    if (nativeDebugCallbackEnabled) {
        fprintf(stdout, "NATIVE:");
        vfprintf(stdout, fmt, ap);
        fprintf(stdout, "\n");
        fflush(stdout);
    }
    va_end(ap);
}

// --- Error handling

void vthrowException(JNIEnv *env, const char *name, const char *fmt, va_list ap) {
    char msg[1064];
    if (env == NULL) {
        return;
    }
    vsnprintf(msg, 1064, fmt, ap);
    if ((*env)->ExceptionCheck(env)) {
        ndebug("ERROR: can't throw second exception %s(%s)", name, msg);
        return;
    }
    jclass cls = (*env)->FindClass(env, name);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
        (*env)->ThrowNew(env, cls, msg);
        /* free the local ref */
        (*env)->DeleteLocalRef(env, cls);
    } else {
        debug("Can't find Exception %s", name);
        (*env)->FatalError(env, name);
    }

}

void throwException(JNIEnv *env, const char *name, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    vthrowException(env, name, fmt, ap);
    va_end(ap);
}

void throwRuntimeException(JNIEnv *env, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    vthrowException(env, cRuntimeException, fmt, ap);
    va_end(ap);
}

void throwIOException(JNIEnv *env, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    vthrowException(env, cIOException, fmt, ap);
    va_end(ap);
}

void throwInterruptedIOException(JNIEnv *env, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    vthrowException(env, cInterruptedIOException, fmt, ap);
    va_end(ap);
}

void throwServiceRegistrationException(JNIEnv *env, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);
    vthrowException(env, cServiceRegistrationException, fmt, ap);
    va_end(ap);
}

void throwBluetoothStateException(JNIEnv *env, const char *fmt, ...) {
   va_list ap;
    va_start(ap, fmt);
    vthrowException(env, cBluetoothStateException, fmt, ap);
    va_end(ap);
}

void throwBluetoothConnectionException(JNIEnv *env, int error, const char *fmt, ...) {
    va_list ap;
    va_start(ap, fmt);

    char msg[1064];
    if (env == NULL) {
        va_end(ap);
        return;
    }
    vsnprintf(msg, 1064, fmt, ap);

    if ((*env)->ExceptionCheck(env)) {
        debug("ERROR: can't throw second exception %s(%s)", cBluetoothConnectionException, msg);
        va_end(ap);
        return;
    }
    jclass cls = (*env)->FindClass(env, cBluetoothConnectionException);
    /* if cls is NULL, an exception has already been thrown */
    if (cls != NULL) {
        jmethodID methodID = (*env)->GetMethodID(env, cls, "<init>", "(ILjava/lang/String;)V");
        if (methodID == NULL) {
            (*env)->FatalError(env, "Fail to get constructor for Exception");
        } else {
            jstring excMessage = (*env)->NewStringUTF(env, msg);
            jthrowable obj = (jthrowable)(*env)->NewObject(env, cls, methodID, error, excMessage);
            if (obj != NULL) {
                (*env)->Throw(env, obj);
            } else {
                (*env)->FatalError(env, "Fail to create new Exception");
            }
        }
        /* free the local ref */
        (*env)->DeleteLocalRef(env, cls);
    } else {
        (*env)->FatalError(env, cBluetoothConnectionException);
    }

    va_end(ap);
}

// --- Interaction with java classes

bool isCurrentThreadInterrupted(JNIEnv *env, jobject peer) {
    jclass peerClass = (*env)->GetObjectClass(env, peer);
    if (peerClass == NULL) {
        throwRuntimeException(env, "Fail to get Object Class");
        return true;
    }
    jmethodID aMethod = getGetMethodID(env, peerClass, "isCurrentThreadInterruptedCallback", "()Z");
    if (aMethod == NULL) {
        throwRuntimeException(env, "Fail to get MethodID isCurrentThreadInterruptedCallback");
        return true;
    }
    if ((*env)->CallBooleanMethod(env, peer, aMethod)) {
        throwInterruptedIOException(env, "thread interrupted");
        return true;
    }
    return (*env)->ExceptionCheck(env);
}

bool threadSleep(JNIEnv *env, jlong millis) {
    jclass clazz = (*env)->FindClass(env, "java/lang/Thread");
    if (clazz == NULL) {
        throwRuntimeException(env, "Fail to get Thread class");
        return false;
    }
    jmethodID methodID = (*env)->GetStaticMethodID(env, clazz, "sleep", "(J)V");
    if (methodID == NULL) {
        throwRuntimeException(env, "Fail to get MethodID Thread.sleep");
        return false;
    }
    (*env)->CallStaticVoidMethod(env, clazz, methodID, millis);
    if ((*env)->ExceptionCheck(env)) {
        return false;
    }
    return true;
}

jmethodID getGetMethodID(JNIEnv * env, jclass clazz, const char *name, const char *sig) {
    if (clazz == NULL) {
        throwRuntimeException(env, "Fail to get MethodID %s for NULL class", name);
        return NULL;
    }
    jmethodID methodID = (*env)->GetMethodID(env, clazz, name, sig);
    if (methodID == NULL) {
        throwRuntimeException(env, "Fail to get MethodID %s", name);
        return NULL;
    }
    return methodID;
}

void DeviceInquiryCallback_Init(struct DeviceInquiryCallback* callback) {
    callback->inquiryRunnable = NULL;
    callback->deviceDiscoveredCallbackMethod = NULL;
    callback->startedNotify = NULL;
    callback->startedNotifyNotifyMethod = NULL;
}

bool DeviceInquiryCallback_builDeviceInquiryCallbacks(JNIEnv * env, struct DeviceInquiryCallback* callback, jobject inquiryRunnable, jobject startedNotify) {
    jclass inquiryRunnableClass = (*env)->GetObjectClass(env, inquiryRunnable);

    if (inquiryRunnableClass == NULL) {
        throwRuntimeException(env, "Fail to get Object Class");
        return false;
    }

    jmethodID deviceDiscoveredCallbackMethod = (*env)->GetMethodID(env, inquiryRunnableClass, "deviceDiscoveredCallback", "(Ljavax/bluetooth/DiscoveryListener;JILjava/lang/String;Z)V");
    if (deviceDiscoveredCallbackMethod == NULL) {
        throwRuntimeException(env, "Fail to get MethodID deviceDiscoveredCallback");
        return false;
    }

    jclass notifyClass = (*env)->GetObjectClass(env, startedNotify);
    if (notifyClass == NULL) {
        throwRuntimeException(env, "Fail to get Object Class");
        return false;
    }
    jmethodID notifyMethod = (*env)->GetMethodID(env, notifyClass, "deviceInquiryStartedCallback", "()V");
    if (notifyMethod == NULL) {
        throwRuntimeException(env, "Fail to get MethodID deviceInquiryStartedCallback");
        return false;
    }

    callback->inquiryRunnable = inquiryRunnable;
    callback->deviceDiscoveredCallbackMethod = deviceDiscoveredCallbackMethod;
    callback->startedNotify = startedNotify;
    callback->startedNotifyNotifyMethod = notifyMethod;

    return true;
}

bool DeviceInquiryCallback_callDeviceInquiryStartedCallback(JNIEnv * env, struct DeviceInquiryCallback* callback) {
    if ((callback->startedNotify == NULL) || (callback->startedNotifyNotifyMethod == NULL)) {
        throwRuntimeException(env, "DeviceInquiryCallback not initialized");
        return false;
    }
    (*env)->CallVoidMethod(env, callback->startedNotify, callback->startedNotifyNotifyMethod);
    if ((*env)->ExceptionCheck(env)) {
        return false;
    } else {
        return true;
    }
}

bool DeviceInquiryCallback_callDeviceDiscovered(JNIEnv * env, struct DeviceInquiryCallback* callback, jobject listener, jlong deviceAddr, jint deviceClass, jstring name, jboolean paired) {
    if ((callback->inquiryRunnable == NULL) || (callback->deviceDiscoveredCallbackMethod == NULL)) {
        throwRuntimeException(env, "DeviceInquiryCallback not initialized");
        return false;
    }
    (*env)->CallVoidMethod(env, callback->inquiryRunnable, callback->deviceDiscoveredCallbackMethod, listener, deviceAddr, deviceClass, name, paired);
    if ((*env)->ExceptionCheck(env)) {
        return false;
    } else {
        return true;
    }
}

