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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import javax.bluetooth.DataElement;
import javax.bluetooth.DeviceClass;
import javax.bluetooth.ServiceRecord;
import javax.bluetooth.UUID;
import javax.obex.ResponseCodes;

import net.sf.bluecove.Consts;

public abstract class BluetoothTypesInfo {

	public static final String NULL = "{null}";

	public static final String PROTOCOL_SCHEME_L2CAP = "btl2cap";

	public static final String PROTOCOL_SCHEME_RFCOMM = "btspp";

	public static final String PROTOCOL_SCHEME_BT_OBEX = "btgoep";

	public static final String PROTOCOL_SCHEME_TCP_OBEX = "tcpobex";

	public static final int ServiceClassIDList = 0x0001;

	public static String extractBluetoothAddress(String serverURL) {
		int start = serverURL.indexOf("//");
		if (start == -1) {
			return null;
		}
		start += 2;
		int end = serverURL.indexOf(":", start);
		if (end == -1) {
			return null;
		}
		return serverURL.substring(start, end);
	}

	public static boolean isRFCOMM(String serverURL) {
		return ((serverURL != null) && (serverURL.startsWith(PROTOCOL_SCHEME_RFCOMM)));
	}

	public static boolean isL2CAP(String serverURL) {
		return ((serverURL != null) && (serverURL.startsWith(PROTOCOL_SCHEME_L2CAP)));
	}

	public static class UUIDConsts {

		private static String SHORT_BASE = "00001000800000805F9B34FB";

		private static Hashtable uuidNames = new Hashtable();

		private static void addName(String uuid, String name) {
			uuidNames.put(uuid.toUpperCase(), name);
		}

		private static void addName(int uuid, String name) {
			addName(new UUID(uuid).toString(), name);
		}

		static {
			addName(0x0001, "SDP");
			addName(0x0002, "UDP");
			addName(0x0003, "RFCOMM");
			addName(0x0004, "TCP");
			addName(0x0008, "OBEX");
			addName(0x000C, "HTTP");
			addName(0x000F, "BNEP");
			addName(0x0100, "L2CAP");

			addName(0x1000, "SDP_SERVER");
			addName(0x1001, "BROWSE_GROUP_DESCRIPTOR");
			addName(0x1002, "PUBLICBROWSE_GROUP");
			addName(0x1101, "SERIAL_PORT");
			addName(0x1102, "LAN_ACCESS_PPP");
			addName(0x1103, "DIALUP_NETWORKING");
			addName(0x1104, "IR_MC_SYNC");
			addName(0x1105, "OBEX_OBJECT_PUSH");
			addName(0x1106, "OBEX_FILE_TRANSFER");
			addName(0x1107, "IR_MC_SYNC_COMMAND");
			addName(0x1108, "HEADSET");
			addName(0x1109, "CORDLESS_TELEPHONY");
			addName(0x110A, "AUDIO_SOURCE");
			addName(0x110B, "AUDIO_SINK");
			addName(0x110C, "AV_REMOTE_CTL_TARGET");
			addName(0x110D, "ADVANCED_AUDIO_DISTRIB");
			addName(0x110E, "AV_REMOTE_CTL");
			addName(0x110F, "VIDEO_CONFERENCING");
			addName(0x1110, "INTERCOM");
			addName(0x1111, "FAX");
			addName(0x1112, "HEADSET_AUDIO_GATEWAY");
			addName(0x1113, "WAP");
			addName(0x1114, "WAP_CLIENT");
			addName(0x1115, "PAN_USER");
			addName(0x1116, "NETWORK_ACCESS_POINT");
			addName(0x1117, "GROUP_NETWORK");
			addName(0x1118, "DIRECT_PRINTING");
			addName(0x1119, "REFERENCE_PRINTING");
			addName(0x111A, "IMG");
			addName(0x111B, "IMG_RESPONDER");
			addName(0x111C, "IMG_AUTO_ARCHIVE");
			addName(0x111D, "IMG_REFERENCE_OBJECTS");
			addName(0x111E, "HANDSFREE");
			addName(0x111F, "HANDSFREE_AUDIO_GATEWAY");
			addName(0x1120, "DIRECT_PRINT");
			addName(0x1121, "REFLECTED_UI");
			addName(0x1122, "BASIC_PRINTING");
			addName(0x1123, "PRINTING_STATUS");
			addName(0x1124, "HI_DEVICE");
			addName(0x1125, "HARD_COPY_CABLE_REPLACE");
			addName(0x1126, "HCR_PRINT");
			addName(0x1127, "HCR_SCAN");
			addName(0x1128, "COMMON_ISDN_ACCESS");
			addName(0x1129, "VIDEO_CONF_GW");
			addName(0x112A, "UDI_MT");
			addName(0x112B, "UDI_TA");
			addName(0x112C, "AUDIO_VIDEO");
			addName(0x112D, "SIM_ACCESS");
			addName(0x1200, "PNP_INFO");
			addName(0x1201, "GENERIC_NETWORKING");
			addName(0x1202, "GENERIC_FILE_TRANSFER");
			addName(0x1203, "GENERIC_AUDIO");
			addName(0x1204, "GENERIC_TELEPHONY");
			addName(0x1205, "UPNP_SERVICE");
			addName(0x1206, "UPNP_IP_SERVICE");
			addName(0x1300, "ESDP_UPNP_IP_PAN");
			addName(0x1301, "ESDP_UPNP_IP_LAP");
			addName(0x1302, "ESDP_UPNP_L2CAP");

			addName(Consts.RESPONDER_SHORT_UUID, "BlueCoveT RFCOMM short");
			addName(Consts.RESPONDER_LONG_UUID, "BlueCoveT RFCOMM long");

			addName(Consts.RESPONDER_SHORT_UUID_L2CAP, "BlueCoveT L2CAP short");
			addName(Consts.RESPONDER_LONG_UUID_L2CAP, "BlueCoveT L2CAP long");

			addName(Consts.RESPONDER_LONG_UUID_OBEX, "BlueCoveT OBEX");

			addName(Consts.RESPONDER_SERVICECLASS_UUID, "BlueCoveT SrvClassExt");

			addName("3B9FA89520078C303355AAA694238F07", "JSR-82 TCK L2CAP AGENT");
			addName("2000000031b811d88698000874b33fc0", "JSR-82 TCK RFCOMM AGENT");
			addName("3000000031b811d88698000874b33fc0", "JSR-82 TCK BTGOEP AGENT");
			addName("4000000031b811d88698000874b33fc0", "JSR-82 OBEX TCK AGENT");
			addName("102030405060708090A1B1C1D1D1E100", "JSR-82 TCK");

		}

		public static String getName(UUID uuid) {
			if (uuid == null) {
				return null;
			}
			String str = uuid.toString().toUpperCase();
			String name = (String) uuidNames.get(str);
			if (name != null) {
				return name;
			}
			int shortIdx = str.indexOf(SHORT_BASE);
			if ((shortIdx != -1) && (shortIdx + SHORT_BASE.length() == str.length())) {
				// This is short 16-bit or 32-bit UUID
				return toHexString(Integer.parseInt(str.substring(0, shortIdx), 16));
			}
			return null;
		}

		public static UUID getUUID(String uuidValue) {
			if (uuidValue == null) {
				return null;
			}
			String uuidValueUpper = uuidValue.toUpperCase();
			if (uuidNames.contains(uuidValueUpper)) {
				for (Enumeration iter = uuidNames.keys(); iter.hasMoreElements();) {
					String uuidValueKey = (String) iter.nextElement();
					String name = (String) uuidNames.get(uuidValueKey);
					if ((name != null) && (uuidValueUpper.equals(name))) {
						return new UUID(uuidValueKey, false);
					}
				}
			}
			UUID uuid;
			if (uuidValue.startsWith("0x")) {
                uuidValue = uuidValue.substring(2);
            } else if (uuidValue.endsWith("l")) {
                return new UUID(Long.parseLong(uuidValue.substring(0, uuidValue.length() - 1)));
            }
			if (uuidValue.length() <= 8) {
				uuid = new UUID(uuidValue, true);
			} else {
				uuid = new UUID(uuidValue, false);
			}
			return uuid;
		}
	}

	public static String toString(ServiceRecord sr) {
		if (sr == null) {
			return NULL;
		}
		int[] ids = sr.getAttributeIDs();
		if (ids == null) {
			return "attributes " + NULL;
		}
		if (ids.length == 0) {
			return "no attributes";
		}
		Vector sorted = new Vector();
		for (int i = 0; i < ids.length; i++) {
			sorted.addElement(new Integer(ids[i]));
		}
		CollectionUtils.sort(sorted);
		StringBuffer buf = new StringBuffer();
		for (Enumeration en = sorted.elements(); en.hasMoreElements();) {
			int id = ((Integer) en.nextElement()).intValue();
			buf.append(toHexString(id));
			buf.append(" ");
			buf.append(toStringServiceAttributeID(id));
			buf.append(":  ");

			DataElement d = sr.getAttributeValue(id);
			buf.append(toString(d));
			buf.append("\n");
		}
		return buf.toString();
	}

	public static String toStringServiceAttributeID(int id) {
		switch (id) {
		case 0x0000:
			return "ServiceRecordHandle";
		case 0x0001:
			return "ServiceClassIDList";
		case 0x0002:
			return "ServiceRecordState";
		case 0x0003:
			return "ServiceID";
		case 0x0004:
			return "ProtocolDescriptorList";
		case 0x0005:
			return "BrowseGroupList";
		case 0x0006:
			return "LanguageBasedAttributeIDList";
		case 0x0007:
			return "ServiceInfoTimeToLive";
		case 0x0008:
			return "ServiceAvailability";
		case 0x0009:
			return "BluetoothProfileDescriptorList";
		case 0x000A:
			return "DocumentationURL";
		case 0x000B:
			return "ClientExecutableURL";
		case 0x000C:
			return "IconURL";
		case 0x000D:
			return "AdditionalProtocol";
		case 0x0100:
			return "ServiceName";
		case 0x0101:
			return "ServiceDescription";
		case 0x0102:
			return "ProviderName";
		case 0x0200:
			return "GroupID";
		case 0x0201:
			return "ServiceDatabaseState";
		case 0x0300:
			return "ServiceVersion";
		case 0x0301:
			return "ExternalNetwork";
		case 0x0302:
			return "RemoteAudioVolumeControl";
		case 0x0303:
			return "SupportedFormatList";
		case 0x0304:
			return "FaxClass2Support";
		case 0x0305:
			return "AudioFeedbackSupport";
		case 0x0306:
			return "NetworkAddress";
		case 0x0307:
			return "WAPGateway";
		case 0x0308:
			return "HomePageURL";
		case 0x0309:
			return "WAPStackType";
		case 0x030A:
			return "SecurityDescription";
		case 0x030B:
			return "NetAccessType";
		case 0x030C:
			return "MaxNetAccessrate";
		case 0x030D:
			return "IPv4Subnet";
		case 0x030E:
			return "IPv6Subnet";
		case 0x0310:
			return "SupportedCapabalities";
		case 0x0311:
			return "SupportedFeatures";
		case 0x0312:
			return "SupportedFunctions";
		case 0x0313:
			return "TotalImagingDataCapacity";
		default:
			return "";
		}
	}

	public static String toStringDataElementType(int type) {
		switch (type) {
		case DataElement.NULL:
			return "NULL";
		case DataElement.U_INT_1:
			return "U_INT_1";
		case DataElement.U_INT_2:
			return "U_INT_2";
		case DataElement.U_INT_4:
			return "U_INT_4";
		case DataElement.U_INT_8:
			return "U_INT_8";
		case DataElement.U_INT_16:
			return "U_INT_16";
		case DataElement.INT_1:
			return "INT_1";
		case DataElement.INT_2:
			return "INT_2";
		case DataElement.INT_4:
			return "INT_4";
		case DataElement.INT_8:
			return "INT_8";
		case DataElement.INT_16:
			return "INT_16";
		case DataElement.URL:
			return "URL";
		case DataElement.STRING:
			return "STRING";
		case DataElement.UUID:
			return "UUID";
		case DataElement.DATSEQ:
			return "DATSEQ";
		case DataElement.BOOL:
			return "BOOL";
		case DataElement.DATALT:
			return "DATALT";
		default:
			return "Unknown" + type;
		}
	}

	public static String toStringObexResponseCodes(int code) {
		switch (code) {
		case 0x90:
			return "OBEX_RESPONSE_CONTINUE";
		case ResponseCodes.OBEX_HTTP_OK:
			return "OBEX_HTTP_OK";
		case ResponseCodes.OBEX_HTTP_CREATED:
			return "OBEX_HTTP_CREATED";
		case ResponseCodes.OBEX_HTTP_ACCEPTED:
			return "OBEX_HTTP_ACCEPTED";
		case ResponseCodes.OBEX_HTTP_NOT_AUTHORITATIVE:
			return "OBEX_HTTP_NOT_AUTHORITATIVE";
		case ResponseCodes.OBEX_HTTP_NO_CONTENT:
			return "OBEX_HTTP_NO_CONTENT";
		case ResponseCodes.OBEX_HTTP_RESET:
			return "OBEX_HTTP_RESET";
		case ResponseCodes.OBEX_HTTP_PARTIAL:
			return "OBEX_HTTP_PARTIAL";
		case ResponseCodes.OBEX_HTTP_MULT_CHOICE:
			return "OBEX_HTTP_MULT_CHOICE";
		case ResponseCodes.OBEX_HTTP_MOVED_PERM:
			return "OBEX_HTTP_MOVED_PERM";
		case ResponseCodes.OBEX_HTTP_MOVED_TEMP:
			return "OBEX_HTTP_MOVED_TEMP";
		case ResponseCodes.OBEX_HTTP_SEE_OTHER:
			return "OBEX_HTTP_SEE_OTHER";
		case ResponseCodes.OBEX_HTTP_NOT_MODIFIED:
			return "OBEX_HTTP_NOT_MODIFIED";
		case ResponseCodes.OBEX_HTTP_USE_PROXY:
			return "OBEX_HTTP_USE_PROXY";
		case ResponseCodes.OBEX_HTTP_BAD_REQUEST:
			return "OBEX_HTTP_BAD_REQUEST";
		case ResponseCodes.OBEX_HTTP_UNAUTHORIZED:
			return "OBEX_HTTP_UNAUTHORIZED";
		case ResponseCodes.OBEX_HTTP_PAYMENT_REQUIRED:
			return "OBEX_HTTP_PAYMENT_REQUIRED";
		case ResponseCodes.OBEX_HTTP_FORBIDDEN:
			return "OBEX_HTTP_FORBIDDEN";
		case ResponseCodes.OBEX_HTTP_NOT_FOUND:
			return "OBEX_HTTP_NOT_FOUND";
		case ResponseCodes.OBEX_HTTP_BAD_METHOD:
			return "OBEX_HTTP_BAD_METHOD";
		case ResponseCodes.OBEX_HTTP_NOT_ACCEPTABLE:
			return "OBEX_HTTP_NOT_ACCEPTABLE";
		case ResponseCodes.OBEX_HTTP_PROXY_AUTH:
			return "OBEX_HTTP_PROXY_AUTH";
		case ResponseCodes.OBEX_HTTP_TIMEOUT:
			return "OBEX_HTTP_TIMEOUT";
		case ResponseCodes.OBEX_HTTP_CONFLICT:
			return "OBEX_HTTP_CONFLICT";
		case ResponseCodes.OBEX_HTTP_GONE:
			return "OBEX_HTTP_GONE";
		case ResponseCodes.OBEX_HTTP_LENGTH_REQUIRED:
			return "OBEX_HTTP_LENGTH_REQUIRED";
		case ResponseCodes.OBEX_HTTP_PRECON_FAILED:
			return "OBEX_HTTP_PRECON_FAILED";
		case ResponseCodes.OBEX_HTTP_ENTITY_TOO_LARGE:
			return "OBEX_HTTP_ENTITY_TOO_LARGE";
		case ResponseCodes.OBEX_HTTP_REQ_TOO_LARGE:
			return "OBEX_HTTP_REQ_TOO_LARGE";
		case ResponseCodes.OBEX_HTTP_UNSUPPORTED_TYPE:
			return "OBEX_HTTP_UNSUPPORTED_TYPE";
		case ResponseCodes.OBEX_HTTP_INTERNAL_ERROR:
			return "OBEX_HTTP_INTERNAL_ERROR";
		case ResponseCodes.OBEX_HTTP_NOT_IMPLEMENTED:
			return "OBEX_HTTP_NOT_IMPLEMENTED";
		case ResponseCodes.OBEX_HTTP_BAD_GATEWAY:
			return "OBEX_HTTP_BAD_GATEWAY";
		case ResponseCodes.OBEX_HTTP_UNAVAILABLE:
			return "OBEX_HTTP_UNAVAILABLE";
		case ResponseCodes.OBEX_HTTP_GATEWAY_TIMEOUT:
			return "OBEX_HTTP_GATEWAY_TIMEOUT";
		case ResponseCodes.OBEX_HTTP_VERSION:
			return "OBEX_HTTP_VERSION";
		case ResponseCodes.OBEX_DATABASE_FULL:
			return "OBEX_DATABASE_FULL";
		case ResponseCodes.OBEX_DATABASE_LOCKED:
			return "OBEX_DATABASE_LOCKED";
		default:
			return "Unknown " + toHexString(code);
		}
	}

	public static String toHexStringSigned(long l) {
		if (l >= 0) {
			return toHexString(l);
		} else {
			return "-" + toHexString(-l);
		}
	}

	public static String toHexString(long l) {
		if (l > 0xffffffffl) {
			String lo = Integer.toHexString((int) l);
			while (lo.length() < 8) {
				lo = "0" + lo;
			}
			return "0x" + Integer.toHexString((int) (l >> 32)) + lo;
		} else {
			return "0x" + Integer.toHexString((int) l);
		}
	}

	public static String toHexString(int i) {
		String s = Integer.toHexString(i);
		s = s.toUpperCase();
		switch (s.length()) {
		case 1:
			return "0x000" + s;
		case 2:
			return "0x00" + s;
		case 3:
			return "0x0" + s;
		case 4:
			return "0x" + s;
		default:
			return s;
		}
	}

	public static String toString(DataElement d) {
		return toString(d, "");
	}

	public static String toString(DataElement d, String ident) {
		if (d == null) {
			return NULL;
		}
		StringBuffer buf = new StringBuffer();

		int valueType = d.getDataType();
		buf.append(ident);
		buf.append(toStringDataElementType(valueType));

		switch (valueType) {
		case DataElement.U_INT_1:
		case DataElement.U_INT_2:
		case DataElement.U_INT_4:
			buf.append(" ").append(toHexString(d.getLong()));
			break;
		case DataElement.INT_1:
		case DataElement.INT_2:
		case DataElement.INT_4:
		case DataElement.INT_8:
			buf.append(" ").append(toHexStringSigned(d.getLong()));
			break;
		case DataElement.BOOL:
			buf.append(" ").append(d.getBoolean());
			break;
		case DataElement.URL:
		case DataElement.STRING:
			buf.append(" ").append(d.getValue());
			break;
		case DataElement.UUID:
			buf.append(" ").append(toString((UUID) d.getValue()));
			break;
		case DataElement.U_INT_8:
		case DataElement.U_INT_16:
		case DataElement.INT_16:
			byte[] b = (byte[]) d.getValue();
			buf.append(" ");
			for (int i = 0; i < b.length; i++) {
				buf.append(Integer.toHexString(b[i] >> 4 & 0xf));
				buf.append(Integer.toHexString(b[i] & 0xf));
			}
			break;
		case DataElement.DATALT:
		case DataElement.DATSEQ:
			buf.append(" {\n");
			for (Enumeration e = (Enumeration) (d.getValue()); e.hasMoreElements();) {
				buf.append(toString((DataElement) e.nextElement(), ident + "  ")).append("\n");
			}
			buf.append(ident).append("}");
			break;
		}
		return buf.toString();
	}

	public static String toString(UUID uuid) {
		if (uuid == null) {
			return NULL;
		}
		StringBuffer buf = new StringBuffer();
		buf.append(uuid.toString());
		String name = UUIDConsts.getName(uuid);
		if (name != null) {
			buf.append(" (").append(name).append(")");
		}
		return buf.toString();
	}

	public static String toString(DeviceClass dc) {
		return DeviceClassConsts.toString(dc);
	}

	public static class DeviceClassConsts {

		/*
		 * service classes
		 */

		public static final int LIMITED_DISCOVERY_SERVICE = 0x002000;

		public static final int RESERVED1_SERVICE = 0x004000;

		public static final int RESERVED2_SERVICE = 0x008000;

		public static final int POSITIONING_SERVICE = 0x010000;

		public static final int NETWORKING_SERVICE = 0x020000;

		public static final int RENDERING_SERVICE = 0x040000;

		public static final int CAPTURING_SERVICE = 0x080000;

		public static final int OBJECT_TRANSFER_SERVICE = 0x100000;

		public static final int AUDIO_SERVICE = 0x200000;

		public static final int TELEPHONY_SERVICE = 0x400000;

		public static final int INFORMATION_SERVICE = 0x800000;

		/*
		 * major class codes
		 */

		public static final int MAJOR_MISCELLANEOUS = 0x0000;

		public static final int MAJOR_COMPUTER = 0x0100;

		public static final int MAJOR_PHONE = 0x0200;

		public static final int MAJOR_LAN_ACCESS = 0x0300;

		public static final int MAJOR_AUDIO = 0x0400;

		public static final int MAJOR_PERIPHERAL = 0x0500;

		public static final int MAJOR_IMAGING = 0x0600;

		public static final int MAJOR_UNCLASSIFIED = 0x1F00;

		/*
		 * minor class codes
		 */

		public static final int COMPUTER_MINOR_UNCLASSIFIED = 0x00;

		public static final int COMPUTER_MINOR_DESKTOP = 0x04;

		public static final int COMPUTER_MINOR_SERVER = 0x08;

		public static final int COMPUTER_MINOR_LAPTOP = 0x0c;

		public static final int COMPUTER_MINOR_HANDHELD = 0x10;

		public static final int COMPUTER_MINOR_PALM = 0x14;

		public static final int COMPUTER_MINOR_WEARABLE = 0x18;

		public static final int PHONE_MINOR_UNCLASSIFIED = 0x00;

		public static final int PHONE_MINOR_CELLULAR = 0x04;

		public static final int PHONE_MINOR_CORDLESS = 0x08;

		public static final int PHONE_MINOR_SMARTPHONE = 0x0c;

		public static final int PHONE_MINOR_WIRED_MODEM = 0x10;

		public static final int PHONE_MINOR_ISDN = 0x14;

		public static final int PHONE_MINOR_BANANA = 0x18;

		public static final int LAN_MINOR_TYPE_MASK = 0x1c;

		public static final int LAN_MINOR_ACCESS_MASK = 0xe0;

		public static final int LAN_MINOR_UNCLASSIFIED = 0x00;

		public static final int LAN_MINOR_ACCESS_0_USED = 0x00;

		public static final int LAN_MINOR_ACCESS_17_USED = 0x20;

		public static final int LAN_MINOR_ACCESS_33_USED = 0x40;

		public static final int LAN_MINOR_ACCESS_50_USED = 0x60;

		public static final int LAN_MINOR_ACCESS_67_USED = 0x80;

		public static final int LAN_MINOR_ACCESS_83_USED = 0xa0;

		public static final int LAN_MINOR_ACCESS_99_USED = 0xc0;

		public static final int LAN_MINOR_ACCESS_FULL = 0xe0;

		public static final int AUDIO_MINOR_UNCLASSIFIED = 0x00;

		public static final int AUDIO_MINOR_HEADSET = 0x04;

		public static final int AUDIO_MINOR_HANDS_FREE = 0x08;

		// public static final int AUDIO_MINOR_RESERVED = 0x0c;
		public static final int AUDIO_MINOR_MICROPHONE = 0x10;

		public static final int AUDIO_MINOR_LOUDSPEAKER = 0x14;

		public static final int AUDIO_MINOR_HEADPHONES = 0x18;

		public static final int AUDIO_MINOR_PORTABLE_AUDIO = 0x1c;

		public static final int AUDIO_MINOR_CAR_AUDIO = 0x20;

		public static final int AUDIO_MINOR_SET_TOP_BOX = 0x24;

		public static final int AUDIO_MINOR_HIFI_AUDIO = 0x28;

		public static final int AUDIO_MINOR_VCR = 0x2c;

		public static final int AUDIO_MINOR_VIDEO_CAMERA = 0x30;

		public static final int AUDIO_MINOR_CAMCORDER = 0x34;

		public static final int AUDIO_MINOR_VIDEO_MONITOR = 0x38;

		public static final int AUDIO_MINOR_VIDEO_DISPLAY_LOUDSPEAKER = 0x3c;

		public static final int AUDIO_MINOR_VIDEO_DISPLAY_CONFERENCING = 0x40;

		// public static final int AUDIO_MINOR_RESERVED = 0x44;
		public static final int AUDIO_MINOR_GAMING_TOY = 0x48;

		public static final int PERIPHERAL_MINOR_TYPE_MASK = 0x3c;

		public static final int PERIPHERAL_MINOR_KEYBOARD_MASK = 0x40;

		public static final int PERIPHERAL_MINOR_POINTER_MASK = 0x80;

		public static final int PERIPHERAL_MINOR_UNCLASSIFIED = 0x00;

		public static final int PERIPHERAL_MINOR_JOYSTICK = 0x04;

		public static final int PERIPHERAL_MINOR_GAMEPAD = 0x08;

		public static final int PERIPHERAL_MINOR_REMOTE_CONTROL = 0x0c;

		public static final int PERIPHERAL_MINOR_SENSING = 0x10;

		public static final int PERIPHERAL_MINOR_DIGITIZER = 0x14;

		public static final int PERIPHERAL_MINOR_CARD_READER = 0x18;

		public static final int IMAGING_MINOR_DISPLAY_MASK = 0x10;

		public static final int IMAGING_MINOR_CAMERA_MASK = 0x20;

		public static final int IMAGING_MINOR_SCANNER_MASK = 0x40;

		public static final int IMAGING_MINOR_PRINTER_MASK = 0x80;

		private static boolean append(StringBuffer buf, String str, boolean comma) {
			if (comma) {
				buf.append(',');
			}

			buf.append(str);

			return true;
		}

		public static String toString(DeviceClass dc) {
			if (dc == null) {
				return NULL;
			}
			StringBuffer buf = new StringBuffer();

			switch (dc.getMajorDeviceClass()) {
			case MAJOR_MISCELLANEOUS:
				buf.append("Miscellaneous");
				break;
			case MAJOR_COMPUTER:
				buf.append("Computer");

				switch (dc.getMinorDeviceClass()) {
				case COMPUTER_MINOR_UNCLASSIFIED:
					buf.append("/Unclassified");
					break;
				case COMPUTER_MINOR_DESKTOP:
					buf.append("/Desktop");
					break;
				case COMPUTER_MINOR_SERVER:
					buf.append("/Server");
					break;
				case COMPUTER_MINOR_LAPTOP:
					buf.append("/Laptop");
					break;
				case COMPUTER_MINOR_HANDHELD:
					buf.append("/Handheld");
					break;
				case COMPUTER_MINOR_PALM:
					buf.append("/Palm");
					break;
				case COMPUTER_MINOR_WEARABLE:
					buf.append("/Wearable");
					break;
				default:
					buf.append("/Unknown");
					break;
				}

				break;
			case MAJOR_PHONE:
				buf.append("Phone");

				switch (dc.getMinorDeviceClass()) {
				case PHONE_MINOR_UNCLASSIFIED:
					buf.append("/Unclassified");
					break;
				case PHONE_MINOR_CELLULAR:
					buf.append("/Cellular");
					break;
				case PHONE_MINOR_CORDLESS:
					buf.append("/Cordless");
					break;
				case PHONE_MINOR_SMARTPHONE:
					buf.append("/Smartphone");
					break;
				case PHONE_MINOR_WIRED_MODEM:
					buf.append("/Wired Modem");
					break;
				case PHONE_MINOR_ISDN:
					buf.append("/ISDN");
					break;
				case PHONE_MINOR_BANANA:
					buf.append("/Ring ring ring ring ring ring ring");
					break;
				default:
					buf.append("/Unknown");
					break;
				}

				break;
			case MAJOR_LAN_ACCESS: {
				buf.append("LAN Access");

				int minor = dc.getMinorDeviceClass();

				switch (minor & LAN_MINOR_TYPE_MASK) {
				case LAN_MINOR_UNCLASSIFIED:
					buf.append("/Unclassified");
					break;
				default:
					buf.append("/Unknown");
					break;
				}

				switch (minor & LAN_MINOR_ACCESS_MASK) {
				case LAN_MINOR_ACCESS_0_USED:
					buf.append("/0% used");
					break;
				case LAN_MINOR_ACCESS_17_USED:
					buf.append("/1-17% used");
					break;
				case LAN_MINOR_ACCESS_33_USED:
					buf.append("/18-33% used");
					break;
				case LAN_MINOR_ACCESS_50_USED:
					buf.append("/34-50% used");
					break;
				case LAN_MINOR_ACCESS_67_USED:
					buf.append("/51-67% used");
					break;
				case LAN_MINOR_ACCESS_83_USED:
					buf.append("/68-83% used");
					break;
				case LAN_MINOR_ACCESS_99_USED:
					buf.append("/84-99% used");
					break;
				case LAN_MINOR_ACCESS_FULL:
					buf.append("/100% used");
					break;
				}

				break;
			}
			case MAJOR_AUDIO:
				buf.append("Audio");

				switch (dc.getMinorDeviceClass()) {
				case AUDIO_MINOR_UNCLASSIFIED:
					buf.append("/Unclassified");
					break;
				case AUDIO_MINOR_HEADSET:
					buf.append("/Headset");
					break;
				case AUDIO_MINOR_HANDS_FREE:
					buf.append("/Hands-free");
					break;
				case AUDIO_MINOR_MICROPHONE:
					buf.append("/Microphone");
					break;
				case AUDIO_MINOR_LOUDSPEAKER:
					buf.append("/Loudspeaker");
					break;
				case AUDIO_MINOR_HEADPHONES:
					buf.append("/Headphones");
					break;
				case AUDIO_MINOR_PORTABLE_AUDIO:
					buf.append("/Portable");
					break;
				case AUDIO_MINOR_CAR_AUDIO:
					buf.append("/Car");
					break;
				case AUDIO_MINOR_SET_TOP_BOX:
					buf.append("/Set-top Box");
					break;
				case AUDIO_MINOR_HIFI_AUDIO:
					buf.append("/HiFi");
					break;
				case AUDIO_MINOR_VCR:
					buf.append("/VCR");
					break;
				case AUDIO_MINOR_VIDEO_CAMERA:
					buf.append("/Video Camera");
					break;
				case AUDIO_MINOR_CAMCORDER:
					buf.append("/Camcorder");
					break;
				case AUDIO_MINOR_VIDEO_MONITOR:
					buf.append("/Video Monitor");
					break;
				case AUDIO_MINOR_VIDEO_DISPLAY_LOUDSPEAKER:
					buf.append("/Video Display Loudspeaker");
					break;
				case AUDIO_MINOR_VIDEO_DISPLAY_CONFERENCING:
					buf.append("/Video Display Conferencing");
					break;
				case AUDIO_MINOR_GAMING_TOY:
					buf.append("/Gaming Toy");
					break;
				default:
					buf.append("/Unknown");
					break;
				}

				break;
			case MAJOR_PERIPHERAL: {
				buf.append("Peripheral");

				int minor = dc.getMinorDeviceClass();

				switch (minor & (PERIPHERAL_MINOR_KEYBOARD_MASK | PERIPHERAL_MINOR_POINTER_MASK)) {
				case 0:
					buf.append("/()");
					break;
				case PERIPHERAL_MINOR_KEYBOARD_MASK:
					buf.append("/(Keyboard)");
					break;
				case PERIPHERAL_MINOR_POINTER_MASK:
					buf.append("/(Pointer)");
					break;
				case PERIPHERAL_MINOR_KEYBOARD_MASK | PERIPHERAL_MINOR_POINTER_MASK:
					buf.append("/(Keyboard,Pointer)");
					break;
				}

				switch (minor & PERIPHERAL_MINOR_TYPE_MASK) {
				case PERIPHERAL_MINOR_UNCLASSIFIED:
					buf.append("/Unclassified");
					break;
				case PERIPHERAL_MINOR_JOYSTICK:
					buf.append("/Joystick");
					break;
				case PERIPHERAL_MINOR_GAMEPAD:
					buf.append("/Gamepad");
					break;
				case PERIPHERAL_MINOR_REMOTE_CONTROL:
					buf.append("/Remote Control");
					break;
				case PERIPHERAL_MINOR_SENSING:
					buf.append("/Sensing");
					break;
				case PERIPHERAL_MINOR_DIGITIZER:
					buf.append("/Digitizer");
					break;
				case PERIPHERAL_MINOR_CARD_READER:
					buf.append("/Card Reader");
					break;
				default:
					buf.append("/Unknown");
					break;
				}

				break;
			}
			case MAJOR_IMAGING: {
				buf.append("Peripheral/(");

				int minor = dc.getMinorDeviceClass();

				boolean comma = false;

				if ((minor & IMAGING_MINOR_DISPLAY_MASK) != 0)
					comma = append(buf, "Display", comma);
				if ((minor & IMAGING_MINOR_CAMERA_MASK) != 0)
					comma = append(buf, "Camera", comma);
				if ((minor & IMAGING_MINOR_SCANNER_MASK) != 0)
					comma = append(buf, "Scanner", comma);
				if ((minor & IMAGING_MINOR_PRINTER_MASK) != 0)
					comma = append(buf, "Printer", comma);

				buf.append(')');

				break;
			}
			case MAJOR_UNCLASSIFIED:
				buf.append("Unclassified");
				break;
			default:
				buf.append("Unknown");
				break;
			}

			buf.append("(");

			boolean comma = false;

			int record = dc.getServiceClasses();

			if ((record & LIMITED_DISCOVERY_SERVICE) != 0)
				comma = append(buf, "Limited Discovery", comma);
			if ((record & POSITIONING_SERVICE) != 0)
				comma = append(buf, "Positioning", comma);
			if ((record & NETWORKING_SERVICE) != 0)
				comma = append(buf, "Networking", comma);
			if ((record & RENDERING_SERVICE) != 0)
				comma = append(buf, "Rendering", comma);
			if ((record & CAPTURING_SERVICE) != 0)
				comma = append(buf, "Capturing", comma);
			if ((record & OBJECT_TRANSFER_SERVICE) != 0)
				comma = append(buf, "Object Transfer", comma);
			if ((record & AUDIO_SERVICE) != 0)
				comma = append(buf, "Audio", comma);
			if ((record & TELEPHONY_SERVICE) != 0)
				comma = append(buf, "Telephony", comma);
			if ((record & INFORMATION_SERVICE) != 0)
				comma = append(buf, "Information", comma);

			buf.append(')');

			return buf.toString();
		}
	}
}
