/*******************************************************************************
 * Copyright (c) 2016 Red Hat, Inc.
 * Distributed under license by Red Hat, Inc. All rights reserved.
 * This program is made available under the terms of the
 * Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat, Inc. - initial API and implementation
 ******************************************************************************/
package org.jboss.tools.jmx.jolokia.internal.connection;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.InstanceAlreadyExistsException;
import javax.management.InstanceNotFoundException;
import javax.management.IntrospectionException;
import javax.management.InvalidAttributeValueException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanRegistrationException;
import javax.management.MBeanServerConnection;
import javax.management.MalformedObjectNameException;
import javax.management.NotCompliantMBeanException;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ObjectInstance;
import javax.management.ObjectName;
import javax.management.QueryExp;
import javax.management.ReflectionException;

import org.jboss.tools.jmx.jolokia.internal.Activator;
import org.jolokia.client.JolokiaClient;
import org.jolokia.client.JolokiaQueryParameter;
import org.jolokia.client.exception.JolokiaException;
import org.jolokia.client.request.HttpMethod;
import org.jolokia.client.request.JolokiaExecRequest;
import org.jolokia.client.request.JolokiaListRequest;
import org.jolokia.client.request.JolokiaReadRequest;
import org.jolokia.client.request.JolokiaSearchRequest;
import org.jolokia.client.request.JolokiaWriteRequest;
import org.jolokia.client.response.JolokiaExecResponse;
import org.jolokia.client.response.JolokiaListResponse;
import org.jolokia.client.response.JolokiaReadResponse;
import org.jolokia.client.response.JolokiaResponse;
import org.jolokia.client.response.JolokiaSearchResponse;
import org.jolokia.json.JSONArray;

import org.jolokia.json.JSONObject;
/**
 * A very basic implementation of MBeanServerConnection, using the jolokia client jar
 * to make the various requests to the server.  It is not 100% functional, but works for 
 * the base case that our UI uses. 
 * 
 * This code could be improved. 
 */
public class JolokiaMBeanServerConnection implements MBeanServerConnection {
	private JolokiaClient jolokiaClient;
	private HttpMethod type;
	private JolokiaJavaTypeConverter converter = new JolokiaJavaTypeConverter();
	
	public JolokiaMBeanServerConnection(JolokiaClient jolokiaClient, HttpMethod type) {
		this.jolokiaClient = jolokiaClient;
		this.type = type;
	}

	@Override
	public String getDefaultDomain() throws IOException {
		throw new IOException("Unsupported");
	}

	@Override
	public String[] getDomains() throws IOException {
		try {
			 Set<ObjectName> on = queryNames(new ObjectName("*:*"), null);
			 return on.stream()
					 .map(ObjectName::getDomain)
					 .toArray(String[]::new);
		} catch (MalformedObjectNameException e) {
			throw new IOException(e);
		}
	}

	@Override
	public Integer getMBeanCount() throws IOException {
		try {
			return queryNames(new ObjectName("*:*"), null).size();
		} catch (MalformedObjectNameException e) {
			throw new IOException(e);
		}
	}

	@Override
	public MBeanInfo getMBeanInfo(ObjectName name)
			throws InstanceNotFoundException, IntrospectionException, ReflectionException, IOException {
		try {
			JolokiaListRequest request = new JolokiaListRequest(name);
			JolokiaListResponse resp = jolokiaClient.execute(request, type);
			JSONObject o = resp.getValue();
			return new JolokiaMBeanUtility().createMBeanInfoFromSingletonList(unwrapListResponse(o));
		} catch (JolokiaException e) {
			throw new IOException(e);
		}
	}

	private static JSONObject unwrapListResponse(JSONObject value) {
		if (value == null || value.containsKey("class")) {
			return value;
		}
		for (Object v1 : value.values()) {
			if (v1 instanceof JSONObject) {
				JSONObject j1 = (JSONObject) v1;
				if (j1.containsKey("class")) {
					return j1;
				}
				for (Object v2 : j1.values()) {
					if (v2 instanceof JSONObject && ((JSONObject) v2).containsKey("class")) {
						return (JSONObject) v2;
					}
				}
			}
		}
		return value;
	}
	
	@Override
	public boolean isRegistered(ObjectName name) throws IOException {
		return !queryNames(name, null).isEmpty();
	}


	@Override
	public Set<ObjectName> queryNames(ObjectName name, QueryExp query) throws IOException {
		try {
			JolokiaSearchRequest request = new JolokiaSearchRequest(name.getCanonicalName());
			Map<JolokiaQueryParameter,String> processingOptions = new EnumMap<>(JolokiaQueryParameter.class);
			processingOptions.put(JolokiaQueryParameter.CANONICAL_NAMING, Boolean.FALSE.toString());
			JolokiaSearchResponse resp = jolokiaClient.execute(request, type, processingOptions);
			HashSet<ObjectName> toFilter = new HashSet<>(resp.getObjectNames());
			
			return toFilter;
		} catch (MalformedObjectNameException | JolokiaException e) {
			throw new IOException(e);
		}
	}
	
	/*  Get / set attributes */
	
	
	@Override
	public void setAttribute(ObjectName name, Attribute attribute)
			throws InstanceNotFoundException, AttributeNotFoundException, InvalidAttributeValueException,
			MBeanException, ReflectionException, IOException {
		JolokiaWriteRequest req = new JolokiaWriteRequest(name, attribute.getName(), converter.getJson(attribute.getValue()));
		try {
			JolokiaResponse<JolokiaWriteRequest> r = jolokiaClient.execute(req, type);
			Object o = r.asJSONObject().get("status");
			if( o == null ) {
			} else if( !o.equals(Long.valueOf(200))) {
				throw new IOException("Failed to update attribute " + attribute.getName() + " on object " + name.getCanonicalName());
			}
		} catch (JolokiaException e) {
			throw new IOException(e);
		}
	}

	@Override
	public AttributeList setAttributes(ObjectName name, AttributeList attributes)
			throws InstanceNotFoundException, ReflectionException, IOException {
		AttributeList result = new AttributeList();
		for (Attribute attribute : attributes.asList()) {
			try {
				setAttribute(name, attribute);
				result.add(attribute);
			} catch (AttributeNotFoundException | InvalidAttributeValueException | MBeanException e) {
				Activator.pluginLog().logError(e);
			}
		}
		return result;
	}

	@Override
	public Object getAttribute(ObjectName name, String attribute) throws MBeanException, AttributeNotFoundException,
			InstanceNotFoundException, ReflectionException, IOException {
		AttributeList l = getAttributes(name, new String[]{attribute});
		if( !l.isEmpty() ) {
			return l.get(0);
		}
		return null;
	}

	@Override
	public AttributeList getAttributes(ObjectName name, String[] attributeNames)
			throws InstanceNotFoundException, ReflectionException, IOException {
		List<MBeanAttributeInfo> attributesInfos = getAttributesInfos(name);
		
		
		AttributeList al = new AttributeList();
		JolokiaReadRequest req = new JolokiaReadRequest(name, attributeNames);
		Object response = null;
		try {
			response = jolokiaClient.execute(req, type);
		} catch (JolokiaException e) {
			throw new IOException(e);
		}
		if(response instanceof List) {
			List<JolokiaResponse<JolokiaReadRequest>> resp = (List<JolokiaResponse<JolokiaReadRequest>>)response;
			Iterator<JolokiaResponse<JolokiaReadRequest>> c = resp.iterator();
			while(c.hasNext()) {
				al.addAll(extractAttributesFromResponse(attributesInfos, c.next()));
			}
		} else if(response instanceof JolokiaReadResponse){
			if(attributeNames.length == 1){
				MBeanAttributeInfo mBeanAttributeInfo = findAttributeInfoWithName(attributesInfos, attributeNames[0]);
				al.add(converter.getConvertedToCorrectTypeReturnedValue(mBeanAttributeInfo, ((JolokiaReadResponse) response).getValue()));
			} else {
				al.addAll(extractAttributesFromResponse(attributesInfos, (JolokiaReadResponse) response));
			}
		}
		return al;
	}

	private List<MBeanAttributeInfo> getAttributesInfos(ObjectName name)
			throws InstanceNotFoundException, ReflectionException, IOException {
		try {
			MBeanInfo mBeanInfo = getMBeanInfo(name);
			return Arrays.asList(mBeanInfo.getAttributes());
		} catch (IntrospectionException e) {
			Activator.pluginLog().logError(e);
		}
		return Collections.emptyList();
	}

	private List<Object> extractAttributesFromResponse(List<MBeanAttributeInfo> attributesInfos, JolokiaResponse<?> response) {
		List<Object> extractedAttributes = new ArrayList<>();
		Object o22 = response.getValue();
		if(o22 instanceof JSONObject){
			Set<Map.Entry<String, Object>> entrySet = ((JSONObject)o22).entrySet();
			for (Map.Entry<String, Object> entry : entrySet) {
				String attributeName = entry.getKey();
				MBeanAttributeInfo mBeanAttributeInfo = findAttributeInfoWithName(attributesInfos, attributeName);
				Object jolokiaReturnedValue = entry.getValue();
				Object convertedToCorrectTypeReturnedValue = converter.getConvertedToCorrectTypeReturnedValue(mBeanAttributeInfo, jolokiaReturnedValue);
				extractedAttributes.add(new Attribute(attributeName, convertedToCorrectTypeReturnedValue));
			}
		} else {
			extractedAttributes.add(o22);
		}
		return extractedAttributes;
	}


	private MBeanAttributeInfo findAttributeInfoWithName(List<MBeanAttributeInfo> attributesInfos, String attributeName) {
		return attributesInfos.parallelStream()
				.filter(attributeInfo -> attributeName.equals(attributeInfo.getName()))
				.findAny().orElse(null);
	}
	
	/* Operation Invocations */
	@Override
	public Object invoke(ObjectName name, String operationName, Object[] params, String[] signature)
			throws InstanceNotFoundException, MBeanException, ReflectionException, IOException {
		String operationNameWithSignature = createOperationNameWithSignature(operationName, signature);
		String specifiedReturnedType = getSpecifiedReturnedType(name, operationName, params);
		
		JolokiaExecRequest req = createJolokiaExecRequest(name, params, operationNameWithSignature);
		try {
			JolokiaExecResponse resp = jolokiaClient.execute(req, type);
			return converter.getConvertedToCorrectType(resp.getValue(), specifiedReturnedType);
		} catch (JolokiaException e) {
			throw new IOException("Operation Signature of failed request: " + operationNameWithSignature, e);
		}
	}

	private String getSpecifiedReturnedType(ObjectName name, String operationName, Object[] params) throws InstanceNotFoundException, ReflectionException, IOException {
		try {
			MBeanInfo mBeanInfo = getMBeanInfo(name);
			if(mBeanInfo != null){
				int paramCount = params != null ? params.length : 0;
				List<MBeanOperationInfo> operations = Arrays.asList(mBeanInfo.getOperations()).stream()
						.filter(operationInfo -> operationName.equals(operationInfo.getName()))
						.filter(operationInfo -> operationInfo.getSignature() != null && paramCount == operationInfo.getSignature().length)
						.collect(Collectors.toList());

				if(operations.size() == 1){
					return operations.get(0).getReturnType();
				} else {
					Activator.pluginLog().logInfo("Method invocation of "+ operationName +" might return the wrong Return Type due to current implementation limitations.");
				}
			}
					
		} catch (IntrospectionException e1) {
			Activator.pluginLog().logError(e1);
		}
		return null;
	}

	private JolokiaExecRequest createJolokiaExecRequest(ObjectName name, Object[] params, String operationNameWithSignature) {
		if(params == null || params.length == 0){
			return new JolokiaExecRequest(name, operationNameWithSignature);
		} else {
			return new JolokiaExecRequest(name, operationNameWithSignature, params);
		}
	}

	private String createOperationNameWithSignature(String operationName, String[] signature) {
		StringJoiner stringJoiner = new StringJoiner(",", "(", ")");
		Stream.of(signature).forEach(stringJoiner::add);
		return operationName + stringJoiner.toString();
	}

	@Override
	public ObjectInstance getObjectInstance(ObjectName name) throws InstanceNotFoundException, IOException {
		return createObjectInstance(name);
	}

	@Override
	public Set<ObjectInstance> queryMBeans(ObjectName name, QueryExp query) throws IOException {
		Set<ObjectInstance> res = new HashSet<>();
		try {
			JolokiaSearchRequest req = new JolokiaSearchRequest(name.getCanonicalName());
			JolokiaResponse<JolokiaSearchRequest> j4pResponse = jolokiaClient.execute(req, type);
			Object value = j4pResponse.getValue();
			if(value instanceof JSONArray){
				for (Object mbean : (JSONArray)value) {
					if(mbean instanceof String){
						res.add(createObjectInstance((String)mbean));
					}
				}
			}
		} catch (MalformedObjectNameException | JolokiaException e) {
			Activator.pluginLog().logError(e);
		}
		return res;
	}

	private ObjectInstance createObjectInstance(String mbean) throws MalformedObjectNameException {
		ObjectName objectName = new ObjectName(mbean);
		return createObjectInstance(objectName);
	}

	private ObjectInstance createObjectInstance(ObjectName objectName) {
		String classname = retrieveClassName(objectName);
		return new ObjectInstance(objectName, classname);
	}

	private String retrieveClassName(ObjectName objectName) {
		try {
			return getMBeanInfo(objectName).getClassName();
		} catch (Exception e) {
			return "";
		}
	}
	
	
	@Override
	public boolean isInstanceOf(ObjectName name, String className) throws InstanceNotFoundException, IOException {
		String mBeanClass = retrieveClassName(name);
		if(className != null && className.equals(mBeanClass)){
			return true;
		}
		try {
			return Class.forName(mBeanClass).isInstance(Class.forName(className));
		} catch (ClassNotFoundException e) {
			return false;
		}
	}	
	
	/*
	 * Unsupported operations are below.  
	 * At this time I have no intention on implementing these operations, 
	 * though contributions are welcome. 
	 */

	
	
	/* Add / Remove mbeans */

	@Override
	public void unregisterMBean(ObjectName name)
			throws InstanceNotFoundException, MBeanRegistrationException, IOException {
		// TODO Auto-generated method stub
		
	}

	
	@Override
	public ObjectInstance createMBean(String className, ObjectName name)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanException,
			NotCompliantMBeanException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanException,
			NotCompliantMBeanException, InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectInstance createMBean(String className, ObjectName name, Object[] params, String[] signature)
			throws ReflectionException, InstanceAlreadyExistsException, MBeanException,
			NotCompliantMBeanException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public ObjectInstance createMBean(String className, ObjectName name, ObjectName loaderName, Object[] params,
			String[] signature) throws ReflectionException, InstanceAlreadyExistsException,
			MBeanException, NotCompliantMBeanException, InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		return null;
	}

	
	
	
	
	/* Notifications */

	@Override
	public void addNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void addNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNotificationListener(ObjectName name, ObjectName listener)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNotificationListener(ObjectName name, NotificationListener listener)
			throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNotificationListener(ObjectName name, ObjectName listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void removeNotificationListener(ObjectName name, NotificationListener listener, NotificationFilter filter,
			Object handback) throws InstanceNotFoundException, ListenerNotFoundException, IOException {
		// TODO Auto-generated method stub
		
	}

	
}
