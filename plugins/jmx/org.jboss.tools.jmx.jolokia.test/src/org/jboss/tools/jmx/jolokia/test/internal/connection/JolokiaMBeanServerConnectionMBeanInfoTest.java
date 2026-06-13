package org.jboss.tools.jmx.jolokia.test.internal.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.management.MBeanInfo;
import javax.management.ObjectName;

import org.jboss.tools.jmx.jolokia.test.util.JolokiaTestEnvironmentSetup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JolokiaMBeanServerConnectionMBeanInfoTest extends JolokiaTestEnvironmentSetup {

    @Test
    public void testClassNameAvailable() throws Exception {
        String listJson = "{\"status\":200,\"value\":{"
                + "\"class\":\"com.example.TestBean\","
                + "\"description\":\"A test MBean\","
                + "\"attr\":{},"
                + "\"op\":{}}}";
        mockServer.setListResponse(listJson);

        MBeanInfo info = jolokiaMBeanServerConnection.getMBeanInfo(
                new ObjectName("test.domain:type=operation"));

        assertNotNull(info);
        assertEquals("com.example.TestBean", info.getClassName());
    }

    @Test
    public void testGetOperations() throws Exception {
        String listJson = "{\"status\":200,\"value\":{"
                + "\"class\":\"com.example.TestBean\","
                + "\"description\":\"A test MBean\","
                + "\"attr\":{},"
                + "\"op\":{\"reset\":{\"ret\":\"void\",\"desc\":\"\",\"args\":[]},"
                + "       \"echo\":{\"ret\":\"java.lang.String\",\"desc\":\"\",\"args\":["
                + "           {\"name\":\"msg\",\"type\":\"java.lang.String\",\"desc\":\"\"}]}}}}";
        mockServer.setListResponse(listJson);

        MBeanInfo info = jolokiaMBeanServerConnection.getMBeanInfo(
                new ObjectName("test.domain:type=operation"));

        assertNotNull(info);
        assertEquals(2, info.getOperations().length);
    }
}
