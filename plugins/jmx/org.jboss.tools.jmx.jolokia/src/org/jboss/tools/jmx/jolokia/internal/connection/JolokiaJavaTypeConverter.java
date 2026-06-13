/*******************************************************************************
 * Copyright (c) 2017 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.jmx.jolokia.internal.connection;

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.management.AttributeNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.openmbean.SimpleType;

import org.jboss.tools.jmx.jolokia.internal.Activator;
import org.jolokia.converter.object.ObjectToObjectConverter;
import org.jolokia.core.util.DateUtil;
import org.jolokia.json.JSONArray;

import org.jolokia.json.JSONObject;

public class JolokiaJavaTypeConverter {

	private ObjectToObjectConverter objectToObjectConverter = new ObjectToObjectConverter();

	public Object getJson(Object value) {
		if (value == null || value instanceof String || value instanceof Number || value instanceof Boolean
				|| value instanceof JSONObject || value instanceof JSONArray) {
			return value;
		}
		if (value instanceof Date) {
			return DateUtil.toISO8601((Date) value);
		}
		if (value instanceof Collection) {
			return new JSONArray((Collection<?>) value);
		}
		if (value.getClass().isArray()) {
			JSONArray arr = new JSONArray();
			for (int i = 0; i < Array.getLength(value); i++) {
				arr.add(Array.get(value, i));
			}
			return arr;
		}
		return value;
	}

	public Object getConvertedToCorrectTypeReturnedValue(MBeanAttributeInfo mBeanAttributeInfo, Object jolokiaReturnedValue) {
		String realType = mBeanAttributeInfo != null ? mBeanAttributeInfo.getType() : null;
		return getConvertedToCorrectType(jolokiaReturnedValue, realType);
	}

	public Object getConvertedToCorrectType(Object jolokiaReturnedValue, String realType) {
		if(realType != null && jolokiaReturnedValue instanceof String){
			return getConvertedString(jolokiaReturnedValue, realType);
		} else if(realType != null && jolokiaReturnedValue instanceof Number){
			return getConvertedNumeric((Number)jolokiaReturnedValue, realType);
		} else if(realType != null && jolokiaReturnedValue instanceof JSONArray){
			return getConvertedCollection((JSONArray) jolokiaReturnedValue, realType);
		}
		return jolokiaReturnedValue;
	}

	protected Object getConvertedString(Object jolokiaReturnedValue, String realType) {
		if(Date.class.getName().equals(realType)) {
			return DateUtil.fromISO8601((String) jolokiaReturnedValue);
		} else {
			return objectToObjectConverter.convert(realType, jolokiaReturnedValue);
		}
	}

	protected Object getConvertedCollection(JSONArray jolokiaReturnedValue, String realType) {
		if(Set.class.getName().equals(realType) || HashSet.class.getName().equals(realType)) {
			return new HashSet<Object>(jolokiaReturnedValue);
		} else {
			return jolokiaReturnedValue;
		}
	}

	protected Object getConvertedNumeric(Number jolokiaReturnedValue, String realType) {
		if("int".equals(realType) || Integer.class.getName().equals(realType)) {
			return jolokiaReturnedValue.intValue();
		} else if("short".equals(realType) || Short.class.getName().equals(realType)) {
			return jolokiaReturnedValue.shortValue();
		} else if("float".equals(realType) || Float.class.getName().equals(realType)) {
			return jolokiaReturnedValue.floatValue();
		}
		return jolokiaReturnedValue;
	}

}
