/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2007-2008 Vlad Skarzhevskyy
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * @version $Id$
 */
#define CPP__FILE "BlueCoveBlueZ_Discovery.c"

#include "BlueCoveBlueZ.h"

#include <bluetooth/hci.h>
#include <sys/ioctl.h>

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_runDeviceInquiryImpl
(JNIEnv *env, jobject peer, jobject inquiryRunnable, jobject startedNotify, jint deviceID, jint deviceDescriptor, jint accessCode, jint inquiryLength, jint maxResponses, jobject listener) {
    struct DeviceInquiryCallback callback;
    DeviceInquiryCallback_Init(&callback);
    if (!DeviceInquiryCallback_builDeviceInquiryCallbacks(env, &callback, inquiryRunnable, startedNotify)) {
        return INQUIRY_ERROR;
    }
    if (!DeviceInquiryCallback_callDeviceInquiryStartedCallback(env, &callback)) {
        return INQUIRY_ERROR;
    }
    int max_rsp = maxResponses;
    inquiry_info *ii = NULL;
    int num_rsp = hci_inquiry(deviceID, inquiryLength, max_rsp, NULL, &ii, accessCode);
    int rc = INQUIRY_COMPLETED;
    if (num_rsp < 0) {
        rc = INQUIRY_ERROR;
    } else {
        int i;
        for(i = 0; i < num_rsp; i++) {
            bdaddr_t* address = &(ii+i)->bdaddr;
            jlong addressLong = deviceAddrToLong(address);
            uint8_t *dev_class = (ii+i)->dev_class;
            int deviceClass = deviceClassBytesToInt(dev_class);

            jboolean paired = false; // TODO

            jstring name = NULL; // Names are stored in RemoteDeviceHelper and can be reused.

            if (!DeviceInquiryCallback_callDeviceDiscovered(env, &callback, listener, addressLong, deviceClass, name, paired)) {
                rc = INQUIRY_ERROR;
                break;
            }
        }
    }
    free(ii);
    return rc;
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_deviceInquiryCancelImpl
(JNIEnv *env, jobject peer, jint deviceDescriptor) {
    int err = hci_send_cmd(deviceDescriptor, OGF_LINK_CTL, OCF_INQUIRY_CANCEL, 0, NULL);
    return (err == 0);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_getRemoteDeviceFriendlyNameImpl
(JNIEnv *env, jobject peer, jint deviceDescriptor, jlong remoteAddress) {
    bdaddr_t address;
    longToDeviceAddr(remoteAddress, &address);
    char name[DEVICE_NAME_MAX_SIZE];
    int error = hci_read_remote_name(deviceDescriptor, &address, sizeof(name), name, READ_REMOTE_NAME_TIMEOUT);
    if (error < 0) {
        throwIOException(env, "Can not get remote device name");
        return NULL;
    }
    return (*env)->NewStringUTF(env, name);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_getRemoteDeviceVersionInfoImpl
  (JNIEnv *env, jobject peer, jint deviceDescriptor, jlong remoteDeviceAddressLong) {
    struct hci_conn_info_req *conn_info;
    struct hci_version ver;
    char info[256];

    conn_info = (struct hci_conn_info_req*)malloc(sizeof(*conn_info) + sizeof(struct hci_conn_info));
    if (!conn_info) {
        throwRuntimeException(env, cOUT_OF_MEMORY);
        return NULL;
    }
    memset(conn_info, 0, sizeof(struct hci_conn_info));
    longToDeviceAddr(remoteDeviceAddressLong, &(conn_info->bdaddr));

    conn_info->type = ACL_LINK;
    if (ioctl((int)deviceDescriptor, HCIGETCONNINFO, (unsigned long) conn_info) < 0) {
        free(conn_info);
        throwRuntimeException(env, "Fail to get connection info");
        return NULL;
    }

    int error = hci_read_remote_version((int)deviceDescriptor, conn_info->conn_info->handle, &ver, READ_REMOTE_NAME_TIMEOUT);
    if (error < 0) {
        throwRuntimeException(env, "Can not get remote device info");
        free(conn_info);
        return NULL;
    }
    snprintf(info, 256, "manufacturer=%i,lmp_version=%i,lmp_sub_version=%i", ver.manufacturer, ver.lmp_ver, ver.lmp_subver);
    free(conn_info);
    return (*env)->NewStringUTF(env, info);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_getRemoteDeviceRSSIImpl
  (JNIEnv *env, jobject peer, jint deviceDescriptor, jlong remoteDeviceAddressLong) {
    struct hci_request rq;
    struct hci_conn_info_req *conn_info;
    read_rssi_rp rssi_rp;

    conn_info = (struct hci_conn_info_req*)malloc(sizeof(*conn_info) + sizeof(struct hci_conn_info));
    if (!conn_info) {
        throwRuntimeException(env, cOUT_OF_MEMORY);
        return -1;
    }
    memset(conn_info, 0, sizeof(struct hci_conn_info));
    longToDeviceAddr(remoteDeviceAddressLong, &(conn_info->bdaddr));

    conn_info->type = ACL_LINK;
    if (ioctl((int)deviceDescriptor, HCIGETCONNINFO, (unsigned long) conn_info) < 0) {
        free(conn_info);
        throwRuntimeException(env, "Fail to get connection info");
        return -1;
    }

    memset(&rq, 0, sizeof(rq));
    rq.ogf    = OGF_STATUS_PARAM;
    rq.ocf    = OCF_READ_RSSI;
    rq.cparam = &conn_info->conn_info->handle;
    rq.clen   = 2;
    rq.rparam = &rssi_rp;
    rq.rlen   = READ_RSSI_RP_SIZE;

    if ((hci_send_req((int)deviceDescriptor, &rq, READ_REMOTE_NAME_TIMEOUT) < 0) || rssi_rp.status) {
        free(conn_info);
        throwRuntimeException(env, "Fail to send hci request");
        return -1;
    }
    free(conn_info);
    return rssi_rp.rssi;
}
