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
package org.apache.camel.component.cxf.common.message;

import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("deprecation")
public class CxfOperationHeadersTest {

    private Message message;

    @BeforeEach
    public void setUp() {
        message = new DefaultExchange(new DefaultCamelContext()).getIn();
    }

    @AfterEach
    public void tearDown() {
        System.clearProperty(CxfConstants.LEGACY_OPERATION_HEADERS_PROPERTY);
    }

    @Test
    public void testPrefixedHeaders() {
        message.setHeader(CxfConstants.OPERATION_NAME, "greetMe");
        message.setHeader(CxfConstants.OPERATION_NAMESPACE, "urn:test");
        assertEquals("greetMe", CxfOperationHeaders.getOperationName(message));
        assertEquals("urn:test", CxfOperationHeaders.getOperationNamespace(message));
    }

    @Test
    public void testLegacyHeadersFallback() {
        message.setHeader(CxfConstants.LEGACY_OPERATION_NAME, "sayHi");
        message.setHeader(CxfConstants.LEGACY_OPERATION_NAMESPACE, "urn:legacy");
        assertEquals("sayHi", CxfOperationHeaders.getOperationName(message));
        assertEquals("urn:legacy", CxfOperationHeaders.getOperationNamespace(message));
        assertTrue(CxfOperationHeaders.hasOperationHeaders(message.getHeaders()));
    }

    @Test
    public void testPrefixedHeadersWin() {
        message.setHeader(CxfConstants.OPERATION_NAME, "greetMe");
        message.setHeader(CxfConstants.LEGACY_OPERATION_NAME, "sayHi");
        assertEquals("greetMe", CxfOperationHeaders.getOperationName(message));
    }

    @Test
    public void testLegacyHeadersDisabled() {
        System.setProperty(CxfConstants.LEGACY_OPERATION_HEADERS_PROPERTY, "false");
        message.setHeader(CxfConstants.LEGACY_OPERATION_NAME, "sayHi");
        assertNull(CxfOperationHeaders.getOperationName(message));
        assertFalse(CxfOperationHeaders.hasOperationHeaders(message.getHeaders()));

        CxfOperationHeaders.setOperationName(message, "greetMe");
        assertEquals("greetMe", message.getHeader(CxfConstants.OPERATION_NAME));
        assertEquals("sayHi", message.getHeader(CxfConstants.LEGACY_OPERATION_NAME));
    }

    @Test
    public void testSetBothHeaders() {
        CxfOperationHeaders.setOperationName(message, "greetMe");
        CxfOperationHeaders.setOperationNamespace(message, "urn:test");
        assertEquals("greetMe", message.getHeader(CxfConstants.OPERATION_NAME));
        assertEquals("greetMe", message.getHeader(CxfConstants.LEGACY_OPERATION_NAME));
        assertEquals("urn:test", message.getHeader(CxfConstants.OPERATION_NAMESPACE));
        assertEquals("urn:test", message.getHeader(CxfConstants.LEGACY_OPERATION_NAMESPACE));
    }
}
