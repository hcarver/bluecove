/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package net.sf.bluecove.util;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import org.bluecove.tester.log.Logger;


/**
 *
 */
public class StorageRMS implements Storage {

	public String retriveData(String name) {
		RecordStore recordStore = null;
		try {
			recordStore = RecordStore.openRecordStore("BlueCove" + name, false);
			if (recordStore.getNumRecords() > 0) {
				int recordId = 1;
				byte[] data = recordStore.getRecord(recordId);
				return new String(data);
			} else {
				// recordStore empty
				return null;
			}
		} catch (Throwable e) {
			Logger.error("error accessing RecordStore", e);
			return null;
		} finally {
			closeQuietly(recordStore);
		}
	}

	public void storeData(String name, String value) {
		RecordStore recordStore = null;
		try {
			recordStore = RecordStore.openRecordStore("BlueCove" + name, true);
			byte[] data = value.getBytes();
			int recordId;
			if (recordStore.getNumRecords() > 0) {
				recordId = 1;
				recordStore.setRecord(recordId, data, 0, data.length);
			} else {
				recordId = recordStore.addRecord(data, 0, data.length);
			}
		} catch (Throwable e) {
			Logger.error("error accessing RecordStore", e);
		} finally {
			closeQuietly(recordStore);
		}
	}

	public static void closeQuietly(RecordStore recordStore) {
		try {
			if (recordStore != null) {
				recordStore.closeRecordStore();
			}
		} catch (RecordStoreException ignore) {
			// ignore
		}
	}
}
