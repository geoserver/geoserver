/*
 * @(#)SimplePDP.java
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

package com.sun.xacml.support;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.sun.xacml.ConfigurationStore;
import com.sun.xacml.Indenter;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.ParsingException;
import com.sun.xacml.combine.PermitOverridesPolicyAlg;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import com.sun.xacml.finder.impl.SelectorModule;
import com.sun.xacml.support.finder.StaticPolicyFinderModule;
import com.sun.xacml.support.finder.StaticRefPolicyFinderModule;
import com.sun.xacml.support.finder.URLPolicyFinderModule;

/**
 * This is a simple, command-line driven XACML PDP. It acts both as an example of how to write a
 * full-featured PDP and as a sample program that lets you evaluate requests against policies. See
 * the comments for the main() method for correct usage.
 * <p>
 * As of the 2.0 release, this has been moved into the new support tree of the codebase. It has also
 * been updated to use several of the new finder modules provided in the support tree codebase, so
 * that static and dynamic references are supported, policies can be loaded from URLs, top-level
 * policies are wrapped in a policy set when more than one applies, etc.
 * <p>
 * If you don't use a configuration file, then the default modules can all optionally support schema
 * validation. To turn this on, provide the filename of the schema file in the property
 * "com.sun.xacml.PolicySchema". You can also turn this on if you use a configuration file and it
 * includes the modules provided in the support package.
 * 
 * @since 1.1
 * @author Seth Proctor
 * 
 *         Adding generic type support by Christian Mueller (geotools)
 */
public class SimplePDP {

    // this is the actual PDP object we'll use for evaluation
    private PDP pdp = null;

    /**
     * Default constructor. This creates a <code>SimplePDP</code> with a <code>PDP</code> based on
     * the configuration defined by the runtime property com.sun.xcaml.PDPConfigFile.
     */
    public SimplePDP() throws Exception {
        // load the configuration
        ConfigurationStore store = new ConfigurationStore();

        // use the default factories from the configuration
        store.useDefaultFactories();

        // get the PDP configuration's and setup the PDP
        pdp = new PDP(store.getDefaultPDPConfig());
    }

    /**
     * Constructor that takes an array of filenames and URLs, each of which points to an XACML
     * policy, and sets up a <code>PDP</code> with access to these policies only. These policies may
     * be accessed based on context matching or by reference (based on their policy identifiers).
     * The <code>PDP</code> is also setup to support dynamic URL references.
     * 
     * @param policies
     *            an arry of filenames and URLs that specify policies
     */
    public SimplePDP(String[] policies) throws Exception {
        // Create the two static modules with the given policies so that
        // we have context-based and reference-based access to all the
        // policies provided on the command-line
        List<String> policyList = Arrays.asList(policies);
        StaticPolicyFinderModule staticModule = new StaticPolicyFinderModule(
                PermitOverridesPolicyAlg.algId, policyList);
        StaticRefPolicyFinderModule staticRefModule = new StaticRefPolicyFinderModule(policyList);

        // also create a module that lets us get at URL-based policies
        URLPolicyFinderModule urlModule = new URLPolicyFinderModule();

        // next, setup the PolicyFinder that this PDP will use
        PolicyFinder policyFinder = new PolicyFinder();
        Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
        policyModules.add(staticModule);
        policyModules.add(staticRefModule);
        policyModules.add(urlModule);
        policyFinder.setModules(policyModules);

        // now setup attribute finder modules for the current date/time and
        // AttributeSelectors (selectors are optional, but this project does
        // support a basic implementation)
        CurrentEnvModule envAttributeModule = new CurrentEnvModule();
        SelectorModule selectorAttributeModule = new SelectorModule();

        // Setup the AttributeFinder just like we setup the PolicyFinder. Note
        // that unlike with the policy finder, the order matters here. See the
        // the javadocs for more details.
        AttributeFinder attributeFinder = new AttributeFinder();
        List<AttributeFinderModule> attributeModules = new ArrayList<AttributeFinderModule>();
        attributeModules.add(envAttributeModule);
        attributeModules.add(selectorAttributeModule);
        attributeFinder.setModules(attributeModules);

        // finally, initialize our pdp
        pdp = new PDP(new PDPConfig(attributeFinder, policyFinder, null));
    }

    /**
     * Evaluates the given request and returns the Response that the PDP will hand back to the PEP.
     * 
     * @param requestFile
     *            the name of a file that contains a Request
     * 
     * @return the result of the evaluation
     * 
     * @throws IOException
     *             if there is a problem accessing the file
     * @throws ParsingException
     *             if the Request is invalid
     */
    public ResponseCtx evaluate(String requestFile) throws IOException, ParsingException {
        // setup the request based on the file
        RequestCtx request = RequestCtx.getInstance(new FileInputStream(requestFile));

        // evaluate the request
        return pdp.evaluate(request);
    }

    /**
     * Main-line driver for this sample code. This method lets you invoke the PDP directly from the
     * command-line.
     * 
     * @param args
     *            the input arguments to the class. They are either the flag "-config" followed by a
     *            request file, or a request file followed by one or more policy files. In the case
     *            that the configuration flag is used, the configuration file must be specified in
     *            the standard java property, com.sun.xacml.PDPConfigFile.
     */
    public static void main(String[] args) throws Exception {
        if (args.length < 2) {
            System.out.println("Usage: -config <request>");
            System.out.println("       <request> <policy> [policies]");
            System.exit(1);
        }

        SimplePDP simplePDP = null;
        String requestFile = null;

        if (args[0].equals("-config")) {
            requestFile = args[1];
            simplePDP = new SimplePDP();
        } else {
            requestFile = args[0];
            String[] policyFiles = new String[args.length - 1];

            for (int i = 1; i < args.length; i++)
                policyFiles[i - 1] = args[i];

            simplePDP = new SimplePDP(policyFiles);
        }

        // evaluate the request
        ResponseCtx response = simplePDP.evaluate(requestFile);

        // for this sample program, we'll just print out the response
        response.encode(System.out, new Indenter());
    }

}
