/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package com.intel.bluetooth;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public class NativeLibLoader {

    public static final String NATIVE_LIB = "intelbth";

    private static boolean triedToLoadAlredy;

    private static boolean libraryAvailable;

    public static boolean isAvailable() {
        if (triedToLoadAlredy) {
            return libraryAvailable;
        }
        String libName = NATIVE_LIB;
        String libFileName = libName;

        String sysName = System.getProperty("os.name");

        if (sysName == null) {
        	System.err.println("Native Library " + NATIVE_LIB + " not avalable on unknown platform");
        }
        
        sysName = sysName.toLowerCase(Locale.ENGLISH);
        
        if (sysName.indexOf("windows") != -1)  {
        	if (sysName.indexOf("ce") != -1) {
        		libName += "_ce";
        		libFileName = libName;
        	}
            libFileName = libFileName + ".dll";
        } else if (sysName.indexOf("mac os x") != -1) {
        	libFileName = "lib" +libFileName + ".jnilib";
//        } else if (.indexOf("linux") != -1) {
//            libFileName = "lib" + libFileName + ".so";
        } else {
        	System.err.println("Native Library " + NATIVE_LIB + " not avalable on platform " + sysName);
        	triedToLoadAlredy = true;
            libraryAvailable = false;
            return libraryAvailable;
        }

        String path = System.getProperty("bluecove.native.path");
        if (path != null) {
        	libraryAvailable = tryloadPath(path, libFileName);
        }
        boolean useResource = true;
        String d = System.getProperty("bluecove.native.resource");
        if ((d != null) && (d.equalsIgnoreCase("false"))) {
        	useResource = false;
        }

        if ((!libraryAvailable) && (useResource)) {
            libraryAvailable = loadAsSystemResource(libFileName);
        }
        if (!libraryAvailable) {
        	libraryAvailable = tryload(libName);
        }

        if (!libraryAvailable) {
            System.err.println("Native Library " + libName + " not avalable");
            DebugLog.debug("java.library.path", System.getProperty("java.library.path"));
        }
        triedToLoadAlredy = true;
        return libraryAvailable;
    }

    private static boolean tryload(String name) {
        try {
            System.loadLibrary(name);
            DebugLog.debug("Library loaded", name);
        } catch (Throwable e) {
        	DebugLog.debug("Library " + name + " not loaded " + e.toString());
            return false;
        }
        return true;
    }

    private static boolean tryloadPath(String path, String name) {
        try {
        	File f = new File(path, name);
        	if (!f.canRead()) {
        		System.err.println("Native Library " + f.getAbsolutePath() + " not found");
        		return false;
        	}
            System.load(f.getAbsolutePath());
            DebugLog.debug("Library loaded", f.getAbsolutePath());
        } catch (Throwable e) {
        	 DebugLog.error("cant load library from path " + path, e);
            return false;
        }
        return true;
    }

    private static boolean loadAsSystemResource(String libFileName) {
        InputStream is = null;
        try {
            ClassLoader clo = NativeLibLoader.class.getClassLoader();
            if (clo == null) {
                is = ClassLoader.getSystemResourceAsStream(libFileName);
            } else {
                is = clo.getResourceAsStream(libFileName);
            }
        } catch (Throwable e) {
        	DebugLog.error("Native Library " + libFileName + " is not a ressource !");
            return false;
        }
        if (is == null) {
        	DebugLog.error("Native Library " + libFileName + " is not a ressource !");
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
        fd.deleteOnExit();
//        deleteOnExit(fd);
        try {
            System.load(fd.getAbsolutePath());
            DebugLog.debug("Library loaded from", fd);
        } catch (Throwable e) {
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
        String tmpDir = System.getProperty("java.io.tmpdir");
        String uname = System.getProperty("user.name");
        int count = 0;
        File fd = null;
        File dir = null;
        while (true) {
            if (count > 10) {
            	DebugLog.debug("Can't create temporary dir " + dir.getAbsolutePath());
            	return new File(tmpDir, libFileName);
            }
            dir = new File(tmpDir, "bluecove_" + uname + "_"+ (count ++));
            fd = new File(dir, libFileName);
            if ((fd.exists()) && (!fd.delete())) {
                continue;
            }
            if ((!dir.exists()) && (!dir.mkdirs())) {
                DebugLog.debug("Can't create temporary dir ", dir.getAbsolutePath());
                continue;
            }
            dir.deleteOnExit();
            try {
				if (!fd.createNewFile()) {
				    DebugLog.debug("Can't create file in temporary dir ", fd.getAbsolutePath());
				    continue;
				}
			} catch (IOException e) {
				DebugLog.debug("Can't create file in temporary dir ", fd.getAbsolutePath());
				continue;
			}
            break;
        }
        return fd;
    }

//    private static void deleteOnExit(final File fd) {
//        Runnable r = new Runnable() {
//            public void run() {
//                if (!fd.delete()) {
//                    System.err.println("Can't remove Native Library " + fd);
//                }
//            }
//        };
//        Runtime.getRuntime().addShutdownHook(new Thread(r));
//    }

    static {
        isAvailable();
    }
}
