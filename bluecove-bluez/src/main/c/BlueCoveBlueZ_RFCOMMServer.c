/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
#define CPP__FILE "BlueCoveBlueZ_RFCOMMServer.c"

#include "BlueCoveBlueZ.h"

#include <sys/socket.h>
#include <sys/unistd.h>
#include <bluetooth/rfcomm.h>
#include <fcntl.h>

int dynamic_bind_rc(int sock, struct sockaddr_rc *sockaddr, uint8_t *port) {
    int err;
    for ((*port) = 1; (*port) <= 31; (*port)++) {
        sockaddr->rc_channel = *port;
        err = bind(sock,(struct sockaddr *)sockaddr,sizeof(sockaddr));
        if (!err) {
            break;
        }
    }
    if ((*port) == 31) {
        err = -1;
    }
    return err;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_rfServerOpenImpl
  (JNIEnv* env, jobject peer, jlong localDeviceBTAddress, jboolean authorize, jboolean authenticate, jboolean encrypt, jboolean master, jboolean timeouts, jint backlog) {
    // allocate socket
    int handle = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
    if (handle < 0) {
        throwIOException(env, "Failed to create socket. [%d] %s", errno, strerror(errno));
        return 0;
    }

    struct sockaddr_rc localAddr;
    memset(&localAddr, 0, sizeof(localAddr));
    //bind local address
    localAddr.rc_family = AF_BLUETOOTH;
    // TODO for kernel versions 2.6.6 and before use dynamic_bind_rc
    localAddr.rc_channel = 0;
    //bacpy(&localAddr.rc_bdaddr, BDADDR_ANY);
    longToDeviceAddr(localDeviceBTAddress, &localAddr.rc_bdaddr);

    if (bind(handle, (struct sockaddr *)&localAddr, sizeof(localAddr)) < 0) {
        throwIOException(env, "Failed to  bind socket. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }

    // TODO verify how this works, I think device needs to paird before this can be setup.
    // Set link security options
    if (encrypt || authenticate || authorize || master) {
        int socket_opt = 0;
        socklen_t len = sizeof(socket_opt);
        if (getsockopt(handle, SOL_RFCOMM, RFCOMM_LM, &socket_opt, &len) < 0) {
            throwIOException(env, "Failed to read RFCOMM server mode. [%d] %s", errno, strerror(errno));
            close(handle);
            return 0;
        }
        if (master) {
            socket_opt |= RFCOMM_LM_MASTER;
        }
        if (authenticate) {
            socket_opt |= RFCOMM_LM_AUTH;
            debug("RFCOMM set authenticate");
        }
        if (encrypt) {
            socket_opt |= RFCOMM_LM_ENCRYPT;
        }
        if (authorize) {
            socket_opt |= RFCOMM_LM_SECURE;
        }

        if ((socket_opt != 0) && setsockopt(handle, SOL_RFCOMM, RFCOMM_LM, &socket_opt, sizeof(socket_opt)) < 0) {
            throwIOException(env, "Failed to set RFCOMM server mode. [%d] %s", errno, strerror(errno));
            close(handle);
            return 0;
        }
    }

    // use non-blocking mode
    int flags = fcntl(handle, F_GETFL, 0);
    if (SOCKET_ERROR == flags) {
        throwIOException(env, "Failed to read RFCOMM server descriptor flags. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }
    if (SOCKET_ERROR == fcntl(handle, F_SETFL, flags | O_NONBLOCK)) {
        throwIOException(env, "Failed to set RFCOMM server non-blocking flags. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }
    // put socket into listening mode
    if (listen(handle, backlog) < 0) {
        throwIOException(env, "Failed to listen for RFCOMM connections. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }

    return handle;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_rfServerGetChannelIDImpl
  (JNIEnv* env, jobject peer, jlong handle) {
    struct sockaddr_rc localAddr;
    memset(&localAddr, 0, sizeof(localAddr));
    socklen_t len = sizeof(localAddr);
    if (getsockname(handle, (struct sockaddr*)&localAddr, &len) < 0) {
        throwIOException(env, "Failed to get rc_channel. [%d] %s", errno, strerror(errno));
        return -1;
    }
    return localAddr.rc_channel;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_rfServerCloseImpl
  (JNIEnv* env, jobject peer, jlong handle, jboolean quietly) {
    debug("RFCOMM close server handle %li", handle);
    // Closing channel, further sends and receives will be disallowed.
    if (shutdown(handle, SHUT_RDWR) < 0) {
        debug("server shutdown failed. [%d] %s", errno, strerror(errno));
    }
    if (close(handle) < 0) {
        if (quietly) {
            debug("Failed to close server socket. [%d] %s", errno, strerror(errno));
        } else {
            throwIOException(env, "Failed to close server socket. [%d] %s", errno, strerror(errno));
        }
    }
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_rfServerAcceptAndOpenRfServerConnection
  (JNIEnv* env, jobject peer, jlong handle) {
    struct sockaddr_rc remoteAddr;
    memset(&remoteAddr, 0, sizeof(remoteAddr));
    socklen_t  remoteAddrLen = sizeof(remoteAddr);
    int client_socket = SOCKET_ERROR;
    do {
        client_socket = accept(handle, (struct sockaddr*)&remoteAddr, &remoteAddrLen);
        if (SOCKET_ERROR == client_socket) {
            if (errno == EWOULDBLOCK) {
                if (isCurrentThreadInterrupted(env, peer)) {
                    return 0;
                }
                if (!threadSleep(env, 100)) {
                    return 0;
                }
                continue;
            } else {
                throwIOException(env, "Failed to accept RFCOMM client connection. [%d] %s", errno, strerror(errno));
                return 0;
            }
        }
    } while (SOCKET_ERROR == client_socket);
    debug("RFCOMM client accepted, handle %li", client_socket);
    return client_socket;
}
