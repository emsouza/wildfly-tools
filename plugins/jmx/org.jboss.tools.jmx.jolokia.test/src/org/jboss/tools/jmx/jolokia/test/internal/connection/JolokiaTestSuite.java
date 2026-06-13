package org.jboss.tools.jmx.jolokia.test.internal.connection;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({
    JolokiaMBeanServerConnectionGetDomainsTest.class,
    JolokiaMBeanServerConnectionInvocationTest.class,
    JolokiaMBeanServerConnectionMBeanInfoTest.class,
    JolokiaMBeanServerConnectionQueryMBeansTest.class,
    JolokiaMBeanServerConnectionGetSetAttributeTest.class,
})
public class JolokiaTestSuite {
}
