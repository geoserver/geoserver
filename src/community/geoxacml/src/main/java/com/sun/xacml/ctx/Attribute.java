/*
 * @(#)Attribute.java
 *
 * Copyright 2003-2004 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.ctx;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.UnknownIdentifierException;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.attr.DateTimeAttribute;

/**
 * Represents the AttributeType XML type found in the context schema.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class Attribute {

    // required meta-data attributes
    private URI id;

    private URI type;

    // optional meta-data attributes
    private String issuer = null;

    private DateTimeAttribute issueInstant = null;

    // the single value associated with this attribute
    private AttributeValue value;

    /**
     * Creates a new <code>Attribute</code> of the type specified in the given
     * <code>AttributeValue</code>.
     * 
     * @param id
     *            the id of the attribute
     * @param issuer
     *            the attribute's issuer or null if there is none
     * @param issueInstant
     *            the moment when the attribute was issued, or null if it's unspecified
     * @param value
     *            the actual value associated with the attribute meta-data
     */
    public Attribute(URI id, String issuer, DateTimeAttribute issueInstant, AttributeValue value) {
        this(id, value.getType(), issuer, issueInstant, value);
    }

    /**
     * Creates a new <code>Attribute</code>
     * 
     * @deprecated As of version 1.1, replaced by
     *             {@link #Attribute(URI,String,DateTimeAttribute,AttributeValue)}. This constructor
     *             has some ambiguity in that it allows a specified datatype and a value that
     *             already has some associated datatype. The new constructor clarifies this issue by
     *             removing the datatype parameter and using the datatype specified by the given
     *             value.
     * 
     * @param id
     *            the id of the attribute
     * @param type
     *            the type of the attribute
     * @param issuer
     *            the attribute's issuer or null if there is none
     * @param issueInstant
     *            the moment when the attribute was issued, or null if it's unspecified
     * @param value
     *            the actual value associated with the attribute meta-data
     */
    public Attribute(URI id, URI type, String issuer, DateTimeAttribute issueInstant,
            AttributeValue value) {
        this.id = id;
        this.type = type;
        this.issuer = issuer;
        this.issueInstant = issueInstant;
        this.value = value;
    }

    /**
     * Creates an instance of an <code>Attribute</code> based on the root DOM node of the XML data.
     * 
     * @param root
     *            the DOM root of the AttributeType XML type
     * 
     * @return the attribute
     * 
     *         throws ParsingException if the data is invalid
     */
    public static Attribute getInstance(Node root) throws ParsingException {
        URI id = null;
        URI type = null;
        String issuer = null;
        DateTimeAttribute issueInstant = null;
        // AttributeValue value = null;
        List<AttributeValue> valueList = new ArrayList<AttributeValue>();

        AttributeFactory attrFactory = AttributeFactory.getInstance();

        // First check that we're really parsing an Attribute
        if (!root.getNodeName().equals("Attribute")) {
            throw new ParsingException("Attribute object cannot be created "
                    + "with root node of type: " + root.getNodeName());
        }

        NamedNodeMap attrs = root.getAttributes();

        try {
            id = new URI(attrs.getNamedItem("AttributeId").getNodeValue());
        } catch (Exception e) {
            throw new ParsingException("Error parsing required attribute "
                    + "AttributeId in AttributeType", e);
        }

        try {
            type = new URI(attrs.getNamedItem("DataType").getNodeValue());
        } catch (Exception e) {
            throw new ParsingException("Error parsing required attribute "
                    + "DataType in AttributeType", e);
        }

        try {
            Node issuerNode = attrs.getNamedItem("Issuer");
            if (issuerNode != null)
                issuer = issuerNode.getNodeValue();

            Node instantNode = attrs.getNamedItem("IssueInstant");
            if (instantNode != null)
                issueInstant = DateTimeAttribute.getInstance(instantNode.getNodeValue());
        } catch (Exception e) {
            // shouldn't happen, but just in case...
            throw new ParsingException("Error parsing optional AttributeType" + " attribute", e);
        }

        // now we get the attribute value
        NodeList nodes = root.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeName().equals("AttributeValue")) {
                // only one value can be in an Attribute
                // if (value != null)
                // throw new ParsingException("Too many values in Attribute");

                // now get the value
                try {
                    AttributeValue value = attrFactory.createValue(node, type);
                    valueList.add(value);
                } catch (UnknownIdentifierException uie) {
                    throw new ParsingException("Unknown AttributeId", uie);
                }
            }
        }

        // make sure we got a value
        // if (value == null)
        // throw new ParsingException("Attribute must contain a value");
        if (valueList.isEmpty())
            throw new ParsingException("Attribute must contain a value");
        else if (valueList.size() == 1)
            return new Attribute(id, type, issuer, issueInstant, valueList.get(0));
        else {
            BagAttribute bag = new BagAttribute(type, valueList);
            return new Attribute(id, type, issuer, issueInstant, bag);
        }
    }

    /**
     * Returns the id of this attribute
     * 
     * @return the attribute id
     */
    public URI getId() {
        return id;
    }

    /**
     * Returns the data type of this attribute
     * 
     * @return the attribute's data type
     */
    public URI getType() {
        return type;
    }

    /**
     * Returns the issuer of this attribute, or null if no issuer was named
     * 
     * @return the issuer or null
     */
    public String getIssuer() {
        return issuer;
    }

    /**
     * Returns the moment at which the attribute was issued, or null if no issue time was provided
     * 
     * @return the time of issuance or null
     */
    public DateTimeAttribute getIssueInstant() {
        return issueInstant;
    }

    /**
     * The value of this attribute, or null if no value was included
     * 
     * @return the attribute's value or null
     */
    public AttributeValue getValue() {
        return value;
    }

    /**
     * Encodes this attribute into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this attribute into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        // setup the formatting & outstream stuff
        String indent = indenter.makeString();
        PrintStream out = new PrintStream(output);

        // write out the encoded form
        out.println(indent + encode());
    }

    /**
     * Simple encoding method that returns the text-encoded version of this attribute with no
     * formatting.
     * 
     * @return the text-encoded XML
     */
    public String encode() {
        String encoded = "<Attribute AttributeId=\"" + id.toString() + "\" " + "DataType=\""
                + type.toString() + "\"";

        if (issuer != null)
            encoded += " Issuer=\"" + issuer + "\"";

        if (issueInstant != null)
            encoded += " IssueInstant=\"" + issueInstant.encode() + "\"";

        encoded += ">" + value.encodeWithTags(false) + "</Attribute>";

        return encoded;
    }

}
