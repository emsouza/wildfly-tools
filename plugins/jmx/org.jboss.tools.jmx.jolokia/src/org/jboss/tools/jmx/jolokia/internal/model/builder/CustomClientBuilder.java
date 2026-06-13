/*******************************************************************************
 * Copyright (c) 2020 Red Hat, Inc. 
 * Distributed under license by Red Hat, Inc. All rights reserved. 
 * This program is made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, 
 * and is available at http://www.eclipse.org/legal/epl-v20.html
 * 
 * Contributors: Red Hat, Inc.
 ******************************************************************************/
package org.jboss.tools.jmx.jolokia.internal.model.builder;

import org.jolokia.client.JolokiaClient;
import org.jolokia.client.JolokiaClientBuilder;

public class CustomClientBuilder extends JolokiaClientBuilder {

    public CustomClientBuilder() {
        connectionTimeout(20 * 1000);
        socketTimeout(0);
        maxTotalConnections(20);
        maxConnectionPoolTimeout(500);
        contentCharset("ISO-8859-1");
        expectContinue(true);
        tcpNoDelay(true);
        socketBufferSize(8192);
        pooledConnections();
        user(null);
        password(null);
        responseExtractor(org.jolokia.client.response.ValidatingResponseExtractor.DEFAULT);
    }

}
