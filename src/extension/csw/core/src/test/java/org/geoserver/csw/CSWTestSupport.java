/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import static org.junit.Assert.fail;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import javax.xml.transform.dom.DOMSource;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.csw.CSW;
import org.geotools.csw.CSWConfiguration;
import org.geotools.csw.DC;
import org.geotools.csw.DCT;
import org.geotools.filter.v1_1.OGC;
import org.geotools.xlink.XLINK;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.geotools.xsd.ows.OWS;
import org.junit.BeforeClass;
import org.w3c.dom.Document;
import org.xml.sax.SAXParseException;

public abstract class CSWTestSupport extends GeoServerSystemTestSupport {
    protected static final String BASEPATH = "csw";

    @BeforeClass
    public static void configureXMLUnit() throws Exception {
        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("dc", DC.NAMESPACE);
        namespaces.put("dct", DCT.NAMESPACE);
        namespaces.put("csw", CSW.NAMESPACE);
        namespaces.put("ows", OWS.NAMESPACE);
        namespaces.put("ogc", OGC.NAMESPACE);
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", XLINK.NAMESPACE);
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("xsd", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    };

    protected String root() {
        return "csw?";
    }

    /** Validates a document based on the CSW schemas */
    protected void checkValidationErrors(Document dom) throws Exception {
        checkValidationErrors(dom, new CSWConfiguration());
    }

    /** Validates a document against the */
    protected void checkValidationErrors(Document dom, Configuration configuration)
            throws Exception {
        Parser p = new Parser(configuration);
        p.setValidating(true);
        p.parse(new DOMSource(dom));

        if (!p.getValidationErrors().isEmpty()) {
            for (Iterator e = p.getValidationErrors().iterator(); e.hasNext(); ) {
                SAXParseException ex = (SAXParseException) e.next();
                System.out.println(
                        ex.getLineNumber() + "," + ex.getColumnNumber() + " -" + ex.toString());
            }
            fail("Document did not validate.");
        }
    }

    /** Loads the specified resource into a string */
    protected String getResourceAsString(String resourceLocation) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourceLocation)) {
            return IOUtils.toString(is, "UTF-8");
        }
    }

    /** Loads the specified resource into a reader */
    protected Reader getResourceAsReader(String resourceLocation) throws IOException {
        InputStream is = getClass().getResourceAsStream(resourceLocation);
        return new InputStreamReader(is);
    }
}
