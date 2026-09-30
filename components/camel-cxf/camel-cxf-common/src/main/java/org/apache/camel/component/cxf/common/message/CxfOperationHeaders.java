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

import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.camel.Message;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Reads and writes the CXF operation selection headers.
 * <p>
 * Since CVE-2026-46592 the operation is selected with the Camel prefixed {@link CxfConstants#OPERATION_NAME} and
 * {@link CxfConstants#OPERATION_NAMESPACE} headers, which are filtered on transport boundaries. The legacy
 * {@link CxfConstants#LEGACY_OPERATION_NAME} and {@link CxfConstants#LEGACY_OPERATION_NAMESPACE} headers are still
 * honoured as a fallback (the prefixed header always wins) so that existing routes keep working; the HTTP header filter
 * strategies strip them from inbound requests so they can only be set by the route itself. Set the
 * {@link CxfConstants#LEGACY_OPERATION_HEADERS_PROPERTY} system property to {@code false} to disable the fallback.
 */
@SuppressWarnings("deprecation")
public final class CxfOperationHeaders {

    private static final Logger LOG = LoggerFactory.getLogger(CxfOperationHeaders.class);
    private static final AtomicBoolean LEGACY_WARNED = new AtomicBoolean();

    private CxfOperationHeaders() {
        // Utility class
    }

    public static boolean isLegacyHeadersEnabled() {
        return !"false".equalsIgnoreCase(System.getProperty(CxfConstants.LEGACY_OPERATION_HEADERS_PROPERTY));
    }

    public static String getOperationName(Message message) {
        return getHeader(message, CxfConstants.OPERATION_NAME, CxfConstants.LEGACY_OPERATION_NAME);
    }

    public static String getOperationNamespace(Message message) {
        return getHeader(message, CxfConstants.OPERATION_NAMESPACE, CxfConstants.LEGACY_OPERATION_NAMESPACE);
    }

    public static void setOperationName(Message message, String operationName) {
        message.setHeader(CxfConstants.OPERATION_NAME, operationName);
        if (isLegacyHeadersEnabled()) {
            message.setHeader(CxfConstants.LEGACY_OPERATION_NAME, operationName);
        }
    }

    public static void setOperationNamespace(Message message, String operationNamespace) {
        message.setHeader(CxfConstants.OPERATION_NAMESPACE, operationNamespace);
        if (isLegacyHeadersEnabled()) {
            message.setHeader(CxfConstants.LEGACY_OPERATION_NAMESPACE, operationNamespace);
        }
    }

    /**
     * Whether the given headers select an operation, either with the prefixed or (if enabled) the legacy headers.
     */
    public static boolean hasOperationHeaders(Map<String, Object> headers) {
        if (headers.get(CxfConstants.OPERATION_NAME) != null || headers.get(CxfConstants.OPERATION_NAMESPACE) != null) {
            return true;
        }
        return isLegacyHeadersEnabled()
                && (headers.get(CxfConstants.LEGACY_OPERATION_NAME) != null
                        || headers.get(CxfConstants.LEGACY_OPERATION_NAMESPACE) != null);
    }

    private static String getHeader(Message message, String name, String legacyName) {
        String answer = message.getHeader(name, String.class);
        if (answer == null && isLegacyHeadersEnabled()) {
            answer = message.getHeader(legacyName, String.class);
            if (answer != null && LEGACY_WARNED.compareAndSet(false, true)) {
                LOG.warn("The CXF operation is selected with the deprecated '{}' header, use '{}' instead."
                         + " Set the system property {}=false to disable the deprecated headers.",
                        legacyName, name, CxfConstants.LEGACY_OPERATION_HEADERS_PROPERTY);
            }
        }
        return answer;
    }
}
