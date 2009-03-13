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
package net.sf.bluecove.se;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import net.sf.bluecove.util.Storage;

/**
 * 
 */
public class FileStorage implements Storage {

	private Properties properties;

	private long propertiesFileLoadedLastModified = 0;

	private File propertyFile;

	public FileStorage() {

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.util.Storage#retriveData(java.lang.String)
	 */
	public synchronized String retriveData(String name) {
		return getProperties().getProperty(name);
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.util.Storage#storeData(java.lang.String,
	 * java.lang.String)
	 */
	public synchronized void storeData(String name, String value) {
		Properties p = getProperties();
		if (name != null) {
			if (value == null) {
				if (p.remove(name) == null) {
					// Not updated
					return;
				}
			} else {
				if (value.equals(p.put(name, value))) {
					// Not updated
					return;
				}
			}
		}
		File f = getPropertyFile();
		if (f == null) {
			return;
		}
		FileOutputStream out = null;
		try {
			out = new FileOutputStream(f);
			// we run on Java 1.1
			p.save(out, "");
		} catch (FileNotFoundException ignore) {
		}
		try {
			out.close();
		} catch (Throwable ignore) {
		}
		propertiesFileLoadedLastModified = f.lastModified();
	}

	private static File homePath() {
		String path = ".bluecove";
		boolean isWindows = false;
		String sysName = System.getProperty("os.name");
		if (sysName != null) {
			sysName = sysName.toLowerCase();
			if (sysName.indexOf("windows") != -1) {
				isWindows = true;
				path = "Application Data";
			}
		}
		File dir;
		try {
			dir = new File(System.getProperty("user.home"), path);
			if (!dir.exists()) {
				if (!dir.mkdirs()) {
					throw new SecurityException();
				}
			}
		} catch (SecurityException e) {
			dir = new File(new File(System.getProperty("java.io.tmpdir"), System.getProperty("user.name")), path);
		}
		if (isWindows) {
			dir = new File(dir, "BlueCove");
		}
		if (!dir.exists()) {
			if (!dir.mkdirs()) {
				return null;
			}
		} else if (!dir.isDirectory()) {
			dir.delete();
			if (!dir.mkdirs()) {
				return null;
			}
		}
		return dir;
	}

	private File getPropertyFile() {
		if (propertyFile != null) {
			return propertyFile;
		}
		File homeDir = homePath();
		if (homeDir == null) {
			try {
				propertyFile = File.createTempFile("bluecove-tester", ".properties");
				return propertyFile;
			} catch (IOException e) {
				return null;
			}
		}
		// Position and history for different stacks and device IDs different
		// for testing convenience
		String id = "";
		try {
			String stack = System.getProperty("bluecove.stack");
			if (stack != null) {
				id = stack;
			}
		} catch (SecurityException ignore) {

		}
		try {
			String deviceID = System.getProperty("bluecove.deviceID");
			if (deviceID != null) {
				id += deviceID;
			}
		} catch (SecurityException ignore) {

		}
		propertyFile = new File(homeDir, "bluecove-tester" + id + ".properties");
		return propertyFile;
	}

	private Properties getProperties() {
		File f = getPropertyFile();
		long lastModified = 0;

		if (f != null && f.exists()) {
			lastModified = f.lastModified();
		} else {
			lastModified = propertiesFileLoadedLastModified;
		}

		if ((properties != null) && (propertiesFileLoadedLastModified == lastModified)) {
			return properties;
		}
		Properties p = new Properties();
		if (f != null && f.exists()) {
			FileInputStream in = null;
			try {
				in = new FileInputStream(f);
				p.load(in);
			} catch (IOException ignore) {
			} finally {
				try {
					in.close();
				} catch (Throwable ignore) {
				}
			}
		}
		propertiesFileLoadedLastModified = lastModified;
		properties = p;
		return properties;
	}

}
