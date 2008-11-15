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
 *  @version $Id$
 */
package net.sf.bluecove.awt;

import java.awt.BorderLayout;
import java.awt.Button;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Panel;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import net.sf.bluecove.Configuration;

/**
 * @author vlads
 * 
 */
public abstract class OkCancelDialog extends Dialog {

	private static final long serialVersionUID = 1L;

	protected Panel panelBtns;

	Button btnOk, btnCancel;

	public OkCancelDialog(Frame owner, String title, boolean modal) {
		super(owner, title, modal);

		panelBtns = new Panel();
		this.add(panelBtns, BorderLayout.SOUTH);

		panelBtns.add(btnOk = new Button("OK"));
		btnOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onClose(false);
			}
		});

		panelBtns.add(btnCancel = new Button("Cancel"));
		btnCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onClose(true);
			}
		});

		this.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				onClose(true);
			}
		});
	}

	protected abstract void onClose(boolean isCancel);

	public static void centerParent(Component window) {
		if (!Configuration.screenSizeSmall) {
			try {
				Rectangle b = window.getParent().getBounds();
				// b.getWidth(); Not for J9
				int bWidth = b.getBounds().width;
				window.setLocation(b.x + (int) ((bWidth - window.getWidth()) / 2), b.y + 60);
			} catch (Throwable java11) {
			}
		}
	}
}
