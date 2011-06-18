/*
 * @(#)TestResourceFinderModule.java
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

package com.sun.xacml.test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashSet;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.attr.AnyURIAttribute;
import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.finder.ResourceFinderModule;
import com.sun.xacml.finder.ResourceFinderResult;

/**
 * A <code>ResourceFinderModule</code> used to handle the hierarchical resources in the conformance
 * tests.
 * 
 * @author Seth Proctor
 */
public class TestResourceFinderModule extends ResourceFinderModule {

    /**
     * Default constructor.
     */
    public TestResourceFinderModule() {

    }

    /**
     * Always returns true, since child resource resolution is supported.
     * 
     * @return true
     */
    public boolean isChildSupported() {
        return true;
    }

    /**
     * Always returns true, since descendant resource resolution is supported.
     * 
     * @return true
     */
    public boolean isDescendantSupported() {
        return true;
    }

    /**
     * Finds the children resources associated with the given root, assuming the hierarchy is one
     * that this module handles.
     * 
     * @param root
     *            the root resource in the hierarchy
     * @param context
     *            the evaluation's context
     * 
     * @return the resource hierarchy
     */
    public ResourceFinderResult findChildResources(AttributeValue root, EvaluationCtx context) {
        // make sure we can handle this hierarchy
        if (!requestApplies(root))
            return new ResourceFinderResult();

        // add the root to the set of resolved resources
        HashSet<AttributeValue> set = new HashSet<AttributeValue>();
        set.add(root);

        // add the other resources, which are defined by the conformance tests
        try {
            set.add(new AnyURIAttribute(new URI("urn:root:child1")));
            set.add(new AnyURIAttribute(new URI("urn:root:child2")));
        } catch (URISyntaxException urise) {
            // this will never happen
        }

        return new ResourceFinderResult(set);
    }

    /**
     * Finds the children resources associated with the given root, assuming the hierarchy is one
     * that this module handles.
     * 
     * @param root
     *            the root resource in the hierarchy
     * @param context
     *            the evaluation's context
     * 
     * @return the resource hierarchy
     */
    public ResourceFinderResult findDescendantResources(AttributeValue root, EvaluationCtx context) {
        // make sure we can handle this hierarchy
        if (!requestApplies(root))
            return new ResourceFinderResult();

        // add the root to the set of resolved resources
        HashSet<AttributeValue> set = new HashSet<AttributeValue>();
        set.add(root);

        // add the other resources, which are defined by the conformance tests
        try {
            set.add(new AnyURIAttribute(new URI("urn:root:child1")));
            set.add(new AnyURIAttribute(new URI("urn:root:child1:descendant1")));
            set.add(new AnyURIAttribute(new URI("urn:root:child1:descendant2")));
            set.add(new AnyURIAttribute(new URI("urn:root:child2")));
            set.add(new AnyURIAttribute(new URI("urn:root:child2:descendant1")));
            set.add(new AnyURIAttribute(new URI("urn:root:child2:descendant2")));
        } catch (URISyntaxException urise) {
            // this will never happen
        }

        return new ResourceFinderResult(set);
    }

    /**
     * Private helper method that checks if the given resource is the root of a hierarchy that we
     * know how to handle.
     */
    private boolean requestApplies(AttributeValue root) {
        // make sure the resource-id is a URI
        if (!root.getType().toString().equals(AnyURIAttribute.identifier))
            return false;

        // make sure that the root is urn:root
        if (!((AnyURIAttribute) root).getValue().toString().equals("urn:root"))
            return false;

        return true;
    }

}
