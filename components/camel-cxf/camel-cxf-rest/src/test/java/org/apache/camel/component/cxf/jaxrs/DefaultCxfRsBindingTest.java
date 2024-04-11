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
package org.apache.camel.component.cxf.jaxrs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.HttpHeaders;

import org.apache.camel.Exchange;
import org.apache.camel.Message;
import org.apache.camel.impl.DefaultCamelContext;
import org.apache.camel.support.DefaultExchange;
import org.apache.camel.support.DefaultHeaderFilterStrategy;
import org.apache.camel.support.DefaultMessage;
import org.apache.camel.support.ExchangeHelper;
import org.apache.cxf.jaxrs.client.WebClient;
import org.apache.cxf.message.MessageImpl;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class DefaultCxfRsBindingTest {

    private DefaultCamelContext context = new DefaultCamelContext();

    @Test
    public void testSetCharsetWithContentType() {
        DefaultCxfRsBinding cxfRsBinding = new DefaultCxfRsBinding();
        Exchange exchange = new DefaultExchange(context);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/xml;charset=ISO-8859-1");
        cxfRsBinding.setCharsetWithContentType(exchange);

        String charset = ExchangeHelper.getCharsetName(exchange);
        assertEquals("ISO-8859-1", charset, "Get a wrong charset");

        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "text/xml");
        cxfRsBinding.setCharsetWithContentType(exchange);
        charset = ExchangeHelper.getCharsetName(exchange);
        assertEquals("UTF-8", charset, "Get a worng charset name");
    }

    @Test
    public void testCopyProtocolHeader() {
        DefaultCxfRsBinding cxfRsBinding = new DefaultCxfRsBinding();
        cxfRsBinding.setHeaderFilterStrategy(new DefaultHeaderFilterStrategy());
        Exchange exchange = new DefaultExchange(context);
        Message camelMessage = new DefaultMessage(context);
        org.apache.cxf.message.Message cxfMessage = new MessageImpl();
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("emptyList", Collections.<String> emptyList());
        headers.put("zeroSizeList", new ArrayList<String>(0));
        cxfMessage.put(org.apache.cxf.message.Message.PROTOCOL_HEADERS, headers);
        cxfRsBinding.copyProtocolHeader(cxfMessage, camelMessage, exchange);
        assertNull(camelMessage.getHeader("emptyList"), "We should get nothing here");
        assertNull(camelMessage.getHeader("zeroSizeList"), "We should get nothing here");
    }

    @Test
    public void testContentLanguage() throws Exception {
        DefaultCxfRsBinding cxfRsBinding = new DefaultCxfRsBinding();
        cxfRsBinding.setHeaderFilterStrategy(new DefaultHeaderFilterStrategy());
        Exchange exchange = new DefaultExchange(context);
        Message camelMessage = new DefaultMessage(context);

        Map<String, List<String>> headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_LANGUAGE, Arrays.asList(Locale.US.getLanguage()));

        Entity<Object> entity0 = cxfRsBinding.bindCamelMessageToRequestEntity("body", camelMessage, exchange, null);
        assertEquals(Locale.US.getLanguage(), entity0.getLanguage().getLanguage());

        WebClient deWebClient = WebClient.create("http://localhost");
        deWebClient.header(HttpHeaders.CONTENT_LANGUAGE, Locale.GERMAN.getLanguage());

        Entity<Object> entity = cxfRsBinding.bindCamelMessageToRequestEntity("body", camelMessage, exchange, deWebClient);
        assertEquals(Locale.GERMAN.getLanguage(), entity.getLanguage().getLanguage());

        Entity<Object> entity2 = cxfRsBinding.bindCamelMessageToRequestEntity("body", camelMessage, exchange, deWebClient);
        assertEquals(Locale.GERMAN.getLanguage(), entity2.getLanguage().getLanguage());

        Entity<Object> entity3 = cxfRsBinding.bindCamelMessageToRequestEntity("body", camelMessage, exchange, null);
        assertEquals(Locale.GERMAN.getLanguage(), entity3.getLanguage().getLanguage());

        //check that if a new webclient with a different language

        WebClient frWebClient = WebClient.create("http://localhost");
        frWebClient.header(HttpHeaders.CONTENT_LANGUAGE, Locale.FRANCE.getLanguage());

        Entity<Object> entity4 = cxfRsBinding.bindCamelMessageToRequestEntity("body", camelMessage, exchange, frWebClient);
        assertEquals(Locale.FRANCE.getLanguage(), entity4.getLanguage().getLanguage());

    }
}
