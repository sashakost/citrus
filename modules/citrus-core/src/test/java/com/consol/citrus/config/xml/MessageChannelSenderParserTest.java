/*
 * Copyright 2006-2010 the original author or authors.
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

package com.consol.citrus.config.xml;

import java.util.Map;

import org.testng.Assert;
import org.testng.annotations.Test;

import com.consol.citrus.TestActor;
import com.consol.citrus.channel.MessageChannelSender;
import com.consol.citrus.testng.AbstractBeanDefinitionParserTest;

/**
 * @author Christoph Deppisch
 */
public class MessageChannelSenderParserTest extends AbstractBeanDefinitionParserTest {

    @Test
    public void testFailActionParser() {
        Map<String, MessageChannelSender> messageSenders = beanDefinitionContext.getBeansOfType(MessageChannelSender.class);
        
        Assert.assertEquals(messageSenders.size(), 4);
        
        // 1st message sender
        MessageChannelSender messageSender = messageSenders.get("messageChannelSender1");
        Assert.assertEquals(messageSender.getChannelName(), "channelName");
        Assert.assertNull(messageSender.getChannel());
        Assert.assertNotNull(messageSender.getChannelResolver());
        
        // 2nd message sender
        messageSender = messageSenders.get("messageChannelSender2");
        Assert.assertNull(messageSender.getChannelName());
        Assert.assertNotNull(messageSender.getChannel());
        Assert.assertNull(messageSender.getChannelResolver());
        
        // 3rd message sender
        messageSender = messageSenders.get("messageChannelSender3");
        Assert.assertNull(messageSender.getChannelName());
        Assert.assertNull(messageSender.getChannel());
        Assert.assertNull(messageSender.getChannelResolver());
        
        // 4th message sender
        messageSender = messageSenders.get("messageChannelSender4");
        Assert.assertNotNull(messageSender.getActor());
        Assert.assertEquals(messageSender.getActor(), beanDefinitionContext.getBean("testActor", TestActor.class));
    }
}
