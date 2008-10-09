/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove;

import javax.microedition.lcdui.Alert;
import javax.microedition.lcdui.AlertType;
import javax.microedition.lcdui.Display;
import javax.microedition.lcdui.Displayable;
import javax.microedition.midlet.MIDlet;
import javax.microedition.midlet.MIDletStateChangeException;

import net.sf.bluecove.util.CLDC10;
import net.sf.bluecove.util.CLDC11;
import net.sf.bluecove.util.StringUtils;

public class BlueCoveTestMIDlet extends MIDlet {

	static BlueCoveTestMIDlet instance;

	static Display display;

	private Displayable tester;

	/** The messages are shown in this app this amount of time. */
	static final int ALERT_TIMEOUT = 10000;

	protected void startApp() throws MIDletStateChangeException {
		Configuration.isJ2ME = true;

		Configuration.CLDC_1_0 = StringUtils.equalsIgnoreCase(System.getProperty("microedition.configuration"),
				"CLDC-1.0");

		if (!Configuration.CLDC_1_0) {
			Configuration.cldcStub = new CLDC11();
		} else {
			Configuration.cldcStub = new CLDC10();
		}

		instance = this;
		display = Display.getDisplay(this);
		if (tester == null) {
			tester = new BlueCoveTestCanvas();
		}
		showMain();
	}

	protected void destroyApp(boolean unconditional) throws MIDletStateChangeException {
	}

	protected void pauseApp() {
	}

	public static void showMain() {
		setCurrentDisplayable(instance.tester);
	}

	public static void setCurrentDisplayable(Displayable nextDisplayable) {
		display.setCurrent(nextDisplayable);
	}

	public static void exit() {
		try {
			instance.destroyApp(true);
		} catch (MIDletStateChangeException e) {
		}
		instance.notifyDestroyed();
	}

	/** Show error message to user when something wrong with app */
	public static void error(final String message) {
		Logger.error(message);
		Alert al = new Alert("Error", message, null, AlertType.ERROR);
		al.setTimeout(ALERT_TIMEOUT);
		showAlertLater(al);
	}

	public static void alert(final String title, final String message) {
		Logger.debug(title + " " + message);
		Alert al = new Alert(title, message, null, AlertType.INFO);
		al.setTimeout(ALERT_TIMEOUT);
		showAlertLater(al);
	}

	public static void alert(String message) {
		alert("Message", message);
	}

	public static void message(final String title, final String message) {
		Logger.debug(title + " " + message);
		Alert al = new Alert(title, message, null, AlertType.INFO);
		al.setTimeout(ALERT_TIMEOUT);
		showAlertLater(al);
	}

	private static void showAlertLater(final Alert al) {
		Runnable r = new Runnable() {
			public void run() {
				setCurrentDisplayable(al);
			}
		};
		// invokeLater
		display.callSerially(r);
	}

	static void invokeLater(Runnable r) {
		Thread t = new Thread(r);
		t.start();
	}
}
