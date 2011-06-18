/*
 * @(#)ResourceFinderResult.java
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

package com.sun.xacml.finder;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.sun.xacml.attr.AttributeValue;
import com.sun.xacml.ctx.Status;

/**
 * This is used to return Resource Ids from the ResourceFinder. Unlike the PolicyFinder, this never
 * returns an empty set, since it will always contain at least the original parent resource. This
 * class will provide two sets of identifiers: those that were successfully resolved and those that
 * had an error.
 * 
 * @since 1.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class ResourceFinderResult {

    // the set of resource identifiers
    private Set<AttributeValue> resources;

    // the map of failed identifiers to their failure status data
    private Map<AttributeValue, Status> failures;

    // a flag specifying whether or not result contains resource listings
    private boolean empty;

    /**
     * Creates an empty result.
     */
    public ResourceFinderResult() {
        resources = Collections.unmodifiableSet(new HashSet<AttributeValue>());
        failures = Collections.unmodifiableMap(new HashMap<AttributeValue, Status>());
        empty = true;
    }

    /**
     * Creates a result containing the given <code>Set</code> of resource identifiers. The
     * <code>Set</code>must not be null. The new <code>ResourceFinderResult</code> represents a
     * resource retrieval that encountered no errors.
     * 
     * @param resources
     *            a non-null <code>Set</code> of <code>AttributeValue</code>s
     */
    public ResourceFinderResult(Set<AttributeValue> resources) {
        this(resources, new HashMap<AttributeValue, Status>());
    }

    /**
     * Creates a result containing only Resource Ids that caused errors. The <code>Map</code> must
     * not be null. The keys in the <code>Map</code> are <code>AttributeValue</code>s identifying
     * the resources that could not be resolved, and they map to a <code>Status</code> object
     * explaining the error. The new <code>ResourceFinderResult</code> represents a resource
     * retrieval that did not succeed in finding any resource identifiers.
     * 
     * @param failures
     *            a non-null <code>Map</code> mapping failed <code>AttributeValue</code> identifiers
     *            to their <code>Status</code>
     */
    public ResourceFinderResult(HashMap<AttributeValue, Status> failures) {
        this(new HashSet<AttributeValue>(), failures);
    }

    /**
     * Creates a new result containing both successfully resolved Resource Ids and resources that
     * caused errors.
     * 
     * @param resources
     *            a non-null <code>Set</code> of <code>AttributeValue</code>s
     * @param failures
     *            a non-null <code>Map</code> mapping failed <code>AttributeValue</code> identifiers
     *            to their <code>Status</code>
     */
    public ResourceFinderResult(Set<AttributeValue> resources, Map<AttributeValue, Status> failures) {
        this.resources = Collections.unmodifiableSet(new HashSet<AttributeValue>(resources));
        this.failures = Collections.unmodifiableMap(new HashMap<AttributeValue, Status>(failures));
        empty = false;
    }

    /**
     * Returns whether or not this result contains any Resource Id listings. This will return false
     * if either the set of successfully resolved resource identifiers or the map of failed
     * resources is not empty.
     * 
     * @return false if this result names any resources, otherwise true
     */
    public boolean isEmpty() {
        return empty;
    }

    /**
     * Returns the <code>Set</code> of successfully resolved Resource Id <code>AttributeValue</code>
     * s, which will be empty if no resources were successfully resolved.
     * 
     * @return a <code>Set</code> of <code>AttributeValue</code>s
     */
    public Set<AttributeValue> getResources() {
        return resources;
    }

    /**
     * Returns the <code>Map</code> of Resource Ids that caused an error on resolution, which will
     * be empty if no resources caused any error.
     * 
     * @return a <code>Map</code> of <code>AttributeValue</code>s to <code>Status</code>
     */
    public Map<AttributeValue, Status> getFailures() {
        return failures;
    }

}
