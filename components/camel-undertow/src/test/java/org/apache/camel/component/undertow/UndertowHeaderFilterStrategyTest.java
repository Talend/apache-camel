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
package org.apache.camel.component.undertow;

import org.apache.camel.spi.HeaderFilterStrategy;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class UndertowHeaderFilterStrategyTest {

    @Test
    public void testFilterInboundLegacyCxfOperationHeaders() {
        HeaderFilterStrategy filter = new UndertowHeaderFilterStrategy();
        // CVE-2026-46592: an HTTP client must not select the cxf operation
        assertTrue(filter.applyFilterToExternalHeaders("operationName", "deleteAll", null));
        assertTrue(filter.applyFilterToExternalHeaders("OPERATIONNAME", "deleteAll", null));
        assertTrue(filter.applyFilterToExternalHeaders("operationNamespace", "urn:test", null));
        assertTrue(filter.applyFilterToExternalHeaders("CamelCxfOperationName", "deleteAll", null));

        assertFalse(filter.applyFilterToExternalHeaders("MyWorld", "just a test", null));
    }
}
