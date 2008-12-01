/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;

/**
 * @author vlads
 * 
 */
public class Deploy implements UserInteraction {

	private String fileName;

	private int progressMaximum;

	public static void main(String[] args) {
		if ((args.length < 2) || args[0].equalsIgnoreCase("--help")) {
			StringBuffer usage = new StringBuffer();
			usage.append("Usage:\n java ").append(Deploy.class.getName());
			usage.append(" bluetoothURL yourApp.jar\n");
			System.out.println(usage);
			System.exit(1);
			return;
		}
		String obexUrl = args[0];

		String filePath = args[1];

		Logger.debugOn = false;
		Deploy d = new Deploy();
		byte[] data = d.readFile(filePath);
		if (data == null) {
			System.exit(1);
			return;
		}
		ObexBluetoothClient o = new ObexBluetoothClient(d, d.fileName, data);
		if (o.obexPut(obexUrl)) {
			System.exit(0);
		} else {
			System.exit(2);
		}
	}

	private Deploy() {
	}

	private static String simpleFileName(String filePath) {
		int idx = filePath.lastIndexOf('/');
		if (idx == -1) {
			idx = filePath.lastIndexOf('\\');
		}
		if (idx == -1) {
			return filePath;
		}
		return filePath.substring(idx + 1);
	}

	private byte[] readFile(final String filePath) {
		InputStream is = null;
		byte[] data = null;
		try {
			String path = filePath;
			String inputFileName;
			File file = new File(filePath);
			if (file.exists()) {
				is = new FileInputStream(file);
				inputFileName = file.getName();
			} else {
				URL url = new URL(path);
				is = url.openConnection().getInputStream();
				inputFileName = url.getFile();
			}
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			byte[] buffer = new byte[0xFF];
			int i = is.read(buffer);
			int done = 0;
			while (i != -1) {
				bos.write(buffer, 0, i);
				done += i;
				// setProgressValue(done);
				i = is.read(buffer);
			}
			data = bos.toByteArray();
			fileName = simpleFileName(inputFileName);
			showStatus((data.length / 1024) + "k " + fileName);
		} catch (Throwable e) {
			Logger.error(e);
			showStatus("Download error " + e.getMessage());
		} finally {
			IOUtils.closeQuietly(is);
		}
		return data;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.obex.UserInteraction#setProgressMaximum(int)
	 */
	public void setProgressMaximum(int n) {
		progressMaximum = n;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.obex.UserInteraction#setProgressValue(int)
	 */
	public void setProgressValue(int n) {
		// TODO Auto-generated method stub

	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.obex.UserInteraction#setProgressDone()
	 */
	public void setProgressDone() {
		// TODO Auto-generated method stub
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see net.sf.bluecove.obex.UserInteraction#showStatus(java.lang.String)
	 */
	public void showStatus(String message) {
		System.out.println(message);
	}

}
