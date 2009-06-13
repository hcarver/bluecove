/**
 * BlueCove BlueZ module - Java library for Bluetooth on Linux
 *  Copyright (C) 2008 Mina Shokry
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
#define CPP__FILE "BlueCoveBlueZ_LocalDevice.c"

#include "BlueCoveBlueZ.h"

#include <bluetooth/hci.h>
#include <bluetooth/hci_lib.h>
#include <sys/ioctl.h>

JNIEXPORT jintArray JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_getLocalDevicesID
(JNIEnv *env, jobject peer) {
    int s = socket(AF_BLUETOOTH, SOCK_RAW, BTPROTO_HCI);
    if (s < 0) {
        return NULL;
    }

    struct hci_dev_list_req *dl;
    struct hci_dev_req *dr;
    dl = (struct hci_dev_list_req*)malloc(HCI_MAX_DEV * sizeof(*dr) + sizeof(*dl));
    if (!dl) {
        close(s);
        throwRuntimeException(env, cOUT_OF_MEMORY);
        return NULL;
    }
    dl->dev_num = HCI_MAX_DEV;
    if (ioctl(s, HCIGETDEVLIST, dl) < 0) {
        free(dl);
        close(s);
        return NULL;
    }
    int flag = HCI_UP;
    int i;
    int count = 0;
    dr = dl->dev_req;
    for (i = 0; i < dl->dev_num; i++, dr++) {
        if (hci_test_bit(flag, &dr->dev_opt)) {
            count ++;
        }
    }
    jintArray result = NULL;
    int k = 0;
    jint *ints;
    result = (*env)->NewIntArray(env, count);
    if (result == NULL) {
        free(dl);
        close(s);
        return NULL;
    }
    ints = (*env)->GetIntArrayElements(env, result, 0);
    if (ints == NULL) {
        free(dl);
        close(s);
        return NULL;
    }
    dr = dl->dev_req;
    for (i = 0; i < dl->dev_num; i++, dr++) {
        if (hci_test_bit(flag, &dr->dev_opt)) {
            ints[k] = dr->dev_id;
            k ++;
        }
    }
    (*env)->ReleaseIntArrayElements(env, result, ints, 0);
    free(dl);
    close(s);
    return result;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceID
(JNIEnv *env, jobject peer, jint findNumber, jint findBlueZDeviceID, jlong findLocalDeviceBTAddress) {
    bool findDevice = (findNumber >= 0) || (findLocalDeviceBTAddress > 0) || (findBlueZDeviceID >=0);
    if (findDevice) {
        int s = socket(AF_BLUETOOTH, SOCK_RAW, BTPROTO_HCI);
        if (s < 0) {
            throwBluetoothStateException(env, "Failed to create Bluetooth socket. [%d] %s", errno, strerror(errno));
            return 0;
        }
        struct hci_dev_list_req *dl;
        struct hci_dev_req *dr;
        dl = (struct hci_dev_list_req*)malloc(HCI_MAX_DEV * sizeof(*dr) + sizeof(*dl));
        if (!dl) {
            close(s);
            throwRuntimeException(env, cOUT_OF_MEMORY);
            return 0;
        }
        dl->dev_num = HCI_MAX_DEV;
        dr = dl->dev_req;
        if (ioctl(s, HCIGETDEVLIST, dl) < 0) {
            free(dl);
            close(s);
            throwBluetoothStateException(env, "Failed to list Bluetooth devices. [%d] %s", errno, strerror(errno));
            return 0;
        }
        int dev_id = -1;
        int flag = HCI_UP;
        int i;
        for (i = 0; i < dl->dev_num; i++, dr++) {
            if (hci_test_bit(flag, &dr->dev_opt)) {
                if (findNumber == i) {
                    dev_id = dr->dev_id;
                    break;
                }
                if (findBlueZDeviceID == dr->dev_id) {
                    dev_id = dr->dev_id;
                    break;
                }
                if (findLocalDeviceBTAddress > 0) {
                    // Select device by address
                    int dd = hci_open_dev(dr->dev_id);
                    if (dd >= 0) {
                        bdaddr_t address;
                        hci_read_bd_addr(dd, &address, 1000);
                        hci_close_dev(dd);
                        if (deviceAddrToLong(&address) == findLocalDeviceBTAddress) {
                            dev_id = dr->dev_id;
                            break;
                        }
                    }
                }
            }
        }

        free(dl);
        close(s);
        if (dev_id < 0) {
            if (findNumber >= 0) {
                throwBluetoothStateException(env, "Bluetooth Device %i not found", findNumber);
            } else if (findBlueZDeviceID >=0) {
                throwBluetoothStateException(env, "Bluetooth BlueZ Device %i not found", findBlueZDeviceID);
            } else {
                throwBluetoothStateException(env, "Bluetooth Device %X not found", findLocalDeviceBTAddress);
            }
        }
        return dev_id;
    } else {
        int dev_id = hci_get_route(NULL);
        if (dev_id < 0) {
            debug("hci_get_route : %i", dev_id);
            throwBluetoothStateException(env, "Bluetooth Device is not available");
            return 0;
        } else {
            return dev_id;
        }
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeOpenDevice
(JNIEnv *env, jobject peer, jint deviceID) {
    int deviceDescriptor = hci_open_dev(deviceID);
    if (deviceDescriptor < 0) {
        debug("hci_open_dev : %i", deviceDescriptor);
        throwBluetoothStateException(env, "HCI device open failed");
        return 0;
    }
    return deviceDescriptor;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeCloseDevice
(JNIEnv *env, jobject peer, jint deviceDescriptor) {
    hci_close_dev(deviceDescriptor);
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_getLocalDeviceBluetoothAddressImpl
(JNIEnv *env, jobject peer, jint deviceDescriptor) {
    bdaddr_t address;
    int error = hci_read_bd_addr(deviceDescriptor, &address, LOCALDEVICE_ACCESS_TIMEOUT);
    if (error != 0) {
        switch (error) {
        case HCI_HARDWARE_FAILURE:
            throwBluetoothStateException(env, "Bluetooth Device is not available");
        default:
            throwBluetoothStateException(env, "Bluetooth Device is not ready. [%d] %s", errno, strerror(errno));
        }
        return 0;
    }
    return deviceAddrToLong(&address);
}

JNIEXPORT jstring JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceName
(JNIEnv *env, jobject peer, jint deviceDescriptor) {
    char* name = (char*)malloc(DEVICE_NAME_MAX_SIZE);
    jstring nameString = NULL;
    if (!hci_read_local_name(deviceDescriptor, 100, name, LOCALDEVICE_ACCESS_TIMEOUT)) {
        nameString = (*env)->NewStringUTF(env, name);
    }
    free(name);
    return nameString;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetDeviceClass
(JNIEnv *env, jobject peer, jint deviceDescriptor) {
    uint8_t deviceClass[3];
    if (!hci_read_class_of_dev(deviceDescriptor, deviceClass, LOCALDEVICE_ACCESS_TIMEOUT)) {
        return deviceClassBytesToInt(deviceClass);
    } else {
        return 0xff000000;
    }
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeSetLocalDeviceDiscoverable
(JNIEnv *env, jobject peer, jint deviceDescriptor, jint mode) {

    uint8_t scan_enable = SCAN_PAGE;
    if ((mode == GIAC) || (mode == LIAC)) {
        scan_enable = (SCAN_PAGE | SCAN_INQUIRY);
    }

    struct hci_request rq;
    uint8_t status = 0;

    memset(&rq, 0, sizeof(rq));
    rq.ogf    = OGF_HOST_CTL;
    rq.ocf    = OCF_WRITE_SCAN_ENABLE;
    rq.cparam = &scan_enable;
    rq.clen   = sizeof(scan_enable);
    rq.rparam = &status;
    rq.rlen   = sizeof(status);
    rq.event = EVT_CMD_COMPLETE;
    if (hci_send_req(deviceDescriptor, &rq, LOCALDEVICE_ACCESS_TIMEOUT) < 0) {
        throwBluetoothStateException(env, "Bluetooth Device is not ready. [%d] %s", errno, strerror(errno));
        return -1;
    }

    uint8_t lap[3];
    lap[0] = mode & 0xff;
    lap[1] = (mode & 0xff00) >> 8;
    lap[2] = (mode & 0xff0000) >> 16;

    return hci_write_current_iac_lap(deviceDescriptor, 1, lap, LOCALDEVICE_ACCESS_TIMEOUT);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackBlueZ_nativeGetLocalDeviceDiscoverable
(JNIEnv *env, jobject peer, jint deviceDescriptor) {
    uint8_t lap[3  * MAX_IAC_LAP];
    uint8_t num_iac = 1;
    read_scan_enable_rp rp;
    struct hci_request rq;
    int error;

    memset(&rq, 0, sizeof(rq));
    rq.ogf    = OGF_HOST_CTL;
    rq.ocf    = OCF_READ_SCAN_ENABLE;
    rq.rparam = &rp;
    rq.rlen   = READ_SCAN_ENABLE_RP_SIZE;
    if ((hci_send_req(deviceDescriptor, &rq, LOCALDEVICE_ACCESS_TIMEOUT) < 0) || (rp.status)) {
        throwRuntimeException(env, "Unable to retrieve the local scan mode.");
        return 0;
    }
    if ((rp.enable & SCAN_INQUIRY) == 0) {
        return NOT_DISCOVERABLE;
    }

    error = hci_read_current_iac_lap(deviceDescriptor, &num_iac, lap, LOCALDEVICE_ACCESS_TIMEOUT);
    //M.S.  I don't know why to check for num_iac to be less than or equal to one but avetana to this.
    //---------------------------
    // We don't know a good reason for checking num_iac to be <= 1 and it seems it takes values
    // greater than 1 with BlueZ 4.x and it works without this check.
    if ((error < 0) /*|| (num_iac > 1)*/) {
        throwRuntimeException(env, "Unable to retrieve the local discovery mode.");
        return 0;
    }
    return (lap[0] & 0xff) | ((lap[1] & 0xff) << 8) | ((lap[2] & 0xff) << 16);
}


