package org.jboss.tools.jmx.jolokia.test.internal.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.Set;

import javax.management.ObjectInstance;
import javax.management.ObjectName;

import org.jboss.tools.jmx.jolokia.test.util.JolokiaTestEnvironmentSetup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JolokiaMBeanServerConnectionQueryMBeansTest extends JolokiaTestEnvironmentSetup {

    @Test
    public void testQueryWithFullySpecifiedName() throws Exception {
        String searchJson = "{\"status\":200,\"value\":[\"java.lang:type=Memory\"]}";
        mockServer.setSearchResponse(searchJson);
        String listJson = "{\"status\":200,\"value\":{"
                + "\"class\":\"sun.management.MemoryImpl\",\"description\":\"\",\"attr\":{},\"op\":{}}}";
        mockServer.setListResponse(listJson);

        ObjectName memoryON = new ObjectName("java.lang:type=Memory");
        Set<ObjectInstance> mBeans = jolokiaMBeanServerConnection.queryMBeans(memoryON, null);

        assertFalse("should not be empty", mBeans.isEmpty());
        assertEquals("sun.management.MemoryImpl", mBeans.iterator().next().getClassName());
    }

    @Test
    public void testQueryWithPartialSpecifiedName() throws Exception {
        String searchJson = "{\"status\":200,\"value\":[\"test.domain:type=operation\",\"test.domain:type=attributetest\"]}";
        mockServer.setSearchResponse(searchJson);
        String listJson = "{\"status\":200,\"value\":{\"class\":\"com.example.Operation\",\"description\":\"\",\"attr\":{},\"op\":{}}}";
        mockServer.setListResponse(listJson);

        Set<ObjectInstance> mBeans = jolokiaMBeanServerConnection.queryMBeans(
                new ObjectName("test.domain:*"), null);

        assertEquals(2, mBeans.size());
    }
}
