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
 *  @version $Id$
 */
package com.intel.bluetooth;

import java.util.NoSuchElementException;

/**
 * Simple implementation of java.util.StringTokenizer for J2ME Taken from
 * com.pyx4me.core.utils
 *
 * Created on 11-Jun-2006
 *
 * @author vlads
 * @version $Revision$ ($Author$) $Date: 2007-07-31
 *          16:15:58 -0400 (Tue, 31 Jul 2007) $
 */
class UtilsStringTokenizer {

	private int currentPosition;

	private int newPosition;

	private String str;

	private String delimiter;

	/**
	 * Constructs a string tokenizer for the specified string. The characters in
	 * the delim argument are the delimiters for separating tokens. Delimiter
	 * characters themselves will not be treated as tokens.
	 *
	 * @param str
	 *            a string to be parsed
	 * @param delimiter
	 *            the delimiter
	 */
	public UtilsStringTokenizer(String str, String delimiter) {
		this.str = str;
		this.delimiter = delimiter;
		this.currentPosition = 0;
		nextPosition();
	}

	/**
	 * @return True, if there is a token left
	 */
	public boolean hasMoreTokens() {
		return (newPosition != -1) && (currentPosition < newPosition);
	}

	private void nextPosition() {
		this.newPosition = this.str.indexOf(this.delimiter, this.currentPosition);
		if (this.newPosition == -1) {
			this.newPosition = this.str.length();
		} else if (this.newPosition == this.currentPosition) {
			// Zero len token
			this.currentPosition += 1;
			nextPosition();
		}
	}

	/**
	 *
	 * @return Next token
	 * @throws NoSuchElementException
	 *             If there is no token left
	 */
	public String nextToken() throws NoSuchElementException {
		if (!hasMoreTokens()) {
			throw new NoSuchElementException();
		}

		String next = this.str.substring(this.currentPosition, this.newPosition);

		this.currentPosition = this.newPosition + 1;
		nextPosition();
		return next;
	}
}
