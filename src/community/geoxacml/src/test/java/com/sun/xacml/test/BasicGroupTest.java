/*
 * @(#)BasicGroupTest.java
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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.PDP;

/**
 * A basic implementation of a group of tests.
 * 
 * @author Seth Proctor
 */
public class BasicGroupTest implements Test {

    // the name of this test
    private String name;

    // whether this test is experimental
    private boolean experimental;

    // the tests contained in this group
    private List<Test> tests;

    /**
     * Constructor that accepts all the required values
     * 
     * @param name
     *            the name of this group
     * @param experimental
     *            true if this is an experimental group
     * @param tests
     *            the groups of tests
     */
    public BasicGroupTest(String name, boolean experimental, List<Test> tests) {
        this.name = name;
        this.experimental = experimental;
        this.tests = tests;
    }

    /**
     * Creates an instance of a group of tests from its XML representation.
     * 
     * @param root
     *            the root of the XML-encoded data for this group
     * @param pdp
     *            the <code>PDP</code> used by any sub-tests
     * @param module
     *            the module used for any policies loaded by sub-tests
     */
    public static BasicGroupTest getInstance(Node root, PDP pdp, TestPolicyFinderModule module) {
        NamedNodeMap map = root.getAttributes();

        // the name is required
        String name = map.getNamedItem("name").getNodeValue();

        // the experimental tag isn't
        boolean experimental = false;
        Node attrNode = map.getNamedItem("experimental");
        if (attrNode == null)
            experimental = false;
        else
            experimental = attrNode.getNodeValue().equals("true");

        // now get all the elements
        NodeList children = root.getChildNodes();
        ArrayList<Test> tests = new ArrayList<Test>();

        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String nodeName = child.getNodeName();

            // if we find another group or an individual test, load it
            if (nodeName.equals("group"))
                tests.add(BasicGroupTest.getInstance(child, pdp, module));
            else if (nodeName.equals("test"))
                tests.add(BasicTest.getInstance(child, pdp, module));
        }

        // create the new group
        return new BasicGroupTest(name, experimental, tests);
    }

    public String getName() {
        return name;
    }

    public boolean isErrorExpected() {
        return false;
    }

    public boolean isExperimental() {
        return experimental;
    }

    /**
     * Returns the tests contained in this group.
     * 
     * @return a <code>List</code> of <code>Test</code>s
     */
    public List<Test> getTests() {
        return tests;
    }

    public int run(String testPrefix) {
        Iterator<Test> it = tests.iterator();
        int errorCount = 0;

        System.out.println("Running group " + name);

        while (it.hasNext())
            errorCount += it.next().run(testPrefix + name);

        System.out.println("Finished group " + name + " [failures: " + errorCount + "]");

        return errorCount;
    }

}
