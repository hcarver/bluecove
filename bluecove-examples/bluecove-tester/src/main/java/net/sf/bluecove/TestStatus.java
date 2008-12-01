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

/**
 * 
 */
public class TestStatus {

	private String name;

	boolean streamClosed = false;

	boolean isSuccess = false;

	boolean isError = false;

	boolean runCompleate = false;

	String pairBTAddress;

	StringBuffer replyMessage;

	public TestStatus() {
		setName("");
	}

	public TestStatus(int testType) {
		setName(testType);
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setName(int testType) {
		setName(String.valueOf(testType));
	}

	public boolean isRunCompleate() {
		return this.runCompleate;
	}

	public void setRunCompleate() {
		this.runCompleate = true;
	}

	public void setStreamClosed() {
		this.streamClosed = true;
	}

	public void addReplyMessage(String message) {
		if (replyMessage == null) {
			replyMessage = new StringBuffer(message);
		} else {
			replyMessage.append('\n').append(message);
		}
	}
}
