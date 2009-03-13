/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2008-2009 Vlad Skarzhevskyy
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
 *  @version $Id: NativeExceptionTest.java 1570 2008-01-16 22:15:56Z skarzhevskyy $
 */
package com.intel.bluetooth;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

import javax.bluetooth.DataElement;
import javax.bluetooth.UUID;

/**
 *
 */
public class NativeServiceRecordTest extends NativeTestCase {

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
