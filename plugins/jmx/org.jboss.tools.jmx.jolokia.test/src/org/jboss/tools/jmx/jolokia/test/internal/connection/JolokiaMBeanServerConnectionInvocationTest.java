package org.jboss.tools.jmx.jolokia.test.internal.connection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.management.ObjectName;

import org.jboss.tools.jmx.jolokia.test.util.JolokiaTestEnvironmentSetup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

@RunWith(Parameterized.class)
public class JolokiaMBeanServerConnectionInvocationTest extends JolokiaTestEnvironmentSetup {

    private static final String TEST_OBJ = "test.domain:type=operation";

    @Test
    public void testInvocationWithoutParameters() throws Exception {
        String listJson = buildOperationListJson("reset", "void", 0);
        mockServer.setListResponse(listJson);
        mockServer.setExecResponse("{\"status\":200,\"value\":null}");

        Object res = jolokiaMBeanServerConnection.invoke(
                new ObjectName(TEST_OBJ), "reset", new Object[]{}, new String[]{});
        assertEquals(null, res);
    }

    @Test
    public void testInvocationReturningLong() throws Exception {
        String listJson = buildOperationListJson("returnLong", "long", 0);
        mockServer.setListResponse(listJson);
        mockServer.setExecResponse("{\"status\":200,\"value\":1}");

        Object res = jolokiaMBeanServerConnection.invoke(
                new ObjectName(TEST_OBJ), "returnLong", new Object[]{}, new String[]{});
        assertEquals(1L, res);
    }

    @Test
    public void testInvocationReturningInt() throws Exception {
        String listJson = buildOperationListJson("returnInt", "int", 0);
        mockServer.setListResponse(listJson);
        mockServer.setExecResponse("{\"status\":200,\"value\":1}");

        Object res = jolokiaMBeanServerConnection.invoke(
                new ObjectName(TEST_OBJ), "returnInt", new Object[]{}, new String[]{});
        assertEquals(1, res);
    }

    @Test
    public void testInvocationWithParameter() throws Exception {
        String listJson = buildOperationListJson("overloadedMethod", "long",
                1, "java.lang.String");
        mockServer.setListResponse(listJson);
        mockServer.setExecResponse("{\"status\":200,\"value\":1}");

        Object res = jolokiaMBeanServerConnection.invoke(
                new ObjectName(TEST_OBJ), "overloadedMethod",
                new Object[]{"dummy"}, new String[]{String.class.getName()});
        assertEquals(1L, res);
    }

    @Test
    public void testInvocationWithSetResult() throws Exception {
        String listJson = buildOperationListJson("setOfResult", "java.util.Set", 0);
        mockServer.setListResponse(listJson);
        mockServer.setExecResponse("{\"status\":200,\"value\":[\"value1\",\"value2\"]}");

        Object res = jolokiaMBeanServerConnection.invoke(
                new ObjectName(TEST_OBJ), "setOfResult", new Object[]{}, new String[]{});
        assertNotNull(res);
    }

    private static String buildOperationListJson(String opName, String retType, int argCount, String... argTypes) {
        StringBuilder args = new StringBuilder("[");
        for (int i = 0; i < argCount; i++) {
            if (i > 0) args.append(",");
            String t = i < argTypes.length ? argTypes[i] : "java.lang.String";
            args.append("{\"name\":\"arg").append(i).append("\",\"type\":\"").append(t).append("\",\"desc\":\"\"}");
        }
        args.append("]");

        return "{\"status\":200,\"value\":{\"op\":{"
                + "\"" + opName + "\":{\"ret\":\"" + retType
                + "\",\"desc\":\"\",\"args\":" + args + "}"
                + "},\"class\":\"java.lang.Object\",\"description\":\"test\"}}";
    }
}
