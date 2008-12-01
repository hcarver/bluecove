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
 * This interface defines the stream connection over which content is passed.
 */
public interface ContentConnection extends StreamConnection {

	/**
	 * Returns the type of content that the resource connected to is providing. 
	 * For instance, if the connection is via HTTP, then the value of the content-type header field is returned.
	 * 
	 * @return the content type of the resource that the URL references, or null if not known.
	 */
	public String getType();

	/**
	 * Returns a string describing the encoding of the content which the resource connected to is providing. 
	 * E.g. if the connection is via HTTP, the value of the content-encoding header field is returned.
	 * 
	 * @return the content encoding of the resource that the URL references, or null if not known.
	 */
	public String getEncoding();

	/**
	 * Returns the length of the content which is being provided. 
	 * E.g. if the connection is via HTTP, then the value of the content-length header field is returned.
	 * 
	 * @return the content length of the resource that this connection's URL references, or -1 if the content length is not known.
	 */
	public long getLength();

}