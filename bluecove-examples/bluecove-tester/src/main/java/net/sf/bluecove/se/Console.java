/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2006-2008 Vlad Skarzhevskyy
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
package net.sf.bluecove.se;

import java.io.IOException;

import org.bluecove.tester.log.Logger;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.Consts;
import net.sf.bluecove.Switcher;
import net.sf.bluecove.TestResponderCommon;

/**
 * 
 */
public class Console {

	public static void main(String[] args) {
		JavaSECommon.initOnce();
		Configuration.storage = new FileStorage();
		help();

		while (true) {
			try {
				String cmd = readCommand();
				if (cmd == null) {
					quit();
					return;
				}
				if (cmd.length() == 0) {
					help();
					continue;
				}
				cmd = cmd.toUpperCase();
				char user_input = cmd.charAt(0);
				switch (user_input) {
				case 'Q':
					quit();
					break;
				case '\n':
				case '?':
				case 'H':
					help();
					break;
				case '4':
					UIHelper.printFailureLog();
					break;
				case '*':
					Switcher.startDiscovery();
					break;
				case '7':
					Switcher.startServicesSearch();
					break;
				case '2':
					Switcher.startClient();
					break;
				case '3':
					Switcher.clientShutdown();
					break;
				case '5':
					Switcher.startServer();
					break;
				case '6':
					Switcher.serverShutdown();
					break;
				case 'P':
					TestResponderCommon.printLocalDeviceInfo();
					break;
				case 'D':
					boolean dbg = BlueCoveSpecific.changeDebug();
					if (dbg) {
						System.out.println("BlueCove Debug ON");
					} else {
						System.out.println("BlueCove Debug OFF");
					}
					break;
				case 'T':
				    Switcher.startTCKAgent();
				    break;
				case 'S':
				    if (cmd.equals("SRR")) {
				        //RFCOMM Read test
				        UIHelper.configurationForSpeedTest(Consts.TRAFFIC_GENERATOR_WRITE, false);
				    } else if (cmd.equals("SRW")) {
				        //RFCOMM Write test
				        UIHelper.configurationForSpeedTest(Consts.TRAFFIC_GENERATOR_READ, false);
				    } else if (cmd.equals("SLR")) {
				        //L2CAP Read test
				        UIHelper.configurationForSpeedTest(Consts.TRAFFIC_GENERATOR_WRITE, true);
				    } else if (cmd.equals("SLW")) {
				        //L2CAP Write test
				        UIHelper.configurationForSpeedTest(Consts.TRAFFIC_GENERATOR_READ, true);  
				    } else {
				        System.out.println("Unknown speed test command " + cmd);
				        break;
				    }
				    Switcher.startClient();
				    break;
				default :
				    System.out.println("Unknown command " + cmd);
				}
			} catch (Throwable e) {
			    System.out.println("Exception " + e.getMessage());
			    e.printStackTrace(System.out);
			}
		}
	}

	private static String readCommand() throws IOException {
	    StringBuffer buf = new StringBuffer();
	    int b;
	    do {
	        b = System.in.read();
	        if (b == '\n') {
	            return buf.toString();
	        }
	        buf.append(b);
	    } while (b != -1);
		return null;

	}

	private static void help() {
		System.out.println("BlueCove tester Console application (keyboard codes the same as in MIDP application)");
		System.out.println("\t2 - Start Client");
		System.out.println("\t3 - Stop Client");
		System.out.println("\t5 - Start Server");
		System.out.println("\t6 - Stop Server");
		System.out.println("\t* - Run Discovery");
		System.out.println("\t7 - Services Search");
		System.out.println("\td - toggle BlueCove Debug");
		System.out.println("\tT - Start TCK Agent");
		System.out.println("\tP - Print LocalDevice Info");
		System.out.println("\tSRR|SRW|SLR|SLW  - Speed tests RFCOMM|L2CAP Read|Write");
		System.out.println("\tq - Quit");
		System.out.flush();
	}

	private static void quit() {
		Logger.debug("quit");
		Switcher.clientShutdown();
		Switcher.serverShutdownOnExit();
		System.exit(0);
	}
}
