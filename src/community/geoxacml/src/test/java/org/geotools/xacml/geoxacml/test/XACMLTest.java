/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */

package org.geotools.xacml.geoxacml.test;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.geotools.xacml.geoxacml.config.GeoXACML;
import org.geotools.xacml.test.TestSupport;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.sun.xacml.ConfigurationStore;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.test.BasicGroupTest;
import com.sun.xacml.test.BasicTest;
import com.sun.xacml.test.Test;
import com.sun.xacml.test.TestPolicyFinderModule;

/**
 * @author Christian Mueller
 * 
 *         Tests for XACML
 * 
 */
public class XACMLTest extends TestCase {

    private PDP pdp;

    private TestPolicyFinderModule policyModule;

    private ArrayList<Test> tests;

    public XACMLTest() {
        super();

    }

    public XACMLTest(String arg0) {
        super(arg0);

    }

    @Override
    protected void setUp() throws Exception {
        GeoXACML.initialize();
        TestSupport.initOutputDir();
    }

    private void loadTests(String testFile) throws Exception {
        // load the test file
        Node root = getRootNode(testFile);

        // go through each of the top-level tests, and handle as appropriate
        NodeList children = root.getChildNodes();
        for (int i = 0; i < children.getLength(); i++) {
            Node child = children.item(i);
            String childName = child.getNodeName();

            if (childName.equals("group"))
                tests.add(BasicGroupTest.getInstance(child, pdp, policyModule));
            else if (childName.equals("test"))
                tests.add(BasicTest.getInstance(child, pdp, policyModule));
        }
    }

    private void configurePDP() throws Exception {
        // load the configuration

        System.setProperty(ConfigurationStore.PDP_CONFIG_PROPERTY, "target" + File.separator
                + "resources" + File.separator + "config.xml");
        ConfigurationStore cs = new ConfigurationStore();

        // use the default factories from the configuration
        cs.useDefaultFactories();

        // get the PDP configuration's policy finder modules...
        PDPConfig config = cs.getDefaultPDPConfig();
        PolicyFinder finder = config.getPolicyFinder();
        Set<PolicyFinderModule> policyModules = finder.getModules();

        // ...and add the module used by the tests
        policyModules.add(policyModule);
        finder.setModules(policyModules);

        // finally, setup the PDP
        pdp = new PDP(config);
    }

    /**
     * Private helper that parses the file and sets up the DOM tree.
     */
    private Node getRootNode(String configFile) throws Exception {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();

        dbFactory.setIgnoringComments(true);
        dbFactory.setNamespaceAware(false);
        dbFactory.setValidating(false);

        DocumentBuilder db = dbFactory.newDocumentBuilder();
        Document doc = db.parse(new FileInputStream(configFile));
        Element root = doc.getDocumentElement();

        if (!root.getTagName().equals("tests"))
            throw new Exception("unknown document type: " + root.getTagName());

        return root;
    }

    public void testXACML() {
        // policyModule = new TestPolicyFinderModule();
        // tests = new ArrayList<Test>();
        //
        // try {
        // configurePDP();
        // loadTests("target"+File.separator+"resources"+File.separator+"tests2.xml");
        // } catch (Exception e) {
        // e.printStackTrace();
        // Assert.fail();
        // return;
        // }
        //            
        // Iterator<Test> it = tests.iterator();
        // int errorCount = 0;
        //    
        // System.out.println("STARTING TESTS at " + new Date());
        //    
        // while (it.hasNext()) {
        // Test test = it.next();
        // int error=
        // test.run("target"+File.separator+"resources"+File.separator+"xml"+File.separator);
        // if (error >0 ) {
        // System.out.println("FAILED: "+test.getName());
        // }
        // errorCount += error;
        // }
        //    
        // System.out.println("FINISHED TESTS at " + new Date());
        // System.out.println("Total Failed: " + errorCount);
        //
    }

}
