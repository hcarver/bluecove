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
package net.sf.bluecove.util;

import junit.framework.TestCase;

/**
 * @author vlads
 * 
 */
public class TestStatistic extends TestCase {

	public void testCountStatisticAvg() {
		CountStatistic c = new CountStatistic();
		assertEquals("avg", "0", c.avg());
		assertEquals("avgPrc", "0%", c.avgPrc());
		c.add(1);
		assertEquals("avg", "1.0000", c.avg());
		assertEquals("avgPrc", "100.0000%", c.avgPrc());
		c.add(2);
		assertEquals("avg", "1.5000", c.avg());
		assertEquals("avgPrc", "150.0000%", c.avgPrc());
		c.add(1);
		assertEquals("avg", "1.3333", c.avg());
		assertEquals("avgPrc", "133.3333%", c.avgPrc());
	}

	public void testCountStatisticPrc1() {
		CountStatistic c = new CountStatistic();
		c.add(1);
		assertEquals("avg", "1.0000", c.avg());
		assertEquals("avgPrc", "100.0000%", c.avgPrc());
		c.add(1);
		assertEquals("avg", "1.0000", c.avg());
		assertEquals("avgPrc", "100.0000%", c.avgPrc());
		c.add(4);
		assertEquals("avg", "2.0000", c.avg());
		assertEquals("avgPrc", "200.0000%", c.avgPrc());
		c.add(2);
		c.add(2);
		c.add(2);
		assertEquals("avg", "2.0000", c.avg());
		assertEquals("avgPrc", "200.0000%", c.avgPrc());

		c.add(3);
		assertEquals("avg", "2.1428", c.avg());
		assertEquals("avgPrc", "214.2857%", c.avgPrc());
	}

	public void testCountStatisticPrc2() {
		CountStatistic c = new CountStatistic();
		c.add(0);
		assertEquals("avg", "0.0000", c.avg());
		assertEquals("avgPrc", "0.0000%", c.avgPrc());
		c.add(1);
		assertEquals("avg", "0.5000", c.avg());
		assertEquals("avgPrc", "50.0000%", c.avgPrc());
		c.add(0);
		assertEquals("avg", "0.3333", c.avg());
		assertEquals("avgPrc", "33.3333%", c.avgPrc());
	}

}
