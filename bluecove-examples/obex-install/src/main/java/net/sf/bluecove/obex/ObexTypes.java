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
package net.sf.bluecove.obex;

import java.util.Hashtable;

public class ObexTypes {

	private static Hashtable types = new Hashtable();

	static {
		types.put("jpg", "image/jpeg");
		types.put("jpeg", "image/jpeg");
		types.put("gif", "image/gif");
		types.put("png", "image/png");
		types.put("mp3", "audio/mpeg");
		types.put("txt", "text/plain");
		types.put("jar", "application/java-archive");
		types.put("jad", "text/vnd.sun.j2me.app-descriptor"); 
	}

	static String getFileExtension(String fileName) {
		int extEnd = fileName.lastIndexOf('.');
		if (extEnd == -1) {
			return "";
		} else {
			return fileName.substring(extEnd + 1).toLowerCase();
		}
	}

	static String getObexFileType(String fileName) {
		return (String) types.get(getFileExtension(fileName));
	}
}
