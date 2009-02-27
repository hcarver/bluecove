/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
 *
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 * Author: vlads
 * Created on Java 15, 2008, extracted from bluecove code
 *
 * @version $Id$
 */

#ifndef _BLUECOVE_COMMON_H
#define _BLUECOVE_COMMON_H

#include <jni.h>

typedef unsigned char bool;
enum {true = 1, false = 0};

// --- Debug
#define STD_DEBUG
#define EXT_DEBUG

void enableNativeDebug(JNIEnv * env, jobject loggerClass, jboolean on);

void callDebugListener(JNIEnv *env, const char* fileName, int lineN, const char *fmt, ...);

#ifdef STD_DEBUG
// This can be used in JNI functions. The message would be sent to java code
#define debug(...) callDebugListener(env, CPP__FILE, __LINE__, __VA_ARGS__);
#else
#define debug(...)
#endif

#ifdef EXT_DEBUG
#define Edebug(...) callDebugListener(env, CPP__FILE, __LINE__, __VA_ARGS__);
#else
#define Edebug(...)
#endif

// This will use stdout and can be used in native function callbacks
void ndebug(const char *fmt, ...);

// --- Error handling

void throwException(JNIEnv *env, const char *name, const char *fmt, ...);
void throwRuntimeException(JNIEnv *env, const char *fmt, ...);
void throwIOException(JNIEnv *env, const char *fmt, ...);
void throwInterruptedIOException(JNIEnv *env, const char *fmt, ...);
void throwServiceRegistrationException(JNIEnv *env, const char *fmt, ...);
void throwBluetoothStateException(JNIEnv *env, const char *fmt, ...);
void throwBluetoothConnectionException(JNIEnv *env, int error, const char *fmt, ...);

// --- Interaction with java classes

jmethodID getGetMethodID(JNIEnv * env, jclass clazz, const char *name, const char *sig);

bool isCurrentThreadInterrupted(JNIEnv *env, jobject peer);
bool threadSleep(JNIEnv *env, jlong millis);

#endif  /* _BLUECOVE_COMMON_H */

