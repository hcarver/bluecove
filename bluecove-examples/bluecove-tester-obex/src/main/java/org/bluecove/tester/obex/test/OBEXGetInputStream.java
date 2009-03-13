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
import java.io.InputStream;

import javax.obex.ClientSession;
import javax.obex.HeaderSet;
import javax.obex.Operation;
import javax.obex.ResponseCodes;

import junit.framework.Assert;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.IOUtils;

public class OBEXGetInputStream extends OBEXClientBase {

	protected void execute(ClientSession con) throws IOException {
		Logger.debug("connect");
		HeaderSet headerC = con.connect(null);
		Assert.assertEquals("CONNECT", ResponseCodes.OBEX_HTTP_OK, headerC.getResponseCode());

		HeaderSet header = con.createHeaderSet();
		header.setHeader(HeaderSet.NAME, "READ");
		header.setHeader(HeaderSet.TYPE, "text");
		Logger.debug("get");
		Operation op = con.get(header);

		Logger.debug("openOutputStream");
		InputStream in = op.openInputStream();
		Logger.debug("read");
		StringBuffer buf = new StringBuffer();
		int data;
		while ((data = in.read()) != -1) {
			buf.append((char) data);
		}
		int debugMax = buf.length();
		if (debugMax > 20) {
			debugMax = 40;
		}
		Logger.debug("got", buf.toString().substring(0, debugMax));
		IOUtils.closeQuietly(in);

		Logger.debug("op.close");
		op.close();
		con.disconnect(null);
	}

}
