/*
 * @(#)SelectorModule.java
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

package com.sun.xacml.finder.impl;

import java.net.URI;
import java.util.ArrayList;

import org.apache.xpath.XPathAPI;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.UnknownIdentifierException;
import com.sun.xacml.attr.AttributeFactory;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.BagAttribute;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.ctx.Status;
import com.sun.xacml.finder.AttributeFinderModule;

/**
 * This module implements the basic behavior of the AttributeSelectorType, looking for attribute
 * values in the physical request document using the given XPath expression. This is implemented as
 * a separate module (instead of being implemented directly in <code>AttributeSelector</code> so
 * that programmers can remove this functionality if they want (it's optional in the spec), so they
 * can replace this code with more efficient, specific code as needed, and so they can easily swap
 * in different XPath libraries.
 * <p>
 * Note that if no matches are found, this module will return an empty bag (unless some error
 * occurred). The <code>AttributeSelector</code> is still deciding what to return to the policy
 * based on the MustBePresent attribute.
 * <p>
 * This module uses the Xalan XPath implementation, and supports only version 1.0 of XPath. It is a
 * fully functional, correct implementation of XACML's AttributeSelector functionality, but is not
 * designed for environments that make significant use of XPath queries. Developers for any such
 * environment should consider implementing their own module.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class SelectorModule extends AttributeFinderModule {

    /**
     * Returns true since this module supports retrieving attributes based on the data provided in
     * an AttributeSelectorType.
     * 
     * @return true
     */
    public boolean isSelectorSupported() {
        return true;
    }

    /**
     * Private helper to create a new processing error status result
     */
    private EvaluationResult createProcessingError(String msg) {
        ArrayList<String> code = new ArrayList<String>();
        code.add(Status.STATUS_PROCESSING_ERROR);
        return new EvaluationResult(new Status(code, msg));
    }

    /**
     * Tries to find attribute values based on the given selector data. The result, if successful,
     * always contains a <code>BagAttribute</code>, even if only one value was found. If no values
     * were found, but no other error occurred, an empty bag is returned.
     * 
     * @param path
     *            the XPath expression to search against
     * @param namespaceNode
     *            the DOM node defining namespace mappings to use, or null if mappings come from the
     *            context root
     * @param type
     *            the datatype of the attributes to find
     * @param context
     *            the representation of the request data
     * @param xpathVersion
     *            the XPath version to use
     * 
     * @return the result of attribute retrieval, which will be a bag of attributes or an error
     */
    public EvaluationResult findAttribute(String path, Node namespaceNode, URI type,
            EvaluationCtx context, String xpathVersion) {
        // we only support 1.0
        if (!xpathVersion.equals(PolicyMetaData.XPATH_1_0_IDENTIFIER))
            return new EvaluationResult(BagAttribute.createEmptyBag(type));

        // get the DOM root of the request document
        Node root = context.getRequestRoot();

        // if we were provided with a non-null namespace node, then use it
        // to resolve namespaces, otherwise use the context root node
        Node nsNode = (namespaceNode != null) ? namespaceNode : root;

        // setup the root path (pre-pended to the context path), which...
        String rootPath = "";

        // ...only has content if the context path is relative
        if (path.charAt(0) != '/') {
            String rootName = root.getLocalName();

            // see if the request root is in a namespace
            String namespace = root.getNamespaceURI();

            if (namespace == null) {
                // no namespacing, so we're done
                rootPath = "/" + rootName + "/";
            } else {
                // namespaces are used, so we need to lookup the correct
                // prefix to use in the search string
                NamedNodeMap nmap = namespaceNode.getAttributes();
                rootPath = null;

                for (int i = 0; i < nmap.getLength(); i++) {
                    Node n = nmap.item(i);
                    if (n.getNodeValue().equals(namespace)) {
                        // we found the matching namespace, so get the prefix
                        // and then break out
                        String name = n.getNodeName();
                        int pos = name.indexOf(':');

                        if (pos == -1) {
                            // the namespace was the default namespace
                            rootPath = "/";
                        } else {
                            // we found a prefixed namespace
                            rootPath = "/" + name.substring(pos + 1);
                        }

                        // finish off the string
                        rootPath += ":" + rootName + "/";

                        break;
                    }
                }

                // if the rootPath is still null, then we don't have any
                // definitions for the namespace
                if (rootPath == null)
                    return createProcessingError("Failed to map a namespace"
                            + " in an XPath expression");
            }
        }

        // now do the query, pre-pending the root path to the context path
        NodeList matches = null;
        try {
            // NOTE: see comments in XALAN docs about why this is slow
            matches = XPathAPI.selectNodeList(root, rootPath + path, nsNode);
        } catch (Exception e) {
            // in the case of any exception, we need to return an error
            return createProcessingError("error in XPath: " + e.getMessage());
        }

        if (matches.getLength() == 0) {
            // we didn't find anything, so we return an empty bag
            return new EvaluationResult(BagAttribute.createEmptyBag(type));
        }

        // there was at least one match, so try to generate the values
        try {
            ArrayList<AttributeValue> list = new ArrayList<AttributeValue>();
            AttributeFactory attrFactory = AttributeFactory.getInstance();

            for (int i = 0; i < matches.getLength(); i++) {
                String text = null;
                Node node = matches.item(i);
                short nodeType = node.getNodeType();

                // see if this is straight text, or a node with data under
                // it and then get the values accordingly
                if ((nodeType == Node.CDATA_SECTION_NODE) || (nodeType == Node.COMMENT_NODE)
                        || (nodeType == Node.TEXT_NODE) || (nodeType == Node.ATTRIBUTE_NODE)) {
                    // there is no child to this node
                    text = node.getNodeValue();
                } else {
                    // the data is in a child node
                    text = node.getFirstChild().getNodeValue();
                }

                list.add(attrFactory.createValue(type, text));
            }

            return new EvaluationResult(new BagAttribute(type, list));
        } catch (ParsingException pe) {
            return createProcessingError(pe.getMessage());
        } catch (UnknownIdentifierException uie) {
            return createProcessingError("unknown attribute type: " + type);
        }
    }

}
