package org.jboss.tools.jmx.jolokia.test.internal.connection;

import static org.junit.Assert.assertTrue;

import org.jboss.tools.jmx.jolokia.test.util.JolokiaTestEnvironmentSetup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JolokiaMBeanServerConnectionGetDomainsTest extends JolokiaTestEnvironmentSetup {

    @Test
    public void testGetDomains() throws Exception {
        String searchJson = "{\"status\":200,\"value\":[\"java.lang:type=Memory\",\"jolokia.it:type=operation\"]}";
        mockServer.setSearchResponse(searchJson);

        String[] res = jolokiaMBeanServerConnection.getDomains();

        assertTrue("should contain java.lang", contains(res, "java.lang"));
        assertTrue("should contain jolokia.it", contains(res, "jolokia.it"));
    }

    private static boolean contains(String[] arr, String val) {
        for (String s : arr) {
            if (val.equals(s)) return true;
        }
        return false;
    }
}
