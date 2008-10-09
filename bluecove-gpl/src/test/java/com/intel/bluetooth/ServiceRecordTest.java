/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008 Vlad Skarzhevskyy
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
 *  @version $Id: NativeExceptionTest.java 1570 2008-01-16 22:15:56Z skarzhevskyy $
 */
package com.intel.bluetooth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

/**
 * @author vlads
 * 
 */
public class ServiceRecordTest extends NativeTestCase {

	public void validateServiceRecordConvert(ServiceRecordImpl serviceRecord) throws IOException {
		byte[] inRecordData = serviceRecord.toByteArray();
		DebugLog.debug("inRecordData", inRecordData);
		byte[] nativeRecord = BluetoothStackBlueZNativeTests.testServiceRecordConvert(inRecordData);
		DebugLog.debug("nativeRecord", nativeRecord);
		assertEquals("length", inRecordData.length, nativeRecord.length);
		for (int k = 0; k < inRecordData.length; k++) {
			assertEquals("byteAray[" + k + "]", inRecordData[k], nativeRecord[k]);
		}

	}

	public void testServiceRecordConvert() throws IOException {
		ServiceRecordImpl serviceRecord = new ServiceRecordImpl(null, null, 0);
		serviceRecord.populateL2CAPAttributes(1, 2, new UUID(3), "BBBB");
		validateServiceRecordConvert(serviceRecord);
	}
	
	public void xtestServiceRecordConvertLarge() throws IOException {
		ServiceRecordImpl serviceRecord = new ServiceRecordImpl(null, null, 0);
		serviceRecord.populateL2CAPAttributes(1, 2, new UUID(3), "BBBB");
		
		final int baseID = 0x200;
		DataElement base = new DataElement(DataElement.DATSEQ);
		serviceRecord.setAttributeValue(baseID, base);
		
		for (int i = 0; i < 253; i++) {
			DataElement d;
			//d = new DataElement(DataElement.STRING, "C");
			d = new DataElement(DataElement.NULL);
			base.addElement(d);
		}
		
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		(new SDPOutputStream(out)).writeElement(base);
		byte bp[] = out.toByteArray();
		System.out.println("DATSEQ LEN " + bp.length);
		
		validateServiceRecordConvert(serviceRecord);
	}
}
