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
	isClientOpen = FALSE;
	incomingConnectionCount = 0;
	sdpService = NULL;
	
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
	service_name[0] = '\0';
}

WIDCOMMStackL2CapConn::~WIDCOMMStackL2CapConn() {
	CloseHandle(hDataReceivedEvent);
	CloseHandle(hConnectionEvent);
	if (sdpService != NULL) {
		delete sdpService;
		sdpService = NULL;
	}
	l2CapIf.Deregister();
}

void WIDCOMMStackL2CapConn::OnConnected() {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2)) {
		return;
	}
	isConnected = TRUE;
	SetEvent(hConnectionEvent);
}

void WIDCOMMStackL2CapConn::closeConnection(JNIEnv *env) {
	debugs("closeConnection handle %i", internalHandle);
	Disconnect();
	if (sdpService != NULL) {
		delete sdpService;
		sdpService = NULL;
	}
	isConnected = FALSE;
	l2CapIf.Deregister();
	SetEvent(hConnectionEvent);
}

void WIDCOMMStackL2CapConn::closeServerConnection(JNIEnv *env) {
	debugs("closeServerConnection handle %i", internalHandle);
	if (isClientOpen) {
		Disconnect();
		isConnected = FALSE;
		isClientOpen = FALSE;
		SetEvent(hConnectionEvent);
	}
}

void WIDCOMMStackL2CapConn::OnIncomingConnection() {
	incomingConnectionCount ++; 
	Accept(receiveMTU);
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

void WIDCOMMStackL2CapConn::selectConnectionTransmitMTU(JNIEnv *env) {
	UINT16 remoteMtu;
	#ifdef _WIN32_WCE
	remoteMtu = GetRemoteMtu();
	#else // _WIN32_WCE
	remoteMtu = m_RemoteMtu;
    #endif // #else // _WIN32_WCE
	
	if (transmitMTU == -1) {
		connectionTransmitMTU = remoteMtu;
	} else if (transmitMTU < remoteMtu) {
		connectionTransmitMTU = transmitMTU;
	} else {
		connectionTransmitMTU = remoteMtu;
	}
	debug1("connection TransmitMTU %i", connectionTransmitMTU);
	debug1("connection  ReceiveMTU %i", receiveMTU);
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
(JNIEnv *env, jobject, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint receiveMTU, jint transmitMTU) { 
	BD_ADDR bda;
	LongToBcAddr(address, bda);

	WIDCOMMStackL2CapConn* l2c = NULL;
	EnterCriticalSection(&stack->csCRfCommIf);
	//vc6 __try {
		l2c = stack->createL2CapConn();
		if (l2c == NULL) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
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
			throwBluetoothConnectionExceptionExt(env, BT_CONNECTION_ERROR_UNKNOWN_PSM, "failed to assign PSM 0x%X", (UINT16)channel);
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
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_SECURITY_BLOCK, "Error setting security level");
			open_l2client_return 0;
        }

		//debug("OpenL2CAPClient");
		l2c->transmitMTU = (UINT16)transmitMTU;
		l2c->receiveMTU = (UINT16)receiveMTU;
		BOOL rc = l2c->Connect(l2CapIf, bda, l2c->receiveMTU);
		if (!rc) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to Connect");
			open_l2client_return 0;
		}

		//debug("waitFor Connection signal");
		DWORD waitStart = GetTickCount();
		while ((stack != NULL) && (!l2c->isConnected)) {
			DWORD  rc = WaitForSingleObject(l2c->hConnectionEvent, 500);
			if (rc == WAIT_FAILED) {
				_throwRuntimeException(env, "WaitForSingleObject");
				open_l2client_return 0;
			}
			if ((GetTickCount() - waitStart)  > COMMPORTS_CONNECT_TIMEOUT) {
				throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_TIMEOUT, "Connection timeout");
				open_l2client_return 0;
			}
		}
		if (stack == NULL) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to connect");
			open_l2client_return 0;
		}

	    debug("connected");
		l2c->selectConnectionTransmitMTU(env);
		jlong handle = l2c->internalHandle;
		l2c = NULL;
		open_l2client_return handle;
	/* vc6 } __finally {} */
}

#define open_l2server_return  open_l2server_finally(env, l2c); return

void open_l2server_finally(JNIEnv *env, WIDCOMMStackL2CapConn* l2c) {
	if ((l2c != NULL) && (stack != NULL)) {
		l2c->Disconnect();
		stack->deleteL2CapConn(l2c);
	}
	if (stack != NULL) {
		LeaveCriticalSection(&stack->csCRfCommIf);
	}
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2ServerOpenImpl
(JNIEnv *env, jobject, jbyteArray uuidValue, jboolean authenticate, jboolean encrypt, jstring name, jint receiveMTU, jint transmitMTU) {
	if (stack == NULL) {
		throwIOException(env, "Stack closed");
		return 0;
	}
	EnterCriticalSection(&stack->csCRfCommIf);
	WIDCOMMStackL2CapConn* l2c = NULL;
//VC6	__try {
		l2c = stack->createL2CapConn();
		if (l2c == NULL) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
			open_l2client_return 0;
		}
		debugs("L2CapConn server handle %i", l2c->internalHandle);
		if ((l2c->hConnectionEvent == NULL) || (l2c->hDataReceivedEvent == NULL)) {
			_throwRuntimeException(env, "fails to CreateEvent");
			open_l2client_return 0;
		}
		l2c->transmitMTU = (UINT16)transmitMTU;
		l2c->receiveMTU = (UINT16)receiveMTU;

		jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);
		convertUUIDBytesToGUID(bytes, &(l2c->service_guid));
		env->ReleaseByteArrayElements(uuidValue, bytes, 0);

		CL2CapIf *l2CapIf;
		l2CapIf = &l2c->l2CapIf;

		if (!l2CapIf->AssignPsmValue(&(l2c->service_guid))) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_UNKNOWN_PSM, "failed to assign PSM");
			open_l2server_return 0;
		}
		l2CapIf->Register();

		const char *cname = env->GetStringUTFChars(name, 0);
		UINT32 service_name_len;
		#ifdef _WIN32_WCE
			swprintf_s((wchar_t*)l2c->service_name, BT_MAX_SERVICE_NAME_LEN, L"%s", cname);
			service_name_len = wcslen((wchar_t*)l2c->service_name);
		#else // _WIN32_WCE
			sprintf_s(l2c->service_name, BT_MAX_SERVICE_NAME_LEN, "%s", cname);
			service_name_len = (UINT32)strlen(l2c->service_name);
		#endif // #else // _WIN32_WCE
		env->ReleaseStringUTFChars(name, cname);

		CSdpService *sdpService = new CSdpService();
		l2c->sdpService = sdpService;
		if (sdpService->AddServiceClassIdList(1, &(l2c->service_guid)) != SDP_OK) {
			_throwIOException(env, "Error AddServiceClassIdList");
			open_l2server_return 0;
		}
		if (sdpService->AddServiceName(l2c->service_name) != SDP_OK) {
			_throwIOException(env, "Error AddServiceName");
			open_l2server_return 0;
		}
		if (sdpService->AddL2CapProtocolDescriptor(l2CapIf->GetPsm()) != SDP_OK) {
			_throwIOException(env, "Error AddL2CapProtocolDescriptor");
			open_l2server_return 0;
		}
		if (sdpService->AddAttribute(0x0100, TEXT_STR_DESC_TYPE, service_name_len, (UINT8*)l2c->service_name) != SDP_OK) {
			_throwIOException(env, "Error AddAttribute ServiceName");
			open_l2server_return 0;
		}
		if (sdpService->MakePublicBrowseable() != SDP_OK) {
			_throwIOException(env, "Error MakePublicBrowseable");
			open_l2server_return 0;
		}

		UINT8 sec_level = BTM_SEC_NONE;
		if (authenticate) {
			sec_level = BTM_SEC_IN_AUTHENTICATE;
		}
		if (encrypt) {
			sec_level = sec_level | BTM_SEC_IN_ENCRYPT;
		}
		if (!l2CapIf->SetSecurityLevel(l2c->service_name, sec_level, TRUE)) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_SECURITY_BLOCK, "Error setting security level");
			open_l2server_return 0;
        }

		jlong handle = l2c->internalHandle;
		l2c = NULL;
		open_l2server_return handle;
/*VC6} __finally { } */
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2ServerAcceptAndOpenServerConnection
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return 0;
	}
	if (l2c->sdpService == NULL) {
		_throwIOException(env, "Connection closed");
		return 0;
	}
	if (l2c->isClientOpen) {
		debug("L2CAP server waits for client prev connection to close");
		while ((stack != NULL) && (l2c->isClientOpen) && (l2c->sdpService != NULL)) {
			DWORD  rc = WaitForSingleObject(l2c->hConnectionEvent, 500);
			if (rc == WAIT_FAILED) {
				_throwRuntimeException(env, "WaitForSingleObject");
				return 0;
			}
		}
		if ((stack == NULL) || (l2c->sdpService == NULL)) {
			_throwIOException(env, "Connection closed");
			return 0;
		}
	}

	l2c->isConnected = FALSE;
	l2c->receiveBuffer.reset();

	BOOL rc = l2c->Listen(&(l2c->l2CapIf));
	if (!rc) {
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to Listen");
		return 0;
	}
	debug("L2CAP server waits for connection");
	long incomingConnectionCountWas = l2c->incomingConnectionCount;
	while ((stack != NULL) && (!l2c->isConnected) && (l2c->sdpService != NULL)) {
		DWORD  rc = WaitForSingleObject(l2c->hConnectionEvent, 500);
		if (rc == WAIT_FAILED) {
			_throwRuntimeException(env, "WaitForSingleObject");
			return 0;
		}
		if ((stack != NULL) && (incomingConnectionCountWas != l2c->incomingConnectionCount)) {
			debugs("L2CAP server incomingConnectionCount %i", l2c->incomingConnectionCount);
			incomingConnectionCountWas = l2c->incomingConnectionCount;
		}
	}
	if ((stack == NULL) || (l2c->sdpService == NULL)) {
		_throwIOException(env, "Connection closed");
		return 0;
	}

	debug("L2CAP server connection made");
	l2c->selectConnectionTransmitMTU(env);
	l2c->isClientOpen = TRUE;
	return l2c->internalHandle;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2ServerPSM
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return -1;
	}
	return l2c->l2CapIf.GetPsm();
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

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2CloseServerConnection
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return;
	}
	l2c->closeServerConnection(env);
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2GetReceiveMTU
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return 0;
	}
	return l2c->receiveMTU;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2GetTransmitMTU
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return 0;
	}
	return l2c->connectionTransmitMTU;
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2RemoteAddress
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return 0;
	}
	if (!l2c->isConnected ) {
		_throwIOException(env, "connection is closed");
		return 0;
	}
	#ifdef _WIN32_WCE
		BD_ADDR connected_bd_addr;
		l2c->GetRemoteBdAddr(connected_bd_addr);
		return BcAddrToLong(connected_bd_addr);
	#else // _WIN32_WCE
	  return BcAddrToLong(l2c->m_RemoteBdAddr);
    #endif // #else // _WIN32_WCE
}

JNIEXPORT jboolean JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2Ready
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return JNI_FALSE;
	}
	if (l2c->receiveBuffer.available() > sizeof(UINT16)) {
		return JNI_TRUE;
	}
	if (!l2c->isConnected) {
		debug("->l2Ready()");
		_throwIOException(env, "connection is closed");
		return JNI_FALSE;
	}
	return JNI_FALSE;
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2Receive
(JNIEnv *env, jobject, jlong handle, jbyteArray inBuf) {
	debug("->receive(byte[])");
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return 0;
	}
	if ((!l2c->isConnected ) && (l2c->receiveBuffer.available() < sizeof(UINT16))) {
		_throwIOException(env, "Failed to read from closed connection");
		return 0;
	}
	if (l2c->receiveBuffer.isOverflown()) {
		_throwIOException(env, "Receive buffer overflown");
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
			_throwRuntimeException(env, "WaitForMultipleObjects");
			return 0;
		}
		debug1("receive[] waits returns %s", waitResultsString(rc));
	}
	if ((stack == NULL) || ((!l2c->isConnected) && (l2c->receiveBuffer.available() <= paketLengthSize)) ) {
		_throwIOException(env, "Connection closed");
		return 0;
	}

	int count = l2c->receiveBuffer.available();
	if (count < paketLengthSize) {
		_throwIOException(env, "Receive buffer corrupted (1)");
		return 0;
	}
	UINT16 paketLength = 0;
	int done = l2c->receiveBuffer.read(&paketLength, paketLengthSize);
	if ((done != paketLengthSize) || (paketLength > (count - paketLengthSize))) {
		_throwIOException(env, "Receive buffer corrupted (2)");
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
	if (readLen > l2c->receiveMTU) {
		readLen = l2c->receiveMTU;
	}

	done = l2c->receiveBuffer.read(bytes, readLen);
	if (done != readLen) {
		_throwIOException(env, "Receive buffer corrupted (3)");
	}
	if (done < paketLength) {
		// the rest will be discarded. 
		int skip = paketLength - done;
		if (skip != l2c->receiveBuffer.skip(skip)) {
			_throwIOException(env, "Receive buffer corrupted (4)");
		}
	}

	env->ReleaseByteArrayElements(inBuf, bytes, 0);
	debug1("receive[] returns %i", done);
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
		_throwIOException(env, "Failed to write to closed connection");
		return;
	}
	jbyte *bytes = env->GetByteArrayElements(data, 0);
	UINT16 len = (UINT16)env->GetArrayLength(data);
		
	if (len > l2c->connectionTransmitMTU) {
		len = l2c->connectionTransmitMTU;
	}

	UINT16 written = 0;
	BOOL rc = l2c->Write((void*)bytes, (UINT16)len, &written);
	if (!rc) {
		env->ReleaseByteArrayElements(data, bytes, 0);
		_throwIOException(env, "Failed to write");
		return;
	}
	debugs("sent %i", written);
	if (written < len) {
		debug("throw");
		throwIOExceptionExt(env, "Failed to write all data, send %i from %i", written, len);
	}

	env->ReleaseByteArrayElements(data, bytes, 0);
}

#endif //  _BTWLIB