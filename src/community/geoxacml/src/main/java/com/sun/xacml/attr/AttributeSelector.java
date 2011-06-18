/*
 * @(#)AttributeSelector.java
 *
 * Copyright 2003-2005 Sun Microsystems, Inc. All Rights Reserved.
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
 */

package com.sun.xacml.attr;

import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.Indenter;
import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.cond.Evaluatable;
import com.sun.xacml.cond.EvaluationResult;
import com.sun.xacml.cond.Expression;
import com.sun.xacml.ctx.Status;

/**
 * Supports the standard selector functionality in XACML, which uses XPath expressions to resolve
 * values from the Request or elsewhere. All selector queries are done by
 * <code>AttributeFinderModule</code>s so that it's easy to plugin different XPath implementations.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class AttributeSelector implements Evaluatable {

    // the data type returned by this selector
    private URI type;

    // the XPath to search
    private String contextPath;

    // must resolution find something
    private boolean mustBePresent;

    // the xpath version we've been told to use
    private String xpathVersion;

    // the policy root, where we get namespace mapping details
    private Node policyRoot;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(AttributeSelector.class.getName());

    /**
     * Creates a new <code>AttributeSelector</code> with no policy root.
     * 
     * @param type
     *            the data type of the attribute values this selector looks for
     * @param contextPath
     *            the XPath to query
     * @param mustBePresent
     *            must resolution find a match
     * @param xpathVersion
     *            the XPath version to use, which must be a valid XPath version string (the
     *            identifier for XPath 1.0 is provided in <code>PolicyMetaData</code>)
     */
    public AttributeSelector(URI type, String contextPath, boolean mustBePresent,
            String xpathVersion) {
        this(type, contextPath, null, mustBePresent, xpathVersion);
    }

    /**
     * Creates a new <code>AttributeSelector</code>.
     * 
     * @param type
     *            the data type of the attribute values this selector looks for
     * @param contextPath
     *            the XPath to query
     * @param policyRoot
     *            the root DOM Element for the policy containing this selector, which defines
     *            namespace mappings
     * @param mustBePresent
     *            must resolution find a match
     * @param xpathVersion
     *            the XPath version to use, which must be a valid XPath version string (the
     *            identifier for XPath 1.0 is provided in <code>PolicyMetaData</code>)
     */
    public AttributeSelector(URI type, String contextPath, Node policyRoot, boolean mustBePresent,
            String xpathVersion) {
        this.type = type;
        this.contextPath = contextPath;
        this.mustBePresent = mustBePresent;
        this.xpathVersion = xpathVersion;
        this.policyRoot = policyRoot;
    }

    /**
     * Creates a new <code>AttributeSelector</code> based on the DOM root of the XML type. Note that
     * as of XACML 1.1 the XPathVersion element is required in any policy that uses a selector, so
     * if the <code>xpathVersion</code> string is null, then this will throw an exception.
     * 
     * @deprecated As of 2.0 you should avoid using this method and should instead use the version
     *             that takes a <code>PolicyMetaData</code> instance. This method will only work for
     *             XACML 1.x policies.
     * 
     * @param root
     *            the root of the DOM tree for the XML AttributeSelectorType XML type
     * @param xpathVersion
     *            the XPath version to use, or null if this is unspecified (ie, not supplied in the
     *            defaults section of the policy)
     * 
     * @return an <code>AttributeSelector</code>
     * 
     * @throws ParsingException
     *             if the AttributeSelectorType was invalid
     */
    public static AttributeSelector getInstance(Node root, String xpathVersion)
            throws ParsingException {
        return getInstance(root, new PolicyMetaData(PolicyMetaData.XACML_1_0_IDENTIFIER,
                xpathVersion));
    }

    /**
     * Creates a new <code>AttributeSelector</code> based on the DOM root of the XML type. Note that
     * as of XACML 1.1 the XPathVersion element is required in any policy that uses a selector, so
     * if the <code>xpathVersion</code> string is null, then this will throw an exception.
     * 
     * @param root
     *            the root of the DOM tree for the XML AttributeSelectorType XML type
     * @param metaData
     *            the meta-data associated with the containing policy
     * 
     * @return an <code>AttributeSelector</code>
     * 
     * @throws ParsingException
     *             if the AttributeSelectorType was invalid
     */
    public static AttributeSelector getInstance(Node root, PolicyMetaData metaData)
            throws ParsingException {
        URI type = null;
        String contextPath = null;
        boolean mustBePresent = false;
        String xpathVersion = metaData.getXPathIdentifier();

        // make sure we were given an xpath version
        if (xpathVersion == null)
            throw new ParsingException("An XPathVersion is required for "
                    + "any policies that use selectors");

        NamedNodeMap attrs = root.getAttributes();

        try {
            // there's always a DataType attribute
            type = new URI(attrs.getNamedItem("DataType").getNodeValue());
        } catch (Exception e) {
            throw new ParsingException("Error parsing required DataType "
                    + "attribute in AttributeSelector", e);
        }

        try {
            // there's always a RequestPath
            contextPath = attrs.getNamedItem("RequestContextPath").getNodeValue();
        } catch (Exception e) {
            throw new ParsingException("Error parsing required "
                    + "RequestContextPath attribute in " + "AttributeSelector", e);
        }

        try {
            // there may optionally be a MustBePresent
            Node node = attrs.getNamedItem("MustBePresent");
            if (node != null)
                if (node.getNodeValue().equals("true"))
                    mustBePresent = true;
        } catch (Exception e) {
            // this shouldn't happen, since we check the cases, but still...
            throw new ParsingException("Error parsing optional attributes "
                    + "in AttributeSelector", e);
        }

        // as of 1.2 we need the root element of the policy so we can get
        // the namespace mapping, but in order to leave the APIs unchanged,
        // we'll walk up the tree to find the root rather than pass this
        // element around through all the code
        Node policyRoot = null;
        Node node = root.getParentNode();

        while ((node != null) && (node.getNodeType() == Node.ELEMENT_NODE)) {
            policyRoot = node;
            node = node.getParentNode();
        }

        // create the new selector
        return new AttributeSelector(type, contextPath, policyRoot, mustBePresent, xpathVersion);
    }

    /**
     * Returns the data type of the attribute values that this selector will resolve
     * 
     * @return the data type of the values found by this selector
     */
    public URI getType() {
        return type;
    }

    /**
     * Returns the XPath query used to resolve attribute values.
     * 
     * @return the XPath query
     */
    public String getContextPath() {
        return contextPath;
    }

    /**
     * Returns whether or not a value is required to be resolved by this selector.
     * 
     * @return true if a value is required, false otherwise
     */
    public boolean mustBePresent() {
        return mustBePresent;
    }

    /**
     * Always returns true, since a selector always returns a bag of attribute values.
     * 
     * @return true
     */
    public boolean returnsBag() {
        return true;
    }

    /**
     * Always returns true, since a selector always returns a bag of attribute values.
     * 
     * @deprecated As of 2.0, you should use the <code>returnsBag</code> method from the
     *             super-interface <code>Expression</code>.
     * 
     * @return true
     */
    public boolean evaluatesToBag() {
        return true;
    }

    /**
     * Always returns an empty list since selectors never have children.
     * 
     * @return an empty <code>List</code>
     */
    public List<Expression> getChildren() {
        return Collections.emptyList();
    }

    /**
     * Returns the XPath version this selector is supposed to use. This is typically provided by the
     * defaults section of the policy containing this selector.
     * 
     * @return the XPath version
     */
    public String getXPathVersion() {
        return xpathVersion;
    }

    /**
     * Invokes the <code>AttributeFinder</code> used by the given <code>EvaluationCtx</code> to try
     * to resolve an attribute value. If the selector is defined with MustBePresent as true, then
     * failure to find a matching value will result in Indeterminate, otherwise it will result in an
     * empty bag. To support the basic selector functionality defined in the XACML specification,
     * use a finder that has only the <code>SelectorModule</code> as a module that supports selector
     * finding.
     * 
     * @param context
     *            representation of the request to search
     * 
     * @return a result containing a bag either empty because no values were found or containing at
     *         least one value, or status associated with an Indeterminate result
     */
    public EvaluationResult evaluate(EvaluationCtx context) {
        // query the context
        EvaluationResult result = context.getAttribute(contextPath, policyRoot, type, xpathVersion);

        // see if we got anything
        if (!result.indeterminate()) {
            BagAttribute bag = (BagAttribute) (result.getAttributeValue());

            // see if it's an empty bag
            if (bag.isEmpty()) {
                // see if this is an error or not
                if (mustBePresent) {
                    // this is an error
                    if (logger.isLoggable(Level.INFO))
                        logger.info("AttributeSelector failed to resolve a "
                                + "value for a required attribute: " + contextPath);

                    ArrayList<String> code = new ArrayList<String>();
                    code.add(Status.STATUS_MISSING_ATTRIBUTE);
                    String message = "couldn't resolve XPath expression " + contextPath
                            + " for type " + type.toString();
                    return new EvaluationResult(new Status(code, message));
                } else {
                    // return the empty bag
                    return result;
                }
            } else {
                // return the values
                return result;
            }
        } else {
            // return the error
            return result;
        }
    }

    /**
     * Encodes this selector into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with no indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     */
    public void encode(OutputStream output) {
        encode(output, new Indenter(0));
    }

    /**
     * Encodes this selector into its XML representation and writes this encoding to the given
     * <code>OutputStream</code> with indentation.
     * 
     * @param output
     *            a stream into which the XML-encoded data is written
     * @param indenter
     *            an object that creates indentation strings
     */
    public void encode(OutputStream output, Indenter indenter) {
        PrintStream out = new PrintStream(output);
        String indent = indenter.makeString();

        String tag = "<AttributeSelector RequestContextPath=\"" + contextPath + "\" DataType=\""
                + type.toString() + "\"";

        if (mustBePresent)
            tag += " MustBePresent=\"true\"";

        tag += "/>";

        out.println(indent + tag);
    }

}
