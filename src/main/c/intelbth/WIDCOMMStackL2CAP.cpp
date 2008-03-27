/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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

int incomingL2CAPConnectionCount = 0;

WIDCOMMStackL2CapConn::WIDCOMMStackL2CapConn() {
    isDisconnected = FALSE;
    pL2CapIf = NULL;
	hDataReceivedEvent = CreateEvent(
            NULL,     // no security attributes
            FALSE,     // auto-reset event
            FALSE,     // initial state is NOT signaled
            NULL);    // object not named
}

WIDCOMMStackL2CapConn::~WIDCOMMStackL2CapConn() {
	CloseHandle(hDataReceivedEvent);
	if (pL2CapIf != NULL) {
	    delete(pL2CapIf);
	    pL2CapIf = NULL;
	}
}

void WIDCOMMStackL2CapConn::OnConnected() {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2) || (!isValidStackObject(this))) {
	    ndebug(("e.l2(%i) l2OnConnected for invlaid object", internalHandle));
		return;
	}
	ndebug(("l2(%i) l2OnConnected", internalHandle));
	isConnected = TRUE;
	SetEvent(hConnectionEvent);
}

void WIDCOMMStackL2CapConn::close(JNIEnv *env, BOOL allowExceptions) {
	debug(("l2(%i) l2CloseConnection handle", internalHandle));
	Disconnect();
	if (pL2CapIf != NULL) {
        pL2CapIf->Deregister();
    }
	isConnected = FALSE;
	SetEvent(hConnectionEvent);
}

void WIDCOMMStackL2CapConn::OnIncomingConnection() {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2) || (!isValidStackObject(this))) {
	    ndebug(("e.l2(%i) l2OnIncomingConnection for invlaid object", internalHandle));
		return;
	}
	incomingL2CAPConnectionCount ++;
	ndebug(("l2(%i) l2OnIncomingConnection", internalHandle));
	if (Accept(receiveMTU)) {
	    isConnected = TRUE;
	} else {
	    isDisconnected = TRUE;
	}
	SetEvent(hConnectionEvent);
}

void WIDCOMMStackL2CapConn::OnDataReceived(void *p_data, UINT16 length) {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2) || (!isConnected) || (!isValidStackObject(this))) {
	    ndebug(("e.l2(%i) l2OnDataReceived for invlaid object", internalHandle));
		return;
	}
	receiveBuffer.write_with_len(p_data, length);
	SetEvent(hDataReceivedEvent);
}

void WIDCOMMStackL2CapConn::OnRemoteDisconnected(UINT16 reason) {
	if ((magic1 != MAGIC_1) || (magic2 != MAGIC_2) || (!isConnected) || (!isValidStackObject(this))) {
	    ndebug(("e.l2(%i) l2OnRemoteDisconnected for invlaid object", internalHandle));
		return;
	}
	ndebug(("l2(%i) l2OnRemoteDisconnected", internalHandle));
	isDisconnected = TRUE;
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
	debug(("connection TransmitMTU %i", connectionTransmitMTU));
	debug(("connection  ReceiveMTU %i", receiveMTU));
}

#define open_l2client_return  open_l2client_finally(l2c); return

void open_l2client_finally(WIDCOMMStackL2CapConn* l2c) {
	if ((l2c != NULL) && (stack != NULL)) {
		// Just in case Close
		l2c->Disconnect();
		stack->deleteConnection(l2c);
	}
	if (stack != NULL) {
		LeaveCriticalSection(&stack->csCommIf);
	}
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2OpenClientConnectionImpl
(JNIEnv *env, jobject peer, jlong address, jint channel, jboolean authenticate, jboolean encrypt, jint receiveMTU, jint transmitMTU, jint timeout) {
	BD_ADDR bda;
	LongToBcAddr(address, bda);

	WIDCOMMStackL2CapConn* l2c = NULL;
	EnterCriticalSection(&stack->csCommIf);
	//vc6 __try {
		l2c = stack->createL2CapConn();
		if (l2c == NULL) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
			open_l2client_return 0;
		}
		debug(("l2(%i) L2CapConn handle created", l2c->internalHandle));
		if ((l2c->hConnectionEvent == NULL) || (l2c->hDataReceivedEvent == NULL)) {
			throwRuntimeException(env, "fails to CreateEvent");
			open_l2client_return 0;
		}
		debug(("L2CapConn channel 0x%X", channel));
		//debug(("AssignPsmValue");

		CL2CapIf *l2CapIf = new CL2CapIf();
		l2c->pL2CapIf = l2CapIf;

		if (!l2CapIf->AssignPsmValue(&(l2c->service_guid), (UINT16)channel)) {
		    // What GUID do we need in call to CL2CapIf.AssignPsmValue() if we don't have any?
		    // NEED This for stack version 3.0.1.905
		    // TODO test on  v5.0.1.2800 and v4.0.1.2900
		    GUID any_client_service_guid = {2970356705 , 4369, 4369, {17 , 17, 17 , 17, 17, 17 , 0, 1}};
		    memcpy(&(l2c->service_guid), &any_client_service_guid, sizeof(GUID));

			if (!l2CapIf->AssignPsmValue(&(l2c->service_guid), (UINT16)channel)) {
			    throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_UNKNOWN_PSM, "failed to assign PSM 0x%X", (UINT16)channel);
			    open_l2client_return 0;
			}
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

		//debug(("OpenL2CAPClient"));
		l2c->transmitMTU = (UINT16)transmitMTU;
		l2c->receiveMTU = (UINT16)receiveMTU;
		BOOL rc = l2c->Connect(l2CapIf, bda, l2c->receiveMTU);
		if (!rc) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to Connect");
			open_l2client_return 0;
		}

		//debug(("waitFor Connection signal");
		DWORD waitStart = GetTickCount();
		while ((stack != NULL) && (!l2c->isConnected) && (!l2c->isDisconnected)) {
			DWORD  rc = WaitForSingleObject(l2c->hConnectionEvent, 500);
			if (rc == WAIT_FAILED) {
				throwRuntimeException(env, "WaitForSingleObject");
				open_l2client_return 0;
			}
			if (isCurrentThreadInterrupted(env, peer)) {
			    debug(("Interrupted while writing"));
			    open_l2client_return 0;
		    }
			if ((timeout > 0) && ((GetTickCount() - waitStart)  > (DWORD)timeout)) {
				throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_TIMEOUT, "Connection timeout");
				open_l2client_return 0;
			}
		}
		if (stack == NULL) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to connect");
			open_l2client_return 0;
		}
		if (!l2c->isConnected) {
		    throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to connect");
		    open_l2client_return 0;
		}

	    debug(("l2(%i) l2Client connected", l2c->internalHandle));
		l2c->selectConnectionTransmitMTU(env);
		jlong handle = l2c->internalHandle;
		l2c = NULL;
		open_l2client_return handle;
	/* vc6 } __finally {} */
}

WIDCOMMStackL2CapServer::WIDCOMMStackL2CapServer() {
}

WIDCOMMStackL2CapServer::~WIDCOMMStackL2CapServer() {
}

void WIDCOMMStackL2CapServer::close(JNIEnv *env, BOOL allowExceptions) {
    WIDCOMMStackServerConnectionBase::close(env, allowExceptions);
    debug(("l2s(%i) Deregister", internalHandle));
    l2CapIf.Deregister();
}

#define open_l2server_return  open_l2server_finally(env, srv); return

void open_l2server_finally(JNIEnv *env, WIDCOMMStackL2CapServer* srv) {
	if ((srv != NULL) && (stack != NULL)) {
		srv->close(env, false);
		stack->deleteConnection(srv);
	}
	if (stack != NULL) {
		LeaveCriticalSection(&stack->csCommIf);
	}
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2ServerOpenImpl
(JNIEnv *env, jobject, jbyteArray uuidValue, jboolean authenticate, jboolean encrypt, jstring name, jint receiveMTU, jint transmitMTU, jint assignPsm) {
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return 0;
	}
	EnterCriticalSection(&stack->csCommIf);
	WIDCOMMStackL2CapServer* srv = NULL;
//VC6	__try {
		srv = stack->createL2CapServer();
		if (srv == NULL) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
			open_l2server_return 0;
		}
		debug(("l2s(%i) L2CapConn server handle created", srv->internalHandle));
		if (srv->hConnectionEvent == NULL) {
			throwRuntimeException(env, "fails to CreateEvent");
			open_l2server_return 0;
		}
		srv->transmitMTU = (UINT16)transmitMTU;
		srv->receiveMTU = (UINT16)receiveMTU;

		jbyte *bytes = env->GetByteArrayElements(uuidValue, 0);
		convertUUIDBytesToGUID(bytes, &(srv->service_guid));
		env->ReleaseByteArrayElements(uuidValue, bytes, 0);

		CL2CapIf *l2CapIf;
		l2CapIf = &srv->l2CapIf;

		if (!l2CapIf->AssignPsmValue(&(srv->service_guid), (UINT16)assignPsm)) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_UNKNOWN_PSM, "failed to assign PSM");
			open_l2server_return 0;
		}
		l2CapIf->Register();

		const char *cname = env->GetStringUTFChars(name, 0);
		UINT32 service_name_len;
		#ifdef _WIN32_WCE
			swprintf_s((wchar_t*)srv->service_name, BT_MAX_SERVICE_NAME_LEN, L"%s", cname);
			service_name_len = wcslen((wchar_t*)srv->service_name);
		#else // _WIN32_WCE
			sprintf_s(srv->service_name, BT_MAX_SERVICE_NAME_LEN, "%s", cname);
			service_name_len = (UINT32)strlen(srv->service_name);
		#endif // #else // _WIN32_WCE
		env->ReleaseStringUTFChars(name, cname);

		CSdpService *sdpService = new CSdpService();
		srv->sdpService = sdpService;
		if (sdpService->AddServiceClassIdList(1, &(srv->service_guid)) != SDP_OK) {
			throwIOException(env, "Error AddServiceClassIdList");
			open_l2server_return 0;
		}
		if (sdpService->AddServiceName(srv->service_name) != SDP_OK) {
			throwIOException(env, "Error AddServiceName");
			open_l2server_return 0;
		}
		if (sdpService->AddL2CapProtocolDescriptor(l2CapIf->GetPsm()) != SDP_OK) {
			throwIOException(env, "Error AddL2CapProtocolDescriptor");
			open_l2server_return 0;
		}
		if (sdpService->AddAttribute(0x0100, TEXT_STR_DESC_TYPE, service_name_len, (UINT8*)srv->service_name) != SDP_OK) {
			throwIOException(env, "Error AddAttribute ServiceName");
			open_l2server_return 0;
		}
		if (sdpService->MakePublicBrowseable() != SDP_OK) {
			throwIOException(env, "Error MakePublicBrowseable");
			open_l2server_return 0;
		}

		UINT8 sec_level = BTM_SEC_NONE;
		if (authenticate) {
			sec_level = BTM_SEC_IN_AUTHENTICATE;
		}
		if (encrypt) {
			sec_level = sec_level | BTM_SEC_IN_ENCRYPT;
		}
		if (!l2CapIf->SetSecurityLevel(srv->service_name, sec_level, TRUE)) {
			throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_SECURITY_BLOCK, "Error setting security level");
			open_l2server_return 0;
        }

		jlong handle = srv->internalHandle;
		srv = NULL;
		open_l2server_return handle;
/*VC6} __finally { } */
}

#define accept_server_return  accept_server_finally(env, l2c); return

void accept_server_finally(JNIEnv *env, WIDCOMMStackL2CapConn* l2c) {
	if ((l2c != NULL) && (stack != NULL)) {
		l2c->close(env, false);
		stack->deleteConnection(l2c);
	}
}

JNIEXPORT jlong JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2ServerAcceptAndOpenServerConnection
(JNIEnv *env, jobject peer, jlong handle) {
	WIDCOMMStackL2CapServer* srv = validL2CapServerHandle(env, handle);
	if (srv == NULL) {
		return 0;
	}
	if (srv->sdpService == NULL) {
		throwIOException(env, cCONNECTION_CLOSED);
		return 0;
	}

	#ifdef BWT_SINCE_SDK_6_0_1
	srv->sdpService->CommitRecord();
	#endif

	EnterCriticalSection(&stack->csCommIf);
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
	}

    WIDCOMMStackL2CapConn* l2c = stack->createL2CapConn();
    if (l2c == NULL) {
        throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_NO_RESOURCES, "No free connections Objects in Pool");
        LeaveCriticalSection(&stack->csCommIf);
		return 0;
    }
	srv->addClient(l2c);
	l2c->receiveMTU = srv->receiveMTU;
    l2c->transmitMTU = srv->transmitMTU;

	BOOL rc = l2c->Listen(&(srv->l2CapIf));
	if (stack != NULL) {
		LeaveCriticalSection(&stack->csCommIf);
	}

	if (!rc) {
		throwBluetoothConnectionException(env, BT_CONNECTION_ERROR_FAILED_NOINFO, "Failed to Listen");
		accept_server_return 0;
	}

    HANDLE hEvents[2];
	hEvents[0] = l2c->hConnectionEvent;
	hEvents[1] = srv->hConnectionEvent;

	debug(("l2s(%i) l2(%i) L2CAP server waits for connection", srv->internalHandle, l2c->internalHandle));
	long incomingConnectionCountWas = incomingL2CAPConnectionCount;
    while ((stack != NULL) && (!srv->isClosing)  && (!l2c->isConnected) && (!l2c->isDisconnected) && (srv->sdpService != NULL)) {
		DWORD  rc = WaitForMultipleObjects(2, hEvents, FALSE, 500);
		if (rc == WAIT_FAILED) {
			throwRuntimeException(env, "WaitForSingleObject");
			accept_server_return 0;
		}
		if ((stack != NULL) && (incomingConnectionCountWas != incomingL2CAPConnectionCount)) {
			debug(("L2CAP server incomingConnectionCount %i", incomingL2CAPConnectionCount));
			incomingConnectionCountWas = incomingL2CAPConnectionCount;
		}
		if (isCurrentThreadInterrupted(env, peer)) {
			debug(("Interrupted while waiting for connections"));
			accept_server_return 0;
		}
	}
	if (stack == NULL) {
		throwIOException(env, cSTACK_CLOSED);
		return 0;
	}
	if (!l2c->isConnected) {
        if (srv->isClosing || (srv->sdpService == NULL)) {
			throwInterruptedIOException(env, cCONNECTION_CLOSED);
		} else if (l2c->isDisconnected) {
            throwIOException(env, "Connection error");
        } else {
		    throwIOException(env, "Failed to connect");
        }
        accept_server_return 0;
    }

	debug(("l2s(%i) l2(%i) L2CAP server connection made", srv->internalHandle, l2c->internalHandle));
	l2c->selectConnectionTransmitMTU(env);
	return l2c->internalHandle;
}

JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2ServerPSM
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapServer* srv = validL2CapServerHandle(env, handle);
	if (srv == NULL) {
		return -1;
	}
	return srv->l2CapIf.GetPsm();
}


JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2CloseClientConnection
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return;
	}
	l2c->close(env, TRUE);
	if (stack != NULL) {
		stack->deleteConnection(l2c);
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2CloseServerConnection
(JNIEnv *env, jobject, jlong handle) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return;
	}
	debug(("l2(%i) closing server client", l2c->internalHandle));
	WIDCOMMStackServerConnectionBase* srv = l2c->server;
	if (srv != NULL) {
	    srv->closeClient(env, l2c);
	} else {
	    l2c->close(env, true);
    }
	if (stack != NULL) {
		stack->deleteConnection(l2c);
	}
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2ServerCloseImpl
(JNIEnv *env, jobject, jlong handle) {
    WIDCOMMStackL2CapServer* srv = validL2CapServerHandle(env, handle);
	if (srv == NULL) {
		return;
	}
	srv->close(env, true);
	// Some worker thread is still trying to access this object, delete later
	if (stack != NULL) {
		stack->deleteConnection(srv);
	}
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
	if (!l2c->isConnected) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
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
	if (l2c->receiveBuffer.available() > l2c->receiveBuffer.sizeof_len()) {
		return JNI_TRUE;
	}
	if (!l2c->isConnected) {
		debug(("->l2Ready()"));
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return JNI_FALSE;
	}
	return JNI_FALSE;
}


JNIEXPORT jint JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2Receive
(JNIEnv *env, jobject peer, jlong handle, jbyteArray inBuf) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return 0;
	}
	debug(("l2(%i) receive(byte[])", l2c->internalHandle));
	if ((!l2c->isConnected ) && (l2c->receiveBuffer.available() < sizeof(UINT16))) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
		return 0;
	}
	if (l2c->receiveBuffer.isOverflown()) {
		throwIOException(env, "Receive buffer overflown");
		return 0;
	}

	HANDLE hEvents[2];
	hEvents[0] = l2c->hConnectionEvent;
	hEvents[1] = l2c->hDataReceivedEvent;

	int paketLengthSize = l2c->receiveBuffer.sizeof_len();

	while ((stack != NULL) && l2c->isConnected  && (l2c->receiveBuffer.available() <= paketLengthSize)) {
		debug(("receive[] waits for data"));
		DWORD  rc = WaitForMultipleObjects(2, hEvents, FALSE, INFINITE);
		if (rc == WAIT_FAILED) {
			throwRuntimeException(env, "WaitForMultipleObjects");
			return 0;
		}
		debug(("receive[] waits returns %s", waitResultsString(rc)));
		if (isCurrentThreadInterrupted(env, peer)) {
			debug(("Interrupted while receiving"));
			return 0;
		}
	}
	if ((stack == NULL) || ((!l2c->isConnected) && (l2c->receiveBuffer.available() <= paketLengthSize)) ) {
		throwIOException(env, cCONNECTION_CLOSED);
		return 0;
	}

	int count = l2c->receiveBuffer.available();
	if (count < paketLengthSize) {
		throwIOException(env, "Receive buffer corrupted (1)");
		return 0;
	}
	int paketLength = 0;
	int done = l2c->receiveBuffer.read_len(&paketLength);
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
	if (readLen > l2c->receiveMTU) {
		readLen = l2c->receiveMTU;
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
	debug(("receive[] returns %i", done));
	return done;
}

JNIEXPORT void JNICALL Java_com_intel_bluetooth_BluetoothStackWIDCOMM_l2Send
(JNIEnv *env, jobject, jlong handle, jbyteArray data) {
	WIDCOMMStackL2CapConn* l2c = validL2CapConnHandle(env, handle);
	if (l2c == NULL) {
		return;
	}
	debug(("l2(%i) send(byte[])", l2c->internalHandle));
	if (!l2c->isConnected ) {
		throwIOException(env, cCONNECTION_IS_CLOSED);
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
		throwIOException(env, "Failed to write");
		return;
	}
	debug(("sent %i", written));
	if (written < len) {
		debug(("throw"));
		throwIOException(env, "Failed to write all data, send %i from %i", written, len);
	}

	env->ReleaseByteArrayElements(data, bytes, 0);
}

#endif //  _BTWLIB