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
package org.apache.camel.component.cxf.jaxrs.simplebinding;

import jakarta.xml.bind.JAXBContext;

import org.apache.camel.Exchange;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.cxf.common.CXFTestSupport;
import org.apache.camel.component.cxf.jaxrs.simplebinding.testbean.Customer;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClientBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Routes written before CVE-2026-46592 dispatch on the legacy "operationName" header set by the cxfrs consumer; they
 * must keep working, and an HTTP client must not be able to override that header.
 */
public class CxfRsConsumerLegacyOperationHeaderTest extends CamelTestSupport {
    private static final String PORT_PATH = CXFTestSupport.getPort1() + "/CxfRsConsumerLegacyOperationHeaderTest";
    private static final String CXF_RS_ENDPOINT_URI = "cxfrs://http://localhost:" + PORT_PATH
                                                      + "/rest?resourceClasses=org.apache.camel.component.cxf.jaxrs.simplebinding.testbean.CustomerServiceResource&bindingStyle=SimpleConsumer";

    @Override
    protected RouteBuilder createRouteBuilder() throws Exception {
        return new RouteBuilder() {
            public void configure() {
                from(CXF_RS_ENDPOINT_URI)
                        .recipientList(simple("direct:${header.operationName}"));

                from("direct:getCustomer").process(exchange -> {
                    exchange.getMessage().setBody(new Customer(exchange.getIn().getHeader("id", Long.class), "Raul"));
                    exchange.getMessage().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
                });

                from("direct:newCustomer").setHeader(Exchange.HTTP_RESPONSE_CODE, constant(500));
            }
        };
    }

    @Test
    public void testLegacyOperationNameHeader() throws Exception {
        JAXBContext jaxb = JAXBContext.newInstance(Customer.class);
        try (CloseableHttpClient httpclient = HttpClientBuilder.create().build()) {
            HttpGet get = new HttpGet("http://localhost:" + PORT_PATH + "/rest/customerservice/customers/123");
            get.addHeader("Accept", "text/xml");
            // must be ignored: the consumer selects the operation from the invoked resource method
            get.addHeader("operationName", "newCustomer");
            try (CloseableHttpResponse response = httpclient.execute(get)) {
                assertEquals(200, response.getCode());
                Customer entity = (Customer) jaxb.createUnmarshaller().unmarshal(response.getEntity().getContent());
                assertEquals(123, entity.getId());
            }
        }
    }
}
