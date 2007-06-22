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
 *
 *  Java docs licensed under the Apache License, Version 2.0
 *  http://www.apache.org/licenses/LICENSE-2.0 
 *   (c) Copyright 2001, 2002 Motorola, Inc.  ALL RIGHTS RESERVED.
 *
 *   
 *  @version $Id$
 */ 
package javax.bluetooth;

/**
 * The L2CAPConnection interface represents a connection-oriented L2CAP channel.
 * This interface is to be used as part of the CLDC Generic Connection Framework.
 *  <p>
 * To create a client connection, the protocol is btl2cap. The target is the 
 * combination of the address of the Bluetooth device to connect to and the Protocol 
 * Service Multiplexor (PSM) of the service. The PSM value is used by the L2CAP to 
 * determine which higher level protocol or application is the recipient of the 
 * messages the layer receives.
 * <p>
 * The parameters defined specific to L2CAP are ReceiveMTU (Maximum Transmission 
 * Unit (MTU)) and TransmitMTU. The ReceiveMTU and TransmitMTU parameters are 
 * optional. ReceiveMTU specifies the maximum payload size this connection can 
 * accept, and TransmitMTU specifies the maximum payload size this connection can 
 * send. An example of a valid L2CAP client connection string is:
 * {@code btl2cap://0050CD00321B:1003;ReceiveMTU=512;TransmitMTU=512}
 *
 * @version 1.0 February 11, 2002
 */
public interface L2CAPConnection extends javax.microedition.io.Connection {

	/**
	 * Default MTU value for connection-oriented channels is 672 bytes.
	 * <p>
	 * The value of {@code DEFAULT_MTU} is 0x02A0 (672).
	 */
	public static final int DEFAULT_MTU = 672;

	/**
	 * Minimum MTU value for connection-oriented channels is 48 bytes.
	 * <p>
	 * The value of {@code MINIMUM_MTU} is 0x30 (48).
	 */
	public static final int MINIMUM_MTU = 48;

	/**
	 * Returns the ReceiveMTU that the connection supports. If the connection string
	 * did not specify a ReceiveMTU, the value returned will be less than or equal 
	 * to the {@code DEFAULT_MTU}. Also, if the connection string did specify an MTU, this 
	 * value will be less than or equal to the value specified in the connection 
	 * string.
	 * 
	 * @return the maximum number of bytes that can be read in a single
	 *  		call to receive()
	 * @throws java.io.IOException if the connection is closed
	 */
	public int getReceiveMTU() throws java.io.IOException;

	/**
	 * Returns the MTU that the remote device supports. This value is obtained 
	 * after the connection has been configured. If the application had specified 
	 * TransmitMTU in the {@code Connector.open()} string then this value should be equal 
	 * to that. If the application did not specify any TransmitMTU, then this value 
	 * should be less than or equal to the ReceiveMTU the remote device advertised 
	 * during channel configuration.
	 * 
	 * @return	the maximum number of bytes that can be sent in a single call to 
	 * 			{@code send()} without losing any data
	 * @throws 	java.io.IOException if the connection is closed
	 */
	public int getTransmitMTU() throws java.io.IOException;

	/**
	 * Requests that data be sent to the remote device. The TransmitMTU determines 
	 * the amount of data that can be successfully sent in a single send operation. 
	 * If the size of data is greater than the TransmitMTU, then only the first 
	 * TransmitMTU bytes of the packet are sent, and the rest will be discarded. 
	 * If data is of length 0, an empty L2CAP packet will be sent.
	 * 
	 * @param data	data to be sent
	 * @throws java.io.IOException 	if data cannot be sent successfully or if the 
	 * 								connection is closed
	 * @throws NullPointerException if the data is {@code null}
	 */
	public void send(byte[] data) throws java.io.IOException;
	
	/**
	 * Reads a packet of data. The amount of data received in this operation is 
	 * related to the value of ReceiveMTU. If the size of {@code inBuf} is greater than 
	 * or equal to ReceiveMTU, then no data will be lost. Unlike {@code read()} on an 
	 * {@code java.io.InputStream}, if the size of inBuf is smaller than ReceiveMTU, then 
	 * the portion of the L2CAP payload that will fit into inBuf will be placed 
	 * in inBuf, the rest will be discarded. If the application is aware of the 
	 * number of bytes (less than ReceiveMTU) it will receive in any transaction, 
	 * then the size of {@code inBuf} can be less than ReceiveMTU and no data will be lost. 
	 * If {@code inBuf} is of length 0, all data sent in one packet is lost unless the 
	 * length of the packet is 0.
	 * 
	 * @param 	inBuf byte array to store the received data
	 * @return 	the actual number of bytes read; 0 if a zero length packet is 
	 * 			received; 0 if inBuf length is zero
	 * @throws 	java.io.IOException  	if an I/O error occurs or the connection 
	 * 									has been closed
	 * @throws	InterruptedIOException 	if the request timed out
	 * @throws	NullPointerException 	if inBuf is {@code null}
	 */
	public int receive(byte[] inBuf) throws java.io.IOException;

	/**
	 * Determines if there is a packet that can be read via a call to {@code receive()}. 
	 * If {@code true}, a call to {@code receive()} will not block the application.
	 * 
	 * @return	{@code true} if there is data to read; {@code false} if there is no data to read
	 * @throws java.io.IOException  if the connection is closed
	 * @see		#receive(byte[])
	 */
	public boolean ready() throws java.io.IOException;
}
