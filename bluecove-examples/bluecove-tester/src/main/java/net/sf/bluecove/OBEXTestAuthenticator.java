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
package net.sf.bluecove;

import javax.obex.Authenticator;
import javax.obex.PasswordAuthentication;

import org.bluecove.tester.log.Logger;

/**
 *
 */
public class OBEXTestAuthenticator implements Authenticator {

	String userName;
	
	public OBEXTestAuthenticator(String userName) {
		this.userName = userName;
	}
	
	public PasswordAuthentication onAuthenticationChallenge(String description, boolean isUserIdRequired, boolean isFullAccess) {
		Logger.debug("challenge " + (isUserIdRequired?"U":"") + (isFullAccess?"A":"") + " " + description);
		return new PasswordAuthentication(userName.getBytes(), (new String("password")).getBytes());
	}

	public byte[] onAuthenticationResponse(byte[] userName) {
		Logger.debug("authenticate " + new String(userName));
		return (new String("password")).getBytes();
	}

}
