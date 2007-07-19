/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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

#include "WIDCOMMStack.h"

#ifdef _BTWLIB

#ifdef VC6
#define CPP_FILE "WIDCOMMStackL2CAP.cpp"
#endif

WIDCOMMStackL2CapConn::WIDCOMMStackL2CapConn() {
	isConnected = FALSE;
	
	hConnectionEvent = CreateEvent(
            NULL,     // no security attributes
            FALSE,     // auto-reset event
            FALSE,    // initial state is NOT signaled
            NULL);    // object not named
    
	hDataReceivedEvent = CreateEvent(
            NULL,     // no security attributes
            FALSE,     // auto-reset event
            FALSE,     // initial state is NOT signaled
            NULL);    // object not named
	
	memset(&service_guid, 0, sizeof(GUID));
}

WIDCOMMStackL2CapConn::~WIDCOMMStackL2CapConn() {
	CloseHandle(hDataReceivedEvent);
	CloseHandle(hConnectionEvent);
}

void WIDCOMMStackL2CapConn::OnConnected() {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2)) {
		return;
	}
	isConnected = TRUE;
	SetEvent(hConnectionEvent);
}

void WIDCOMMStackL2CapConn::closeConnection(JNIEnv *env) {
	isConnected = FALSE;
	Disconnect();
	l2CapIf.Deregister();
	SetEvent(hConnectionEvent);
}

void WIDCOMMStackL2CapConn::OnDataReceived(void *p_data, UINT16 length) {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2) || (!isConnected)) {
		return;
	}
	receiveBuffer.write(&length, sizeof(UINT16));
	receiveBuffer.write(p_data, length);
	SetEvent(hDataReceivedEvent);
}

void WIDCOMMStackL2CapConn::OnRemoteDisconnected(UINT16 reason) {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2) || (!isConnected)) {
		return;
	}
	isConnected = FALSE;
	SetEvent(hConnectionEvent);
}

#define open_l2client_return  open_l2client_finally(l2c); return

void open_l2client_finally(WIDCOMMStackL2CapConn* l2c) {
	if ((l2c != NULL) && (stack != NULL)) {
		// Just in case Close
		l2c->Disconnect();
		stack->deleteL2CapConn(l2c);
	}
	if (stack != NULL) {
		LeaveCriticalSection(&stack->csCRfCommIf);
	}
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2OpenClientConnectionImpl
(JNIEnv *env, jobject, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint mtu) { 
	BD_ADDR bda;
	LongToBcAddr(address, bda);

	WIDCOMMStackL2CapConn* l2c = NULL;
	EnterCriticalSection(&stack->csCRfCommIf);
	//vc6 __try {
		l2c = stack->createL2CapConn();
		if (l2c == NULL) {
			throwIOException(env, "No free connections Objects in Pool");
			open_l2client_return 0;
		}
		debugs("L2CapConn handle %i", l2c->internalHandle);
		if ((l2c->hConnectionEvent == NULL) || (l2c->hDataReceivedEvent == NULL)) {
			throwRuntimeException(env, "fails to CreateEvent");
			open_l2client_return 0;
		}
		debugs("L2CapConn channel 0x%X", channel);
		//debug("AssignPsmValue");
		// What GUID do we need in call to CL2CapIf.AssignPsmValue() if we don't have any?
		//GUID test_client_service_guid = {2970356705 , 4369, 4369, {17 , 17, 17 , 17, 17, 17 , 0, 1}};
		//memcpy(&(l2c->service_guid), &test_client_service_guid, sizeof(GUID));
		CL2CapIf *l2CapIf;
		l2CapIf = &l2c->l2CapIf;
		//l2CapIf = new CL2CapIf();

		if (!l2CapIf->AssignPsmValue(&(l2c->service_guid), (UINT16)channel)) {
			throwIOExceptionExt(env, "failed to assign PSM 0x%X", (UINT16)channel);
			open_l2client_return 0;
		}
		l2CapIf->Register();

		UINT8 sec_level = BTM_SEC_NONE;
		if (authenticate) {
			sec_level = BTM_SEC_OUT_AUTHENTICATE;
		}
		if (encrypt) {
			sec_level = sec_level | BTM_SEC_OUT_ENCRYPT;
		}

		BT_CHAR *p_service_name;
		#ifdef _WIN32_WCE
			p_service_name = (BT_CHAR*)L"bluecovesrv";
		#else // _WIN32_WCE
			p_service_name = "bluecovesrv";
		#endif // #else // _WIN32_WCE

		if (!l2CapIf->SetSecurityLevel(p_service_name, sec_level, FALSE)) {
			throwIOException(env, "Error setting security level");
			open_l2client_return 0;
        }

		//debug("OpenL2CAPClient");
		l2c->mtu = (UINT16)mtu;
		BOOL rc = l2c->Connect(l2CapIf, bda, l2c->mtu);
		if (!rc) {
			throwIOException(env, "Failed to Connect");
			open_l2client_return 0;
		}

		//debug("waitFor Connection signal");
		DWORD waitStart = GetTickCount();
		while ((stack != NULL) && (!l2c->isConnected)) {
			DWORD  rc = WaitForSingleObject(l2c->hConnectionEvent, 500);
			if (rc == WAIT_FAILED) {
				throwRuntimeException(env, "WaitForSingleObject");
				open_l2client_return 0;
			}
			if ((GetTickCount() - waitStart)  > COMMPORTS_CONNECT_TIMEOUT) {
				throwIOException(env, "Connection timeout");
				open_l2client_return 0;
			}
		}
		if (stack == NULL) {
			throwIOException(env, "Failed to connect");
			open_l2client_return 0;
		}

	    debug("connected");
		jlong handle = l2c->internalHandle;
		l2c = NULL;
		open_l2client_return handle;
	/* vc6 } __finally {} */
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2CloseClientConnection
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return;
	}
	l2c->closeConnection(env);
	if (stack != NULL) {
		stack->deleteL2CapConn(l2c);
	}
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2GetMTUImpl
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return 0;
	}
	return l2c->mtu;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2RemoteAddress
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return 0;
	}
	if (!l2c->isConnected ) {
		throwIOException(env, "connection is closed");
		return 0;
	}
	return BcAddrToLong(l2c->m_RemoteBdAddr);
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2Ready
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return JNI_FALSE;
	}
	if (!l2c->isConnected ) {
		throwIOException(env, "connection is closed");
		return JNI_FALSE;
	}
	return (l2c->receiveBuffer.available() > sizeof(UINT16));
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2Receive
(JNIEnv *env, jobject, jlong handle, jbyteArray inBuf) {
	debug("->receive(byte[])");
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return 0;
	}
	if (!l2c->isConnected ) {
		throwIOException(env, "Failed to read from closed connection");
		return 0;
	}
	if (l2c->receiveBuffer.isOverflown()) {
		throwIOException(env, "Receive buffer overflown");
		return 0;
	}
	
	HANDLE hEvents[2];
	hEvents[0] = l2c->hConnectionEvent;
	hEvents[1] = l2c->hDataReceivedEvent;

	int paketLengthSize = sizeof(UINT16);

	while ((stack != NULL) && l2c->isConnected  && (l2c->receiveBuffer.available() <= paketLengthSize)) {
		debug("receive[] waits for data");
		DWORD  rc = WaitForMultipleObjects(2, hEvents, FALSE, INFINITE);
		if (rc == WAIT_FAILED) {
			throwRuntimeException(env, "WaitForMultipleObjects");
			return 0;
		}
		debug1("receive waits returns %s", waitResultsString(rc));
	}
	if ((stack == NULL) || (!l2c->isConnected)) {
		throwIOException(env, "Connection closed");
		return 0;
	}

	int count = l2c->receiveBuffer.available();
	if (count < paketLengthSize) {
		throwIOException(env, "Receive buffer corrupted (1)");
		return 0;
	}
	UINT16 paketLength = 0;
	int done = l2c->receiveBuffer.read(&paketLength, paketLengthSize);
	if ((done != paketLengthSize) || (paketLength > (count - paketLengthSize))) {
		throwIOException(env, "Receive buffer corrupted (2)");
		return 0;
	}
	if (paketLength == 0) {
		return 0;
	}

	jbyte *bytes = env->GetByteArrayElements(inBuf, 0);
	UINT16 inBufLen = (UINT16)env->GetArrayLength(inBuf);

	int readLen = paketLength;
	if (readLen > inBufLen) {
		readLen = inBufLen;
	}
	done = l2c->receiveBuffer.read(bytes, readLen);
	if (done != readLen) {
		throwIOException(env, "Receive buffer corrupted (3)");
	}
	if (done < paketLength) {
		// the rest will be discarded. 
		int skip = paketLength - done;
		if (skip != l2c->receiveBuffer.skip(skip)) {
			throwIOException(env, "Receive buffer corrupted (4)");
		}
	}

	env->ReleaseByteArrayElements(inBuf, bytes, 0);
	return done;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2Send
(JNIEnv *env, jobject, jlong handle, jbyteArray data) {
	debug("->send(byte[])");
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return;
	}
	if (!l2c->isConnected ) {
		throwIOException(env, "Failed to write to closed connection");
		return;
	}
	jbyte *bytes = env->GetByteArrayElements(data, 0);
	UINT16 len = (UINT16)env->GetArrayLength(data);
	UINT16 done = 0;

	while ((done < len) && l2c->isConnected) {
		UINT16 written = 0;
		BOOL rc = l2c->Write((void*)(bytes + done), (UINT16)(len - done), &written);
		if (!rc) {
			env->ReleaseByteArrayElements(data, bytes, 0);
			throwIOException(env, "Failed to write");
			return;
		}
		done += written;
	}

	env->ReleaseByteArrayElements(data, bytes, 0);
}

#endif //  _BTWLIB