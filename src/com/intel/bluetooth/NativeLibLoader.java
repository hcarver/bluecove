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

    private static final boolean debug = false;

    public static boolean isAvailable() {
        if (triedToLoadAlredy) {
            return libraryAvailable;
        }
        String libFileName = NATIVE_LIB;
        String sysName = System.getProperty("os.name");

        if (sysName.toLowerCase(Locale.ENGLISH).indexOf("windows") != -1)  {
            libFileName = libFileName + ".dll";
//        } else if (sysName.toLowerCase(Locale.ENGLISH).indexOf("linux") != -1) {
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
        	libraryAvailable = tryload(NATIVE_LIB);
        }


        if (!libraryAvailable) {
            System.err.println("Native Library " + NATIVE_LIB + " not avalable");
        }
        triedToLoadAlredy = true;
        return libraryAvailable;
    }

    private static boolean tryload(String name) {
        try {
            System.loadLibrary(name);
            DebugLog.debug("Library loaded", name);
        } catch (Throwable e) {
            return false;
        }
        return true;
    }

    private static boolean tryloadPath(String path, String name) {
        try {
        	File f = new File(path, name);
        	if (!f.canRead()) {
        		return false;
        	}
            System.load(f.getAbsolutePath());
            DebugLog.debug("Library loaded", f.getAbsolutePath());
        } catch (Throwable e) {
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
            throw new Error("Native Library " + libFileName + " is not a ressource !");
        }
        if (is == null) {
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
            System.err.println("Can't create temporary file" + fd.getAbsolutePath());
            if (debug) {
                e.printStackTrace();
            }
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
        String tmppath = System.getProperty("java.io.tmpdir");
        String uname = System.getProperty("user.name");
        int count = 0;
        File fd = null;
        File dir = null;
        while (true) {
            if (count > 10) {
                throw new Error("Can't create temporary dir " + dir.getAbsolutePath());
            }
            dir = new File(tmppath, "bluecove_" + uname + "_"+ (count ++));
            fd = new File(dir, libFileName);
            if ((fd.exists()) && (!fd.delete())) {
                continue;
            }
            if (!dir.mkdirs()) {
                if (debug) {
                   System.err.println("Can't create temporary dir " + dir.getAbsolutePath());
                   continue;
                }
            }
            dir.deleteOnExit();

//            if (!fd.canWrite()) {
//                if (debug) {
//                    System.err.println("Can't create file in temporary dir " + fd.getAbsolutePath());
//                    continue;
//                 }
//            }
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
