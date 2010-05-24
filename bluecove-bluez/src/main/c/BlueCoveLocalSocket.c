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

#define _GNU_SOURCE
#include "BlueCoveLocalSocket.h"

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeCreate
  (JNIEnv *env, jobject peer, jboolean stream) {
    int rc;
    rc = socket(PF_UNIX, stream ? SOCK_STREAM : SOCK_DGRAM, 0);
    if (rc < 0) {
        throwIOException(env, "Failed to create socket. [%d] %s", errno, strerror(errno));
        return 0;
    }
    return rc;
}

bool validateSocket(JNIEnv *env, jint handle) {
    if (handle < 0) {
        throwIOException(env, "invalid socket [%d]", handle);
        return false;
    } else {
        return true;
    }
}

struct sockaddr_un* populateSocketAddress(JNIEnv *env, int* paddress_len, jstring name, jboolean abstractNamespace) {
    const char* path;
    int name_len;
    struct sockaddr_un* paddr;

    path = (*env)->GetStringUTFChars(env, name, NULL);
    if (path == NULL) {
        throwRuntimeException(env, "JNI error");
        return NULL;
    }

    name_len = (*env)->GetStringUTFLength(env, name);
    paddr = (struct sockaddr_un*)malloc(sizeof(sa_family_t) + name_len + 1);
    if (paddr == NULL) {
        throwRuntimeException(env, "no memory available");
        return NULL;
    }
    if (abstractNamespace) {
        strncpy(paddr->sun_path + 1, path, name_len);
        paddr->sun_path[0] = '\0';
    } else {
        strncpy(paddr->sun_path, path, name_len + 1);
    }
    paddr->sun_family = AF_UNIX;

    (*env)->ReleaseStringUTFChars(env, name, path);

    (*paddress_len) = offsetof(struct sockaddr_un, sun_path) + name_len + 1;

    return paddr;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeConnect
  (JNIEnv *env, jobject peer, jint handle, jstring name, jboolean abstractNamespace, jint timeout) {
    struct sockaddr_un* paddr;
    int address_len;
    int rc;

    if (!validateSocket(env, handle)) {
        return;
    }
    paddr = populateSocketAddress(env, &address_len, name, abstractNamespace);
    if (paddr == NULL) {
        return;
    }
    rc = connect((int)handle, (struct sockaddr *) paddr, address_len);
    free(paddr);
    if (rc < 0) {
        throwIOException(env, "Failed to connect socket. [%d] %s", errno, strerror(errno));
        return;
    }
    return;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeBind
  (JNIEnv *env, jobject peer, jint handle, jstring name, jboolean abstractNamespace) {
    struct sockaddr_un* paddr;
    int address_len;
    int rc;

    if (!validateSocket(env, handle)) {
        return;
    }
    paddr = populateSocketAddress(env, &address_len, name, abstractNamespace);
    if (paddr == NULL) {
        return;
    }
    rc = bind((int)handle, (struct sockaddr *) paddr, address_len);
    free(paddr);
    if (rc < 0) {
        throwIOException(env, "Failed to bind socket. [%d] %s", errno, strerror(errno));
        return;
    }
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeListen
  (JNIEnv *env, jobject peer, jint handle, jint backlog) {
    int rc;
    int flags;

    if (!validateSocket(env, handle)) {
        return;
    }
    // use non-blocking mode
    flags = fcntl(handle, F_GETFL, 0);
    if (SOCKET_ERROR == flags) {
        throwIOException(env, "Failed to read server descriptor flags. [%d] %s", errno, strerror(errno));
        return;
    }
    if (SOCKET_ERROR == fcntl(handle, F_SETFL, flags | O_NONBLOCK)) {
        throwIOException(env, "Failed to set server non-blocking flags. [%d] %s", errno, strerror(errno));
        return;
    }

    rc = listen(handle, backlog);
    if (rc < 0) {
        throwIOException(env, "Failed to bind socket. [%d] %s", errno, strerror(errno));
        return;
    }
}

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeAccept
  (JNIEnv *env, jobject peer, jint handle) {
    if (!validateSocket(env, handle)) {
        return -1;
    }

    int client_socket = SOCKET_ERROR;
    do {
        client_socket = accept(handle, NULL, NULL);
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
                throwIOException(env, "Failed to accept client connection. [%d] %s", errno, strerror(errno));
                return -1;
            }
        }
    } while (SOCKET_ERROR == client_socket);
    debug("client accepted, handle %li", client_socket);
    return client_socket;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeClose
  (JNIEnv *env, jobject peer, jint handle) {
    if (shutdown(handle, SHUT_RDWR) < 0) {
        debug("shutdown failed. [%d] %s", errno, strerror(errno));
    }
    if (close(handle) < 0) {
        throwIOException(env, "Failed to close socket. [%d] %s", errno, strerror(errno));
    }
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeShutdown
  (JNIEnv *env, jobject peer, jint handle, jboolean read) {
    /* int how = read ? SHUT_RD : SHUT_WR; */
    if (shutdown(handle, SHUT_RDWR) < 0) {
        throwIOException(env, "shutdown failed. [%d] %s", errno, strerror(errno));
    }
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeUnlink
  (JNIEnv *env, jobject peer, jstring path) {
    const char* cpath;
    cpath = (*env)->GetStringUTFChars(env, path, NULL);
    if (cpath == NULL) {
        throwRuntimeException(env, "JNI error");
        return;
    }
    unlink(cpath);
    (*env)->ReleaseStringUTFChars(env, path, cpath);
}

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeAvailable
  (JNIEnv *env, jobject peer, jint handle) {
    struct pollfd fds;
    int timeout = 10; // milliseconds

    if (!validateSocket(env, handle)) {
        return -1;
    }
    memset(&fds, 0, sizeof(fds));
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

JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeRead
  (JNIEnv *env, jobject peer, jint handle, jbyteArray b, jint off, jint len) {
    jbyte *bytes;
    int done;

    if (!validateSocket(env, handle)) {
        return -1;
    }

    bytes = (*env)->GetByteArrayElements(env, b, 0);
    if (bytes == NULL) {
        throwRuntimeException(env, "Invalid argument");
        return -1;
    }
    done = 0;
    while (done == 0) {
        int flags = MSG_DONTWAIT;
        int count = recv(handle, (char *)(bytes + off + done), len - done, flags);
        if (count < 0) {
            if (errno == EAGAIN) { // Try again for non-blocking operation
                count = 0;
                debug("no data available for read");
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
                memset(&fds, 0, sizeof(fds));
                fds.fd = handle;
                fds.events = POLLIN;
                fds.revents = 0;
                //debug("poll: wait");
                int poll_rc = poll(&fds, 1, timeout);
                if (poll_rc > 0) {
                    if (fds.revents & (POLLHUP | POLLERR)) {
                        debug("Stream socket peer closed connection");
                        done = -1;
                        goto rfReadEnd;
                    } else if (fds.revents & POLLNVAL) {
                        // socket closed...
                         done = -1;
                         goto rfReadEnd;
                    } else if (fds.revents & POLLIN) {
                        debug("poll: data to read available");
                        available = true;
                    } else {
                        debug("poll: revents %i", fds.revents);
                    }
                } else if (poll_rc == -1) {
                    //Edebug("poll: call error %i", errno);
                    throwIOException(env, "Failed to poll. [%d] %s", errno, strerror(errno));
                    done = 0;
                    goto rfReadEnd;
                } else {
                    //debug("poll: call timed out");
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

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeWrite
  (JNIEnv *env, jobject peer, jint handle, jbyteArray b, jint off, jint len) {
    jbyte *bytes;
    int done;

    if (!validateSocket(env, handle)) {
        return;
    }

    bytes = (*env)->GetByteArrayElements(env, b, 0);
    if (bytes == NULL) {
        throwRuntimeException(env, "Invalid argument");
        return;
    }
    done = 0;
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

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeReadCredentials
  (JNIEnv *env, jobject peer, jint handle, jintArray b) {
    jint *rc;
    struct ucred cr;
    socklen_t cr_len;

    if (!validateSocket(env, handle)) {
        return;
    }

    cr_len = sizeof(cr);
    memset(&cr, 0, sizeof(cr));

    if ((getsockopt(handle, SOL_SOCKET, SO_PEERCRED, &cr, &cr_len) < 0) || (cr_len != sizeof (cr)))  {
        throwIOException(env, "Failed to read getsockopt. [%d] %s", errno, strerror(errno));
        return;
    }

    rc = (*env)->GetIntArrayElements(env, b, 0);
    if (rc == NULL) {
        throwRuntimeException(env, "Invalid argument");
        return;
    }
    rc[0] = cr.pid;
    rc[1] = cr.uid;
    rc[2] = cr.gid;

    (*env)->ReleaseIntArrayElements(env, b, rc, 0);
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeReadProcessCredentials
  (JNIEnv *env, jclass peerclass, jintArray b) {
    jint *rc;

    rc = (*env)->GetIntArrayElements(env, b, 0);
    if (rc == NULL) {
        throwRuntimeException(env, "Invalid argument");
        return;
    }

    rc[0] = getpid();
    rc[1] = getuid();
    rc[2] = getgid();

    (*env)->ReleaseIntArrayElements(env, b, rc, 0);
}

bool localSocketOptions2unix(int optID, int* optname) {
    switch (optID) {
        case org_bluecove_socket_LocalSocketImpl_LocalSocketOptions_SO_LINGER:
            *optname = SO_LINGER;
            break;
        case org_bluecove_socket_LocalSocketImpl_LocalSocketOptions_SO_RCVTIMEO:
            *optname = SO_RCVTIMEO;
            break;
        case org_bluecove_socket_LocalSocketImpl_LocalSocketOptions_SO_SNDTIMEO:
            *optname = SO_SNDTIMEO;
            break;
        case org_bluecove_socket_LocalSocketImpl_LocalSocketOptions_SO_SNDBUF:
            *optname = SO_SNDBUF;
            break;
        case org_bluecove_socket_LocalSocketImpl_LocalSocketOptions_SO_RCVBUF:
            *optname = SO_RCVBUF;
            break;
        case org_bluecove_socket_LocalSocketImpl_LocalSocketOptions_SO_PASSCRED:
            *optname = SO_PASSCRED;
            break;
        default:
            return false;
    }
    return true;
}

JNIEXPORT void JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeSetOption
  (JNIEnv *env, jobject peer, jint handle, jint optID, jint value) {
    int optname;
    struct linger lingr;
    struct timeval timeout;
    int rc, v;

    if (!localSocketOptions2unix(optID, &optname)) {
        throwRuntimeException(env, "Invalid argument");
        return;
    }

    switch (optname) {
        case SO_LINGER:
            lingr.l_onoff = (value > 0) ? 1 : 0;
            lingr.l_linger = value;
            rc = setsockopt(handle, SOL_SOCKET, optname, &lingr, sizeof(lingr));
            break;
        case SO_SNDTIMEO:
        case SO_RCVTIMEO:
            timeout.tv_sec = value / 1000;
            timeout.tv_usec = (value % 1000) * 1000;
            rc = setsockopt(handle, SOL_SOCKET, optname, (void *)&timeout, sizeof(timeout));
            break;
        default:
            v = value;
            rc = setsockopt(handle, SOL_SOCKET, optname, &v, sizeof(int));
            break;
    }
    if (rc != 0) {
        throwSocketException(env, "Failed to read getsockopt. [%d] %s", errno, strerror(errno));
        return;
    }
}


JNIEXPORT jint JNICALL Java_org_bluecove_socket_LocalSocketImpl_nativeGetOption
  (JNIEnv *env, jobject peer, jint handle, jint optID) {
    int optname;
    struct linger lingr;
    struct timeval timeout;
    socklen_t size, expected_size;
    int rc, value;

    if (!localSocketOptions2unix(optID, &optname)) {
        throwRuntimeException(env, "Invalid argument");
        return -1;
    }

    switch (optname) {
        case SO_LINGER:
            size = sizeof(lingr);
            expected_size = size;
            rc = getsockopt(handle, SOL_SOCKET, optname, &lingr, &size);
            if (!lingr.l_onoff) {
                value = -1;
            } else {
                value = lingr.l_linger;
            }
            break;
        case SO_SNDTIMEO:
        case SO_RCVTIMEO:
            size = sizeof(timeout);
            expected_size = size;
            rc = getsockopt(handle, SOL_SOCKET, optname, (void *)&timeout, &size);
            value = (timeout.tv_sec * 1000) + timeout.tv_usec;
            break;
        default:
            size = sizeof(int);
            expected_size = size;
            rc = getsockopt(handle, SOL_SOCKET, optname, &value, &size);
            break;
    }

    if ((rc != 0) || (expected_size != size)) {
        throwSocketException(env, "Failed to read getsockopt. [%d] %s", errno, strerror(errno));
        return -1;
    } else {
        return value;
    }
}
