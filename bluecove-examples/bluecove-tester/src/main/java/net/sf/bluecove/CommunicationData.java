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
 * 
 */
public class CommunicationData implements Consts {

	public static final String stringData = "TestString2007";

	public static final String stringUTFData = "\u0413\u043E\u043B\u0443\u0431\u043E\u0439\u0417\u0443\u0431";

	public static final int byteCount = 12; // 1024;

	public static final byte[] byteAray = new byte[] { 1, 7, -40, 80, 90, -1, 126, 100, 87, -10, 127, 31, -127, 0, -77 };

	public static final byte streamAvailableByteCount = 126;

	public static final int byteAray8KPlusSize = 0x2010; // More then 8K

	public static final int byteAray64KPlusSize = 0x10100; // More then 64K

	public static final int byteAray128KSize = 0x20000; // 64K

	public static final byte aKnowndPositiveByte = 21;

	public static final byte aKnowndNegativeByte = -33;

}
