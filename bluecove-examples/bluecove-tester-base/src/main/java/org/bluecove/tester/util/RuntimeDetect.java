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
package org.bluecove.tester.util;

/**
 * 
 */
public class RuntimeDetect {

	public static boolean isBlueCove = false;

	public static boolean isJ2ME = false;

	public static boolean CLDC_1_0 = false;

	public static CLDCStub cldcStub;

	public static void detectCLDC() {

		RuntimeDetect.isJ2ME = true;

		RuntimeDetect.CLDC_1_0 = StringUtils.equalsIgnoreCase(System.getProperty("microedition.configuration"),
				"CLDC-1.0");

		if (!RuntimeDetect.CLDC_1_0) {
			RuntimeDetect.cldcStub = new CLDC11();
		} else {
			RuntimeDetect.cldcStub = new CLDC10();
		}
	}
}
