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
 *  @author vlads
 *  @version $Id$
 */
package net.sf.bluecove.util;

/**
 *
 */
public class TimeStatistic {

    public long count;

    public long durationMax;

    public long durationTotal;

    public long durationLast;

    public void clear() {
        count = 0;
        durationMax = 0;
        durationTotal = 0;
    }

    public void add(long duration) {
        count++;
        durationLast = duration;
        durationTotal += duration;
        if (duration > durationMax) {
            durationMax = duration;
        }
    }

    public long avgSec() {
        if (count == 0) {
            return 0;
        }
        return (durationTotal / (1000 * count));
    }

    public long avg() {
        if (count == 0) {
            return 0;
        }
        return (durationTotal / (count));
    }

    public long durationMaxSec() {
        return durationMax / 1000;
    }
}
