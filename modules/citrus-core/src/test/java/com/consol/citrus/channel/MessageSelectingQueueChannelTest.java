/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.channel;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.integration.Message;
import org.springframework.integration.core.MessageSelector;
import org.springframework.integration.support.MessageBuilder;
import org.testng.Assert;
import org.testng.annotations.Test;

import com.consol.citrus.channel.selector.HeaderMatchingMessageSelector;

/**
 * @author Christoph Deppisch
 */
public class MessageSelectingQueueChannelTest {

    @Test
    public void testReceiveSelected() {
        MessageSelectingQueueChannel channel = new MessageSelectingQueueChannel();
        channel.setPollingInterval(100L);
        
        channel.send(MessageBuilder.withPayload("FooMessage").setHeader("foo", "bar").build());
        
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("foo", "bar");
        MessageSelector selector = new HeaderMatchingMessageSelector(headers);
        
        Message<?> receivedMessage = channel.receive(selector, 1000L);
        
        Assert.assertEquals(receivedMessage.getPayload(), "FooMessage");
        Assert.assertEquals(receivedMessage.getHeaders().get("foo"), "bar");
    }
    
    @Test
    public void testWithRetry() {
        MessageSelectingQueueChannel channel = new MessageSelectingQueueChannel();
        channel.setPollingInterval(100L);
        
        channel.send(MessageBuilder.withPayload("FooMessage").setHeader("foo", "bar").build());
        
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("foo", "bar");
        
        final AtomicLong retries = new AtomicLong();
        MessageSelector selector = new HeaderMatchingMessageSelector(headers) {
            @Override
            public boolean accept(Message<?> message) {
                return retries.incrementAndGet() > 7;
            }
        };
        
        Message<?> receivedMessage = channel.receive(selector, 1000L);
        
        Assert.assertEquals(receivedMessage.getPayload(), "FooMessage");
        Assert.assertEquals(receivedMessage.getHeaders().get("foo"), "bar");
        Assert.assertEquals(retries.get(), 8L);
    }
    
    @Test
    public void testRetryExceeded() {
        MessageSelectingQueueChannel channel = new MessageSelectingQueueChannel();
        channel.setPollingInterval(500L);
        
        channel.send(MessageBuilder.withPayload("FooMessage").setHeader("foos", "bars").build());
        
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("foo", "bar");
        
        final AtomicLong retries = new AtomicLong();
        MessageSelector selector = new HeaderMatchingMessageSelector(headers) {
            @Override
            public boolean accept(Message<?> message) {
                retries.incrementAndGet();
                return super.accept(message);
            }
        };
        
        Message<?> receivedMessage = channel.receive(selector, 1000L);
        
        Assert.assertNull(receivedMessage);
        Assert.assertEquals(retries.get(), 3L);
    }
    
    @Test
    public void testRetryExceededWithTimeoutRest() {
        MessageSelectingQueueChannel channel = new MessageSelectingQueueChannel();
        channel.setPollingInterval(400L);
        
        channel.send(MessageBuilder.withPayload("FooMessage").setHeader("foos", "bars").build());
        
        Map<String, String> headers = new HashMap<String, String>();
        headers.put("foo", "bar");
        
        final AtomicLong retries = new AtomicLong();
        MessageSelector selector = new HeaderMatchingMessageSelector(headers) {
            @Override
            public boolean accept(Message<?> message) {
                retries.incrementAndGet();
                return super.accept(message);
            }
        };
        
        Message<?> receivedMessage = channel.receive(selector, 1000L);
        
        Assert.assertNull(receivedMessage);
        Assert.assertEquals(retries.get(), 4L);
    }
}
