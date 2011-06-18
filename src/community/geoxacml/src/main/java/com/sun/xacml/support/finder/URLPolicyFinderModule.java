/*
 * @(#)URLPolicyFinderModule.java
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
import java.util.logging.Logger;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.ParsingException;
import com.sun.xacml.Policy;
import com.sun.xacml.PolicyMetaData;
import com.sun.xacml.PolicyReference;
import com.sun.xacml.PolicySet;
import com.sun.xacml.VersionConstraints;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.PolicyFinderResult;

/**
 * This module supports references made with resolvable URLs (eg, http or file pointers). No
 * policies are cached. Instead, all policy references are resolved in real-time. To make this
 * module as generally applicable as possible, no errors are ever returned when attempting to
 * resolve a policy. This means that if a resolved policy is invalid, a server cannot be contacted,
 * etc., this module simply reports that it cannot provide a policy. If you need to report errors,
 * or support any caching, you have to write your own implementation.
 * <p>
 * This module is provided as an example, but is still fully functional, and should be useful for
 * many simple applications. This is provided in the <code>support</code> package rather than the
 * core codebase because it implements non-standard behavior.
 * 
 * @since 2.0
 * @author Seth Proctor
 */
public class URLPolicyFinderModule extends PolicyFinderModule {

    // the optional schema file for validating policies
    private File schemaFile;

    // the reader used to load all policies
    private PolicyReader reader;

    // the logger we'll use for all messages
    private static final Logger logger = Logger.getLogger(URLPolicyFinderModule.class.getName());

    /**
     * Creates a <code>URLPolicyFinderModule</code>. The schema file used to validate policies is
     * specified by the property <code>PolicyReader.POLICY_SCHEMA_PROPERTY</code>. If the retrieved
     * property is null, then no schema validation will occur.
     */
    public URLPolicyFinderModule() {
        String schemaName = System.getProperty(PolicyReader.POLICY_SCHEMA_PROPERTY);

        if (schemaName != null)
            schemaFile = new File(schemaName);
    }

    /**
     * Creates a <code>URLPolicyFinderModule</code> that may do schema validation of policies.
     * 
     * @param schemaFile
     *            the schema file to use for validation, or null if validation isn't desired
     */
    public URLPolicyFinderModule(String schemaFile) {
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
     * created.
     * 
     * @param finder
     *            the <code>PolicyFinder</code> using this module
     */
    public void init(PolicyFinder finder) {
        reader = new PolicyReader(finder, logger, schemaFile);
    }

    /**
     * Attempts to find a policy by reference, based on the provided parameters. Specifically, this
     * module will try to treat the reference as a URL, and resolve that URL directly. If the
     * reference is not a valid URL, cannot be resolved, or does not resolve to an XACML policy,
     * then no matching policy is returned. This method never returns an error.
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
        // see if the URI is in fact a URL
        URL url = null;
        try {
            url = new URL(idReference.toString());
        } catch (MalformedURLException murle) {
            // it's not a URL, so we can't handle this reference
            return new PolicyFinderResult();
        }

        // try resolving the URL
        AbstractPolicy policy = null;
        try {
            policy = reader.readPolicy(url);
        } catch (ParsingException pe) {
            // An error loading the policy could be many things (the URL
            // doesn't actually resolve a policy, the server is down, the
            // policy is invalid, etc.). This could be interpreted as an
            // error case, or simply as a case where no applicable policy
            // is available (as is done when we pre-load policies). This
            // module chooses the latter interpretation.
            return new PolicyFinderResult();
        }

        // check that we got the right kind of policy...if we didn't, then
        // we can't handle the reference
        if (type == PolicyReference.POLICY_REFERENCE) {
            if (!(policy instanceof Policy))
                return new PolicyFinderResult();
        } else {
            if (!(policy instanceof PolicySet))
                return new PolicyFinderResult();
        }

        // finally, check that the constraints match ... note that in a more
        // powerful module, you could actually have used the constraints to
        // construct a more specific URL, passed the constraints to the
        // server, etc., but this example module is staying simple
        if (!constraints.meetsConstraint(policy.getVersion()))
            return new PolicyFinderResult();

        // if we got here, then we successfully resolved a policy that is
        // the correct type, so return it
        return new PolicyFinderResult(policy);
    }

}
