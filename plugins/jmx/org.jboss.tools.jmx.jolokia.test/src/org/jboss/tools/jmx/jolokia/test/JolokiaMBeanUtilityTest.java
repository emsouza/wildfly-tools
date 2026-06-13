package org.jboss.tools.jmx.jolokia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanOperationInfo;
import javax.management.MBeanParameterInfo;

import org.jboss.tools.jmx.jolokia.internal.connection.JolokiaMBeanUtility;
import org.jolokia.json.JSONArray;
import org.jolokia.json.JSONObject;
import org.junit.Test;

public class JolokiaMBeanUtilityTest {

    private JolokiaMBeanUtility utility = new JolokiaMBeanUtility();

    @Test
    public void getAttributeInfos_null() {
        assertEquals(0, utility.getAttributeInfos(null).length);
    }

    @Test
    public void getAttributeInfos_empty() {
        assertEquals(0, utility.getAttributeInfos(new JSONObject()).length);
    }

    @Test
    public void getAttributeInfos_single() {
        JSONObject obj = new JSONObject();
        JSONObject attr = new JSONObject();
        attr.put("type", "int");
        attr.put("desc", "test attribute");
        attr.put("rw", true);
        obj.put("counter", attr);

        MBeanAttributeInfo[] infos = utility.getAttributeInfos(obj);
        assertEquals(1, infos.length);
        assertEquals("counter", infos[0].getName());
        assertEquals("int", infos[0].getType());
        assertEquals("test attribute", infos[0].getDescription());
        assertTrue(infos[0].isReadable());
        assertTrue(infos[0].isWritable());
    }

    @Test
    public void getAttributeInfos_multiple() {
        JSONObject obj = new JSONObject();
        JSONObject attr1 = new JSONObject();
        attr1.put("type", "int");
        attr1.put("desc", "first");
        attr1.put("rw", false);
        obj.put("attrA", attr1);

        JSONObject attr2 = new JSONObject();
        attr2.put("type", "long");
        attr2.put("desc", "second");
        attr2.put("rw", true);
        obj.put("attrB", attr2);

        MBeanAttributeInfo[] infos = utility.getAttributeInfos(obj);
        assertEquals(2, infos.length);
    }

    @Test
    public void getOperationInfos_null() {
        assertEquals(0, utility.getOperationInfos(null).length);
    }

    @Test
    public void getOperationInfos_single() {
        JSONObject opInfo = new JSONObject();
        opInfo.put("ret", "int");
        opInfo.put("desc", "test operation");
        opInfo.put("args", new JSONArray());

        JSONObject ops = new JSONObject();
        ops.put("doSomething", opInfo);

        MBeanOperationInfo[] infos = utility.getOperationInfos(ops);
        assertEquals(1, infos.length);
        assertEquals("doSomething", infos[0].getName());
        assertEquals("int", infos[0].getReturnType());
    }

    @Test
    public void getOperationInfos_withArgs() {
        JSONObject arg = new JSONObject();
        arg.put("name", "x");
        arg.put("type", "int");
        arg.put("desc", "argument x");

        JSONArray args = new JSONArray();
        args.add(arg);

        JSONObject opInfo = new JSONObject();
        opInfo.put("ret", "void");
        opInfo.put("desc", "op with args");
        opInfo.put("args", args);

        JSONObject ops = new JSONObject();
        ops.put("setValue", opInfo);

        MBeanOperationInfo[] infos = utility.getOperationInfos(ops);
        assertEquals(1, infos.length);
        MBeanParameterInfo[] params = infos[0].getSignature();
        assertEquals(1, params.length);
        assertEquals("x", params[0].getName());
        assertEquals("int", params[0].getType());
    }

    @Test
    public void getOperationInfos_overloaded() {
        JSONObject op1 = new JSONObject();
        op1.put("ret", "int");
        op1.put("desc", "no args");
        op1.put("args", new JSONArray());

        JSONObject op2 = new JSONObject();
        op2.put("ret", "void");
        op2.put("desc", "with arg");
        JSONArray args2 = new JSONArray();
        JSONObject arg = new JSONObject();
        arg.put("name", "x");
        arg.put("type", "int");
        arg.put("desc", "value");
        args2.add(arg);
        op2.put("args", args2);

        JSONArray overloads = new JSONArray();
        overloads.add(op1);
        overloads.add(op2);

        JSONObject ops = new JSONObject();
        ops.put("compute", overloads);

        MBeanOperationInfo[] infos = utility.getOperationInfos(ops);
        assertEquals(2, infos.length);
    }

    @Test
    public void getOperationInfos_empty() {
        assertEquals(0, utility.getOperationInfos(new JSONObject()).length);
    }

    @Test
    public void createMBeanInfoFromSingletonList_full() {
        JSONObject attr = new JSONObject();
        attr.put("type", "int");
        attr.put("desc", "counter attr");
        attr.put("rw", false);

        JSONObject attrs = new JSONObject();
        attrs.put("counter", attr);

        JSONObject opInfo = new JSONObject();
        opInfo.put("ret", "void");
        opInfo.put("desc", "reset");
        opInfo.put("args", new JSONArray());

        JSONObject ops = new JSONObject();
        ops.put("reset", opInfo);

        JSONObject obj = new JSONObject();
        obj.put("class", "com.example.TestBean");
        obj.put("description", "A test MBean");
        obj.put("attr", attrs);
        obj.put("op", ops);

        MBeanInfo info = utility.createMBeanInfoFromSingletonList(obj);
        assertNotNull(info);
        assertEquals("com.example.TestBean", info.getClassName());
        assertEquals("A test MBean", info.getDescription());
        assertEquals(1, info.getAttributes().length);
        assertEquals(1, info.getOperations().length);
        assertEquals("counter", info.getAttributes()[0].getName());
        assertEquals("reset", info.getOperations()[0].getName());
    }

    @Test
    public void createMBeanInfoFromSingletonList_minimal() {
        JSONObject obj = new JSONObject();
        MBeanInfo info = utility.createMBeanInfoFromSingletonList(obj);
        assertNotNull(info);
        assertEquals("Unknown", info.getClassName());
        assertEquals("null", info.getDescription());
        assertEquals(0, info.getAttributes().length);
        assertEquals(0, info.getOperations().length);
    }

    @Test
    public void getContructorInfos() {
        assertEquals(0, utility.getContructorInfos(null).length);
    }

    @Test
    public void getNotificationInfos() {
        assertEquals(0, utility.getNotificationInfos(null).length);
    }
}
