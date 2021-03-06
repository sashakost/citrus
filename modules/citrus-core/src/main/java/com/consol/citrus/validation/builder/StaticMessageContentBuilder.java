/*
 * Copyright 2006-2011 the original author or authors.
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

package com.consol.citrus.validation.builder;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.validation.interceptor.MessageConstructionInterceptor;
import com.consol.citrus.variable.dictionary.DataDictionary;
import org.springframework.integration.Message;
import org.springframework.integration.support.MessageBuilder;

import java.util.ArrayList;
import java.util.List;

/**
 * Message builder returning a static message every time the build mechanism is called. This
 * class is primary used in unit tests and Soap message validators as we have other mechanisms there to
 * construct the control message.
 *  
 * @author Christoph Deppisch
 */
public class StaticMessageContentBuilder<T> implements MessageContentBuilder<T> {

    /** List of manipulators for static message payload */
    private List<MessageConstructionInterceptor> messageInterceptors = new ArrayList<MessageConstructionInterceptor>();

    /** The static message to build here */
    private Message<T> message;

    /** Optional data dictionary that explicitly modifies control message content before construction */
    private DataDictionary dataDictionary;

    /**
     * Default constructor with static message to be built by this message builder.
     */
    public StaticMessageContentBuilder(Message<T> message) {
        this.message = message;
    }
    
    /**
     * Default constructor with static message to be built by this message builder. 
     */
    public static <T> StaticMessageContentBuilder<T> withMessage(Message<T> message) {
        return new StaticMessageContentBuilder<T>(message);
    }
    
    /**
     * Returns the static message every time.
     */
    @Override
    public Message<T> buildMessageContent(TestContext context, String messageType) {
        if (message != null && messageInterceptors.size() > 0) {
            Message<T> result = MessageBuilder.withPayload(message.getPayload()).copyHeaders(message.getHeaders()).build();

            if (dataDictionary != null) {
                result = (Message<T>) dataDictionary.interceptMessageConstruction(result, messageType, context);
            }

            result = (Message<T>) context.getMessageConstructionInterceptors().interceptMessageConstruction(result, messageType, context);

            for (MessageConstructionInterceptor modifyer : messageInterceptors) {
                result = (Message<T>) modifyer.interceptMessageConstruction(result, messageType, context);
            }

            return result;
        }

        return message;
    }

    /**
     * Gets the message.
     * @return the message the message to get.
     */
    public Message<T> getMessage() {
        return message;
    }

    /**
     * Adds a new interceptor to the message construction process.
     * @param interceptor
     */
    public void add(MessageConstructionInterceptor interceptor) {
        messageInterceptors.add(interceptor);
    }

    @Override
    public void setDataDictionary(DataDictionary dataDictionary) {
        this.dataDictionary = dataDictionary;
    }

    /**
     * Gets the messageInterceptors.
     * @return the messageInterceptors
     */
    public List<MessageConstructionInterceptor> getMessageInterceptors() {
        return messageInterceptors;
    }

    /**
     * Sets the messageInterceptors.
     * @param messageInterceptors the messageInterceptors to set
     */
    public void setMessageInterceptors(
            List<MessageConstructionInterceptor> messageInterceptors) {
        this.messageInterceptors = messageInterceptors;
    }
}
