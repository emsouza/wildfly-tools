package org.jboss.tools.jmx.jolokia.test.internal.connection;

import static org.junit.Assert.assertEquals;

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.ObjectName;

import org.jboss.tools.jmx.jolokia.test.util.JolokiaTestEnvironmentSetup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JolokiaMBeanServerConnectionGetSetAttributeTest extends JolokiaTestEnvironmentSetup {

    private static final String TEST_OBJ = "test.domain:type=attributetest";
    private static final String AN_ATTRIBUTE = "AnAttribute";
    private static final String A_SECOND_ATTRIBUTE = "ASecondAttribute";

    @Test
    public void testSetAttribute() throws Exception {
        String listJson = "{\"status\":200,\"value\":{\"attr\":{"
                + "\"AnAttribute\":{\"type\":\"java.lang.String\",\"desc\":\"test\",\"rw\":true},"
                + "\"ASecondAttribute\":{\"type\":\"java.lang.String\",\"desc\":\"test2\",\"rw\":true}"
                + "},\"op\":{},\"class\":\"java.lang.Object\",\"description\":\"test\"}}";
        mockServer.setListResponse(listJson);
        String writeJson = "{\"status\":200,\"value\":\"aValue\"}";
        mockServer.setWriteResponse(writeJson);
        String readJson = "{\"status\":200,\"value\":\"aValue\"}";
        mockServer.setReadResponse(readJson);

        ObjectName on = new ObjectName(TEST_OBJ);
        jolokiaMBeanServerConnection.setAttribute(on, new Attribute(AN_ATTRIBUTE, "aValue"));

        Object result = jolokiaMBeanServerConnection.getAttribute(on, AN_ATTRIBUTE);
        assertEquals("aValue", result);
    }

    @Test
    public void testSetAttributes() throws Exception {
        String listJson = "{\"status\":200,\"value\":{\"attr\":{"
                + "\"AnAttribute\":{\"type\":\"java.lang.String\",\"desc\":\"test\",\"rw\":true},"
                + "\"ASecondAttribute\":{\"type\":\"java.lang.String\",\"desc\":\"test2\",\"rw\":true}"
                + "},\"op\":{},\"class\":\"java.lang.Object\",\"description\":\"test\"}}";
        mockServer.setListResponse(listJson);
        mockServer.setWriteResponse("{\"status\":200,\"value\":{}}");
        String bulkReadJson = "{\"status\":200,\"value\":{"
                + "\"AnAttribute\":\"aValueForSetAttributes\","
                + "\"ASecondAttribute\":\"aSecondValue\"}}";
        mockServer.setReadResponse(bulkReadJson);

        AttributeList attrList = new AttributeList();
        attrList.add(new Attribute(AN_ATTRIBUTE, "aValueForSetAttributes"));
        attrList.add(new Attribute(A_SECOND_ATTRIBUTE, "aSecondValue"));
        ObjectName on = new ObjectName(TEST_OBJ);

        AttributeList result = jolokiaMBeanServerConnection.setAttributes(on, attrList);
        assertEquals(2, result.size());
    }

    @Test
    public void testGetAttributes() throws Exception {
        String listJson = "{\"status\":200,\"value\":{\"attr\":{"
                + "\"AnAttribute\":{\"type\":\"java.lang.String\",\"desc\":\"test\",\"rw\":true},"
                + "\"ASecondAttribute\":{\"type\":\"java.lang.String\",\"desc\":\"test2\",\"rw\":true}"
                + "},\"op\":{},\"class\":\"java.lang.Object\",\"description\":\"test\"}}";
        mockServer.setListResponse(listJson);
        String bulkReadJson = "{\"status\":200,\"value\":{"
                + "\"AnAttribute\":\"aValueForGetAttributes\","
                + "\"ASecondAttribute\":\"aSecondValueForGetAttributes\"}}";
        mockServer.setReadResponse(bulkReadJson);

        ObjectName on = new ObjectName(TEST_OBJ);
        AttributeList attributes = jolokiaMBeanServerConnection.getAttributes(on,
                new String[]{AN_ATTRIBUTE, A_SECOND_ATTRIBUTE});

        assertEquals(2, attributes.size());
    }
}
