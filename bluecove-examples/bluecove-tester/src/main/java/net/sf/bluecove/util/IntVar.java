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
package net.sf.bluecove.util;

/**
 * Class used instead of int since there are no reflection in J2ME. 
 */
public class IntVar {
	
	private int value;
	
	public IntVar() {
		this.value = 0;
	}
	
	public IntVar(int value) {
		this.value = value;
	}
	
	public void setValue(int value) {
		this.value = value;
	}
	
	public void setValue(String value) {
		this.value = Integer.valueOf(value.trim()).intValue();
	}
	
	public int getValue() {
		return this.value;
	}
	
	public boolean equals(Object obj) {
		if (obj instanceof IntVar) {
			return ((IntVar)obj).value == this.value;
		} else {
			return false;
		}
	}
	
	public String toString() {
		return Integer.toString(value);
	}
}
