/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2009 Vlad Skarzhevskyy
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
 * @version $Id$
 */
#define CPP__FILE "BlueCoveLocalSocket.c"

#include "BlueCoveLocalSocket.h"

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeConnect
  (JNIEnv *env, jobject peer, jstring name, jboolean abstractNamespace, jint timeout) {
    return -1;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeBind
  (JNIEnv *env, jobject peer, jint handle, jstring name, jboolean abstractNamespace, jint backlog) {
}

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeAccept
  (JNIEnv *env, jobject peer, jint socket) {
    return -1;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeClose
  (JNIEnv *env, jobject peer, jint handle) {
}


JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeAvailable
  (JNIEnv *env, jobject peer, jint handle) {
    return 0;
}

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeRead
  (JNIEnv *env, jobject peer, jint handle, jbyteArray b, jint off, jint len) {
    return 0;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeWrite
  (JNIEnv *env, jobject peer, jint handle, jbyteArray b, jint off, jint len) {
}