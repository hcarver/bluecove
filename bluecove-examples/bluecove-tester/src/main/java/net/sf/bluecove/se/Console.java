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
package net.sf.bluecove.se;

import java.io.IOException;

import net.sf.bluecove.Configuration;
import net.sf.bluecove.Logger;
import net.sf.bluecove.Switcher;

/**
 * @author vlads
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
				case 'D':
					boolean dbg = BlueCoveSpecific.changeDebug();
					if (dbg) {
						System.out.println("BlueCove Debug ON");
					} else {
						System.out.println("BlueCove Debug OFF");
					}
					break;
				}
			} catch (IOException e) {
				return;
			}
		}
	}

	private static String readCommand() throws IOException {
		int b = System.in.read();
		if (b == -1) {
			return null;
		}
		return new String("" + (char) b);
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
