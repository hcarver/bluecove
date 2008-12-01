/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
 *  @author vlads
 *  @version $Id$
 */
package com.motorola.tck.tests.api.javax.obex.tcpobex;

import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.microedition.io.Connector;
import javax.obex.SessionNotifier;

import com.motorola.tck.tests.api.javax.obex.OBEX_TestCase;

/**
 *
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
