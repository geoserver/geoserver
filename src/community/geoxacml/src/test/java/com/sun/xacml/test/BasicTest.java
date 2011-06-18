/*
 * @(#)BasicTest.java
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

import java.io.FileInputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.PDP;
import com.sun.xacml.ctx.RequestCtx;
import com.sun.xacml.ctx.ResponseCtx;

/**
 * A simple implementation of a single conformance test case.
 * 
 * @author Seth Proctor
 */
public class BasicTest implements Test {

    // the PDP and module that manage this test's policies
    private PDP pdp;

    private TestPolicyFinderModule module;

    // the policies and references used by this test
    private Set<String> policies;

    private Map<String, String> policyRefs;

    private Map<String, String> policySetRefs;

    // meta-data associated with this test
    private String name;

    private boolean errorExpected;

    private boolean experimental;

    /**
     * Constructor that accepts all values associatd with a test.
     * 
     * @param pdp
     *            the pdp that manages this test's evaluations
     * @param module
     *            the module that manages this test's policies
     * @param policies
     *            the files containing the policies used by this test, or null if we only use the
     *            default policy for this test
     * @param policyRefs
     *            the policy references used by this test
     * @param policySetRefs
     *            the policy set references used by this test
     * @param name
     *            the name of this test
     * @param errorExpected
     *            true if en error is expected from a normal run
     * @param experimental
     *            true if this is an experimental test
     */
    public BasicTest(PDP pdp, TestPolicyFinderModule module, Set<String> policies,
            Map<String, String> policyRefs, Map<String, String> policySetRefs, String name,
            boolean errorExpected, boolean experimental) {
        this.pdp = pdp;
        this.module = module;
        this.policies = policies;
        this.policyRefs = policyRefs;
        this.policySetRefs = policySetRefs;
        this.name = name;
        this.errorExpected = errorExpected;
        this.experimental = experimental;
    }

    /**
     * Creates an instance of a test from its XML representation.
     * 
     * @param root
     *            the root of the XML-encoded data for this test
     * @param pdp
     *            the <code>PDP</code> used by this test
     * @param module
     *            the module used for this test's policies
     */
    public static BasicTest getInstance(Node root, PDP pdp, TestPolicyFinderModule module) {
        NamedNodeMap map = root.getAttributes();

        // the name is required...
        String name = map.getNamedItem("name").getNodeValue();

        // ...but the other two aren't
        boolean errorExpected = isAttrTrue(map, "errorExpected");
        boolean experimental = isAttrTrue(map, "experimental");

        // see if there's any content
        Set<String> policies = null;
        Map<String, String> policyRefs = null;
        Map<String, String> policySetRefs = null;
        if (root.hasChildNodes()) {
            NodeList children = root.getChildNodes();
            for (int i = 0; i < children.getLength(); i++) {
                Node child = children.item(i);
                String childName = child.getNodeName();

                if (childName.equals("policy")) {
                    if (policies == null)
                        policies = new HashSet<String>();
                    policies.add(child.getFirstChild().getNodeValue());
                } else if (childName.equals("policyReference")) {
                    if (policyRefs == null)
                        policyRefs = new HashMap<String, String>();
                    policyRefs.put(child.getAttributes().getNamedItem("ref").getNodeValue(), child
                            .getFirstChild().getNodeValue());
                } else if (childName.equals("policySetReference")) {
                    if (policySetRefs == null)
                        policySetRefs = new HashMap<String, String>();
                    policySetRefs.put(child.getAttributes().getNamedItem("ref").getNodeValue(),
                            child.getFirstChild().getNodeValue());
                }
            }
        }

        return new BasicTest(pdp, module, policies, policyRefs, policySetRefs, name, errorExpected,
                experimental);
    }

    /**
     * Private helper that reads a attribute to see if it's set, and if so if its value is "true".
     */
    private static boolean isAttrTrue(NamedNodeMap map, String attrName) {
        Node attrNode = map.getNamedItem(attrName);

        if (attrNode == null)
            return false;

        return attrNode.getNodeValue().equals("true");
    }

    public String getName() {
        return name;
    }

    public boolean isErrorExpected() {
        return errorExpected;
    }

    public boolean isExperimental() {
        return experimental;
    }

    public int run(String testPrefix) {
        System.out.print("test " + name + ": ");
        int errorCount = 0;
        boolean failurePointReached = false;

        // FIXME: we sould get more specific with the exceptions, so we can
        // make sure that an error happened _for the right reason_

        try {
            // load the request for this test
            RequestCtx request = RequestCtx.getInstance(new FileInputStream(testPrefix + name
                    + "Request.xml"));

            // re-set the module to use this test's policies
            if (policies == null) {
                module.setPolicies(testPrefix + name + "Policy.xml");
            } else {
                Iterator<String> it = policies.iterator();
                Set<String> set = new HashSet<String>();

                while (it.hasNext())
                    set.add(testPrefix + it.next());

                module.setPolicies(set);
            }

            // re-set any references we're using
            module.setPolicyRefs(policyRefs, testPrefix);
            module.setPolicySetRefs(policySetRefs, testPrefix);

            // actually do the evaluation
            ResponseCtx response = pdp.evaluate(request);

            // if we're supposed to fail, we should have done so by now
            if (errorExpected) {
                System.out.println("failed");
                errorCount++;
            } else {
                failurePointReached = true;

                // load the reponse that we expectd to get
                ResponseCtx expectedResponse = ResponseCtx.getInstance(new FileInputStream(
                        testPrefix + name + "Response.xml"));

                // see if the actual result matches the expected result
                boolean equiv = TestUtil.areEquivalent(response, expectedResponse);

                if (equiv) {
                    System.out.println("passed");
                } else {
                    System.out.println("failed:");
                    response.encode(System.out);
                    errorCount++;
                }
            }
        } catch (Exception e) {
            // any errors happen as exceptions, and may be successes if we're
            // supposed to fail and we haven't reached the failure point yet
            if (!failurePointReached) {
                if (errorExpected) {
                    System.out.println("passed");
                } else {
                    System.out.println("EXCEPTION: " + e.getMessage());
                    errorCount++;
                }
            } else {
                System.out.println("UNEXPECTED EXCEPTION: " + e.getMessage());
                errorCount++;
            }
        }

        // return the number of errors that occured
        return errorCount;
    }

}
