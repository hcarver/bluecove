/**
 *  BlueCove - Java library for Bluetooth
 *  Copyright (C) 2009 Vlad Skarzhevskyy
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
package org.bluez.dbus;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Map;

import org.freedesktop.dbus.DBusInterface;
import org.freedesktop.dbus.Variant;

public abstract class DBusProperties {

	public interface PropertyEnum {

	}
	
	public enum DBusPropertyAccessType {
	    
	    READWRITE,
	    
	    READONLY;
	}

	@Target({ElementType.FIELD}) @Retention(RetentionPolicy.RUNTIME)
	public @interface DBusProperty {
	    
	    /**
	     * The name of the Property. Defaults to the property or Enum name
	     */
	    String name() default "";
	 
	    /**
	     * The property type class.
	     */
	    Class<?> type() default void.class;
	    
	    DBusPropertyAccessType access() default DBusPropertyAccessType.READWRITE;
	    
    }
	
	public interface PropertiesAccess extends DBusInterface {

		/**
		 * Returns all properties for the interface. See the properties section for available properties.
		 * 
		 * @return
		 */
		public Map<String, Variant<Object>> GetProperties() throws org.bluez.Error.DoesNotExist, org.bluez.Error.InvalidArguments;

		/**
		 * Changes the value of the specified property. Only properties that are listed a read-write are changeable.
		 * 
		 * @param name
		 * @param value
		 */
		public void SetProperty(String name, Variant<Object> value) throws org.bluez.Error.DoesNotExist, org.bluez.Error.InvalidArguments;
	}

	public static String getStringValue(PropertiesAccess dBusInterface, PropertyEnum propertyEnum) {
		return (String) dBusInterface.GetProperties().get(getPropertyName(propertyEnum)).getValue();
	}

	private static String getPropertyName(PropertyEnum propertyEnum) {
		return propertyEnum.toString();
	}
}