/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.obex;

import java.io.IOException;
import java.util.StringTokenizer;

class DeviceInfo implements Storable {

	String btAddress;

	String name;

	String obexUrl;

	boolean obexServiceFound = false;

	public String toString() {
		if ((name != null) && (name.length() > 0)) {
			return name;
		} else {
			return btAddress;
		}
	}

	private String fixNull(String str) {
		if (str.equalsIgnoreCase("null")) {
			return null;
		}
		return str;
	}

	public void loadFromLine(String line) throws IOException {
		StringTokenizer st = new StringTokenizer(line, "|");
		if (!st.hasMoreTokens()) {
			throw new IOException();
		}
		btAddress = fixNull(st.nextToken());
		if (!st.hasMoreTokens()) {
			throw new IOException();
		}
		name = fixNull(st.nextToken());
		if (!st.hasMoreTokens()) {
			throw new IOException();
		}
		obexUrl = fixNull(st.nextToken());
	}

	public String saveAsLine() {
		return btAddress + "|" + name + "|" + obexUrl;
	}

	public boolean isValid() {
		if ((obexUrl == null) || (btAddress == null)) {
			return false;
		}
		return (obexUrl.toLowerCase().indexOf("://" + btAddress.toLowerCase() + ":") != -1);
	}
}