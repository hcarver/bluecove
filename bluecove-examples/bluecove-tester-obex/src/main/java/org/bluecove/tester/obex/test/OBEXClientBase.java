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
package org.bluecove.tester.obex.test;

import java.io.IOException;

import javax.microedition.io.Connector;
import javax.obex.ClientSession;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.obex.TestSelector;
import org.bluecove.tester.util.IOUtils;

public abstract class OBEXClientBase implements Runnable {

	protected abstract void execute(ClientSession con) throws IOException;

	public final void pause(String mesage) {
		TestSelector.testControl.pause(mesage);
	}

	public final void run() {
		ClientSession con = null;
		try {
			con = (ClientSession) Connector.open(Config.getTestObexServerURL());
		} catch (Throwable e) {
			Logger.error("Connect", e);
			IOUtils.closeQuietly(con);
			return;
		}

		try {
			execute(con);
		} catch (Throwable e) {
			Logger.error("Exec", e);
		} finally {
			IOUtils.closeQuietly(con);
		}
	}

}
