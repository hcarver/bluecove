/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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

import java.security.AccessControlContext;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;

import com.intel.bluetooth.BluetoothStack.LibraryInformation;

/**
 *
 * Singleton class used as holder for BluetoothStack.
 *
 * Under security manager all you need to do is initialize BlueCoveImpl inside Privileged context.
 * <p>
 * If automatic Bluetooth Stack detection is not enough Java System property "bluecove.stack" can be used to force
 * desired Stack Initialization. Values "widcomm", "bluesoleil" or "winsock". By default winsock is selected if
 * available.
 * <p>
 * Another property "bluecove.stack.first" is used optimize stack detection. If -Dbluecove.stack.first=widcomm then
 * widcomm (bluecove.dll) stack is loaded first and if not available then BlueCove will switch to winsock. By default
 * intelbth.dll is loaded first.
 * <p>
 * If multiple stacks are detected they are selected in following order: "winsock", "widcomm", "bluesoleil". Since
 * BlueCove v2.0.1 "bluecove.stack.first" will alter the order of stack selection.
 * <p>
 * To use jsr-82 emulator set "bluecove.stack" value to "emulator".
 * <p>
 * If System property is not an option (e.g. when running in Webstart) create text file "bluecove.stack" or
 * "bluecove.stack.first" containing stack name and add this file to BlueCove or Application jar. (Since v2.0.1)
 * <p>
 * Use `LocalDevice.getProperty("bluecove.stack")` to find out which stack is used.
 *
 */
public class BlueCoveImpl {

    public static final String BLUETOOTH_API_VERSION = "1.1.1";

    public static final String OBEX_API_VERSION = BLUETOOTH_API_VERSION;

    public static final int versionMajor1 = 2;

    public static final int versionMajor2 = 1;

    public static final int versionMinor = 1;

    public static final int versionBuild = 0;

    public static final String versionSufix = "-SNAPSHOT"; // SNAPSHOT

    public static final String version = String.valueOf(versionMajor1) + "." + String.valueOf(versionMajor2) + "."
            + String.valueOf(versionMinor) + versionSufix;

    public static final int nativeLibraryVersionExpected = versionMajor1 * 1000000 + versionMajor2 * 10000
            + versionMinor * 100 + versionBuild;

    public static final String STACK_WINSOCK = "winsock";

    public static final String STACK_WIDCOMM = "widcomm";

    public static final String STACK_BLUESOLEIL = "bluesoleil";

    public static final String STACK_TOSHIBA = "toshiba";

    public static final String STACK_BLUEZ = "bluez";

    public static final String STACK_OSX = "mac";

    public static final String STACK_EMULATOR = "emulator";

    // We can't use the same DLL on windows for all implementations.
    // Since WIDCOMM need to be compile /MD using VC6 and winsock /MT using
    // VC2005
    // This variable can be used to simplify development/test builds
    private static final boolean oneDLLbuild = false;

    public static final String NATIVE_LIB_MS = "intelbth";

    public static final String NATIVE_LIB_WIDCOMM = oneDLLbuild ? NATIVE_LIB_MS : "bluecove";

    public static final String NATIVE_LIB_TOSHIBA = "bluecove";

    public static final String NATIVE_LIB_BLUEZ = "bluecove";

    public static final String NATIVE_LIB_OSX = "bluecove";

    /**
     * To work on BlueSoleil version 2.3 we need to compile C++ code /MT the same as winsock.
     */
    public static final String NATIVE_LIB_BLUESOLEIL = NATIVE_LIB_MS;

    static final int BLUECOVE_STACK_DETECT_MICROSOFT = 1;

    static final int BLUECOVE_STACK_DETECT_WIDCOMM = 1 << 1;

    static final int BLUECOVE_STACK_DETECT_BLUESOLEIL = 1 << 2;

    static final int BLUECOVE_STACK_DETECT_TOSHIBA = 1 << 3;

    static final int BLUECOVE_STACK_DETECT_OSX = 1 << 4;

    public static final int BLUECOVE_STACK_DETECT_BLUEZ = 1 << 5;

    public static final int BLUECOVE_STACK_DETECT_EMULATOR = 1 << 6;

    static final String TRUE = "true";

    static final String FALSE = "false";

    private static final String FQCN = BlueCoveImpl.class.getName();

    private static final Vector fqcnSet = new Vector();

    /* The context to be used when loading native DLL */
    private Object accessControlContext;

    private static ShutdownHookThread shutdownHookRegistered;

    private static BlueCoveImpl instance;

    private static BluetoothStackHolder singleStack;

    private static ThreadLocalWrapper threadStack;

    private static BluetoothStackHolder threadStackIDDefault;

    private static Hashtable resourceConfigProperties = new Hashtable();

    private static Hashtable/* <BluetoothStack, BluetoothStackHolder> */stacks = new Hashtable();

    private static Vector initializationProperties = new Vector();

    static {
        fqcnSet.addElement(FQCN);
        for (int i = 0; i < BlueCoveConfigProperties.INITIALIZATION_PROPERTIES.length; i++) {
            initializationProperties.addElement(BlueCoveConfigProperties.INITIALIZATION_PROPERTIES[i]);
        }
    }

    /**
     * Enables the use of Multiple Adapters and Bluetooth Stacks in parallel.
     */
    private static class BluetoothStackHolder {

        private BluetoothStack bluetoothStack;

        Hashtable configProperties = new Hashtable();

        private static BluetoothStack getBluetoothStack() throws BluetoothStateException {
            return instance().getBluetoothStack();
        }

        public String toString() {
            if (bluetoothStack == null) {
                return "not initialized";
            }
            return bluetoothStack.toString();
        }
    }

    /**
     * bluetoothStack.destroy(); May stuck forever. Exit JVM anyway after timeout.
     */
    private class AsynchronousShutdownThread extends Thread {

        final Object monitor = new Object();

        int shutdownStart = 0;

        AsynchronousShutdownThread() {
            super("BluecoveAsynchronousShutdownThread");
        }

        public void run() {
            synchronized (monitor) {
                while (shutdownStart == 0) {
                    try {
                        monitor.wait();
                    } catch (InterruptedException e) {
                        return;
                    }
                }
            }
            if (shutdownStart == -1) {
                return;
            }
            if (!stacks.isEmpty()) {
                for (Enumeration en = stacks.elements(); en.hasMoreElements();) {
                    BluetoothStackHolder s = (BluetoothStackHolder) en.nextElement();
                    if (s.bluetoothStack != null) {
                        try {
                            s.bluetoothStack.destroy();
                        } finally {
                            s.bluetoothStack = null;
                        }
                    }
                }
                stacks.clear();
                System.out.println("BlueCove stack shutdown completed");
            }
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }

        void deRegister() {
            shutdownStart = -1;
            synchronized (monitor) {
                monitor.notifyAll();
            }
        }
    }

    private class ShutdownHookThread extends Thread {

        AsynchronousShutdownThread shutdownHookThread;

        ShutdownHookThread(AsynchronousShutdownThread shutdownHookThread) {
            super("BluecoveShutdownHookThread");
            this.shutdownHookThread = shutdownHookThread;
        }

        public void run() {
            final Object monitor = shutdownHookThread.monitor;
            synchronized (monitor) {
                shutdownHookThread.shutdownStart = 1;
                monitor.notifyAll();
                if (!stacks.isEmpty()) {
                    try {
                        monitor.wait(7000);
                    } catch (InterruptedException e) {
                    }
                }
            }
        }

        void deRegister() {
            shutdownHookRegistered = null;
            UtilsJavaSE.runtimeRemoveShutdownHook(this);
            shutdownHookThread.deRegister();
        }
    }

    /**
     * Applications should not used this function. Allow default initialization. In Secure environment instance() should
     * be called initially from secure context.
     *
     * @return Instance of the class, getBluetoothStack() can be called.
     */
    public static synchronized BlueCoveImpl instance() {
        if (instance == null) {
            instance = new BlueCoveImpl();
        }
        return instance;
    }

    private BlueCoveImpl() {
        try {
            accessControlContext = AccessController.getContext();
        } catch (Throwable javaME) {
        }
        // Initialization in WebStart.
        DebugLog.isDebugEnabled();
        copySystemProperties(null);
    }

    static int getNativeLibraryVersion() {
        return nativeLibraryVersionExpected;
    }

    private synchronized void createShutdownHook() {
        if (shutdownHookRegistered != null) {
            return;
        }
        AsynchronousShutdownThread shutdownHookThread = new AsynchronousShutdownThread();
        if (UtilsJavaSE.runtimeAddShutdownHook(shutdownHookRegistered = new ShutdownHookThread(shutdownHookThread))) {
            UtilsJavaSE.threadSetDaemon(shutdownHookThread);
            shutdownHookThread.start();
        }
    }

    private int getStackId(String stack) {
        if (STACK_WIDCOMM.equalsIgnoreCase(stack)) {
            return BLUECOVE_STACK_DETECT_WIDCOMM;
        } else if (STACK_BLUESOLEIL.equalsIgnoreCase(stack)) {
            return BLUECOVE_STACK_DETECT_BLUESOLEIL;
        } else if (STACK_TOSHIBA.equalsIgnoreCase(stack)) {
            return BLUECOVE_STACK_DETECT_TOSHIBA;
        } else if (STACK_WINSOCK.equalsIgnoreCase(stack)) {
            return BLUECOVE_STACK_DETECT_MICROSOFT;
        } else if (STACK_BLUEZ.equalsIgnoreCase(stack)) {
            return BLUECOVE_STACK_DETECT_BLUEZ;
        } else if (STACK_WINSOCK.equalsIgnoreCase(stack)) {
            return BLUECOVE_STACK_DETECT_OSX;
        } else if (STACK_EMULATOR.equalsIgnoreCase(stack)) {
            return BLUECOVE_STACK_DETECT_EMULATOR;
        } else {
            return 0;
        }
    }

    private Class loadStackClass(String classPropertyName, String classNameDefault) throws BluetoothStateException {
        String className = getConfigProperty(classPropertyName);
        if (className == null) {
            className = classNameDefault;
        }
        try {
            return Class.forName(className);
        } catch (ClassNotFoundException e) {
            DebugLog.error(className, e);
        }
        throw new BluetoothStateException("BlueCove " + className + " not available");
    }

    private BluetoothStack newStackInstance(Class ctackClass) throws BluetoothStateException {
        String className = ctackClass.getName();
        try {
            return (BluetoothStack) ctackClass.newInstance();
        } catch (InstantiationException e) {
            DebugLog.error(className, e);
        } catch (IllegalAccessException e) {
            DebugLog.error(className, e);
        }
        throw new BluetoothStateException("BlueCove " + className + " can't instantiate");
    }

    private BluetoothStack loadStack(String classPropertyName, String classNameDefault) throws BluetoothStateException {
        return newStackInstance(loadStackClass(classPropertyName, classNameDefault));
    }

    static void loadNativeLibraries(BluetoothStack stack) throws BluetoothStateException {
        // Check is libraries already loaded
        try {
            if ((UtilsJavaSE.canCallNotLoadedNativeMethod) && (stack.isNativeCodeLoaded())) {
                return;
            }
        } catch (Error e) {
            // We caught UnsatisfiedLinkError
        }
        LibraryInformation[] libs = stack.requireNativeLibraries();
        if ((libs == null) || (libs.length == 0)) {
            // No native libs for this stack
            return;
        }
        for (int i = 0; i < libs.length; i++) {
            Class c = libs[i].stackClass;
            if (c == null) {
                c = stack.getClass();
            }
            if (!NativeLibLoader.isAvailable(libs[i].libraryName, c)) {
                throw new BluetoothStateException("BlueCove library " + libs[i].libraryName + " not available");
            }
        }
    }

    private static boolean isNativeLibrariesAvailable(BluetoothStack stack) {
        try {
            if (UtilsJavaSE.canCallNotLoadedNativeMethod) {
                return stack.isNativeCodeLoaded();
            }
        } catch (Error e) {
            // We caught UnsatisfiedLinkError
        }
        LibraryInformation[] libs = stack.requireNativeLibraries();
        if ((libs == null) || (libs.length == 0)) {
            // No native libs for this stack
            return true;
        }
        for (int i = 0; i < libs.length; i++) {
            Class c = libs[i].stackClass;
            if (c == null) {
                c = stack.getClass();
            }
            if (!NativeLibLoader.isAvailable(libs[i].libraryName, c)) {
                return false;
            }
        }
        return true;
    }

    private BluetoothStack detectStack() throws BluetoothStateException {

        BluetoothStack detectorStack = null;

        String stackFirstDetector = getConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK_FIRST);

        String stackSelected = getConfigProperty(BlueCoveConfigProperties.PROPERTY_STACK);

        if (stackFirstDetector == null) {
            stackFirstDetector = stackSelected;
        }
        if (STACK_EMULATOR.equals(stackSelected)) {
            detectorStack = loadStack("bluecove.emulator.class", "com.intel.bluetooth.BluetoothEmulator");
        } else {
            switch (NativeLibLoader.getOS()) {
            case NativeLibLoader.OS_LINUX:
                Class stackClass = loadStackClass("bluecove.bluez.class", "com.intel.bluetooth.BluetoothStackBlueZ");
                detectorStack = newStackInstance(stackClass);
                loadNativeLibraries(detectorStack);
                stackSelected = detectorStack.getStackID();
                break;
            case NativeLibLoader.OS_MAC_OS_X:
                detectorStack = new BluetoothStackOSX();
                loadNativeLibraries(detectorStack);
                stackSelected = detectorStack.getStackID();
                break;
            case NativeLibLoader.OS_WINDOWS:
            case NativeLibLoader.OS_WINDOWS_CE:
                detectorStack = createDetectorOnWindows(stackFirstDetector);
                if (DebugLog.isDebugEnabled()) {
                    detectorStack.enableNativeDebug(DebugLog.class, true);
                }
                break;
            default:
                throw new BluetoothStateException("BlueCove not available");

            }
        }

        int libraryVersion = detectorStack.getLibraryVersion();
        if (nativeLibraryVersionExpected != libraryVersion) {
            DebugLog.fatal("BlueCove native library version mismatch " + libraryVersion + " expected "
                    + nativeLibraryVersionExpected);
            throw new BluetoothStateException("BlueCove native library version mismatch");
        }

        if (stackSelected == null) {
            // auto detect
            int aval = detectorStack.detectBluetoothStack();
            DebugLog.debug("BluetoothStack detected", aval);
            int detectorID = getStackId(detectorStack.getStackID());
            if ((aval & detectorID) != 0) {
                stackSelected = detectorStack.getStackID();
            } else if ((aval & BLUECOVE_STACK_DETECT_MICROSOFT) != 0) {
                stackSelected = STACK_WINSOCK;
            } else if ((aval & BLUECOVE_STACK_DETECT_WIDCOMM) != 0) {
                stackSelected = STACK_WIDCOMM;
            } else if ((aval & BLUECOVE_STACK_DETECT_BLUESOLEIL) != 0) {
                stackSelected = STACK_BLUESOLEIL;
            } else if ((aval & BLUECOVE_STACK_DETECT_TOSHIBA) != 0) {
                stackSelected = STACK_TOSHIBA;
            } else if ((aval & BLUECOVE_STACK_DETECT_OSX) != 0) {
                stackSelected = STACK_OSX;
            } else {
                DebugLog.fatal("BluetoothStack not detected");
                throw new BluetoothStateException("BluetoothStack not detected");
            }
        } else {
            DebugLog.debug("BluetoothStack selected", stackSelected);
        }

        BluetoothStack stack = setBluetoothStack(stackSelected, detectorStack);
        stackSelected = stack.getStackID();
        copySystemProperties(stack);
        if (!stackSelected.equals(STACK_EMULATOR)) {
            System.out.println("BlueCove version " + version + " on " + stackSelected);
        }
        return stack;
    }

    /**
     * List the local adapters that can be initialized using configuration property "bluecove.deviceID". (Linux BlueZ
     * and Emulator)
     *
     * The first stack/adapter would be initialized if not initialized already, you can exclude it from the list.
     *
     * The function return empty list on non BlueZ environment.
     *
     * @return List of Strings
     * @throws BluetoothStateException
     *             if stack interface can't be initialized
     * @see com.intel.bluetooth.BlueCoveConfigProperties#PROPERTY_LOCAL_DEVICE_ID
     * @see com.intel.bluetooth.BlueCoveLocalDeviceProperties#LOCAL_DEVICE_PROPERTY_DEVICE_ID
     */
    public static Vector getLocalDevicesID() throws BluetoothStateException {
        Vector v = new Vector();
        String ids = BluetoothStackHolder.getBluetoothStack().getLocalDeviceProperty(
                BlueCoveLocalDeviceProperties.LOCAL_DEVICE_DEVICES_LIST);
        if (ids != null) {
            UtilsStringTokenizer tok = new UtilsStringTokenizer(ids, ",");
            while (tok.hasMoreTokens()) {
                v.addElement(tok.nextToken());
            }
        }
        return v;
    }

    /**
     * API that enables the use of Multiple Adapters and Bluetooth Stacks in parallel in the same JVM. Each thread
     * should call setThreadBluetoothStackID() before using JSR-82 API.
     *
     * Affects the following JSR-82 API methods:
     *
     * <pre>
     *  LocalDevice.getLocalDevice();
     *  LocalDevice.getProperty(String);
     *  Connector.open(...);
     *  methods of RemoteDevice instance created by user.
     * </pre>
     *
     * <STRONG>Example</STRONG>
     * <P>
     *
     * <pre>
     * BlueCoveImpl.useThreadLocalBluetoothStack();
     * // On Windows
     * BlueCoveImpl.setConfigProperty(&quot;bluecove.stack&quot;, &quot;widcomm&quot;);
     * // On Linux or in Emulator
     * // BlueCoveImpl.setConfigProperty(&quot;bluecove.deviceID&quot;, &quot;0&quot;);
     *
     * final Object id1 = BlueCoveImpl.getThreadBluetoothStackID();
     * ... do some work with stack 1
     *
     * // Illustrates attaching thread to already initialized stack interface
     * Thread t1 = new Thread() {
     *    public void run() {
     *        BlueCoveImpl.setThreadBluetoothStackID(id1);
     *        agent = LocalDevice.getLocalDevice().getDiscoveryAgent();
     *        agent.startInquiry(...);
     *        .....
     *    }
     * };
     * t1.start();
     *
     * // Illustrates initialization of new/different stack interface in new thread
     * // Start another thread that is using different stack
     * Thread t2 = new Thread() {
     *    public void run() {
     *        // On Windows
     *        BlueCoveImpl.setConfigProperty(&quot;bluecove.stack&quot;, &quot;winsock&quot;);
     *        // On Linux or in Emulator
     *        // BlueCoveImpl.setConfigProperty(&quot;bluecove.deviceID&quot;, &quot;1&quot;);
     *        agent = LocalDevice.getLocalDevice().getDiscoveryAgent();
     *        agent.startInquiry(...);
     *        .....
     *    }
     * }
     * t2.start();
     *
     * Thread t3 = new Thread() {
     *    public void run() {
     *        // Wrong, will produce error: Thread StackID not configured
     *        Connector.open(&quot;btspp://12345678:1&quot;);
     *        .....
     *    }
     * };
     * t3.start();
     *
     * </pre>
     *
     * @see #setConfigProperty
     */
    public static synchronized void useThreadLocalBluetoothStack() {
        if (threadStack == null) {
            threadStack = new ThreadLocalWrapper();
        }
        BluetoothStackHolder s = ((BluetoothStackHolder) threadStack.get());
        if (s == null) {
            // Move initialized single stack to this thread
            if (singleStack != null) {
                s = singleStack;
                singleStack = null;
            } else {
                s = new BluetoothStackHolder();
            }
            threadStack.set(s);
        }
    }

    /**
     * Initialize BluetoothStack if not already done and returns the ID to be used in other threads accessing the same
     * stack.
     *
     * @return an object that represents Adapter/BluetoothStack, stackID to be used in call to
     *         <code>setThreadBluetoothStackID</code>
     * @throws BluetoothStateException
     *             if the Bluetooth system could not be initialized
     */
    public static synchronized Object getThreadBluetoothStackID() throws BluetoothStateException {
        useThreadLocalBluetoothStack();
        BluetoothStackHolder.getBluetoothStack();
        return threadStack.get();
    }

    /**
     * Returns the ID to be used in other threads accessing the same stack.
     *
     * @return an object that represents Adapter/BluetoothStack, stackID to be used in call to
     *         <code>setThreadBluetoothStackID</code> or <code>null<code> if ThreadLocalBluetoothStack not used.
     */
    public static synchronized Object getCurrentThreadBluetoothStackID() {
        if (threadStack == null) {
            return null;
        }
        return threadStack.get();
    }

    /**
     * Updates the current Thread BluetoothStack. Updating is possible only if <code>stackID</code> was obtained using
     * the <code>getThreadBluetoothStackID()</code> method. Should be called before connection is made or LocalDevice
     * received from LocalDevice.getLocalDevice().
     *
     * @param stackID
     *            stackID to use or <code>null</code> to detach the current Thread
     */
    public static synchronized void setThreadBluetoothStackID(Object stackID) {
        if ((stackID != null) && (!(stackID instanceof BluetoothStackHolder))) {
            throw new IllegalArgumentException("stackID is not valid");
        }
        if (threadStack == null) {
            throw new IllegalArgumentException("ThreadLocal configuration is not initialized");
        }
        threadStack.set(stackID);
    }

    /**
     * Detach BluetoothStack from ThreadLocal. Used for removing itself from container threads. Also can be use to
     * initialize different stack in the same thread.
     */
    public static synchronized void releaseThreadBluetoothStack() {
        if (threadStack == null) {
            throw new IllegalArgumentException("ThreadLocal configuration is not initialized");
        }
        threadStack.set(null);
    }

    /**
     * Set default Thread BluetoothStack for Threads that do not call <code>setThreadBluetoothStackID(stackID)</code>.
     * Updating is possible only if <code>stackID</code> was obtained using the <code>getThreadBluetoothStackID()</code>
     * method.
     *
     * @param stackID
     *            stackID to use or <code>null</code> to remove default
     */
    public static synchronized void setDefaultThreadBluetoothStackID(Object stackID) {
        if ((stackID != null) && (!(stackID instanceof BluetoothStackHolder))) {
            throw new IllegalArgumentException("stackID is not valid");
        }
        if (threadStack == null) {
            throw new IllegalArgumentException("ThreadLocal configuration is not initialized");
        }
        threadStackIDDefault = (BluetoothStackHolder) stackID;
    }

    static synchronized void setThreadBluetoothStack(BluetoothStack bluetoothStack) {
        if (threadStack == null) {
            return;
        }
        BluetoothStackHolder s = ((BluetoothStackHolder) threadStack.get());
        if ((s != null) && (s.bluetoothStack == bluetoothStack)) {
            return;
        }

        BluetoothStackHolder sh = (BluetoothStackHolder) stacks.get(bluetoothStack);
        if (sh == null) {
            throw new RuntimeException("ThreadLocal not found for BluetoothStack");
        }
        threadStack.set(sh);
    }

    /**
     * Shutdown BluetoothStack assigned for current Thread and clear configuration properties for this thread
     */
    public static synchronized void shutdownThreadBluetoothStack() {
        // ThreadLocal configuration is not initialized
        if (threadStack == null) {
            return;
        }
        BluetoothStackHolder s = ((BluetoothStackHolder) threadStack.get());
        if (s == null) {
            return;
        }
        if (threadStackIDDefault == s) {
            threadStackIDDefault = null;
        }
        s.configProperties.clear();
        if (s.bluetoothStack != null) {
            BluetoothConnectionNotifierBase.shutdownConnections(s.bluetoothStack);
            RemoteDeviceHelper.shutdownConnections(s.bluetoothStack);
            s.bluetoothStack.destroy();
            stacks.remove(s.bluetoothStack);
            s.bluetoothStack = null;
        }
    }

    /**
     * Shutdown all BluetoothStacks interfaces initialized by BlueCove
     */
    public static synchronized void shutdown() {
        for (Enumeration en = stacks.elements(); en.hasMoreElements();) {
            BluetoothStackHolder s = (BluetoothStackHolder) en.nextElement();
            s.configProperties.clear();
            if (s.bluetoothStack != null) {
                BluetoothConnectionNotifierBase.shutdownConnections(s.bluetoothStack);
                RemoteDeviceHelper.shutdownConnections(s.bluetoothStack);
                try {
                    s.bluetoothStack.destroy();
                } finally {
                    s.bluetoothStack = null;
                }
            }
        }
        stacks.clear();
        singleStack = null;
        threadStackIDDefault = null;
        if (shutdownHookRegistered != null) {
            shutdownHookRegistered.deRegister();
        }
        clearSystemProperties();
    }

    /**
     * API that can be used to configure BlueCove properties instead of System properties. Initialization properties
     * should be changed before stack initialized. If <code>null</code> is passed as the <code>value</code> then the
     * property will be removed.
     *
     * @param name
     *            property name
     * @param value
     *            property value
     *
     * @see com.intel.bluetooth.BlueCoveConfigProperties
     *
     * @exception IllegalArgumentException
     *                if the stack already initialized and property can't be changed.
     */
    public static void setConfigProperty(String name, String value) {
        if (name == null) {
            throw new NullPointerException("key is null");
        }
        BluetoothStackHolder sh = currentStackHolder(true);
        if ((sh.bluetoothStack != null) && (initializationProperties.contains(name))) {
            throw new IllegalArgumentException("BlueCove Stack already initialized");
        }
        if (value == null) {
            sh.configProperties.remove(name);
        } else {
            sh.configProperties.put(name, value);
        }
    }

    static String getConfigProperty(String key) {
        if (key == null) {
            throw new NullPointerException("key is null");
        }
        String value = null;
        BluetoothStackHolder sh = currentStackHolder(false);
        if (sh != null) {
            value = (String) sh.configProperties.get(key);
        }
        if (value == null) {
            try {
                value = System.getProperty(key);
            } catch (SecurityException webstart) {
            }
        }
        if (value == null) {
            synchronized (resourceConfigProperties) {
                Object casheValue = resourceConfigProperties.get(key);
                if (casheValue != null) {
                    if (casheValue instanceof String) {
                        value = (String) casheValue;
                    }
                } else {
                    value = Utils.getResourceProperty(BlueCoveImpl.class, key);
                    if (value == null) {
                        resourceConfigProperties.put(key, new Object());
                    } else {
                        resourceConfigProperties.put(key, value);
                    }
                }
            }
        }
        return value;
    }

    static boolean getConfigProperty(String key, boolean defaultValue) {
        String value = getConfigProperty(key);
        if (value != null) {
            return TRUE.equals(value) || "1".equals(value);
        } else {
            return defaultValue;
        }
    }

    static int getConfigProperty(String key, int defaultValue) {
        String value = getConfigProperty(key);
        if (value != null) {
            return Integer.parseInt(value);
        } else {
            return defaultValue;
        }
    }

    static String[] getSystemPropertiesList() {
        String[] p = { BluetoothConsts.PROPERTY_BLUETOOTH_MASTER_SWITCH,
                BluetoothConsts.PROPERTY_BLUETOOTH_SD_ATTR_RETRIEVABLE_MAX,
                BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_DEVICES_MAX,
                BluetoothConsts.PROPERTY_BLUETOOTH_L2CAP_RECEIVEMTU_MAX,
                BluetoothConsts.PROPERTY_BLUETOOTH_SD_TRANS_MAX,
                BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY_SCAN,
                BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE_SCAN,
                BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_INQUIRY, BluetoothConsts.PROPERTY_BLUETOOTH_CONNECTED_PAGE };
        return p;
    }

    static void clearSystemProperties() {
        UtilsJavaSE.setSystemProperty(BluetoothConsts.PROPERTY_BLUETOOTH_API_VERSION, null);
        UtilsJavaSE.setSystemProperty(BluetoothConsts.PROPERTY_OBEX_API_VERSION, null);
        String[] property = getSystemPropertiesList();
        for (int i = 0; i < property.length; i++) {
            UtilsJavaSE.setSystemProperty(property[i], null);
        }
    }

    void copySystemProperties(BluetoothStack bluetoothStack) {
        UtilsJavaSE.setSystemProperty(BluetoothConsts.PROPERTY_BLUETOOTH_API_VERSION,
                BlueCoveImpl.BLUETOOTH_API_VERSION);
        UtilsJavaSE.setSystemProperty(BluetoothConsts.PROPERTY_OBEX_API_VERSION, BlueCoveImpl.OBEX_API_VERSION);
        if (bluetoothStack != null) {
            String[] property = getSystemPropertiesList();
            for (int i = 0; i < property.length; i++) {
                UtilsJavaSE.setSystemProperty(property[i], bluetoothStack.getLocalDeviceProperty(property[i]));
            }
        }
    }

    public String getLocalDeviceFeature(int featureID) throws BluetoothStateException {
        return ((BluetoothStackHolder.getBluetoothStack().getFeatureSet() & featureID) != 0) ? TRUE : FALSE;
    }

    private BluetoothStack createDetectorOnWindows(String stackFirst) throws BluetoothStateException {
        if (stackFirst != null) {
            DebugLog.debug("detector stack", stackFirst);
            BluetoothStack detectorStack;
            if (STACK_WIDCOMM.equalsIgnoreCase(stackFirst)) {
                detectorStack = new BluetoothStackWIDCOMM();
                if (isNativeLibrariesAvailable(detectorStack)) {
                    return detectorStack;
                }
            } else if (STACK_BLUESOLEIL.equalsIgnoreCase(stackFirst)) {
                detectorStack = new BluetoothStackBlueSoleil();
                if (isNativeLibrariesAvailable(detectorStack)) {
                    return detectorStack;
                }
            } else if (STACK_WINSOCK.equalsIgnoreCase(stackFirst)) {
                detectorStack = new BluetoothStackMicrosoft();
                if (isNativeLibrariesAvailable(detectorStack)) {
                    return detectorStack;
                }
            } else if (STACK_TOSHIBA.equalsIgnoreCase(stackFirst)) {
                detectorStack = new BluetoothStackToshiba();
                if (isNativeLibrariesAvailable(detectorStack)) {
                    return detectorStack;
                }
            } else {
                throw new IllegalArgumentException("Invalid BlueCove detector stack [" + stackFirst + "]");
            }
        }
        BluetoothStack stack = new BluetoothStackMicrosoft();
        if (isNativeLibrariesAvailable(stack)) {
            return stack;
        }

        stack = new BluetoothStackWIDCOMM();
        if (isNativeLibrariesAvailable(stack)) {
            return stack;
        }

        throw new BluetoothStateException("BlueCove libraries not available");
    }

    /**
     * @deprecated use setConfigProperty("bluecove.stack", ...);
     *
     * @param stack
     * @return stack ID
     * @throws BluetoothStateException
     */
    public String setBluetoothStack(String stack) throws BluetoothStateException {
        return setBluetoothStack(stack, null).getStackID();
    }

    private synchronized BluetoothStack setBluetoothStack(String stack, BluetoothStack detectorStack)
            throws BluetoothStateException {
        if (singleStack != null) {
            if (singleStack.bluetoothStack != null) {
                singleStack.bluetoothStack.destroy();
                stacks.remove(singleStack.bluetoothStack);
                singleStack.bluetoothStack = null;
            }
        } else if (threadStack != null) {
            BluetoothStackHolder s = ((BluetoothStackHolder) threadStack.get());
            if ((s != null) && (s.bluetoothStack != null)) {
                s.bluetoothStack.destroy();
                stacks.remove(s.bluetoothStack);
                s.bluetoothStack = null;
            }
        }
        BluetoothStack newStack;
        if ((detectorStack != null) && (detectorStack.getStackID()).equalsIgnoreCase(stack)) {
            newStack = detectorStack;
        } else if (STACK_WIDCOMM.equalsIgnoreCase(stack)) {
            newStack = new BluetoothStackWIDCOMM();
        } else if (STACK_BLUESOLEIL.equalsIgnoreCase(stack)) {
            newStack = new BluetoothStackBlueSoleil();
        } else if (STACK_TOSHIBA.equalsIgnoreCase(stack)) {
            newStack = new BluetoothStackToshiba();
        } else {
            newStack = new BluetoothStackMicrosoft();
        }
        loadNativeLibraries(newStack);
        int libraryVersion = newStack.getLibraryVersion();
        if (nativeLibraryVersionExpected != libraryVersion) {
            DebugLog.fatal("BlueCove native library version mismatch " + libraryVersion + " expected "
                    + nativeLibraryVersionExpected);
            throw new BluetoothStateException("BlueCove native library version mismatch");
        }

        if (DebugLog.isDebugEnabled()) {
            newStack.enableNativeDebug(DebugLog.class, true);
        }
        newStack.initialize();
        createShutdownHook();

        // Store stack in Thread or static variables
        BluetoothStackHolder sh = currentStackHolder(true);
        sh.bluetoothStack = newStack;
        stacks.put(newStack, sh);
        if (threadStack != null) {
            threadStack.set(sh);
        }
        return newStack;
    }

    public void enableNativeDebug(boolean on) {
        BluetoothStackHolder s = currentStackHolder(false);
        if ((s != null) && (s.bluetoothStack != null)) {
            s.bluetoothStack.enableNativeDebug(DebugLog.class, on);
        }
    }

    private static BluetoothStackHolder currentStackHolder(boolean create) {
        if (threadStack != null) {
            BluetoothStackHolder s = ((BluetoothStackHolder) threadStack.get());
            if ((s == null) && (threadStackIDDefault != null)) {
                return threadStackIDDefault;
            }
            if ((s == null) && create) {
                s = new BluetoothStackHolder();
                threadStack.set(s);
            }
            return s;
        } else {
            if ((singleStack == null) && create) {
                singleStack = new BluetoothStackHolder();
            }
            return singleStack;
        }
    }

    /**
     * Applications should not used this function.
     *
     * @return current BluetoothStack implementation
     * @throws BluetoothStateException
     *             when BluetoothStack not detected. If one connected the hardware later, BlueCove would be able to
     *             recover and start correctly
     * @exception Error
     *                if called from outside of BlueCove internal code.
     */
    public synchronized BluetoothStack getBluetoothStack() throws BluetoothStateException {
        Utils.isLegalAPICall(fqcnSet);
        BluetoothStackHolder sh = currentStackHolder(false);
        if ((sh != null) && (sh.bluetoothStack != null)) {
            return sh.bluetoothStack;
        } else if ((sh == null) && (threadStack != null)) {
            throw new BluetoothStateException("No BluetoothStack or Adapter for current thread");
        }

        BluetoothStack stack;
        if (accessControlContext == null) {
            stack = detectStack();
        } else {
            stack = detectStackPrivileged();
        }
        return stack;
    }

    private BluetoothStack detectStackPrivileged() throws BluetoothStateException {
        try {
            return (BluetoothStack) AccessController.doPrivileged(new PrivilegedExceptionAction() {
                public Object run() throws BluetoothStateException {
                    return detectStack();
                }
            }, (AccessControlContext) accessControlContext);
        } catch (PrivilegedActionException e) {
            Throwable cause = UtilsJavaSE.getCause(e);
            if (cause instanceof BluetoothStateException) {
                throw (BluetoothStateException) cause;
            }
            throw (BluetoothStateException) UtilsJavaSE.initCause(new BluetoothStateException(e.getMessage()), cause);
        }
    }

}
