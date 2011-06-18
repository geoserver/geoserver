/*
 * @(#)BaseAttributeFactory.java
 *
 * Copyright 2004 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.sun.xacml.attr;

import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;
import com.sun.xacml.UnknownIdentifierException;

/**
 * This is a basic implementation of <code>AttributeFactory</code>. It implements the insertion and
 * retrieval methods, but doesn't actually setup the factory with any datatypes.
 * <p>
 * Note that while this class is thread-safe on all creation methods, it is not safe to add support
 * for a new datatype while creating an instance of a value. This follows from the assumption that
 * most people will initialize these factories up-front, and then start processing without ever
 * modifying the factories. If you need these mutual operations to be thread-safe, then you should
 * write a wrapper class that implements the right synchronization.
 * 
 * @since 1.2
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class BaseAttributeFactory extends AttributeFactory {

    // the map of proxies
    private HashMap<String, AttributeProxy> attributeMap;

    /**
     * Default constructor.
     */
    public BaseAttributeFactory() {
        attributeMap = new HashMap<String, AttributeProxy>();
    }

    /**
     * Constructor that configures this factory with an initial set of supported datatypes.
     * 
     * @param attributes
     *            a <code>Map</code> of <code>String</code>s to </code>AttributeProxy</code>s
     * 
     * @throws IllegalArgumentException
     *             if any elements of the Map are not </code>AttributeProxy</code>s
     */
    public BaseAttributeFactory(Map<String, AttributeProxy> attributes) {
        attributeMap = new HashMap<String, AttributeProxy>();

        Iterator<String> it = attributes.keySet().iterator();
        while (it.hasNext()) {
            try {
                String id = it.next();
                AttributeProxy proxy = (AttributeProxy) (attributes.get(id));
                attributeMap.put(id, proxy);
            } catch (ClassCastException cce) {
                throw new IllegalArgumentException("an element of the map "
                        + "was not an instance of " + "AttributeProxy");
            }
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
     */
    public void addDatatype(String id, AttributeProxy proxy) {
        // make sure this doesn't already exist
        if (attributeMap.containsKey(id))
            throw new IllegalArgumentException("datatype already exists");

        attributeMap.put(id, proxy);
    }

    /**
     * Returns the datatype identifiers supported by this factory.
     * 
     * @return a <code>Set</code> of <code>String</code>s
     */
    public Set<String> getSupportedDatatypes() {
        return Collections.unmodifiableSet(attributeMap.keySet());
    }

    /**
     * Creates a value based on the given DOM root node. The type of the attribute is assumed to be
     * present in the node as an XACML attribute named <code>DataType</code>, as is the case with
     * the AttributeValueType in the policy schema. The value is assumed to be the first child of
     * this node.
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
    public AttributeValue createValue(Node root) throws UnknownIdentifierException,
            ParsingException {
        Node node = root.getAttributes().getNamedItem("DataType");

        return createValue(root, node.getNodeValue());
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
    public AttributeValue createValue(Node root, URI dataType) throws UnknownIdentifierException,
            ParsingException {
        return createValue(root, dataType.toString());
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
    public AttributeValue createValue(Node root, String type) throws UnknownIdentifierException,
            ParsingException {
        AttributeProxy proxy = (AttributeProxy) (attributeMap.get(type));

        if (proxy != null) {
            try {
                return proxy.getInstance(root);
            } catch (Exception e) {
                throw new ParsingException("couldn't create " + type
                        + " attribute based on DOM node");
            }
        } else {
            throw new UnknownIdentifierException("Attributes of type " + type
                    + " aren't supported.");
        }
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
    public AttributeValue createValue(URI dataType, String value)
            throws UnknownIdentifierException, ParsingException {
        String type = dataType.toString();
        AttributeProxy proxy = (AttributeProxy) (attributeMap.get(type));

        if (proxy != null) {
            try {
                return proxy.getInstance(value);
            } catch (Exception e) {
                throw new ParsingException("couldn't create " + type + " attribute from input: "
                        + value);
            }
        } else {
            throw new UnknownIdentifierException("Attributes of type " + type
                    + " aren't supported.");
        }
    }

}
