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
package com.intel.bluetooth;

import java.util.StringTokenizer;

import junit.framework.TestCase;

public class UtilsStringTokenizerTest extends TestCase {
	
	private void validateStringTokenizer(String str, String delimiter) {
		StringTokenizer stRef = new StringTokenizer(str, delimiter);
		UtilsStringTokenizer stImp = new UtilsStringTokenizer(str, delimiter);
		while ((stRef.hasMoreTokens()) && (stImp.hasMoreTokens())) {
			assertEquals("nextToken", stRef.nextToken(), stImp.nextToken());
		}
		assertEquals("hasMoreTokens", stRef.hasMoreTokens(), stImp.hasMoreTokens());
	}
	
	public void testStringTokenizer() {
		validateStringTokenizer("AB", ";");
		validateStringTokenizer("AB;", ";");
		validateStringTokenizer(";AB", ";");
		validateStringTokenizer(";AB;", ";");
		validateStringTokenizer("AB;CD", ";");
		validateStringTokenizer("AB;CD;EF", ";");
		validateStringTokenizer(";", ";");
	}
}
