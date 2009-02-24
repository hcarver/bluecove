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
#define CPP__FILE "BlueCoveBlueZ_L2CAPServer.c"

#include "BlueCoveBlueZ.h"
#include <bluetooth/l2cap.h>

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_l2ServerOpenImpl
  (JNIEnv* env, jobject peer, jlong localDeviceBTAddress, jboolean authorize, jboolean authenticate, jboolean encrypt, jboolean master, jboolean timeouts, jint backlog, jint receiveMTU, jint transmitMTU, jint assignPsm) {

    // allocate socket
    int handle = socket(AF_BLUETOOTH, SOCK_SEQPACKET, BTPROTO_L2CAP);
    if (handle < 0) {
        throwIOException(env, "Failed to create socket. [%d] %s", errno, strerror(errno));
        return 0;
    }

    struct sockaddr_l2 localAddr;
    //bind local address
    localAddr.l2_family = AF_BLUETOOTH;
    localAddr.l2_psm = 0;
    if (assignPsm != 0) {
        localAddr.l2_psm = assignPsm;
    }
    //bacpy(&localAddr.l2_bdaddr, BDADDR_ANY);
    longToDeviceAddr(localDeviceBTAddress, &localAddr.l2_bdaddr);

    if (bind(handle, (struct sockaddr *)&localAddr, sizeof(localAddr)) < 0) {
        throwIOException(env, "Failed to bind socket. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }

    // Set link mtu and security options
    struct l2cap_options opt;
    socklen_t opt_len = sizeof(opt);
    memset(&opt, 0, opt_len);
    opt.imtu = receiveMTU;
    opt.omtu = (transmitMTU > 0)?transmitMTU:L2CAP_DEFAULT_MTU;
    opt.flush_to = L2CAP_DEFAULT_FLUSH_TO;
    Edebug("L2CAP set imtu %i, omtu %i", opt.imtu, opt.omtu);

    if (setsockopt(handle, SOL_L2CAP, L2CAP_OPTIONS, &opt, opt_len) < 0) {
        throwIOException(env, "Failed to set L2CAP mtu options. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }

    // Set link security options
    if (encrypt || authenticate || authorize || master) {
        int socket_opt = 0;
        socklen_t len = sizeof(socket_opt);
        if (getsockopt(handle, SOL_L2CAP, L2CAP_LM, &socket_opt, &len) < 0) {
            throwIOException(env, "Failed to read L2CAP server mode. [%d] %s", errno, strerror(errno));
            close(handle);
            return 0;
        }
        if (master) {
            socket_opt |= L2CAP_LM_MASTER;
        }
        if (authenticate) {
            socket_opt |= L2CAP_LM_AUTH;
            debug("L2CAP set authenticate");
        }
        if (encrypt) {
            socket_opt |= L2CAP_LM_ENCRYPT;
        }
        if (authorize) {
            socket_opt |= L2CAP_LM_SECURE;
        }

        if ((socket_opt != 0) && setsockopt(handle, SOL_L2CAP, L2CAP_LM, &socket_opt, sizeof(socket_opt)) < 0) {
            throwIOException(env, "Failed to set L2CAP server mode. [%d] %s", errno, strerror(errno));
            close(handle);
            return 0;
        }
    }

    // use non-blocking mode
    int flags = fcntl(handle, F_GETFL, 0);
    if (SOCKET_ERROR == flags) {
        throwIOException(env, "Failed to read L2CAP server descriptor flags. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }
    if (SOCKET_ERROR == fcntl(handle, F_SETFL, flags | O_NONBLOCK)) {
        throwIOException(env, "Failed to set L2CAP server non-blocking flags. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }
    // put socket into listening mode
    if (listen(handle, backlog) < 0) {
        throwIOException(env, "Failed to listen for L2CAP connections. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }

    return handle;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_l2ServerGetPSMImpl
  (JNIEnv* env, jobject peer, jlong handle) {
    struct sockaddr_l2 localAddr;
    socklen_t len = sizeof(localAddr);
    if (getsockname(handle, (struct sockaddr*)&localAddr, &len) < 0) {
        throwIOException(env, "Failed to get l2_psm. [%d] %s", errno, strerror(errno));
        return -1;
    }
    return localAddr.l2_psm;
}


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_l2ServerCloseImpl
  (JNIEnv* env, jobject peer, jlong handle, jboolean quietly) {
    debug("L2CAP close server handle %li", handle);
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

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_l2ServerAcceptAndOpenServerConnection
  (JNIEnv* env, jobject peer, jlong handle) {
    struct sockaddr_l2 remoteAddr;
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
                throwIOException(env, "Failed to accept L2CAP client connection. [%d] %s", errno, strerror(errno));
                return 0;
            }
        }
    } while (SOCKET_ERROR == client_socket);
    return client_socket;
}
