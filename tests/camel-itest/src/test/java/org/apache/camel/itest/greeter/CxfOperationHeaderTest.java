/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.camel.itest.greeter;

import org.apache.camel.ProducerTemplate;
import org.apache.camel.component.cxf.common.message.CxfConstants;
import org.apache.camel.test.AvailablePortFinder;
import org.apache.camel.test.spring.junit5.CamelSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * CVE-2026-46592: the operation invoked by a cxf producer can be selected by the route with both the
 * {@link CxfConstants#OPERATION_NAME} and the legacy "operationName" headers, but not by an HTTP client.
 */
@CamelSpringTest
@ContextConfiguration
public class CxfOperationHeaderTest {

    private static int port1 = AvailablePortFinder.getNextAvailable();
    private static int port2 = AvailablePortFinder.getNextAvailable();
    static {
        //set them as system properties so Spring can use the property place holder
        //things to set them into the URL's in the spring contexts
        System.setProperty("CxfOperationHeaderTest.port1", Integer.toString(port1));
        System.setProperty("CxfOperationHeaderTest.port2", Integer.toString(port2));
    }

    @Autowired
    protected ProducerTemplate template;

    @Test
    void testHttpClientCannotSelectOperation() {
        String url = "http://localhost:" + port2 + "/bridge";
        assertEquals("Hello Willem", template.requestBody(url, "Willem", String.class));
        assertEquals("Hello Willem", template.requestBodyAndHeader(url, "Willem", "operationName", "sayHi", String.class));
        assertEquals("Hello Willem", template.requestBodyAndHeader(url, "Willem", "OPERATIONNAME", "sayHi", String.class));
        assertEquals("Hello Willem",
                template.requestBodyAndHeader(url, "Willem", CxfConstants.OPERATION_NAME, "sayHi", String.class));
    }

    @Test
    void testLegacyOperationHeaderSetByRoute() {
        assertEquals("Bonjour", template.requestBody("direct:legacy", new Object[0], String.class));
    }

    @Test
    void testPrefixedOperationHeaderWins() {
        assertEquals("Bonjour",
                template.requestBodyAndHeader("direct:prefixed", new Object[0], CxfConstants.OPERATION_NAME, "sayHi",
                        String.class));
        assertEquals("Hello Willem",
                template.requestBodyAndHeader("direct:legacy", "Willem", CxfConstants.OPERATION_NAME, "greetMe",
                        String.class));
    }
}
