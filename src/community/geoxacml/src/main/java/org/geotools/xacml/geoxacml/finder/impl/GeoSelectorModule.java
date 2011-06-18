/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.xacml.geoxacml.finder.impl;

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
 * @author Christian Mueller
 * 
 *         Modluel to handle XPATH constructs (XACML AttribueSelector)
 * 
 */
public class GeoSelectorModule extends AttributeFinderModule {

    public boolean isSelectorSupported() {
        return true;
    }

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

                AttributeValue attrValue = null;

                if ((nodeType == Node.CDATA_SECTION_NODE) || (nodeType == Node.COMMENT_NODE)
                        || (nodeType == Node.TEXT_NODE) || (nodeType == Node.ATTRIBUTE_NODE)) {
                    // there is no child to this node
                    text = node.getNodeValue();
                    attrValue = attrFactory.createValue(type, text);
                } else if (nodeType == Node.DOCUMENT_NODE || nodeType == Node.ELEMENT_NODE) {
                    attrValue = attrFactory.createValue(node, type);
                } else {
                    // the data is in a child node
                    text = node.getFirstChild().getNodeValue();
                    attrValue = attrFactory.createValue(type, text);
                }

                list.add(attrValue);
            }

            return new EvaluationResult(new BagAttribute(type, list));
        } catch (ParsingException pe) {
            return createProcessingError(pe.getMessage());
        } catch (UnknownIdentifierException uie) {
            return createProcessingError("unknown attribute type: " + type);
        }
    }

}
