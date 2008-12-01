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

public class Logger {

	static boolean debugOn = true;

	static void debug(String message) {
		if (!debugOn) {
			return;
		}
		System.out.println(message);
	}

	static void debug(String message, Object o) {
		if (!debugOn) {
			return;
		}
		System.out.println(message + " " + o);
	}

	static void debug(String message, Throwable e) {
		if (!debugOn) {
			return;
		}
		System.out.println(message + " " + e.getMessage());
		e.printStackTrace();
	}

	static void error(String message, Throwable e) {
		System.out.println(message + " " + e.getMessage());
		e.printStackTrace();
	}

	static void debug(Throwable e) {
		if (!debugOn) {
			return;
		}
		System.out.println(e.getMessage());
		e.printStackTrace();
	}

	static void error(Throwable e) {
		System.out.println(e.getMessage());
		e.printStackTrace();
	}
}
