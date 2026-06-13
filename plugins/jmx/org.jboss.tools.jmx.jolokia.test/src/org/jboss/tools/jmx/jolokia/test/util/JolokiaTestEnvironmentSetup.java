package org.jboss.tools.jmx.jolokia.test.util;

import java.util.Arrays;

import org.jboss.tools.jmx.jolokia.internal.connection.JolokiaMBeanServerConnection;
import org.jolokia.client.JolokiaClient;
import org.jolokia.client.JolokiaClientBuilder;
import org.jolokia.client.request.HttpMethod;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

public class JolokiaTestEnvironmentSetup {

    protected static MockJolokiaServer mockServer;
    protected static JolokiaClient jolokiaClient;
    protected JolokiaMBeanServerConnection jolokiaMBeanServerConnection;

    @Parameter
    public HttpMethod requestType;

    @Parameters(name = "{0}")
    public static Iterable<? extends Object> data() {
        return Arrays.asList(HttpMethod.GET, HttpMethod.POST);
    }

    @BeforeClass
    public static void start() throws Exception {
        mockServer = new MockJolokiaServer();
        mockServer.start();
        JolokiaClientBuilder jb = new JolokiaClientBuilder();
        jb.url(mockServer.getUrl());
        jolokiaClient = jb.build();
    }

    @AfterClass
    public static void stop() throws Exception {
        if (mockServer != null) {
            mockServer.stop();
        }
    }

    @Before
    public void setup() throws Exception {
        jolokiaMBeanServerConnection = new JolokiaMBeanServerConnection(jolokiaClient, requestType);
    }

    @After
    public void tearDown() {
        jolokiaMBeanServerConnection = null;
    }
}
