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

package com.consol.citrus.ws.actions;

import com.consol.citrus.TestAction;
import com.consol.citrus.container.AbstractActionContainer;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.CitrusRuntimeException;
import com.consol.citrus.exceptions.ValidationException;
import com.consol.citrus.util.FileUtils;
import com.consol.citrus.validation.context.ValidationContext;
import com.consol.citrus.ws.message.CitrusSoapMessageHeaders;
import com.consol.citrus.ws.validation.SimpleSoapFaultValidator;
import com.consol.citrus.ws.validation.SoapFaultValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.ws.soap.*;
import org.springframework.ws.soap.client.SoapFaultClientException;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinition;
import org.springframework.ws.soap.server.endpoint.SoapFaultDefinitionEditor;
import org.springframework.ws.soap.soap11.Soap11Body;
import org.springframework.ws.soap.soap12.Soap12Body;
import org.springframework.ws.soap.soap12.Soap12Fault;
import org.springframework.xml.transform.StringSource;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Asserting SOAP fault exception in embedded test action.
 * 
 * Class constructs a control soap fault detail with given expeceted information (faultCode, faultString and faultDetail)
 * and delegates validation to {@link SoapFaultValidator} instance.
 * 
 * @author Christoph Deppisch 
 * @since 2009
 */
public class AssertSoapFault extends AbstractActionContainer {
    /** TestAction to be executed */
    private TestAction action;

    /** Localized fault string */
    private String faultString = null;
    
    /** OName representing fault code */
    private String faultCode = null;
    
    /** Fault actor */
    private String faultActor = null;
    
    /** List of fault details, either inline data or file resource path */
    private List<String> faultDetails = new ArrayList<String>();
    
    /** Soap fault validator implementation */
    private SoapFaultValidator validator = new SimpleSoapFaultValidator();
    
    /** Validation context */
    private ValidationContext validationContext;
    
    /** Message factory creating new soap messages */
    private SoapMessageFactory messageFactory;
    
    /** Logger */
    private static Logger log = LoggerFactory.getLogger(AssertSoapFault.class);

    /**
     * Default constructor.
     */
    public AssertSoapFault() {
        setName("soap-fault");
    }

    @Override
    public void doExecute(TestContext context) {
        log.info("Asserting SOAP fault ...");

        try {
            action.execute(context);
        } catch (SoapFaultClientException e) {
            log.info("Validating SOAP fault ...");
            
            SoapFaultClientException soapFaultException = (SoapFaultClientException)e;

            SoapFault controlFault = constructControlFault(context);
            
            validator.validateSoapFault(soapFaultException.getSoapFault(), controlFault, context, validationContext);
            
            log.info("SOAP fault as expected: " + soapFaultException.getFaultCode() + ": " + soapFaultException.getFaultStringOrReason());
            log.info("SOAP fault validation successful");
            
            return;
        } catch (RuntimeException e) {
            throw new ValidationException("SOAP fault validation failed for asserted exception type - expected: '" + 
                    SoapFaultClientException.class + "' but was: '" + e.getClass().getName() + "'", e);
        } catch (Exception e) {
            throw new ValidationException("SOAP fault validation failed for asserted exception type - expected: '" + 
                    SoapFaultClientException.class + "' but was: '" + e.getClass().getName() + "'", e);
        }
        
        throw new ValidationException("SOAP fault validation failed! Missing asserted SOAP fault exception");
    }

    /**
     * Constructs the control soap fault holding all expected fault information
     * like faultCode, faultString and faultDetail.
     * 
     * @return the constructed SoapFault instance.
     */
    private SoapFault constructControlFault(TestContext context) {
        SoapFault controlFault = null;
        
        try {
            SoapFaultDefinition definition = getSoapFaultDefinition(context);
            SoapBody soapBody = ((SoapMessage)messageFactory.createWebServiceMessage()).getSoapBody();
        
            if (SoapFaultDefinition.SERVER.equals(definition.getFaultCode()) ||
                    SoapFaultDefinition.RECEIVER.equals(definition.getFaultCode())) {
                controlFault = soapBody.addServerOrReceiverFault(definition.getFaultStringOrReason(), 
                        definition.getLocale());
            } else if (SoapFaultDefinition.CLIENT.equals(definition.getFaultCode()) ||
                    SoapFaultDefinition.SENDER.equals(definition.getFaultCode())) {
                controlFault = soapBody.addClientOrSenderFault(definition.getFaultStringOrReason(), 
                        definition.getLocale());
            } else if (soapBody instanceof Soap11Body) {
                Soap11Body soap11Body = (Soap11Body) soapBody;
                controlFault = soap11Body.addFault(definition.getFaultCode(), 
                        definition.getFaultStringOrReason(), 
                        definition.getLocale());
            } else if (soapBody instanceof Soap12Body) {
                Soap12Body soap12Body = (Soap12Body) soapBody;
                Soap12Fault soap12Fault =
                        (Soap12Fault) soap12Body.addServerOrReceiverFault(definition.getFaultStringOrReason(), 
                                definition.getLocale());
                soap12Fault.addFaultSubcode(definition.getFaultCode());
                
                controlFault = soap12Fault;
            } else {
                    throw new CitrusRuntimeException("Found unsupported SOAP implementation. Use SOAP 1.1 or SOAP 1.2.");
            }
            
            if (StringUtils.hasText(faultActor)) {
                controlFault.setFaultActorOrRole(faultActor);
            }
        
            addFaultDetail(controlFault, context);
        } catch (IOException ex) {
            throw new CitrusRuntimeException("Error during SOAP fault validation", ex);
        } catch (TransformerException ex) {
            throw new CitrusRuntimeException("Error during SOAP fault validation", ex);
        }
        
        return controlFault;
    }

    /**
     * Adds a fault detail to soap fault object if specified.
     * @param fault the fault object.
     * @param context the current test context.
     * @throws IOException 
     * @throws TransformerException 
     */
    private void addFaultDetail(SoapFault fault, TestContext context) throws TransformerException, IOException {
        if (!faultDetails.isEmpty()) {
            TransformerFactory transformerFactory = TransformerFactory.newInstance();
            Transformer transformer = transformerFactory.newTransformer();
            
            transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            
            SoapFaultDetail soapFaultDetail = fault.addFaultDetail();
            for (int i = 0; i < faultDetails.size(); i++) {
                String faultDetail = faultDetails.get(i);
                
                if (faultDetail.startsWith(CitrusSoapMessageHeaders.SOAP_FAULT_DETAIL_RESOURCE)) {
                    String resourcePath = faultDetail.substring(CitrusSoapMessageHeaders.SOAP_FAULT_DETAIL_RESOURCE.length() + 1, faultDetail.length() - 1);
                    
                    transformer.transform(new StringSource(
                        context.replaceDynamicContentInString(FileUtils.readToString(FileUtils.getFileResource(resourcePath, context)))), soapFaultDetail.getResult());
                } else {
                    transformer.transform(new StringSource(
                        context.replaceDynamicContentInString(faultDetail)), soapFaultDetail.getResult());
                }
            }
        }
    }

    /**
     * Constructs a new fault definition object from fault code and string.
     * @param context the current test context.
     * @return the soap fault definition.
     */
    private SoapFaultDefinition getSoapFaultDefinition(TestContext context) {
        SoapFaultDefinitionEditor definitionEditor = new SoapFaultDefinitionEditor();
        
        if (StringUtils.hasText(faultString)) {
            definitionEditor.setAsText(context.replaceDynamicContentInString(faultCode) + "," + context.replaceDynamicContentInString(faultString));
        } else {
            definitionEditor.setAsText(context.replaceDynamicContentInString(faultCode));
        }
        
        return (SoapFaultDefinition)definitionEditor.getValue();
    }

    /**
     * Set the nested test action.
     * @param action the action to set
     */
    public void setAction(TestAction action) {
        this.action = action;
    }

	/**
	 * Set the fault code.
	 * @param faultCode the faultCode to set
	 */
	public void setFaultCode(String faultCode) {
		this.faultCode = faultCode;
	}

	/**
	 * Set the fault string.
	 * @param faultString the faultString to set
	 */
	public void setFaultString(String faultString) {
		this.faultString = faultString;
	}

    /**
     * @param validator the validator to set
     */
    public void setValidator(SoapFaultValidator validator) {
        this.validator = validator;
    }

    /**
     * @param messageFactory the messageFactory to set
     */
    public void setMessageFactory(SoapMessageFactory messageFactory) {
        this.messageFactory = messageFactory;
    }
    
    /**
     * @see com.consol.citrus.container.TestActionContainer#addTestAction(com.consol.citrus.TestAction)
     */
    public void addTestAction(TestAction action) {
        this.action = action;
    }

    /**
     * @see com.consol.citrus.container.TestActionContainer#getActionCount()
     */
    public long getActionCount() {
        return 1;
    }

    /**
     * @see com.consol.citrus.container.TestActionContainer#getActionIndex(com.consol.citrus.TestAction)
     */
    public int getActionIndex(TestAction action) {
        return 0;
    }

    /**
     * @see com.consol.citrus.container.TestActionContainer#getActions()
     */
    public List<TestAction> getActions() {
        return Collections.singletonList(action);
    }

    /**
     * @see com.consol.citrus.container.TestActionContainer#getTestAction(int)
     */
    public TestAction getTestAction(int index) {
        if (index == 0) {
            return action;
        } else {
            throw new IndexOutOfBoundsException("Illegal index in action list:" + index);
        }
    }

    /**
     * @see com.consol.citrus.container.TestActionContainer#setActions(java.util.List)
     */
    public void setActions(List<TestAction> actions) {
        if (!CollectionUtils.isEmpty(actions)) {
            action = actions.get(0); 
        }
    }

    /**
     * Gets the action.
     * @return the action
     */
    public TestAction getAction() {
        return action;
    }

    /**
     * Gets the faultString.
     * @return the faultString
     */
    public String getFaultString() {
        return faultString;
    }

    /**
     * Gets the faultCode.
     * @return the faultCode
     */
    public String getFaultCode() {
        return faultCode;
    }

    /**
     * Gets the list of fault details.
     * @return the faultDetails
     */
    public List<String> getFaultDetails() {
        return faultDetails;
    }
    
    /**
     * Sets the faultDetails.
     * @param faultDetails the faultDetails to set
     */
    public void setFaultDetails(List<String> faultDetails) {
        this.faultDetails = faultDetails;
    }

    /**
     * Gets the validator.
     * @return the validator
     */
    public SoapFaultValidator getValidator() {
        return validator;
    }

    /**
     * Gets the messageFactory.
     * @return the messageFactory
     */
    public SoapMessageFactory getMessageFactory() {
        return messageFactory;
    }

    /**
     * Gets the faultActor.
     * @return the faultActor the faultActor to get.
     */
    public String getFaultActor() {
        return faultActor;
    }

    /**
     * Sets the faultActor.
     * @param faultActor the faultActor to set
     */
    public void setFaultActor(String faultActor) {
        this.faultActor = faultActor;
    }

    /**
     * Gets the validationContext.
     * @return the validationContext the validationContext to get.
     */
    public ValidationContext getValidationContext() {
        return validationContext;
    }

    /**
     * Sets the validationContext.
     * @param validationContext the validationContext to set
     */
    public void setValidationContext(ValidationContext validationContext) {
        this.validationContext = validationContext;
    }
    
}
