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
package org.bluecove.tester.obex;

import java.util.Enumeration;
import java.util.Vector;

import javax.microedition.lcdui.Command;
import javax.microedition.lcdui.CommandListener;
import javax.microedition.lcdui.Displayable;
import javax.microedition.lcdui.List;

import org.bluecove.tester.log.Logger;
import org.bluecove.tester.me.BaseTestMIDlet;
import org.bluecove.tester.obex.test.RegisterTests;

public class TestSelector {

	public static TestControl testControl;

	private static Vector tests = new Vector();

	private static TestInfo testSelected = null;

	private static class TestInfo {

		Runnable test;

		String name;
	}

	public static void add(String name, Runnable test) {
		TestInfo ti = new TestInfo();
		ti.test = test;
		ti.name = name;
		tests.addElement(ti);
	}

	public static void selectTest() {

		if (tests.size() == 0) {
			RegisterTests.register();
		}

		final List menuList = new List("Select Test", List.IMPLICIT);
		for (Enumeration iter = tests.elements(); iter.hasMoreElements();) {
			menuList.append(((TestInfo) iter.nextElement()).name, null);
		}
		menuList.setCommandListener(new CommandListener() {
			public void commandAction(Command c, Displayable d) {
				if (c == List.SELECT_COMMAND) {
					testSelected = (TestInfo) tests.elementAt(menuList.getSelectedIndex());
					BaseTestMIDlet.showMain();
				}
			}
		});
		BaseTestMIDlet.setCurrentDisplayable(menuList);
	}

	public static void runTest() {
		if (testSelected == null) {
			if (tests.size() == 0) {
				RegisterTests.register();
			}
			testSelected = (TestInfo) tests.elementAt(0);
		}
		Logger.info(testSelected.name + " start");
		try {
			testSelected.test.run();
		} finally {
			Logger.info(testSelected.name + " ends");
		}

	}
}
