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
package org.geotools.xacml.test;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.geotools.xacml.geoxacml.attr.GeometryAttribute;
import org.geotools.xacml.geoxacml.finder.impl.GeoSelectorModule;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.sun.xacml.AbstractPolicy;
import com.sun.xacml.PDP;
import com.sun.xacml.PDPConfig;
import com.sun.xacml.Policy;
import com.sun.xacml.Rule;
import com.sun.xacml.cond.Apply;
import com.sun.xacml.finder.AttributeFinder;
import com.sun.xacml.finder.AttributeFinderModule;
import com.sun.xacml.finder.PolicyFinder;
import com.sun.xacml.finder.PolicyFinderModule;
import com.sun.xacml.finder.impl.CurrentEnvModule;
import com.sun.xacml.support.finder.FilePolicyModule;

/**
 * @author Christian Mueller
 * 
 *         Collection of methods supporting the test cases
 */
public class TestSupport {

    protected static String OUTPUTDIR_BASE = "target";

    protected static String OUTPUTDIR_RESOURCES = OUTPUTDIR_BASE + File.separator + "resources"
            + File.separator;

    protected static String RESOURCE_ZIP = "src/test/resources/resources.zip";

    public static GeometryAttribute getGeometryAttribute(AbstractPolicy p) {
        Rule rule = (Rule) p.getChildren().get(0);
        Apply apply = (Apply) rule.getCondition().getChildren().get(0);
        GeometryAttribute attr = (GeometryAttribute) apply.getChildren().get(1);
        return attr;
    }

    public static AbstractPolicy policyFromXML(String xmlString) {
        InputStream in = new ByteArrayInputStream(xmlString.getBytes());
        return loadPolicy(in);
    }

    public static AbstractPolicy policyFromFile(String fileName) {
        try {
            return loadPolicy(new FileInputStream(fileName));
        } catch (FileNotFoundException ex) {
            return null;
        }
    }

    static private AbstractPolicy loadPolicy(InputStream in) {
        try {
            // create the factory
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setIgnoringComments(true);
            factory.setNamespaceAware(true);
            // factory.setIgnoringElementContentWhitespace(true);
            factory.setValidating(false);

            // create a builder based on the factory & try to load the policy
            DocumentBuilder db = factory.newDocumentBuilder();
            Document doc = db.parse(in);

            // handle the policy, if it's a known type
            Element root = doc.getDocumentElement();
            String name = root.getLocalName();

            if (name.equals("Policy")) {
                return Policy.getInstance(root);
            }
            // else if (name.equals("PolicySet")) {
            // return PolicySet.getInstance(root, finder);
            // }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    public static String fileContentAsString(String fileName) {
        StringBuffer buff = new StringBuffer();
        try {
            FileInputStream in = new FileInputStream(fileName);
            byte[] bytes = new byte[512];
            int len;
            while ((len = in.read(bytes)) != -1) {
                buff.append(new String(bytes, 0, len));
            }
            in.close();
        } catch (IOException ex) {
            return null;
        }
        return buff.toString();
    }

    static public PDP getPDP(String fileName) {
        FilePolicyModule policyModule = new FilePolicyModule();
        policyModule.addPolicy(fileName);

        PolicyFinder policyFinder = new PolicyFinder();
        Set<PolicyFinderModule> policyModules = new HashSet<PolicyFinderModule>();
        policyModules.add(policyModule);
        policyFinder.setModules(policyModules);

        CurrentEnvModule envModule = new CurrentEnvModule();
        GeoSelectorModule selectorModule = new GeoSelectorModule();

        AttributeFinder attrFinder = new AttributeFinder();
        List<AttributeFinderModule> attrModules = new ArrayList<AttributeFinderModule>();
        attrModules.add(envModule);
        attrModules.add(selectorModule);
        attrFinder.setModules(attrModules);

        PDP pdp = new PDP(new PDPConfig(attrFinder, policyFinder, null));
        return pdp;
    }

    static public String getGeoXACMLFNFor(String resourceDir, String fn) {
        String result = "src" + File.separator + "test" + File.separator + "resources"
                + File.separator + "geoxml" + File.separator + resourceDir + File.separator + fn;

        if (new File(result).exists())
            return result;

        result = "target" + File.separator + "resources" + File.separator + "geoxml"
                + File.separator + resourceDir + File.separator + fn;
        return result;
    }

    static public String getFNFor(String resourceDir, String fn) {
        String result = "src" + File.separator + "test" + File.separator + "resources"
                + File.separator + resourceDir + File.separator + fn;

        if (new File(result).exists())
            return result;

        result = "target" + File.separator + "resources" + File.separator + resourceDir
                + File.separator + fn;
        return result;

    }

    static public void initOutputDir() throws Exception {
        File targetResourcedir = new File(OUTPUTDIR_RESOURCES);

        if (targetResourcedir.exists() == false) {
            createTargetResourceDir(targetResourcedir);
        }

    }

    protected static void createTargetResourceDir(File targetResourcedir) throws Exception {
        targetResourcedir.mkdir();

        ZipFile zipFile = new ZipFile(RESOURCE_ZIP);
        Enumeration<? extends ZipEntry> entries = zipFile.entries();

        while (entries.hasMoreElements()) {
            ZipEntry entry = entries.nextElement();

            if (entry.isDirectory()) {
                File dir = new File(OUTPUTDIR_RESOURCES + entry.getName());
                dir.mkdir();
            } else {
                InputStream in = zipFile.getInputStream(entry);
                FileOutputStream out = new FileOutputStream(OUTPUTDIR_RESOURCES + entry.getName());
                byte[] buff = new byte[4096];
                int count;

                while ((count = in.read(buff)) > 0)
                    out.write(buff, 0, count);

                in.close();
                out.close();
            }
        }

    }

}
