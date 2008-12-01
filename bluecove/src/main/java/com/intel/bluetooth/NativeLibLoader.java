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
package com.intel.bluetooth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Hashtable;

import com.ibm.oti.vm.VM;

/**
 * Load native library from resources.
 *
 *
 * By default Native Library is extracted from from jar to temporary directory
 * `${java.io.tmpdir}/bluecove_${user.name}_N` and loaded from this location.
 * <p>
 * If you wish to load library (.dll) from another location add this system
 * property `-Dbluecove.native.path=/your/path`.
 * <p>
 * If you wish to load library from default location in path e.g.
 * `%SystemRoot%\system32` or any other location in %PATH% use
 * `-Dbluecove.native.resource=false`
 *
 */
public abstract class NativeLibLoader {

	static final int OS_UNSUPPORTED = -1;

	static final int OS_LINUX = 1;

	static final int OS_WINDOWS = 2;

	static final int OS_WINDOWS_CE = 3;

	static final int OS_MAC_OS_X = 4;

	private static int os = 0;

	private static Hashtable libsState = new Hashtable();

	private static String bluecoveDllDir = null;

	private static class LibState {

		boolean triedToLoadAlredy = false;

		boolean libraryAvailable = false;

	}

	private NativeLibLoader() {

	}

	static int getOS() {
		if (os != 0) {
			return os;
		}
		String sysName = System.getProperty("os.name");
		if (sysName == null) {
			DebugLog.fatal("Native Library not available on unknown platform");
			os = OS_UNSUPPORTED;
		} else {
			sysName = sysName.toLowerCase();
			if (sysName.indexOf("windows") != -1) {
				if (sysName.indexOf("ce") != -1) {
					os = OS_WINDOWS_CE;
				} else {
					os = OS_WINDOWS;
				}
			} else if (sysName.indexOf("mac os x") != -1) {
				os = OS_MAC_OS_X;
			} else if (sysName.indexOf("linux") != -1) {
				os = OS_LINUX;
			} else {
				DebugLog.fatal("Native Library not available on platform " + sysName);
				os = OS_UNSUPPORTED;
			}
		}
		return os;
	}

	static boolean isAvailable(String name) {
		return isAvailable(name, null);
	}

	static boolean isAvailable(String name, Class stackClass) {
		LibState state = (LibState) libsState.get(name);
		if (state == null) {
			state = new LibState();
			libsState.put(name, state);
		}
		if (state.triedToLoadAlredy) {
			return state.libraryAvailable;
		}
		String libName = name;
		String libFileName = libName;

		// DebugLog.debug("OS:" + System.getProperty("os.name") + "|" +
		// System.getProperty("os.version") + "|" +
		// System.getProperty("os.arch"));
		// DebugLog.debug("Java:" + System.getProperty("java.vendor") + " " +
		// System.getProperty("java.version"));

		String sysName = System.getProperty("os.name");

		String sysArch = System.getProperty("os.arch");
		if (sysArch != null) {
			sysArch = sysArch.toLowerCase();
		} else {
			sysArch = "";
		}

		switch (getOS()) {
		case OS_UNSUPPORTED:
			DebugLog.fatal("Native Library " + name + " not available on [" + sysName + "] platform");
			state.triedToLoadAlredy = true;
			state.libraryAvailable = false;
			return state.libraryAvailable;
		case OS_WINDOWS_CE:
			libName += "_ce";
			libFileName = libName;
			libFileName = libFileName + ".dll";
			break;
		case OS_WINDOWS:
			if ((sysArch.indexOf("amd64") != -1) || (sysArch.indexOf("x86_64") != -1)) {
				libName += "_x64";
				libFileName = libName;
			}
			libFileName = libFileName + ".dll";
			break;
		case OS_MAC_OS_X:
			libFileName = "lib" + libFileName + ".jnilib";
			break;
		case OS_LINUX:
			if ((sysArch.indexOf("i386") != -1) || (sysArch.length() == 0)) {
				// regular Intel
			} else if ((sysArch.indexOf("amd64") != -1) || (sysArch.indexOf("x86_64") != -1)) {
				libName += "_x64";
			} else if ((sysArch.indexOf("x86") != -1)) {
				// regular Intel under IBM J9
			} else {
				// Any other system
				libName += "_" + sysArch;
			}
			libFileName = libName;
			libFileName = "lib" + libFileName + ".so";
			break;
		default:
			DebugLog.fatal("Native Library " + name + " not available on platform " + sysName);
			state.triedToLoadAlredy = true;
			state.libraryAvailable = false;
			return state.libraryAvailable;
		}

		String path = System.getProperty(BlueCoveConfigProperties.PROPERTY_NATIVE_PATH);
		if (path != null) {
			if (!UtilsJavaSE.ibmJ9midp) {
				state.libraryAvailable = tryloadPath(path, libFileName);
			} else {
				// Not working
				// state.libraryAvailable = tryloadPathIBMj9MIDP(path,
				// libFileName);
			}
		}
		boolean useResource = true;
		String d = System.getProperty(BlueCoveConfigProperties.PROPERTY_NATIVE_RESOURCE);
		if ((d != null) && (d.equalsIgnoreCase("false"))) {
			useResource = false;
		}

		if ((!state.libraryAvailable) && (useResource) && (!UtilsJavaSE.ibmJ9midp)) {
			state.libraryAvailable = loadAsSystemResource(libFileName, stackClass);
		}
		if (!state.libraryAvailable) {
			if (!UtilsJavaSE.ibmJ9midp) {
				state.libraryAvailable = tryload(libName);
			} else {
				state.libraryAvailable = tryloadIBMj9MIDP(libName);
			}
		}

		if (!state.libraryAvailable) {
			System.err.println("Native Library " + libName + " not available");
			DebugLog.debug("java.library.path", System.getProperty("java.library.path"));
		}
		state.triedToLoadAlredy = true;
		return state.libraryAvailable;
	}

	private static boolean tryload(String name) {
		try {
			System.loadLibrary(name);
			DebugLog.debug("Library loaded", name);
		} catch (Throwable e) {
			DebugLog.error("Library " + name + " not loaded ", e);
			return false;
		}
		return true;
	}

	private static boolean tryloadIBMj9MIDP(String name) {
		try {
			VM.loadLibrary(name);
			DebugLog.debug("Library loaded", name);
		} catch (Throwable e) {
			DebugLog.error("Library " + name + " not loaded ", e);
			return false;
		}
		return true;
	}

	private static boolean tryloadPath(String path, String name) {
		try {
			File f = new File(path, name);
			if (!f.canRead()) {
				DebugLog.fatal("Native Library " + f.getAbsolutePath() + " not found");
				return false;
			}
			System.load(f.getAbsolutePath());
			DebugLog.debug("Library loaded", f.getAbsolutePath());
		} catch (Throwable e) {
			DebugLog.error("Can't load library from path " + path, e);
			return false;
		}
		return true;
	}

	private static boolean tryloadPathIBMj9MIDP(String path, String name) {
		try {
			VM.loadLibrary(path + "\\" + name);
			DebugLog.debug("Library loaded", path + "\\" + name);
		} catch (Throwable e) {
			DebugLog.error("Can't load library from path " + path + "\\" + name, e);
			return false;
		}
		return true;
	}

	private static boolean loadAsSystemResource(String libFileName, Class stackClass) {
		InputStream is = null;
		try {
			ClassLoader clo = null;
			try {
				if (stackClass != null) {
					clo = stackClass.getClassLoader();
					DebugLog.debug("Use stack ClassLoader");
				} else {
					clo = NativeLibLoader.class.getClassLoader();
				}
			} catch (Throwable j9) {
			}
			if (clo == null) {
				DebugLog.debug("Use System ClassLoader");
				is = ClassLoader.getSystemResourceAsStream(libFileName);
			} else {
				is = clo.getResourceAsStream(libFileName);
			}
		} catch (Throwable e) {
			DebugLog.error("Native Library " + libFileName + " is not a Resource !");
			return false;
		}
		if (is == null) {
			DebugLog.error("Native Library " + libFileName + " is not a Resource !");
			return false;
		}
		File fd = makeTempName(libFileName);
		try {
			if (!copy2File(is, fd)) {
				return false;
			}
		} finally {
			try {
				is.close();
			} catch (IOException ignore) {
				is = null;
			}
		}
		try {
			fd.deleteOnExit();
		} catch (Throwable e) {
			// Java 1.1 or J9
		}
		// deleteOnExit(fd);
		try {
			System.load(fd.getAbsolutePath());
			DebugLog.debug("Library loaded from", fd);
		} catch (Throwable e) {
			DebugLog.error("Can't load library file ", e);
			return false;
		}
		return true;
	}

	private static boolean copy2File(InputStream is, File fd) {
		FileOutputStream fos = null;
		try {
			fos = new FileOutputStream(fd);
			byte b[] = new byte[1000];
			int len;
			while ((len = is.read(b)) >= 0) {
				fos.write(b, 0, len);
			}
			return true;
		} catch (Throwable e) {
			DebugLog.debug("Can't create temporary file ", e);
			System.err.println("Can't create temporary file " + fd.getAbsolutePath());
			return false;
		} finally {
			if (fos != null) {
				try {
					fos.close();
				} catch (IOException ignore) {
					fos = null;
				}
			}
		}
	}

	private static File makeTempName(String libFileName) {
		if (bluecoveDllDir != null) {
			return new File(bluecoveDllDir, libFileName);
		}
		String tmpDir = System.getProperty("java.io.tmpdir");
		String uname = System.getProperty("user.name");
		int count = 0;
		File fd = null;
		File dir = null;
		selectDirectory: while (true) {
			if (count > 10) {
				DebugLog.debug("Can't create temporary dir " + dir.getAbsolutePath());
				return new File(tmpDir, libFileName);
			}
			dir = new File(tmpDir, "bluecove_" + uname + "_" + (count++));
			if (dir.exists()) {
				if (!dir.isDirectory()) {
					continue selectDirectory;
				}
				// Remove all files.
				try {
					File[] files = dir.listFiles();
					for (int i = 0; i < files.length; i++) {
						if (!files[i].delete()) {
							continue selectDirectory;
						}
					}
				} catch (Throwable e) {
					// Java 1.1 or J9
				}
			}
			if ((!dir.exists()) && (!dir.mkdirs())) {
				DebugLog.debug("Can't create temporary dir ", dir.getAbsolutePath());
				continue selectDirectory;
			}
			try {
				dir.deleteOnExit();
			} catch (Throwable e) {
				// Java 1.1 or J9
			}
			fd = new File(dir, libFileName);
			if ((fd.exists()) && (!fd.delete())) {
				continue;
			}
			try {
				if (!fd.createNewFile()) {
					DebugLog.debug("Can't create file in temporary dir ", fd.getAbsolutePath());
					continue;
				}
			} catch (IOException e) {
				DebugLog.debug("Can't create file in temporary dir ", fd.getAbsolutePath());
				continue;
			} catch (Throwable e) {
				// Java 1.1 or J9
			}
			bluecoveDllDir = dir.getAbsolutePath();
			break;
		}
		return fd;
	}

	// private static void deleteOnExit(final File fd) {
	// Runnable r = new Runnable() {
	// public void run() {
	// if (!fd.delete()) {
	// System.err.println("Can't remove Native Library " + fd);
	// }
	// }
	// };
	// Runtime.getRuntime().addShutdownHook(new Thread(r));
	// }

}
