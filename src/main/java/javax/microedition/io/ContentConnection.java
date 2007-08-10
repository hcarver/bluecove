/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2004 Intel Corporation
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