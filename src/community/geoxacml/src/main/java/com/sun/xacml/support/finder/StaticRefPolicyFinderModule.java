/*
 * @(#)StaticRefPolicyFinderModule.java
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

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.ParsingException;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.VersionConstraints;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;

/**
 * This is a simple implementation of <code>PolicyFinderModule</code> that supports retrieval based
 * on reference, and is designed for use with a run-time configuration. Its constructor accepts a
 * <code>List</code> of <code>String</code>s that represent URLs or files, and these are resolved to
 * policies when the module is initialized. Beyond this, there is no modifying or re-loading the
 * policies represented by this class. The policy's identifiers are used for reference resolution.
 * <p>
 * Note that this class is designed to complement <code>StaticPolicyFinderModule</code>. It would be
 * easy to support both kinds of policy retrieval in a single class, but the functionality is
 * instead split between two classes. The reason is that when you define a configuration for your
 * PDP, it's easier to specify the two sets of policies by using two different finder modules.
 * Typically, there aren't many policies that exist in both sets, so loading the sets separately
 * isn't a problem. If this is a concern to you, simply create your own class and merge the two
 * existing classes.
 * <p>
 * This module is provided as an example, but is still fully functional, and should be useful for
 * many simple applications. This is provided in the <code>support</code> package rather than the
 * core codebase because it implements non-standard behavior.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class StaticRefPolicyFinderModule extends PolicyFinderModule {

    // the list of policy URLs passed to the constructor
    private List<String> policyList;

    // the map of policies
    private PolicyCollection policies;

    // the optional schema file
    private File schemaFile = null;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(StaticRefPolicyFinderModule.class
            .getName());

    /**
     * Creates a <code>StaticRefPolicyFinderModule</code> that provides access to the given
     * collection of policies. Any policy that cannot be loaded will be noted in the log, but will
     * not cause an error. The schema file used to validate policies is defined by the property
     * <code>PolicyReader.POLICY_SCHEMA_PROPERTY</code>. If the retrieved property is null, then no
     * schema validation will occur.
     * 
     * @param policyList
     *            a <code>List</code> of <code>String</code>s that represent URLs or files pointing
     *            to XACML policies
     */
    public StaticRefPolicyFinderModule(List<String> policyList) {
        this.policyList = policyList;
        this.policies = new PolicyCollection();

        String schemaName = System.getProperty(PolicyReader.POLICY_SCHEMA_PROPERTY);
        if (schemaName != null)
            schemaFile = new File(schemaName);
    }

    /**
     * Creates a <code>StaticRefPolicyFinderModule</code> that provides access to the given
     * collection of policyList.
     * 
     * @param policyList
     *            a <code>List</code> of <code>String</code>s that represent URLs or files pointing
     *            to XACML policies
     * @param schemaFile
     *            the schema file to validate policies against, or null if schema validation is not
     *            desired
     */
    public StaticRefPolicyFinderModule(List<String> policyList, String schemaFile) {
        this.policyList = policyList;
        this.policies = new PolicyCollection();

        if (schemaFile != null)
            this.schemaFile = new File(schemaFile);
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
     * created. This method is where the policies are actually loaded.
     * 
     * @param finder
     *            the <code>PolicyFinder</code> using this module
     */
    public void init(PolicyFinder finder) {
        // now that we have the PolicyFinder, we can load the policies
        PolicyReader reader = new PolicyReader(finder, logger, schemaFile);

        for (String str : policyList) {
            AbstractPolicy policy = null;

            try {
                try {
                    // first try to load it as a URL
                    URL url = new URL(str);
                    policy = reader.readPolicy(url);
                } catch (MalformedURLException murle) {
                    // assume that this is a filename, and try again
                    policy = reader.readPolicy(new File(str));
                }

                // we loaded the policy, so try putting it in the collection
                if (!policies.addPolicy(policy))
                    if (logger.isLoggable(Level.WARNING))
                        logger.log(Level.WARNING, "tried to load the same "
                                + "policy multiple times: " + str);
            } catch (ParsingException pe) {
                if (logger.isLoggable(Level.WARNING))
                    logger.log(Level.WARNING, "Error reading policy: " + str, pe);
            }
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
        AbstractPolicy policy = policies.getPolicy(idReference.toString(), type, constraints);

        if (policy == null)
            return new PolicyFinderResult();
        else
            return new PolicyFinderResult(policy);
    }

}
