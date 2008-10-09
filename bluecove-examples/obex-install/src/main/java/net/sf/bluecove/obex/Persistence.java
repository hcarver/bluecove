/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.LineNumberReader;
import java.io.Reader;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Properties;

public class Persistence {

	private static final String configFileName = "obex-install.cfg";

	private static final String devicePrefix = "device:";

	private static final String selectedPrefix = "selected:";

	private static final Properties properties = new Properties();

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

	private static File getConfigFile() {
		File home = homePath();
		if (home == null) {
			return null;
		}
		return new File(home, configFileName);
	}

	public static String loadDevices(Hashtable devices) {
		File cf = getConfigFile();
		if (cf == null || !cf.exists()) {
			return null;
		}
		Reader fr = null;
		LineNumberReader lnr = null;
		try {
			lnr = new LineNumberReader(fr = new FileReader(cf));
			devices.clear();
			String line = lnr.readLine();
			String selected = null;
			while (line != null) {
				if (line.startsWith(devicePrefix)) {
					DeviceInfo di = new DeviceInfo();
					di.loadFromLine(line.substring(devicePrefix.length()));
					if (di.isValid()) {
						devices.put(di.btAddress.toLowerCase(), di);
					}
				} else if (line.startsWith(selectedPrefix)) {
					selected = line.substring(selectedPrefix.length());
				} else {
					int p = line.indexOf('=');
					if (p != -1) {
						properties.put(line.substring(0, p), line.substring(p + 1));
					}
				}
				line = lnr.readLine();
			}
			return selected;
		} catch (Throwable e) {
			Logger.debug(e);
			return null;
		} finally {
			if (lnr != null) {
				try {
					lnr.close();
				} catch (IOException e) {
				}
			}
			if (fr != null) {
				try {
					fr.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static void storeDevices(Hashtable devices, String selected) {
		File cf = getConfigFile();
		if (cf == null) {
			return;
		}
		FileWriter fw = null;
		try {
			fw = new FileWriter(cf, false);
			for (Enumeration i = devices.keys(); i.hasMoreElements();) {
				String addr = (String) i.nextElement();
				DeviceInfo di = (DeviceInfo) devices.get(addr);
				fw.write(devicePrefix + di.saveAsLine() + "\n");
			}
			if (selected != null) {
				fw.write(selectedPrefix + selected + "\n");
			}
			for (Enumeration en = properties.propertyNames(); en.hasMoreElements();) {
				String name = (String) en.nextElement();
				fw.write(name + "=" + properties.getProperty(name) + "\n");
			}
			fw.flush();
		} catch (Throwable e) {
			Logger.debug(e);
			return;
		} finally {
			if (fw != null) {
				try {
					fw.close();
				} catch (IOException e) {
				}
			}
		}
	}

	public static String getProperty(String key, String defaultValue) {
		return properties.getProperty(key, defaultValue);
	}

	public static void setProperty(String key, String value) {
		properties.setProperty(key, value);
	}
}
