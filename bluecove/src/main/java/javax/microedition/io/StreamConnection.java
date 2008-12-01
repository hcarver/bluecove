/**
 *  BlueCove - Java library for Bluetooth
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
 *  @version $Id$
 */
package javax.microedition.io;

/**
 * This interface defines the capabilities that a stream connection must have.
 * <p>
 * In a typical implementation of this interface (for instance in MIDP 2.0), all
 * StreamConnections have one underlying InputStream and one OutputStream.
 * Opening a DataInputStream counts as opening an InputStream and opening a
 * DataOutputStream counts as opening an OutputStream. Trying to open another
 * InputStream or OutputStream causes an IOException. Trying to open the
 * InputStream or OutputStream after they have been closed causes an
 * IOException.
 * <p>
 * The methods of StreamConnection are not synchronized. The only stream method that can be called safely in another thread is close. 
 */
public interface StreamConnection extends InputConnection, OutputConnection {
}