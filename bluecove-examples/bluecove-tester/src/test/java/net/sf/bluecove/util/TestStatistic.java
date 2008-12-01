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
package net.sf.bluecove.util;

import junit.framework.TestCase;

/**
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
