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
package javax.bluetooth;

import com.intel.bluetooth.DebugLog;

public class DeviceClass {
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

	private static final int SERVICE_MASK = 0xffe000;

	private static final int MAJOR_MASK = 0x001f00;

	private static final int MINOR_MASK = 0x0000fc;

	private int record;

	/*
	 * Creates a DeviceClass from the class of device record provided. record
	 * must follow the format of the class of device record in the Bluetooth
	 * specification. Parameters: record - describes the classes of a device
	 * Throws: IllegalArgumentException - if record has any bits between 24 and
	 * 31 set
	 */

	public DeviceClass(int record) {
		
		DebugLog.debug("new DeviceClass", record);
		
		this.record = record;

		if ((record & 0xff000000) != 0)
			throw new IllegalArgumentException();
	}

	/*
	 * Retrieves the major service classes. A device may have multiple major
	 * service classes. When this occurs, the major service classes are bitwise
	 * OR'ed together. Returns: the major service classes
	 */

	public int getServiceClasses() {
		return record & SERVICE_MASK;
	}

	/*
	 * Retrieves the major device class. A device may have only a single major
	 * device class. Returns: the major device class
	 */

	public int getMajorDeviceClass() {
		return record & MAJOR_MASK;
	}

	/*
	 * Retrieves the minor device class. Returns: the minor device class
	 */

	public int getMinorDeviceClass() {
		return record & MINOR_MASK;
	}

	private boolean append(StringBuffer buf, String str, boolean comma) {
		if (comma)
			buf.append(',');

		buf.append(str);

		return true;
	}

	public String toString() {
		StringBuffer buf = new StringBuffer();

		switch (getMajorDeviceClass()) {
		case MAJOR_MISCELLANEOUS:
			buf.append("Miscellaneous");
			break;
		case MAJOR_COMPUTER:
			buf.append("Computer");

			switch (getMinorDeviceClass()) {
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

			switch (getMinorDeviceClass()) {
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

			int minor = getMinorDeviceClass();

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

			switch (getMinorDeviceClass()) {
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

			int minor = getMinorDeviceClass();

			switch (minor
					& (PERIPHERAL_MINOR_KEYBOARD_MASK | PERIPHERAL_MINOR_POINTER_MASK)) {
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

			int minor = getMinorDeviceClass();

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

		buf.append("/(");

		boolean comma = false;

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