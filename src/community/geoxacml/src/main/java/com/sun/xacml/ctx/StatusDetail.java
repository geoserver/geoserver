/*
 * @(#)StatusDetail.java
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

import java.io.ByteArrayInputStream;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Node;

import com.sun.xacml.ParsingException;

/**
 * This class represents the StatusDetailType in the context schema. Because status detail is
 * defined as a sequence of xs:any XML type, the data in this class must be generic, and it is up to
 * the application developer to interpret the data appropriately.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public class StatusDetail {

    // the root node
    private Node detailRoot;

    // the text version, if it's avilable already
    private String detailText = null;

    /**
     * Constructor that uses a <code>List</code> of <code>Attribute</code>s to define the status
     * detail. This is a common form of detail data, and can be used for things like providing the
     * information included with the missing-attribute status code.
     * 
     * @param attributes
     *            a <code>List</code> of <code>Attribute</code>s
     * 
     * @throws IllegalArgumentException
     *             if there is a problem encoding the <code>Attribute</code>s
     */
    public StatusDetail(List<Attribute> attributes) throws IllegalArgumentException {
        detailText = "<StatusDetail>\n";

        for (Attribute attr : attributes)
            detailText += attr.encode() + "\n";

        detailText += "</StatusDetail>";

        try {
            detailRoot = textToNode(detailText);
        } catch (ParsingException pe) {
            // really, this should never happen, since we just made sure that
            // we're working with valid text, but it's possible that encoding
            // the attribute could have caused problems...
            throw new IllegalArgumentException("invalid Attribute data");
        }
    }

    /**
     * Constructor that takes the text-encoded form of the XML to use as the status data. The
     * encoded text will be wrapped with the <code>StatusDetail</code> XML tag, and the resulting
     * text must be valid XML or a <code>ParsingException</code> will be thrown.
     * 
     * @param encoded
     *            a non-null <code>String</code> that encodes the status detail
     * 
     * @throws ParsingException
     *             if the encoded text is invalid XML
     */
    public StatusDetail(String encoded) throws ParsingException {
        detailText = "<StatusDetail>\n" + encoded + "\n</StatusDetail>";
        detailRoot = textToNode(detailText);
    }

    /**
     * Private constructor that just sets the root node. This interface is provided publically
     * through the getInstance method.
     */
    private StatusDetail(Node root) {
        detailRoot = root;
    }

    /**
     * Private helper routine that converts text into a node
     */
    private Node textToNode(String encoded) throws ParsingException {
        try {
            String text = "<?xml version=\"1.0\"?>\n";
            byte[] bytes = (text + encoded).getBytes();

            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = factory.newDocumentBuilder();
            Document doc = db.parse(new ByteArrayInputStream(bytes));

            return doc.getDocumentElement();
        } catch (Exception e) {
            throw new ParsingException("invalid XML for status detail");
        }
    }

    /**
     * Creates an instance of a <code>StatusDetail</code> object based on the given DOM root node.
     * The node must be a valid StatusDetailType root, or else a <code>ParsingException</code> is
     * thrown.
     * 
     * @param root
     *            the DOM root of the StatusDetailType XML type
     * 
     * @return a new <code>StatusDetail</code> object
     * 
     * @throws ParsingException
     *             if the root node is invalid
     */
    public static StatusDetail getInstance(Node root) throws ParsingException {
        // check that it's really a StatusDetailType root
        if (!root.getNodeName().equals("StatusDetail"))
            throw new ParsingException("not a StatusDetail node");

        return new StatusDetail(root);
    }

    /**
     * Returns the StatusDetailType DOM root node. This may contain within it any type of valid XML
     * data, and it is up to the application writer to handle the data accordingly. One common use
     * of status data is to include <code>Attribute</code>s, which can be created from their root
     * DOM nodes using their <code>getInstance</code> method.
     * 
     * @return the DOM root for the StatusDetailType XML type
     */
    public Node getDetail() {
        return detailRoot;
    }

    /**
     * Returns the text-encoded version of this data, if possible. If the <code>String</code> form
     * constructor was used, this will just be the original text wrapped with the StatusData tag. If
     * the <code>List</code> form constructor was used, it will be the encoded attribute data. If
     * this was created using the <code>getInstance</code> method, then <code>getEncoded</code> will
     * throw an exception.
     * 
     * @return the encoded form of this data
     * 
     * @throws IllegalStateException
     *             if this object was created using the <code>getInstance</code> method
     */
    public String getEncoded() throws IllegalStateException {
        if (detailText == null)
            throw new IllegalStateException("no encoded form available");

        return detailText;
    }

}
