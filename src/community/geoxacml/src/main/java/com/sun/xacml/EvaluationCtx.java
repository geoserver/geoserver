/*
 * @(#)EvaluationCtx.java
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
 */

package com.sun.xacml;

import java.net.URI;

import org.w3c.dom.Node;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.attr.DateAttribute;
import com.sun.xacml.attr.DateTimeAttribute;
import com.sun.xacml.attr.TimeAttribute;
import com.sun.xacml.cond.EvaluationResult;

/**
 * Manages the context of a single policy evaluation. Typically, an instance is instantiated
 * whenever the PDP gets a request and needs to perform an evaluation as a result. The
 * <code>BasicEvaluationCtx</code> class provides a basic implementation that is used by default.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public interface EvaluationCtx {

    /**
     * The standard URI for listing a resource's id
     */
    public static final String RESOURCE_ID = "urn:oasis:names:tc:xacml:1.0:resource:resource-id";

    /**
     * The standard URI for listing a resource's scope
     */
    public static final String RESOURCE_SCOPE = "urn:oasis:names:tc:xacml:1.0:resource:scope";

    /**
     * Resource scope of Immediate (only the given resource)
     */
    public static final int SCOPE_IMMEDIATE = 0;

    /**
     * Resource scope of Children (the given resource and its direct children)
     */
    public static final int SCOPE_CHILDREN = 1;

    /**
     * Resource scope of Descendants (the given resource and all descendants at any depth or
     * distance)
     */
    public static final int SCOPE_DESCENDANTS = 2;

    /**
     * Returns the DOM root of the original RequestType XML document, if this context is backed by
     * an XACML Request. If this context is not backed by an XML representation, then an exception
     * is thrown.
     * 
     * @return the DOM root node
     * 
     * @throws UnsupportedOperationException
     *             if the context is not backed by an XML representation
     */
    public Node getRequestRoot();

    /**
     * Returns the resource scope, which will be one of the three fields denoting Immediate,
     * Children, or Descendants.
     * 
     * @return the scope of the resource
     */
    public int getScope();

    /**
     * Returns the identifier for the resource being requested.
     * 
     * @return the resource
     */
    public AttributeValue getResourceId();

    /**
     * Changes the value of the resource-id attribute in this context. This is useful when you have
     * multiple resources (ie, a scope other than IMMEDIATE), and you need to keep changing only the
     * resource-id to evaluate the different effective requests.
     * 
     * @param resourceId
     *            the new resource-id value
     */
    public void setResourceId(AttributeValue resourceId);

    /**
     * Returns the value for the current time as known by the PDP (if this value was also supplied
     * in the Request, this will generally be a different value). Details of caching or
     * location-based resolution are left to the underlying implementation.
     * 
     * @return the current time
     */
    public TimeAttribute getCurrentTime();

    /**
     * Returns the value for the current date as known by the PDP (if this value was also supplied
     * in the Request, this will generally be a different value). Details of caching or
     * location-based resolution are left to the underlying implementation.
     * 
     * @return the current date
     */
    public DateAttribute getCurrentDate();

    /**
     * Returns the value for the current dateTime as known by the PDP (if this value was also
     * supplied in the Request, this will generally be a different value). Details of caching or
     * location-based resolution are left to the underlying implementation.
     * 
     * @return the current date
     */
    public DateTimeAttribute getCurrentDateTime();

    /**
     * Returns available subject attribute value(s) ignoring the issuer.
     * 
     * @param type
     *            the type of the attribute value(s) to find
     * @param id
     *            the id of the attribute value(s) to find
     * @param category
     *            the category the attribute value(s) must be in
     * 
     * @return a result containing a bag either empty because no values were found or containing at
     *         least one value, or status associated with an Indeterminate result
     */
    public EvaluationResult getSubjectAttribute(URI type, URI id, URI category);

    /**
     * Returns available subject attribute value(s).
     * 
     * @param type
     *            the type of the attribute value(s) to find
     * @param id
     *            the id of the attribute value(s) to find
     * @param issuer
     *            the issuer of the attribute value(s) to find or null
     * @param category
     *            the category the attribute value(s) must be in
     * 
     * @return a result containing a bag either empty because no values were found or containing at
     *         least one value, or status associated with an Indeterminate result
     */
    public EvaluationResult getSubjectAttribute(URI type, URI id, URI issuer, URI category);

    /**
     * Returns available resource attribute value(s).
     * 
     * @param type
     *            the type of the attribute value(s) to find
     * @param id
     *            the id of the attribute value(s) to find
     * @param issuer
     *            the issuer of the attribute value(s) to find or null
     * 
     * @return a result containing a bag either empty because no values were found or containing at
     *         least one value, or status associated with an Indeterminate result
     */
    public EvaluationResult getResourceAttribute(URI type, URI id, URI issuer);

    /**
     * Returns available action attribute value(s).
     * 
     * @param type
     *            the type of the attribute value(s) to find
     * @param id
     *            the id of the attribute value(s) to find
     * @param issuer
     *            the issuer of the attribute value(s) to find or null
     * 
     * @return a result containing a bag either empty because no values were found or containing at
     *         least one value, or status associated with an Indeterminate result
     */
    public EvaluationResult getActionAttribute(URI type, URI id, URI issuer);

    /**
     * Returns available environment attribute value(s).
     * <p>
     * Note that if you want to resolve the correct current date, time, or dateTime as seen from an
     * evaluation point of view, you should use this method and supply the corresponding identifier.
     * 
     * @param type
     *            the type of the attribute value(s) to find
     * @param id
     *            the id of the attribute value(s) to find
     * @param issuer
     *            the issuer of the attribute value(s) to find or null
     * 
     * @return a result containing a bag either empty because no values were found or containing at
     *         least one value, or status associated with an Indeterminate result
     */
    public EvaluationResult getEnvironmentAttribute(URI type, URI id, URI issuer);

    /**
     * Returns the attribute value(s) retrieved using the given XPath expression.
     * 
     * @param contextPath
     *            the XPath expression to search
     * @param namespaceNode
     *            the DOM node defining namespace mappings to use, or null if mappings come from the
     *            context root
     * @param type
     *            the type of the attribute value(s) to find
     * @param xpathVersion
     *            the version of XPath to use
     * 
     * @return a result containing a bag either empty because no values were found or containing at
     *         least one value, or status associated with an Indeterminate result
     */
    public EvaluationResult getAttribute(String contextPath, Node namespaceNode, URI type,
            String xpathVersion);

}
