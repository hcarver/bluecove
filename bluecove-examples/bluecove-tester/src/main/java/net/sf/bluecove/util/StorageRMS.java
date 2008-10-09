/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2007 Vlad Skarzhevskyy
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
package net.sf.bluecove.util;

import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import net.sf.bluecove.Logger;

/**
 * @author vlads
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
