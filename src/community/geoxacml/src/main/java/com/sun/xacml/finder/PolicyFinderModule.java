/*
 * @(#)PolicyFinderModule.java
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
 */

package com.sun.xacml.finder;

import java.net.URI;

import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.VersionConstraints;

/**
 * This is the abstract class that all <code>PolicyFinder</code> modules extend. All methods have
 * default values to represent that the given feature isn't supported by this module, so module
 * writers needs only implement the methods for the features they're supporting.
 * 
 * @since 1.0
 * @author Seth Proctor
 */
public abstract class PolicyFinderModule {

    /**
     * Returns this module's identifier. A module does not need to provide a unique identifier, but
     * it is a good idea, especially in support of management software. Common identifiers would be
     * the full package and class name (the default if this method isn't overridden), just the class
     * name, or some other well-known string that identifies this class.
     * 
     * @return this module's identifier
     */
    public String getIdentifier() {
        return getClass().getName();
    }

    /**
     * Returns true if the module supports finding policies based on a request (ie, target
     * matching). By default this method returns false.
     * 
     * @return true if request retrieval is supported
     */
    public boolean isRequestSupported() {
        return false;
    }

    /**
     * Returns true if the module supports finding policies based on an id reference (in a
     * PolicySet). By default this method returns false.
     * 
     * @return true if idReference retrieval is supported
     */
    public boolean isIdReferenceSupported() {
        return false;
    }

    /**
     * Initializes this module for use by the given finder. Typically this is called when a
     * <code>PDP</code> is initialized with a <code>PDPConfig</code> containing the given
     * <code>PolicyFinder</code>. Because <code>PolicyFinderModule</code>s usually need to parse
     * policies, and this requires knowing their <code>PolicyFinder<code>,
     * parsing is usually done at or after this point in the lifetime
     * of this module. This might also be a good time to reset any internal
     * caches or temporary data. Note that this method may be called more
     * than once in the lifetime of a module.
     * 
     * @param finder
     *            the <code>PolicyFinder</code> using this module
     */
    public abstract void init(PolicyFinder finder);

    /**
     * This is an experimental method that asks the module to invalidate any cache values it may
     * contain. This is not used by any of the core processing code, but it may be used by
     * management software that wants to have some control over these modules. Since a module is
     * free to decide how or if it caches values, and whether it is capable of updating values once
     * in a cache, a module is free to intrepret this message in any way it sees fit (including
     * igoring the message). It is preferable, however, for a module to make every effort to clear
     * any dynamically cached values it contains.
     * <p>
     * This method has been introduced to see what people think of this functionality, and how they
     * would like to use it. It may be removed in future versions, or it may be changed to a more
     * general message-passing system (if other useful messages are identified).
     * 
     * @since 1.2
     */
    public void invalidateCache() {

    }

    /**
     * Tries to find one and only one matching policy given the request represented by the context
     * data. If more than one policy is found, this is an error and must be reported as such. If no
     * policies are found, then an empty result must be returned. By default this method returns an
     * empty result. This method should never return null.
     * 
     * @param context
     *            the representation of the request
     * 
     * @return the result of looking for a matching policy
     */
    public PolicyFinderResult findPolicy(EvaluationCtx context) {
        return new PolicyFinderResult();
    }

    /**
     * Tries to find one and only one matching policy given the idReference If more than one policy
     * is found, this is an error and must be reported as such. If no policies are found, then an
     * empty result must be returned. By default this method returns an empty result. This method
     * should never return null.
     * 
     * @param idReference
     *            an identifier specifying some policy
     * @param type
     *            type of reference (policy or policySet) as identified by the fields in
     *            <code>PolicyReference</code>
     * @param constraints
     *            any optional constraints on the version of the referenced policy (this will never
     *            be null, but it may impose no constraints, and in fact will never impose
     *            constraints when used from a pre-2.0 XACML policy)
     * @param parentMetaData
     *            the meta-data from the parent policy, which provides XACML version, factories,
     *            etc.
     * 
     * @return the result of looking for a matching policy
     */
    public PolicyFinderResult findPolicy(URI idReference, int type, VersionConstraints constraints,
            PolicyMetaData parentMetaData) {
        return new PolicyFinderResult();
    }

}
