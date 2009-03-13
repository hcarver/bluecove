/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import javax.bluetooth.UUID;

/**
 *
 *
 */
class BluetoothConnectionNotifierParams {

	UUID uuid;

	boolean authenticate;

	boolean encrypt;

	boolean authorize;

	String name;

	boolean master;

	boolean obex;

	boolean timeouts;

	/**
	 * Enables L2CAP server PSM selections. Usage:
	 * btl2cap://localhost;name=test;bluecovepsm=11 where bluecovepsm is
	 * 4*4(HEXDIG)
	 */
	int bluecove_ext_psm = 0;

	public BluetoothConnectionNotifierParams(UUID uuid, boolean authenticate, boolean encrypt, boolean authorize,
			String name, boolean master) {
		super();
		this.uuid = uuid;
		this.authenticate = authenticate;
		this.encrypt = encrypt;
		this.authorize = authorize;
		this.name = name;
		this.master = master;
		this.obex = false;
	}
}
