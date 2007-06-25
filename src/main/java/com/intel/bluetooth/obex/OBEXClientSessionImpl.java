package com.intel.bluetooth.obex;

import java.io.IOException;

import javax.obex.Authenticator;
import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;

public class OBEXClientSessionImpl implements ClientSession {

	public OBEXClientSessionImpl(long address, int channel, boolean authenticate,	boolean encrypt) throws IOException {
		

	}
	public HeaderSet connect(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public HeaderSet createHeaderSet() {
		// TODO Auto-generated method stub
		return null;
	}

	public HeaderSet delete(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public HeaderSet disconnect(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public Operation get(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public long getConnectionID() {
		// TODO Auto-generated method stub
		return 0;
	}

	public Operation put(HeaderSet headers) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void setAuthenticator(Authenticator auth) {
		// TODO Auto-generated method stub

	}

	public void setConnectionID(long id) {
		// TODO Auto-generated method stub

	}

	public HeaderSet setPath(HeaderSet headers, boolean backup, boolean create) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

}
