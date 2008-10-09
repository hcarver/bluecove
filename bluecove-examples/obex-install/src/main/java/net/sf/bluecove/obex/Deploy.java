/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
 * 
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
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
