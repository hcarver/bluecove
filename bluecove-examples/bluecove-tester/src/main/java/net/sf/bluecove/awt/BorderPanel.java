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

import java.awt.Insets;
import java.awt.LayoutManager;
import java.awt.Panel;

import net.sf.bluecove.Configuration;

/**
 * @author vlads
 *
 */
public class BorderPanel extends Panel {

	private static final long serialVersionUID = 1L;

	private static final Insets insets =  new Insets(10,10,10,10);
	private static final Insets insetsSmall =  new Insets(3,3,3,3);
	
	public BorderPanel() {
		super();
	}
	
	public BorderPanel(LayoutManager layout) {
		super(layout);
	}
	
	public Insets getInsets() {
		if (Configuration.screenSizeSmall) {
			return insetsSmall;
		} else {
			return insets;
		}
	}

}
