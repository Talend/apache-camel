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
package org.apache.camel.component.aws2.ses;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.camel.BindToRegistry;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.test.junit5.CamelTestSupport;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.ses.model.MessageTag;
import software.amazon.awssdk.services.ses.model.SendEmailRequest;

import static org.junit.jupiter.api.Assertions.*;

public class SesComponentTest extends CamelTestSupport {

    @BindToRegistry("amazonSESClient")
    private AmazonSESClientMock sesClient = new AmazonSESClientMock();

    @Test
    public void sendInOnlyMessageUsingUrlOptions() {
        Exchange exchange = template.send("direct:start", new Processor() {
            @Override
            public void process(Exchange exchange) {
                exchange.getIn().setBody("This is my message text.");
            }
        });

        assertEquals("1", exchange.getIn().getHeader(Ses2Constants.MESSAGE_ID));

        SendEmailRequest sendEmailRequest = sesClient.getSendEmailRequest();
        assertEquals("from@example.com", sendEmailRequest.source());
        assertEquals(2, getTo(sendEmailRequest).size());
        assertTrue(getTo(sendEmailRequest).contains("to1@example.com"));
        assertTrue(getTo(sendEmailRequest).contains("to2@example.com"));
        assertEquals("bounce@example.com", sendEmailRequest.returnPath());
        assertEquals(2, sendEmailRequest.replyToAddresses().size());
        assertTrue(sendEmailRequest.replyToAddresses().contains("replyTo1@example.com"));
        assertTrue(sendEmailRequest.replyToAddresses().contains("replyTo2@example.com"));
        assertEquals("Subject", getSubject(sendEmailRequest));
        assertFalse(sendEmailRequest.hasTags());
        assertEquals(0, sendEmailRequest.tags().size());
        assertEquals("This is my message text.", getBody(sendEmailRequest));
    }

    @Test
    public void sendInOutMessageUsingUrlOptions() {
        Exchange exchange = template.request("direct:start", new Processor() {
            @Override
            public void process(Exchange exchange) {
                exchange.getIn().setBody("This is my message text.");
            }
        });

        assertEquals("1", exchange.getMessage().getHeader(Ses2Constants.MESSAGE_ID));
    }

    @Test
    public void sendMessageUsingMessageHeaders() {
        Exchange exchange = template.send("direct:start", new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody("This is my message text.");
                exchange.getIn().setHeader(Ses2Constants.FROM, "anotherFrom@example.com");
                exchange.getIn().setHeader(Ses2Constants.TO, "anotherTo1@example.com, anotherTo2@example.com");
                exchange.getIn().setHeader(Ses2Constants.RETURN_PATH, "anotherBounce@example.com");
                exchange.getIn().setHeader(Ses2Constants.REPLY_TO_ADDRESSES,
                        "anotherReplyTo1@example.com, anotherReplyTo2@example.com");
                exchange.getIn().setHeader(Ses2Constants.SUBJECT, "anotherSubject");
                exchange.getIn().setHeader(Ses2Constants.TAGS, Map.of("tagName", "tagValue"));
            }
        });

        assertEquals("1", exchange.getIn().getHeader(Ses2Constants.MESSAGE_ID));

        SendEmailRequest sendEmailRequest = sesClient.getSendEmailRequest();
        assertEquals("anotherFrom@example.com", sendEmailRequest.source());
        assertEquals(2, getTo(sendEmailRequest).size());
        assertTrue(getTo(sendEmailRequest).contains("anotherTo1@example.com"));
        assertTrue(getTo(sendEmailRequest).contains("anotherTo2@example.com"));
        assertEquals("anotherBounce@example.com", sendEmailRequest.returnPath());
        assertEquals(2, sendEmailRequest.replyToAddresses().size());
        assertTrue(sendEmailRequest.replyToAddresses().contains("anotherReplyTo1@example.com"));
        assertTrue(sendEmailRequest.replyToAddresses().contains("anotherReplyTo2@example.com"));
        assertEquals("anotherSubject", getSubject(sendEmailRequest));
        assertEquals("This is my message text.", getBody(sendEmailRequest));
        assertTrue(sendEmailRequest.hasTags());
        assertEquals(List.of(MessageTag.builder().name("tagName").value("tagValue").build()), sendEmailRequest.tags());
    }

    @Test
    public void sendMessageWithEmptyMessageTagsHeader() {
        Exchange exchange = template.send("direct:start", new Processor() {
            public void process(Exchange exchange) {
                exchange.getIn().setBody("This is my message text.");
                exchange.getIn().setHeader(Ses2Constants.TAGS, new HashMap<String, String>());
            }
        });

        assertEquals("1", exchange.getIn().getHeader(Ses2Constants.MESSAGE_ID));

        SendEmailRequest sendEmailRequest = sesClient.getSendEmailRequest();
        assertFalse(sendEmailRequest.hasTags());
        assertEquals(0, sendEmailRequest.tags().size());
    }

    @Override
    protected RouteBuilder createRouteBuilder() {
        return new RouteBuilder() {
            @Override
            public void configure() {
                from("direct:start")
                        .to("aws2-ses://from@example.com" + "?to=to1@example.com,to2@example.com" + "&subject=Subject"
                            + "&returnPath=bounce@example.com" + "&replyToAddresses=replyTo1@example.com,replyTo2@example.com"
                            + "&amazonSESClient=#amazonSESClient");
            }
        };
    }

    private String getBody(SendEmailRequest sendEmailRequest) {
        return sendEmailRequest.message().body().text().data();
    }

    private String getSubject(SendEmailRequest sendEmailRequest) {
        return sendEmailRequest.message().subject().data();
    }

    private List<String> getTo(SendEmailRequest sendEmailRequest) {
        return sendEmailRequest.destination().toAddresses();
    }
}
