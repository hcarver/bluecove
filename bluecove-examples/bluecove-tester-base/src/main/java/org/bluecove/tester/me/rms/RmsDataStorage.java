/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2009 Vlad Skarzhevskyy
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
package org.bluecove.tester.me.rms;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.rms.RecordEnumeration;
import javax.microedition.rms.RecordStore;
import javax.microedition.rms.RecordStoreException;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.IOUtils;

public abstract class RmsDataStorage {

    /**
     * 
     * @param recordStoreName
     * @param list
     *            can be null, then load only one first Item
     * @param instance
     * @return true in case of success
     */
    public static boolean load(String recordStoreName, Vector list, RmsStorable instance) {
        RecordStore recordStore = null;
        try {
            // RecordStore.deleteRecordStore(name);
            recordStore = RecordStore.openRecordStore(recordStoreName, false);
        } catch (RecordStoreException e) {
            return false;
        }
        try {
            RecordEnumeration re = recordStore.enumerateRecords(null, null, true);

            while (re.hasNextElement()) {
                int id = re.nextRecordId();
                ByteArrayInputStream bais = new ByteArrayInputStream(recordStore.getRecord(id));
                DataInputStream inputStream = new DataInputStream(bais);
                try {
                    if (list == null) {
                        instance.setRmsId(id);
                        instance.readExternal(inputStream);
                        break;
                    } else {
                        RmsStorable item = instance.newInstance(id);
                        item.readExternal(inputStream);
                        list.addElement(item);
                    }
                } catch (IOException e) {
                    Logger.error("RMS decode", e);
                    return false;
                }
            }
        } catch (RecordStoreException e) {
            Logger.error("RMS read", e);
            return false;
        } finally {
            closeQuietly(recordStore);
        }
        return true;
    }

    public static void deleteRecordStore(String recordStoreName) {
        try {
            RecordStore.deleteRecordStore(recordStoreName);
        } catch (Exception rse) {
        }
    }

    public static boolean saveAll(String recordStoreName, Vector list) {
        // Delete any previous record store with same name.
        // Silently ignore failure.
        try {
            RecordStore.deleteRecordStore(recordStoreName);
        } catch (Exception rse) {
        }

        RecordStore recordStore = null;
        // Create new RMS store. If we fail, return false.
        try {
            recordStore = RecordStore.openRecordStore(recordStoreName, true);
        } catch (RecordStoreException rse) {
            return false;
        }
        try {
            for (Enumeration en = list.elements(); en.hasMoreElements();) {
                try {
                    RmsStorable item = (RmsStorable) en.nextElement();
                    byte[] b = IOUtils.writeByteArray(item);
                    item.setRmsId(recordStore.addRecord(b, 0, b.length));
                } catch (Throwable e) {
                    Logger.error("RMS write", e);
                    return false;
                }
            }
        } finally {
            closeQuietly(recordStore);
        }
        return true;
    }

    public static boolean update(String recordStoreName, RmsStorable item) {
        RecordStore recordStore = null;
        // Create new RMS store. If we fail, return false.
        try {
            recordStore = RecordStore.openRecordStore(recordStoreName, true);
        } catch (RecordStoreException rse) {
            return false;
        }

        try {
            byte[] b = IOUtils.writeByteArray(item);
            if (item.getRmsId() == -1) {
                item.setRmsId(recordStore.addRecord(b, 0, b.length));
            } else {
                recordStore.setRecord(item.getRmsId(), b, 0, b.length);
            }
            return true;
        } catch (Throwable e) {
            Logger.error("RMS write", e);
            return false;
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
