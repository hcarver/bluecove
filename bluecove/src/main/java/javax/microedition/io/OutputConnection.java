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

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;

/**
 * This interface defines the capabilities that an output stream connection must have.
 */
public interface OutputConnection extends Connection {

	/**
	 * Open and return an output stream for a connection. 
	 * 
	 * @return An output stream
	 * 
	 * @exception IOException If an I/O error occur
	 */
	public OutputStream openOutputStream() throws IOException;

	/**
	 * Open and return a data output stream for a connection. 
	 * 
	 * @return An output stream
	 * 
	 * @exception IOException If an I/O error occur
	 */
	public DataOutputStream openDataOutputStream() throws IOException;
}