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
#define CPP__FILE "BlueCoveBlueZ_RFCOMM.c"

#include "BlueCoveBlueZ.h"

#include <poll.h>
#include <bluetooth/rfcomm.h>

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_connectionRfOpenClientConnectionImpl
  (JNIEnv* env, jobject peer, jlong localDeviceBTAddress, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint timeout) {
    debug("RFCOMM connect, channel %d", channel);

    // allocate socket
    int handle = socket(AF_BLUETOOTH, SOCK_STREAM, BTPROTO_RFCOMM);
    if (handle < 0) {
        throwIOException(env, "Failed to create socket. [%d] %s", errno, strerror(errno));
        return 0;
    }

    struct sockaddr_rc localAddr;
    //bind local address
    localAddr.rc_family = AF_BLUETOOTH;
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
    if (encrypt || authenticate) {
        int socket_opt = 0;
        socklen_t len = sizeof(socket_opt);
        if (getsockopt(handle, SOL_RFCOMM, RFCOMM_LM, &socket_opt, &len) < 0) {
            throwIOException(env, "Failed to read RFCOMM link mode. [%d] %s", errno, strerror(errno));
            close(handle);
            return 0;
        }
        //if (master) {
        //  socket_opt |= RFCOMM_LM_MASTER;
        //}
        if (authenticate) {
            socket_opt |= RFCOMM_LM_AUTH;
            debug("RFCOMM set authenticate");
        }
        if (encrypt) {
            socket_opt |= RFCOMM_LM_ENCRYPT;
        }
        //if (socket_opt != 0) {
        //  socket_opt |= RFCOMM_LM_SECURE;
        //}

        if ((socket_opt != 0) && setsockopt(handle, SOL_RFCOMM, RFCOMM_LM, &socket_opt, sizeof(socket_opt)) < 0) {
            throwIOException(env, "Failed to set RFCOMM link mode. [%d] %s", errno, strerror(errno));
            close(handle);
            return 0;
        }
    }

    struct sockaddr_rc remoteAddr;
    remoteAddr.rc_family = AF_BLUETOOTH;
    longToDeviceAddr(address, &remoteAddr.rc_bdaddr);
    remoteAddr.rc_channel = channel;

    // connect to server
    if (connect(handle, (struct sockaddr*)&remoteAddr, sizeof(remoteAddr)) != 0) {
        throwIOException(env, "Failed to connect. [%d] %s", errno, strerror(errno));
        close(handle);
        return 0;
    }
    debug("RFCOMM connected, handle %li", handle);
    return handle;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_connectionRfCloseClientConnection
  (JNIEnv* env, jobject peer, jlong handle) {
    debug("RFCOMM disconnect, handle %li", handle);
    // Closing channel, further sends and receives will be disallowed.
    if (shutdown(handle, SHUT_RDWR) < 0) {
        debug("shutdown failed. [%d] %s", errno, strerror(errno));
    }
    if (close(handle) < 0) {
        throwIOException(env, "Failed to close socket. [%d] %s", errno, strerror(errno));
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_rfGetSecurityOptImpl
  (JNIEnv *env, jobject peer, jlong handle) {
    int socket_opt = 0;
    socklen_t len = sizeof(socket_opt);
    if (getsockopt(handle, SOL_RFCOMM, RFCOMM_LM, &socket_opt, &len) < 0) {
        throwIOException(env, "Failed to get RFCOMM link mode. [%d] %s", errno, strerror(errno));
        return 0;
    }
    bool encrypted = socket_opt &  (RFCOMM_LM_ENCRYPT | RFCOMM_LM_SECURE);
    bool authenticated = socket_opt & RFCOMM_LM_AUTH;
    if (authenticated) {
        return NOAUTHENTICATE_NOENCRYPT;
    }
    if (encrypted) {
        return AUTHENTICATE_ENCRYPT;
    } else {
        return AUTHENTICATE_NOENCRYPT;
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_connectionRfRead
  (JNIEnv* env, jobject peer, jlong handle, jbyteArray b, jint off, jint len ) {
    jbyte *bytes = (*env)->GetByteArrayElements(env, b, 0);
    int done = 0;
    while (done == 0) {
        int flags = MSG_DONTWAIT;
        int count = recv(handle, (char *)(bytes + off + done), len - done, flags);
        if (count < 0) {
            if (errno == EAGAIN) { // Try again for non-blocking operation
                count = 0;
                 Edebug("no data available for read");
            } else if (errno == ECONNRESET) { //104 Connection reset by peer
                debug("Connection closed, Connection reset by peer");
                // See InputStream.read();
                done = -1;
                goto rfReadEnd;
            } else {
                throwIOException(env, "Failed to read. [%d] %s", errno, strerror(errno));
                done = 0;
                goto rfReadEnd;
            }
        } else if (count == 0) {
            debug("Connection closed");
            if (done == 0) {
                // See InputStream.read();
                done = -1;
            }
            goto rfReadEnd;
        }
        done += count;
        if (isCurrentThreadInterrupted(env, peer)) {
            done = 0;
            goto rfReadEnd;
        }
        if (done == 0) {
            // Sleep while not avalable
            bool available = false;
            do {
                struct pollfd fds;
                int timeout = 500; // milliseconds
                fds.fd = handle;
                fds.events = POLLIN | POLLHUP | POLLERR;// | POLLRDHUP;
                fds.revents = 0;
                //Edebug("poll: wait");
                int poll_rc = poll(&fds, 1, timeout);
                if (poll_rc > 0) {
                    if (fds.revents & (POLLHUP | POLLERR /* | POLLRDHUP */)) {
                        debug("Stream socket peer closed connection");
                        done = -1;
                        goto rfReadEnd;
                    } else if (fds.revents & POLLNVAL) {
                        // socket closed...
                         done = -1;
                         goto rfReadEnd;
                    } else if (fds.revents & POLLIN) {
                        //Edebug("poll: data to read available");
                        available = true;
                    } else {
                        Edebug("poll: revents %i", fds.revents);
                    }
                } else if (poll_rc == -1) {
                    //Edebug("poll: call error %i", errno);
                    throwIOException(env, "Failed to poll. [%d] %s", errno, strerror(errno));
                    done = 0;
                    goto rfReadEnd;
                } else {
                    //Edebug("poll: call timed out");
                }
                if (isCurrentThreadInterrupted(env, peer)) {
                    done = -1;
                    goto rfReadEnd;
                }
            } while (!available);
        }
    }
rfReadEnd:
    (*env)->ReleaseByteArrayElements(env, b, bytes, 0);
    return done;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_connectionRfReadAvailable
  (JNIEnv* env, jobject peer, jlong handle) {
    struct pollfd fds;
    int timeout = 10; // milliseconds
    fds.fd = handle;
    fds.events = POLLIN | POLLHUP | POLLERR; // | POLLRDHUP;
    fds.revents = 0;
    int poll_rc = poll(&fds, 1, timeout);
    if (poll_rc > 0) {
        if (fds.revents & (POLLHUP | POLLERR/* | POLLRDHUP */)) {
            throwIOException(env, "Stream socket peer closed connection");
        } else if (fds.revents & POLLIN) {
            return 1;
        }
        // POLLNVAL - this method may choose to throw an IOException if this input stream has been closed by invoking the close() method.
        // We do not
    } else if (poll_rc == -1) {
        throwIOException(env, "Failed to read available. [%d] %s", errno, strerror(errno));
    }
    return 0;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_connectionRfWrite__JI
  (JNIEnv* env, jobject peer, jlong handle, jint b) {
    char c = (char)b;
    if (send(handle, &c, 1, 0) != 1) {
        throwIOException(env, "Failed to write. [%d] %s", errno, strerror(errno));
    }
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_connectionRfWrite__J_3BII
  (JNIEnv* env, jobject peer, jlong handle, jbyteArray b, jint off, jint len) {

    jbyte *bytes = (*env)->GetByteArrayElements(env, b, 0);
    int done = 0;
    while(done < len) {
        int count = send(handle, (char *)(bytes + off + done), len - done, 0);
        if (count < 0) {
            throwIOException(env, "Failed to write. [%d] %s", errno, strerror(errno));
            break;
        }
        if (isCurrentThreadInterrupted(env, peer)) {
            break;
        }
        done += count;
    }
    (*env)->ReleaseByteArrayElements(env, b, bytes, 0);
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_connectionRfFlush
  (JNIEnv* env, jobject peer, jlong handle) {
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZDBus_getConnectionRfRemoteAddress
  (JNIEnv* env, jobject peer, jlong handle) {
    struct sockaddr_rc remoteAddr;
    socklen_t len = sizeof(remoteAddr);
    if (getpeername(handle, (struct sockaddr*)&remoteAddr, &len) < 0) {
        throwIOException(env, "Failed to get peer name. [%d] %s", errno, strerror(errno));
        return -1;
    }
    return deviceAddrToLong(&remoteAddr.rc_bdaddr);
}
