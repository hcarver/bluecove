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
package net.sf.bluecove;

import java.util.Enumeration;
import java.util.Vector;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.DataElement;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.util.RuntimeDetect;

import junit.framework.Assert;
import junit.framework.AssertionFailedError;
import net.sf.bluecove.util.BluetoothTypesInfo;
import net.sf.bluecove.util.CollectionUtils;

/**
 * 
 */
public class ServiceRecordTester {

	public static final int ServiceClassIDList = 0x0001;

	private static Vector allTestServiceAttributes = new Vector();

	static {
		buildAllTestServiceAttributes();
	}

	public static boolean hasServiceClassUUID(ServiceRecord servRecord, UUID uuid) {
		DataElement attrDataElement = servRecord.getAttributeValue(ServiceClassIDList);
		if ((attrDataElement == null) || (attrDataElement.getDataType() != DataElement.DATSEQ)
				|| attrDataElement.getSize() == 0) {
			Logger.warn("Bogus ServiceClassIDList");
			if (RuntimeDetect.isBlueCove) {
				return false;
			}

			// Avetana version 3.17
			if ((attrDataElement != null) && (attrDataElement.getDataType() == DataElement.UUID)) {
				return uuid.equals(attrDataElement.getValue());
			}

			return false;
		}
		// Logger.debug("test ServiceClassIDList:" +
		// BluetoothTypesInfo.toString(attrDataElement));

		Object value = attrDataElement.getValue();
		if ((value == null) || (!(value instanceof Enumeration))) {
			Logger.warn("Bogus Value in DATSEQ");
			if (value != null) {
				Logger.error("DATSEQ class " + value.getClass().getName());
			}
			return false;
		}
		for (Enumeration e = (Enumeration) value; e.hasMoreElements();) {
			Object element = e.nextElement();
			if (!(element instanceof DataElement)) {
				Logger.warn("Bogus element in DATSEQ, " + value.getClass().getName());
				continue;
			}
			DataElement dataElement = (DataElement) element;
			if ((dataElement.getDataType() == DataElement.UUID)) {
				if (uuid.equals(dataElement.getValue())) {
					return true;
				} else {
					// Logger.debug("not same " +
					// BluetoothTypesInfo.toString((UUID)dataElement.getValue()));
				}
			} else {
				// Logger.debug("test not UUID:" +
				// BluetoothTypesInfo.toString(dataElement));
			}
		}

		return false;
	}

	public static boolean hasServiceClassBlieCoveUUID(ServiceRecord servRecord) {
		return hasServiceClassUUID(servRecord, Configuration.blueCoveUUID())
				|| hasServiceClassUUID(servRecord, Configuration.blueCoveL2CAPUUID());
	}

	public static boolean equals(DataElement de1, DataElement de2) {
		if ((de1 == null) || (de2 == null)) {
			return false;
		}
		try {
			if (de1.getDataType() != de2.getDataType()) {
				return false;
			}
			switch (de1.getDataType()) {
			case DataElement.U_INT_1:
			case DataElement.U_INT_2:
			case DataElement.U_INT_4:
			case DataElement.INT_1:
			case DataElement.INT_2:
			case DataElement.INT_4:
			case DataElement.INT_8:
				return (de1.getLong() == de2.getLong());
			case DataElement.URL:
			case DataElement.STRING:
			case DataElement.UUID:
				return de1.getValue().equals(de2.getValue());
			case DataElement.INT_16:
			case DataElement.U_INT_8:
			case DataElement.U_INT_16:
				byte[] byteAray1 = (byte[]) de1.getValue();
				byte[] byteAray2 = (byte[]) de2.getValue();
				if (byteAray1.length != byteAray2.length) {
					return false;
				}
				for (int k = 0; k < byteAray1.length; k++) {
					if (byteAray1[k] != byteAray2[k]) {
						return false;
					}
				}
				return true;
			case DataElement.NULL:
				return true;
			case DataElement.BOOL:
				return (de1.getBoolean() == de2.getBoolean());
			case DataElement.DATSEQ:
			case DataElement.DATALT:
				Enumeration en1 = (Enumeration) de1.getValue();
				Enumeration en2 = (Enumeration) de2.getValue();
				for (; en1.hasMoreElements() && en2.hasMoreElements();) {
					DataElement d1 = (DataElement) en1.nextElement();
					DataElement d2 = (DataElement) en2.nextElement();
					if (!equals(d1, d2)) {
						return false;
					}
				}
				if (en1.hasMoreElements() || en2.hasMoreElements()) {
					return false;
				}
				return true;
			default:
				return false;
			}
		} catch (Throwable e) {
			Logger.error("DataElement equals", e);
			return false;
		}
	}

	public static boolean testServiceAttributes(ServiceRecord servRecord, String servicesOnDeviceName,
			String servicesOnDeviceAddress) {

		boolean isBlueCoveTestService = false;

		boolean hadError = false;

		long variableData = 0;

		if (!Configuration.testServiceAttributes.booleanValue()
				|| ("0".equals(LocalDevice.getProperty("bluetooth.sd.attr.retrievable.max")))) {
			return hasServiceClassBlieCoveUUID(servRecord);
		}

		boolean canTestLong = true;
		DataElement flagDataElement = servRecord.getAttributeValue(Consts.TEST_SERVICE_ATTRIBUTE_INT_ID);
		if (flagDataElement != null) {
			if (flagDataElement.getLong() == Consts.TEST_SERVICE_ATTRIBUTE_INT_VALUE) {
				canTestLong = false;
			}
		}
		if (canTestLong && Configuration.testAllServiceAttributes.booleanValue()) {
			isBlueCoveTestService = hasServiceClassBlieCoveUUID(servRecord);
			if (isBlueCoveTestService) {
				compareAllServiceAttributes(servRecord, servicesOnDeviceName);
			} else {
				Logger.debug("NOT a BlueCove service");
			}
			return isBlueCoveTestService;
		}
		if (!canTestLong && Configuration.testAllServiceAttributes.booleanValue()) {
			Logger.info("can't test all service Attributes");
		}

		try {
			int[] attributeIDs = servRecord.getAttributeIDs();
			// Logger.debug("attributes " + attributeIDs.length);

			boolean foundName = false;
			boolean foundInt = false;
			boolean foundStr = false;
			boolean foundUrl = false;
			boolean foundLong = false;
			boolean foundBytes = false;

			boolean foundIntOK = false;
			boolean foundUrlOK = false;
			boolean foundBytesOK = false;

			for (int j = 0; j < attributeIDs.length; j++) {
				int id = attributeIDs[j];
				try {
					DataElement attrDataElement = servRecord.getAttributeValue(id);
					Assert.assertNotNull("attrValue null", attrDataElement);
					switch (id) {
					case BluetoothTypesInfo.ServiceClassIDList:
						if (!hasServiceClassBlieCoveUUID(servRecord)) {
							TestResponderClient.failure.addFailure("ServiceClassUUID not found on "
									+ servicesOnDeviceName);
						} else {
							isBlueCoveTestService = true;
							if (Configuration.useServiceClassExtUUID.booleanValue()
									&& !hasServiceClassUUID(servRecord, Consts.uuidSrvClassExt)) {
								Logger.warn("srv SrvClassExt attr. not found");
								TestResponderClient.failure.addFailure("SrvClassExt UUID not found on "
										+ servicesOnDeviceName);
							}
						}
						break;
					case 0x0100:
						foundName = true;
						if (!Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()) {
							String nameValue = (String) attrDataElement.getValue();
							Assert.assertTrue("name [" + nameValue + "]", nameValue
									.startsWith((Consts.RESPONDER_SERVERNAME)));
							isBlueCoveTestService = true;
						}
						break;
					case Consts.TEST_SERVICE_ATTRIBUTE_INT_ID:
						foundInt = true;
						Assert.assertEquals("int type", Consts.TEST_SERVICE_ATTRIBUTE_INT_TYPE, attrDataElement
								.getDataType());
						Assert.assertEquals("int", Consts.TEST_SERVICE_ATTRIBUTE_INT_VALUE, attrDataElement.getLong());
						isBlueCoveTestService = true;
						foundIntOK = true;
						break;
					case Consts.TEST_SERVICE_ATTRIBUTE_LONG_ID:
						foundLong = true;
						Assert.assertEquals("long type", Consts.TEST_SERVICE_ATTRIBUTE_LONG_TYPE, attrDataElement
								.getDataType());
						if (!Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()) {
							Assert.assertEquals("long", Consts.TEST_SERVICE_ATTRIBUTE_LONG_VALUE, attrDataElement
									.getLong());
							isBlueCoveTestService = true;
						}
						break;
					case Consts.TEST_SERVICE_ATTRIBUTE_STR_ID:
						foundStr = true;
						Assert.assertEquals("str type", DataElement.STRING, attrDataElement.getDataType());
						if (!Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()) {
							Assert.assertEquals("str", Consts.TEST_SERVICE_ATTRIBUTE_STR_VALUE, attrDataElement
									.getValue());
							isBlueCoveTestService = true;
						}
						break;
					case Consts.TEST_SERVICE_ATTRIBUTE_URL_ID:
						foundUrl = true;
						int urlType = attrDataElement.getDataType();
						// URL is String on Widcomm
						Assert.assertTrue("url type", (DataElement.URL == urlType) || (DataElement.STRING == urlType));
						if (DataElement.URL != urlType) {
							Logger.warn("attr URL decoded as STRING");
						}
						Assert.assertEquals("url", Consts.TEST_SERVICE_ATTRIBUTE_URL_VALUE, attrDataElement.getValue());
						isBlueCoveTestService = true;
						foundUrlOK = true;
						break;
					case Consts.TEST_SERVICE_ATTRIBUTE_BYTES_ID:
						foundBytes = true;
						String byteArrayTypeName = BluetoothTypesInfo
								.toStringDataElementType(Consts.TEST_SERVICE_ATTRIBUTE_BYTES_TYPE);
						Assert.assertEquals("byte[] " + byteArrayTypeName + " type",
								Consts.TEST_SERVICE_ATTRIBUTE_BYTES_TYPE, attrDataElement.getDataType());
						byte[] byteAray;
						try {
							byteAray = (byte[]) attrDataElement.getValue();
						} catch (Throwable e) {
							Logger.warn("attr  " + byteArrayTypeName + " " + id + " " + e.getMessage());
							hadError = true;
							break;
						}
						Assert.assertEquals("byteAray.len of " + byteArrayTypeName,
								Consts.TEST_SERVICE_ATTRIBUTE_BYTES_VALUE.length, byteAray.length);
						for (int k = 0; k < byteAray.length; k++) {
							if (Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()
									&& Configuration.stackWIDCOMM && k >= 4) {
								// INT_16 are truncated in discovery
								break;
							}
							Assert.assertEquals("byte[" + k + "] of " + byteArrayTypeName,
									Consts.TEST_SERVICE_ATTRIBUTE_BYTES_VALUE[k], byteAray[k]);
						}
						isBlueCoveTestService = true;
						foundBytesOK = true;
						break;
					case Consts.VARIABLE_SERVICE_ATTRIBUTE_BYTES_ID:
						Assert.assertEquals("var U_INT_4 type", DataElement.U_INT_4, attrDataElement.getDataType());
						try {
							variableData = attrDataElement.getLong();
							// Logger.debug("Var info:" + variableData);
						} catch (Throwable e) {
							Logger.warn("attr " + id + " " + e.getMessage());
							hadError = true;
						}
						break;
					case Consts.SERVICE_ATTRIBUTE_BYTES_SERVER_INFO:
						Logger.debug("Server info:" + attrDataElement.getValue());
						try {
							Assert.assertEquals("BTAddress", servicesOnDeviceAddress.toUpperCase(),
									getAddressFromBTSystemInfo(attrDataElement.getValue().toString()));
						} catch (AssertionFailedError e) {
							Logger.error("Wrong SR on " + servicesOnDeviceName, e);
							TestResponderClient.failure.addFailure("Wrong SR on " + servicesOnDeviceName, e);
							return false;
						}
						break;
					default:
						if (!Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()) {
							Logger.debug("attribute " + id + " "
									+ BluetoothTypesInfo.toStringDataElementType(attrDataElement.getDataType()));
						}
					}

				} catch (AssertionFailedError e) {
					Logger.warn("attr " + id + " " + e.getMessage());
					// countFailure++;
					hadError = true;
				}
			}
			if ((!Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()) && (!foundName)) {
				Logger.warn("srv name attr. not found");
				TestResponderClient.failure.addFailure("srv name attr. not found on " + servicesOnDeviceName);
			}
			if (!foundInt) {
				Logger.warn("srv INT attr. not found");
				TestResponderClient.failure.addFailure("srv INT attr. not found on " + servicesOnDeviceName);
			}
			if ((!Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()) && (!foundLong)) {
				Logger.warn("srv long attr. not found");
				TestResponderClient.failure.addFailure("srv long attr. not found on " + servicesOnDeviceName);
			}
			if ((!Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()) && (!foundStr)) {
				Logger.warn("srv STR attr. not found");
				TestResponderClient.failure.addFailure("srv STR attr. not found on " + servicesOnDeviceName);
			}
			if (!foundUrl) {
				Logger.warn("srv URL attr. not found");
				TestResponderClient.failure.addFailure("srv URL attr. not found on " + servicesOnDeviceName);
			}
			if (!foundBytes) {
				Logger.warn("srv byte[] attr. not found");
				TestResponderClient.failure.addFailure("srv byte[] attr. not found on " + servicesOnDeviceName);
			}
			// if (variableData == 0) {
			// Logger.warn("srv var data attr. not found");
			// TestResponderClient.failure.addFailure("srv var data attr. not
			// found on " + servicesOnDeviceName);
			// }
			if (foundName && foundUrl && foundInt && foundStr && foundLong && foundBytes && !hadError) {
				Logger.info("all service Attr OK");
				TestResponderClient.countSuccess++;
			} else if ((Configuration.testIgnoreNotWorkingServiceAttributes.booleanValue()) && foundUrl && foundInt
					&& foundBytes && !hadError) {
				Logger.info("service Attr found");
				TestResponderClient.countSuccess++;
			}
			if (foundIntOK && foundUrlOK && foundBytesOK) {
				Logger.info("Common Service Attr OK");
			}
		} catch (Throwable e) {
			Logger.error("attrs", e);
		}

		if (isBlueCoveTestService) {
			RemoteDeviceInfo.deviceServiceFound(servRecord.getHostDevice(), variableData);
		}

		return isBlueCoveTestService;
	}

	private static void buildAllTestServiceAttributes() {
		try {

			final boolean testIntTypes = true;
			final boolean testSimpleSequence = true;

			final boolean extraTestNULL = false;
			final boolean extraTestInt = false;
			final boolean extraTestInt16 = false;
			final boolean extraTestUUIDTypes = false;
			final boolean extraTestComplextSequence = false;
			final boolean extraTestLargeSequence = false;

			if (extraTestNULL) {
				allTestServiceAttributes.addElement(new DataElement(DataElement.NULL));
			}

			if (testIntTypes) {
				// Just some arbitrary number the same on client and server.
				allTestServiceAttributes.addElement(new DataElement(DataElement.U_INT_1, 0xBC));
				allTestServiceAttributes.addElement(new DataElement(DataElement.U_INT_2, 0xABCD));
				allTestServiceAttributes.addElement(new DataElement(DataElement.U_INT_4, 0xABCDEF40l));
				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_1, -0x1E));
				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_2, -0x7EFD));
				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_4, -0x2BC7EF35l));
				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_8, -0x7F893012AB39FB72l));
			}

			if (extraTestInt16) {
				allTestServiceAttributes.addElement(new DataElement(DataElement.U_INT_8, new byte[] { 1, -2, 3, 4, -5,
						6, 7, -8 }));
				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_16, new byte[] { 11, -22, 33, 44,
						-5, 6, 77, 88, 9, -10, 11, 12, -13, 14, 15, 16 }));
				allTestServiceAttributes.addElement(new DataElement(DataElement.U_INT_16, new byte[] { 21, -32, 43, 54,
						-65, 76, 87, 98, 11, -110, 111, 112, -113, 114, 115, 16 }));
			}

			// There are limit on number of attributes we can test on WIDCOMM
			if (extraTestInt) {
				allTestServiceAttributes.addElement(new DataElement(DataElement.U_INT_1, 0));

				allTestServiceAttributes.addElement(new DataElement(DataElement.U_INT_2, 0));
				allTestServiceAttributes.addElement(new DataElement(DataElement.U_INT_4, 0));

				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_1, 0));
				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_1, 0x4C));

				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_2, 0));
				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_2, 0x5BCD));

				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_4, 0));
				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_4, 0x1BCDEF35l));

				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_8, 0));
				allTestServiceAttributes.addElement(new DataElement(DataElement.INT_8, 0x3eC6EF355892EA8Cl));

			}

			allTestServiceAttributes.addElement(new DataElement(DataElement.UUID, new UUID(
					"E10C0FE1121111A11111161911110003", false)));

			if (extraTestUUIDTypes) {
				allTestServiceAttributes.addElement(new DataElement(DataElement.UUID, new UUID(
						"0000110500001000800000805f9b34fb", false)));
				allTestServiceAttributes.addElement(new DataElement(DataElement.UUID, new UUID(0x1105)));
				allTestServiceAttributes.addElement(new DataElement(DataElement.UUID, new UUID(0x21301107)));
				allTestServiceAttributes.addElement(new DataElement(DataElement.UUID, new UUID(
						"2130110800001000800000805f9b34fb", false)));
			}

			if (testSimpleSequence) {
				allTestServiceAttributes.addElement(new DataElement(DataElement.STRING, "BlueCove-2007"));
				allTestServiceAttributes
						.addElement(new DataElement(DataElement.STRING, CommunicationData.stringUTFData));
				DataElement seq1 = new DataElement(DataElement.DATSEQ);
				seq1.addElement(new DataElement(DataElement.STRING, "BlueCove-seq1"));
				seq1.addElement(new DataElement(DataElement.U_INT_1, 0x12));
				allTestServiceAttributes.addElement(seq1);
				allTestServiceAttributes.addElement(new DataElement(true));
			}

			if (extraTestComplextSequence) {
				DataElement seq2 = new DataElement(DataElement.DATSEQ);
				DataElement seq21 = new DataElement(DataElement.DATSEQ);
				seq21.addElement(new DataElement(DataElement.STRING, "BlueCove-seq2.1"));
				seq21.addElement(new DataElement(DataElement.U_INT_1, 0x22));
				seq2.addElement(seq21);
				DataElement seq22 = new DataElement(DataElement.DATSEQ);
				seq22.addElement(new DataElement(DataElement.STRING, "BlueCove-seq2.2"));
				seq22.addElement(new DataElement(DataElement.U_INT_2, 0x2));
				seq22.addElement(new DataElement(DataElement.U_INT_2, 0x3));
				// This do not work on WIDCOMM
				// DataElement seq23 = new DataElement(DataElement.DATSEQ);
				// seq23.addElement(new DataElement(DataElement.STRING,
				// "BlueCove-seq2.3"));
				// seq22.addElement(seq23);
				seq2.addElement(seq22);
				// This do not work on WIDCOMM
				// seq2.addElement(new DataElement(DataElement.U_INT_1, 0x44));
				allTestServiceAttributes.addElement(seq2);
			}

			if (extraTestLargeSequence) {
				DataElement seqLong = new DataElement(DataElement.DATSEQ);
				for (int i = 0; i < 100; i++) {
					seqLong.addElement(new DataElement(DataElement.STRING, "BlueCove-long-seq " + i));
				}
				allTestServiceAttributes.addElement(seqLong);
			}

		} catch (Throwable e) {
			Logger.error("attrs create", e);
		}
	}

	public static void addAllTestServiceAttributes(ServiceRecord servRecord) {

		servRecord.setAttributeValue(Consts.TEST_SERVICE_ATTRIBUTE_INT_ID, new DataElement(
				Consts.TEST_SERVICE_ATTRIBUTE_INT_TYPE, Consts.TEST_SERVICE_ATTRIBUTE_INT_VALUE_TEST_ALL));

		for (int i = 0; i < allTestServiceAttributes.size(); i++) {
			DataElement de = (DataElement) allTestServiceAttributes.elementAt(i);
			servRecord.setAttributeValue(Consts.SERVICE_ATTRIBUTE_ALL_START + i, de);
		}
	}

	public static int allTestServiceAttributesSize() {
		return allTestServiceAttributes.size();
	}

	public static void compareAllServiceAttributes(ServiceRecord servRecord, String servicesOnDeviceName) {
		int[] ids = servRecord.getAttributeIDs();
		if (ids == null) {
			String errorText = "attributes are NULL";
			Logger.error(errorText);
			TestResponderClient.failure.addFailure(errorText + " on " + servicesOnDeviceName);
			return;
		}
		if (ids.length == 0) {
			String errorText = "not attributes";
			Logger.error(errorText);
			TestResponderClient.failure.addFailure(errorText + " on " + servicesOnDeviceName);
			return;
		}
		int countError = 0;
		int countSuccess = 0;
		int countFound = 0;
		boolean[] found = new boolean[allTestServiceAttributes.size()];
		Vector sorted = new Vector();
		for (int i = 0; i < ids.length; i++) {
			sorted.addElement(new Integer(ids[i]));
		}
		CollectionUtils.sort(sorted);
		for (Enumeration en = sorted.elements(); en.hasMoreElements();) {
			int id = ((Integer) en.nextElement()).intValue();
			int index = id - Consts.SERVICE_ATTRIBUTE_ALL_START;
			if ((index < 0) || (index > allTestServiceAttributes.size())) {
				continue;
			}
			found[index] = true;
			countFound++;
			DataElement deGot = servRecord.getAttributeValue(id);
			DataElement deExpect = (DataElement) allTestServiceAttributes.elementAt(index);
			if (equals(deGot, deExpect)) {
				Logger.debug("ServAttr OK " + BluetoothTypesInfo.toString(deGot));
				countSuccess += 1;
			} else {
				countError += 1;
				Logger.error("ServAttr " + id + " expected " + BluetoothTypesInfo.toString(deExpect));
				Logger.error("ServAttr " + id + " received " + BluetoothTypesInfo.toString(deGot));
			}
		}

		if (countSuccess != allTestServiceAttributes.size()) {
			if (countFound != allTestServiceAttributes.size()) {
				String errorText = "missing attributes, found " + countFound + " expect "
						+ allTestServiceAttributes.size();
				Logger.error(errorText);
				TestResponderClient.failure.addFailure(errorText + " on " + servicesOnDeviceName);
			}

			if (countSuccess != 0) {
				for (int i = 0; i < allTestServiceAttributes.size(); i++) {
					if (found[i]) {
						continue;
					}
					DataElement de = (DataElement) allTestServiceAttributes.elementAt(i);
					Logger.error("ServAttr missing " + BluetoothTypesInfo.toString(de));
				}
			}
		} else {
			Logger.info("All Service Attr found - OK");
			TestResponderClient.countSuccess++;
		}
	}

	private static final String ADDRESS = "address:";

	public static String getBTSystemInfo() {
		try {
			LocalDevice localDevice = LocalDevice.getLocalDevice();
			StringBuffer buf = new StringBuffer();
			buf.append(ADDRESS).append(localDevice.getBluetoothAddress()).append(";");
			buf.append(" name:").append(localDevice.getFriendlyName()).append(";");
			return buf.toString();
		} catch (BluetoothStateException e) {
			return "error";
		}
	}

	public static String getAddressFromBTSystemInfo(String sysInfoAttr) {
		if ((sysInfoAttr == null) || (sysInfoAttr.length() < 6)) {
			return null;
		}
		int startIndex = sysInfoAttr.indexOf(ADDRESS);
		if (startIndex == -1) {
			return null;
		}
		startIndex += ADDRESS.length();
		int endIndex = sysInfoAttr.indexOf(';', startIndex);
		if (endIndex == -1) {
			return null;
		}
		return sysInfoAttr.substring(startIndex, endIndex).toUpperCase();
	}
}
