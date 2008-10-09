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
package com.motorola.tck.tests.api.javax.obex.tcpobex;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;
import javax.obex.SessionNotifier;

import com.motorola.tck.tests.api.javax.obex.OBEX_TestCase;

/**
 * @author vlads
 * 
 */
public class TestUtilImpl extends com.motorola.tck.tests.api.javax.obex.TestUtil {

	public TestUtilImpl() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.tck.tests.api.javax.obex.TestUtil#getHelperConnString(java.lang.String)
	 */
	public String getHelperConnString(String address) {
		return "tcpobex://" + address;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.tck.tests.api.javax.obex.TestUtil#getSessionNotifier()
	 */
	public SessionNotifier getSessionNotifier() {
		SessionNotifier service = null;

		try {
			int port = OBEX_TestCase.getPropertyAsInt("tcpobex.client.port");
			service = (SessionNotifier) Connector.open("tcpobex://:" + port);
		} catch (Exception e) {
			System.out.println("Exception :" + e + "\nUnable to create OBEX service");
			return null;
		}
		return service;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.motorola.tck.tests.api.javax.obex.TestUtil#getSessionNotifierConnString(javax.obex.SessionNotifier)
	 */
	public String getSessionNotifierConnString(SessionNotifier service) {
		ServiceRecord servRec;
		LocalDevice localDevice = null;
		String connString = null;
		try {
			localDevice = LocalDevice.getLocalDevice();
			servRec = localDevice.getRecord(service);
			connString = servRec.getConnectionURL(ServiceRecord.NOAUTHENTICATE_NOENCRYPT, false);
		} catch (Exception e) {
			System.out.println("Exception :" + e + "\nUnable to get connection string for " + service.getClass().getName());
			int port = OBEX_TestCase.getPropertyAsInt("tcpobex.client.port");
			return "tcpobex://127.0.0.1:" + port;
		}
		return connString;
	}

}
