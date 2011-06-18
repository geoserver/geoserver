/*
 * @(#)AttributeFactory.java
 *
 * Copyright 2003-2006 Sun Microsystems, Inc. All Rights Reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   1. Redistribution of source code must retain the above copyright notice,
 *      this list of conditions and the following disclaimer.
 * 
 *   2. Redistribution in binary form must reproduce the above copyright
 *      notice, this list of conditions and the following disclaimer in the
 *      documentation and/or other materials provided with the distribution.
 *
 * Neither the name of Sun Microsystems, Inc. or the names of contributors may
 * be used to endorse or promote products derived from this software without
 * specific prior written permission.
 * 
 * This software is provided "AS IS," without a warranty of any kind. ALL
 * EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND WARRANTIES, INCLUDING
 * ANY IMPLIED WARRANTY OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE
 * OR NON-INFRINGEMENT, ARE HEREBY EXCLUDED. SUN MICROSYSTEMS, INC. ("SUN")
 * AND ITS LICENSORS SHALL NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE
 * AS A RESULT OF USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
 * DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR ANY LOST
 * REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL, CONSEQUENTIAL,
 * INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND REGARDLESS OF THE THEORY
 * OF LIABILITY, ARISING OUT OF THE USE OF OR INABILITY TO USE THIS SOFTWARE,
 * EVEN IF SUN HAS BEEN ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.
 *
 * You acknowledge that this software is not designed or intended for use in
 * the design, construction, operation or maintenance of any nuclear facility.
 * 
 * 
 */

package com.sun.xacml.attr;

import java.net.URI;
import java.util.HashMap;
import java.util.Set;

import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.UnknownIdentifierException;

/**
 * This is an abstract factory class for creating XACML attribute values. There may be any number of
 * factories available in the system, though there is always one default factory used by the core
 * code.
 * 
 * @since 1.0
 * @author Seth Proctor
 * @author Marco Barreno
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public abstract class AttributeFactory {

    // the proxy used to get the default factory
    private static AttributeFactoryProxy defaultFactoryProxy;

    // the map of registered factories
    private static HashMap<String, AttributeFactoryProxy> registeredFactories;

    /**
     * static intialiazer that sets up the default factory proxy and registers the standard
     * namespaces
     */
    static {
        AttributeFactoryProxy proxy = new AttributeFactoryProxy() {
            public AttributeFactory getFactory() {
                return StandardAttributeFactory.getFactory();
            }
        };

        registeredFactories = new HashMap<String, AttributeFactoryProxy>();
        registeredFactories.put(PolicyMetaData.XACML_1_0_IDENTIFIER, proxy);
        registeredFactories.put(PolicyMetaData.XACML_2_0_IDENTIFIER, proxy);

        defaultFactoryProxy = proxy;
    };

    /**
     * Default constructor. Used only by subclasses.
     */
    protected AttributeFactory() {

    }

    /**
     * Returns the default factory. Depending on the default factory's implementation, this may
     * return a singleton instance or new instances with each invokation.
     * 
     * @return the default <code>AttributeFactory</code>
     */
    public static final AttributeFactory getInstance() {
        return defaultFactoryProxy.getFactory();
    }

    /**
     * Returns a factory based on the given identifier. You may register as many factories as you
     * like, and then retrieve them through this interface, but a factory may only be registered
     * once using a given identifier. By default, the standard XACML 1.0 and 2.0 identifiers are
     * regsietered to provide the standard factory.
     * 
     * @param identifier
     *            the identifier for a factory
     * 
     * @return an <code>AttributeFactory</code>
     * 
     * @throws UnknownIdentifierException
     *             if the given identifier isn't registered
     */
    public static final AttributeFactory getInstance(String identifier)
            throws UnknownIdentifierException {
        AttributeFactoryProxy proxy = (AttributeFactoryProxy) (registeredFactories.get(identifier));

        if (proxy == null)
            throw new UnknownIdentifierException("Uknown AttributeFactory " + "identifier: "
                    + identifier);

        return proxy.getFactory();
    }

    /**
     * Sets the default factory. This does not register the factory proxy as an identifiable
     * factory.
     * 
     * @param proxy
     *            the <code>AttributeFactoryProxy</code> to set as the new default factory proxy
     */
    public static final void setDefaultFactory(AttributeFactoryProxy proxy) {
        defaultFactoryProxy = proxy;
    }

    /**
     * Registers the given factory proxy with the given identifier. If the identifier is already
     * used, then this throws an exception. If the identifier is not already used, then it will
     * always be bound to the given proxy.
     * 
     * @param identifier
     *            the identifier for the proxy
     * @param proxy
     *            the <code>AttributeFactoryProxy</code> to register with the given identifier
     * 
     * @throws IllegalArgumentException
     *             if the identifier is already used
     */
    public static final void registerFactory(String identifier, AttributeFactoryProxy proxy)
            throws IllegalArgumentException {
        synchronized (registeredFactories) {
            if (registeredFactories.containsKey(identifier))
                throw new IllegalArgumentException("Identifier is already " + "registered as "
                        + "AttributeFactory: " + identifier);

            registeredFactories.put(identifier, proxy);
        }
    }

    /**
     * Adds a proxy to the factory, which in turn will allow new attribute types to be created using
     * the factory. Typically the proxy is provided as an anonymous class that simply calls the
     * getInstance methods (or something similar) of some <code>AttributeValue</code> class.
     * 
     * @param id
     *            the name of the attribute type
     * @param proxy
     *            the proxy used to create new attributes of the given type
     * 
     * @throws IllegalArgumentException
     *             if the given id is already in use
     */
    public abstract void addDatatype(String id, AttributeProxy proxy);

    /**
     * Adds a proxy to the default factory, which in turn will allow new attribute types to be
     * created using the factory. Typically the proxy is provided as an anonymous class that simply
     * calls the getInstance methods (or something similar) of some <code>AttributeValue</code>
     * class.
     * 
     * @deprecated As of version 1.2, replaced by {@link #addDatatype(String,AttributeProxy)}. The
     *             new factory system requires you to get a factory instance and then call the
     *             non-static methods on that factory. The static versions of these methods have
     *             been left in for now, but are slower and will be removed in a future version.
     *             Note that this operates only on the default factory.
     * 
     * @param id
     *            the name of the attribute type
     * @param proxy
     *            the proxy used to create new attributes of the given type
     * 
     * @throws IllegalArgumentException
     *             if the given id is already in use
     */
    public static void addAttributeProxy(String id, AttributeProxy proxy) {
        getInstance().addDatatype(id, proxy);
    }

    /**
     * Returns the datatype identifiers supported by this factory.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public abstract Set<String> getSupportedDatatypes();

    /**
     * Creates a value based on the given DOM root node. The type of the attribute is assumed to be
     * present in the node as an XAML attribute named <code>DataType</code>, as is the case with the
     * AttributeValueType in the policy schema. The value is assumed to be the first child of this
     * node.
     * 
     * @param root
     *            the DOM root of an attribute value
     * 
     * @return a new <code>AttributeValue</code>
     * 
     * @throws UnknownIdentifierException
     *             if the type in the node isn't known to the factory
     * @throws ParsingException
     *             if the node is invalid or can't be parsed by the appropriate proxy
     */
    public abstract AttributeValue createValue(Node root) throws UnknownIdentifierException,
            ParsingException;

    /**
     * Creates a value based on the given DOM root node. The type of the attribute is assumed to be
     * present in the node as an XAML attribute named <code>DataType</code>, as is the case with the
     * AttributeValueType in the policy schema. The value is assumed to be the first child of this
     * node. This uses the default factory.
     * 
     * @deprecated As of version 1.2, replaced by {@link #createValue(Node)}. The new factory system
     *             requires you to get a factory instance and then call the non-static methods on
     *             that factory. The static versions of these methods have been left in for now, but
     *             are slower and will be removed in a future version.
     * 
     * @param root
     *            the DOM root of an attribute value
     * 
     * @return a new <code>AttributeValue</code>
     * 
     * @throws UnknownIdentifierException
     *             if the type in the node isn't known to the factory
     * @throws ParsingException
     *             if the node is invalid or can't be parsed by the appropriate proxy
     */
    public static AttributeValue createAttribute(Node root) throws UnknownIdentifierException,
            ParsingException {
        return getInstance().createValue(root);
    }

    /**
     * Creates a value based on the given DOM root node and data type.
     * 
     * @param root
     *            the DOM root of an attribute value
     * @param dataType
     *            the type of the attribute
     * 
     * @return a new <code>AttributeValue</code>
     * 
     * @throws UnknownIdentifierException
     *             if the data type isn't known to the factory
     * @throws ParsingException
     *             if the node is invalid or can't be parsed by the appropriate proxy
     */
    public abstract AttributeValue createValue(Node root, URI dataType)
            throws UnknownIdentifierException, ParsingException;

    /**
     * Creates a value based on the given DOM root node and data type. This uses the default
     * factory.
     * 
     * @deprecated As of version 1.2, replaced by {@link #createValue(Node,URI)}. The new factory
     *             system requires you to get a factory instance and then call the non-static
     *             methods on that factory. The static versions of these methods have been left in
     *             for now, but are slower and will be removed in a future version.
     * 
     * @param root
     *            the DOM root of an attribute value
     * @param dataType
     *            the type of the attribute
     * 
     * @return a new <code>AttributeValue</code>
     * 
     * @throws UnknownIdentifierException
     *             if the data type isn't known to the factory
     * @throws ParsingException
     *             if the node is invalid or can't be parsed by the appropriate proxy
     */
    public static AttributeValue createAttribute(Node root, URI dataType)
            throws UnknownIdentifierException, ParsingException {
        return getInstance().createValue(root, dataType);
    }

    /**
     * Creates a value based on the given DOM root node and data type.
     * 
     * @param root
     *            the DOM root of an attribute value
     * @param type
     *            the type of the attribute
     * 
     * @return a new <code>AttributeValue</code>
     * 
     * @throws UnknownIdentifierException
     *             if the type isn't known to the factory
     * @throws ParsingException
     *             if the node is invalid or can't be parsed by the appropriate proxy
     */
    public abstract AttributeValue createValue(Node root, String type)
            throws UnknownIdentifierException, ParsingException;

    /**
     * Creates a value based on the given DOM root node and data type. This uses the default
     * factory.
     * 
     * @deprecated As of version 1.2, replaced by {@link #createValue(Node,String)}. The new factory
     *             system requires you to get a factory instance and then call the non-static
     *             methods on that factory. The static versions of these methods have been left in
     *             for now, but are slower and will be removed in a future version.
     * 
     * @param root
     *            the DOM root of an attribute value
     * @param type
     *            the type of the attribute
     * 
     * @return a new <code>AttributeValue</code>
     * 
     * @throws UnknownIdentifierException
     *             if the type isn't known to the factory
     * @throws ParsingException
     *             if the node is invalid or can't be parsed by the appropriate proxy
     */
    public static AttributeValue createAttribute(Node root, String type)
            throws UnknownIdentifierException, ParsingException {
        return getInstance().createValue(root, type);
    }

    /**
     * Creates a value based on the given data type and text-encoded value. Used primarily by code
     * that does an XPath query to get an attribute value, and then needs to turn the resulting
     * value into an Attribute class.
     * 
     * @param dataType
     *            the type of the attribute
     * @param value
     *            the text-encoded representation of an attribute's value
     * 
     * @return a new <code>AttributeValue</code>
     * 
     * @throws UnknownIdentifierException
     *             if the data type isn't known to the factory
     * @throws ParsingException
     *             if the text is invalid or can't be parsed by the appropriate proxy
     */
    public abstract AttributeValue createValue(URI dataType, String value)
            throws UnknownIdentifierException, ParsingException;

    /**
     * Creates a value based on the given data type and text-encoded value. Used primarily by code
     * that does an XPath query to get an attribute value, and then needs to turn the resulting
     * value into an Attribute class. This uses the default factory.
     * 
     * @deprecated As of version 1.2, replaced by {@link #createValue(URI,String)}. The new factory
     *             system requires you to get a factory instance and then call the non-static
     *             methods on that factory. The static versions of these methods have been left in
     *             for now, but are slower and will be removed in a future version.
     * 
     * @param dataType
     *            the type of the attribute
     * @param value
     *            the text-encoded representation of an attribute's value
     * 
     * @return a new <code>AttributeValue</code>
     * 
     * @throws UnknownIdentifierException
     *             if the data type isn't known to the factory
     * @throws ParsingException
     *             if the text is invalid or can't be parsed by the appropriate proxy
     */
    public static AttributeValue createAttribute(URI dataType, String value)
            throws UnknownIdentifierException, ParsingException {
        return getInstance().createValue(dataType, value);
    }

}
