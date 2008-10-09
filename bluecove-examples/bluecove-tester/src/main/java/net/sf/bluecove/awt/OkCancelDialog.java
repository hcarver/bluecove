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

