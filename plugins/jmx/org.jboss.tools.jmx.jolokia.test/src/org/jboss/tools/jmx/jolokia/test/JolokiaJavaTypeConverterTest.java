package org.jboss.tools.jmx.jolokia.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.management.MBeanAttributeInfo;

import org.jboss.tools.jmx.jolokia.internal.connection.JolokiaJavaTypeConverter;
import org.jolokia.json.JSONArray;
import org.jolokia.json.JSONObject;
import org.junit.Test;

public class JolokiaJavaTypeConverterTest {

    private JolokiaJavaTypeConverter converter = new JolokiaJavaTypeConverter();

    @Test
    public void getJson_null() {
        assertNull(converter.getJson(null));
    }

    @Test
    public void getJson_string() {
        assertEquals("hello", converter.getJson("hello"));
    }

    @Test
    public void getJson_number() {
        assertEquals(Integer.valueOf(42), converter.getJson(42));
    }

    @Test
    public void getJson_boolean() {
        assertEquals(Boolean.TRUE, converter.getJson(true));
    }

    @Test
    public void getJson_date() {
        Date now = new Date();
        Object result = converter.getJson(now);
        assertTrue(result instanceof String);
    }

    @Test
    public void getJson_collection() {
        Collection<String> list = Arrays.asList("a", "b", "c");
        Object result = converter.getJson(list);
        assertTrue(result instanceof JSONArray);
        assertEquals(3, ((JSONArray) result).size());
    }

    @Test
    public void getJson_array() {
        int[] arr = { 1, 2, 3 };
        Object result = converter.getJson(arr);
        assertTrue(result instanceof JSONArray);
        assertEquals(3, ((JSONArray) result).size());
        assertEquals(1, ((JSONArray) result).get(0));
        assertEquals(2, ((JSONArray) result).get(1));
        assertEquals(3, ((JSONArray) result).get(2));
    }

    @Test
    public void getJson_stringArray() {
        String[] arr = { "x", "y" };
        Object result = converter.getJson(arr);
        assertTrue(result instanceof JSONArray);
        assertEquals(2, ((JSONArray) result).size());
    }

    @Test
    public void getJson_jsonObject() {
        JSONObject obj = new JSONObject();
        obj.put("key", "value");
        assertSame(obj, converter.getJson(obj));
    }

    @Test
    public void getJson_jsonArray() {
        JSONArray arr = new JSONArray();
        arr.add(1);
        assertSame(arr, converter.getJson(arr));
    }

    @Test
    public void getJson_unknownType() {
        Object custom = new Object() {
            public String toString() { return "custom"; }
        };
        assertEquals(custom, converter.getJson(custom));
    }

    @Test
    public void getConvertedToCorrectType_nullRealType() {
        assertEquals("raw", converter.getConvertedToCorrectType("raw", null));
    }

    @Test
    public void getConvertedToCorrectType_nullValue() {
        assertNull(converter.getConvertedToCorrectType(null, "int"));
    }

    @Test
    public void getConvertedToCorrectType_stringToInt() {
        Object result = converter.getConvertedToCorrectType("42", "int");
        assertEquals(42, result);
    }

    @Test
    public void getConvertedToCorrectType_stringToInteger() {
        Object result = converter.getConvertedToCorrectType("123", Integer.class.getName());
        assertEquals(Integer.valueOf(123), result);
    }

    @Test
    public void getConvertedToCorrectType_stringToDate() throws Exception {
        Object result = converter.getConvertedToCorrectType("2024-01-15T10:30:00Z", Date.class.getName());
        assertTrue(result instanceof Date);
    }

    @Test
    public void getConvertedToCorrectType_numberToInt() {
        Object result = converter.getConvertedToCorrectType(Double.valueOf(42.7), "int");
        assertEquals(42, result);
    }

    @Test
    public void getConvertedToCorrectType_numberToShort() {
        Object result = converter.getConvertedToCorrectType(Double.valueOf(5), "short");
        assertTrue(result instanceof Short);
        assertEquals((short) 5, result);
    }

    @Test
    public void getConvertedToCorrectType_numberToFloat() {
        Object result = converter.getConvertedToCorrectType(Double.valueOf(3.14), "float");
        assertTrue(result instanceof Float);
        assertEquals(3.14f, result);
    }

    @Test
    public void getConvertedToCorrectType_numberUnchanged() {
        Object result = converter.getConvertedToCorrectType(Double.valueOf(3.14), "double");
        assertEquals(Double.valueOf(3.14), result);
    }

    @Test
    public void getConvertedToCorrectType_jsonArrayToSet() {
        JSONArray arr = new JSONArray();
        arr.add("a");
        arr.add("b");
        arr.add("a");
        Object result = converter.getConvertedToCorrectType(arr, Set.class.getName());
        assertTrue(result instanceof Set);
        assertEquals(2, ((Set<?>) result).size());
    }

    @Test
    public void getConvertedToCorrectType_jsonArrayToHashSet() {
        JSONArray arr = new JSONArray();
        arr.add(1);
        arr.add(2);
        Object result = converter.getConvertedToCorrectType(arr, HashSet.class.getName());
        assertTrue(result instanceof HashSet);
        assertEquals(2, ((Set<?>) result).size());
    }

    @Test
    public void getConvertedToCorrectType_jsonArrayUnchanged() {
        JSONArray arr = new JSONArray();
        arr.add("x");
        Object result = converter.getConvertedToCorrectType(arr, List.class.getName());
        assertTrue(result instanceof JSONArray);
    }

    @Test
    public void getConvertedToCorrectTypeReturnedValue_withAttribute() throws Exception {
        MBeanAttributeInfo attr = new MBeanAttributeInfo("test", "int", "desc", true, false, false);
        Object result = converter.getConvertedToCorrectTypeReturnedValue(attr, "42");
        assertEquals(42, result);
    }

    @Test
    public void getConvertedToCorrectTypeReturnedValue_nullAttribute() {
        Object result = converter.getConvertedToCorrectTypeReturnedValue(null, "raw");
        assertEquals("raw", result);
    }

    @Test
    public void getConvertedToCorrectType_nonStringNumberArray() {
        assertEquals(Boolean.TRUE, converter.getConvertedToCorrectType(true, null));
        assertEquals(Double.valueOf(3.14), converter.getConvertedToCorrectType(3.14, "double"));
    }

    private static void assertSame(Object expected, Object actual) {
        assertTrue("Expected same instance but got different", expected == actual);
    }
}
