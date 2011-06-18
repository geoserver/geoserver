/*
 * @(#)BasicPolicyFinderModule.java
 *
 * Copyright 2006 Sun Microsystems, Inc. All Rights Reserved.
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

package com.sun.xacml.support.finder;

import java.net.URI;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.EvaluationCtx;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.VersionConstraints;
import com.sun.xacml.combine.PolicyCombiningAlgorithm;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;

/**
 * This is a basic implementation of <code>PolicyFinderModule</code> that accepts already created
 * <code>AbstractPolicy</code>s and supports finding by context and reference. All policies are held
 * forever once added to this module, and cannot be refreshed or removed. New policies may be added
 * at any point. You may optionally specify a combining algorithm to use when more than one
 * applicable policy is found, and then a new PolicySet is wrapped around the policies using this
 * algorithm. If no combining algorithm is provided, then an error is returned if more than one
 * policy matches.
 * <p>
 * This module is provided as an example, but is still fully functional, and should be useful for
 * many simple applications. This is provided in the <code>support</code> package rather than the
 * core codebase because it implements non-standard behavior.
 * 
 * @since 2.0
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class BasicPolicyFinderModule extends PolicyFinderModule {

    // the collections used to handle both kinds of policies
    private PolicyCollection ctxPolicies;

    private PolicyCollection refPolicies;

    // the combining alg, or null if none is used
    // private PolicyCombiningAlgorithm combiningAlg;

    // the policy identifier for any policy sets we dynamically create
    private static final String POLICY_ID = "urn:com:sun:xacml:support:finder:dynamic-policy-set";

    private static URI policyId = null;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(BasicPolicyFinderModule.class.getName());

    static {
        try {
            policyId = new URI(POLICY_ID);
        } catch (Exception e) {
            // this can't actually happen, but just in case...
            if (logger.isLoggable(Level.SEVERE))
                logger.log(Level.SEVERE, "couldn't assign default policy id");
        }
    };

    /**
     * Creates a <code>BasicPolicyFinderModule</code>.
     */
    public BasicPolicyFinderModule() {
        ctxPolicies = new PolicyCollection();
        refPolicies = new PolicyCollection();
    }

    /**
     * Creates a <code>BasicPolicyFinderModule</code> that can combine multiple applicable policies
     * under a single, dynamic PolicySet.
     * 
     * @param combiningAlg
     *            the algorithm to use in a new PolicySet when more than one policy applies
     */
    public BasicPolicyFinderModule(PolicyCombiningAlgorithm combiningAlg) {
        ctxPolicies = new PolicyCollection(combiningAlg, policyId);
        refPolicies = new PolicyCollection(combiningAlg, policyId);
    }

    /**
     * Adds a policy that will be available both by reference and by matching to a context. The
     * policy's identifier is used for finding by reference. If a policy with the same identifier
     * and version is already handled by this module, then the policy is not added.
     * 
     * @param policy
     *            the policy to add
     * 
     * @return true if the policy was added, false otherwise
     */
    public synchronized boolean addPolicy(AbstractPolicy policy) {
        if (ctxPolicies.addPolicy(policy))
            return refPolicies.addPolicy(policy);
        else
            return false;
    }

    /**
     * Adds a policy that will be available only by matching to a context. If a policy with the same
     * identifier and version is already handled by this module, then the policy is not added.
     * 
     * @param policy
     *            the policy to add
     * 
     * @return true if the policy was added, false otherwise
     */
    public synchronized boolean addPolicyNoRef(AbstractPolicy policy) {
        return ctxPolicies.addPolicy(policy);
    }

    /**
     * Adds a policy that will be available only by reference. The policy's identifier is used for
     * finding by reference. If a policy with the same identifier and version is already handled by
     * this module, then the policy is not added.
     * 
     * @param policy
     *            the policy to add
     * 
     * @return true if the policy was added, false otherwise
     */
    public synchronized boolean addPolicyOnlyRef(AbstractPolicy policy) {
        return refPolicies.addPolicy(policy);
    }

    /**
     * Always returns <code>true</code> since this module does support finding policies based on
     * context matching.
     * 
     * @return true
     */
    public boolean isRequestSupported() {
        return true;
    }

    /**
     * Always returns <code>true</code> since this module does support finding policies based on
     * reference.
     * 
     * @return true
     */
    public boolean isIdReferenceSupported() {
        return true;
    }

    /**
     * Initialize this module. Typically this is called by <code>PolicyFinder</code> when a PDP is
     * created.
     * 
     * @param finder
     *            the <code>PolicyFinder</code> using this module
     */
    public void init(PolicyFinder finder) {
        // we don't need to do anything here
    }

    /**
     * Finds a policy based on a request's context. If more than one policy matches, then this
     * either returns an error or a new policy wrapping the multiple policies (depending on which
     * constructor was used to construct this instance).
     * 
     * @param context
     *            the representation of the request data
     * 
     * @return the result of trying to find an applicable policy
     */
    public PolicyFinderResult findPolicy(EvaluationCtx context) {
        try {
            AbstractPolicy policy = ctxPolicies.getPolicy(context);

            if (policy == null)
                return new PolicyFinderResult();
            else
                return new PolicyFinderResult(policy);
        } catch (TopLevelPolicyException tlpe) {
            return new PolicyFinderResult(tlpe.getStatus());
        }
    }

    /**
     * Attempts to find a policy by reference, based on the provided parameters.
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
        AbstractPolicy policy = refPolicies.getPolicy(idReference.toString(), type, constraints);

        if (policy == null)
            return new PolicyFinderResult();
        else
            return new PolicyFinderResult(policy);
    }

}
