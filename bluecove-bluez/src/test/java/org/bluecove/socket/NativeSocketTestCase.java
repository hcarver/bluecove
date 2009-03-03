/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2009 Vlad Skarzhevskyy
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
package org.bluecove.socket;

import com.intel.bluetooth.NativeTestCase;

public class NativeSocketTestCase extends NativeTestCase {

    private Throwable serverThreadError;

    private Thread serverThread;

    private static int threadNumber;

    private static synchronized int nextThreadNum() {
        return threadNumber++;
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        serverThreadError = null;
        serverThread = runNewServerThread(createTestServer());
    }
    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        if ((serverThread != null) && (serverThread.isAlive())) {
            serverThread.interrupt();
            serverThread.join();
        }
    }

    public interface TestRunnable {
        
        public abstract void run() throws Exception;
        
    }
    
    /**
     * Override if test needs a second Thread with server
     * 
     * @return
     */
    protected TestRunnable createTestServer() {
        return null;
    }

    protected void assertServerErrors() {
        assertNull("Server Errors", serverThreadError);
    }

    private class SafeTestRunnable implements Runnable {

        private TestRunnable runnable;

        private Object startedEvent = new Object();

        private boolean started = false;

        SafeTestRunnable(TestRunnable runnable) {
            this.runnable = runnable;
        }

        public void run() {
            started = true;
            synchronized (startedEvent) {
                started = true;
                startedEvent.notifyAll();
            }

            try {
                runnable.run();
            } catch (Throwable t) {
                serverThreadError = t;
            }
        }
    }

    protected Thread runNewServerThread(TestRunnable runnable) {
        if (runnable == null) {
            return null;
        }
        SafeTestRunnable r = new SafeTestRunnable(runnable);
        int id = nextThreadNum();
        ThreadGroup g = new ThreadGroup("TestServerThreadGroup-" + id);
        Thread t = new Thread(g, r, "TestServerThread-" + id);
        synchronized (r.startedEvent) {
            t.start();
            while (!r.started) {
                try {
                    r.startedEvent.wait();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        }
        return t;
    }
}
