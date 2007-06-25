package com.intel.bluetooth.obex;

import java.io.IOException;

import javax.bluetooth.UUID;
import javax.microedition.io.Connection;
import javax.obex.Authenticator;
import javax.obex.ServerRequestHandler;
import javax.obex.SessionNotifier;

public class OBEXSessionNotifierImpl implements SessionNotifier {

	public OBEXSessionNotifierImpl(UUID uuid, boolean authenticate, boolean encrypt, String name) throws IOException {
		
	}
	
	public Connection acceptAndOpen(ServerRequestHandler handler) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public Connection acceptAndOpen(ServerRequestHandler handler, Authenticator auth) throws IOException {
		// TODO Auto-generated method stub
		return null;
	}

	public void close() throws IOException {
		// TODO Auto-generated method stub

	}

}
