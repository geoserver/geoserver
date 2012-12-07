/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static junit.framework.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.XMLConstants;
import javax.xml.transform.dom.DOMSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import junit.framework.Assert;

import org.apache.xerces.dom.DOMInputImpl;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geoserver.wcs.WebCoverageService111;
import org.geoserver.wcs.kvp.GetCoverageRequestReader;
import org.geoserver.wcs.xml.v1_1_1.WcsXmlReader;
import org.geotools.data.DataUtilities;
import org.geotools.wcs.v2_0.WCSConfiguration;
import org.geotools.xml.Configuration;
import org.geotools.xml.Parser;
import org.geotools.xml.XML;
import org.junit.After;
import org.junit.Before;
import org.opengis.coverage.grid.GridCoverage;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXParseException;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Base support class for wcs tests.
 * 
 * @author Andrea Aime, TOPP
 * 
 */
public abstract class WCSTestSupport extends GeoServerSystemTestSupport {
    protected static XpathEngine xpath;

    protected static final boolean IS_WINDOWS;

    protected static final Schema WCS20_SCHEMA;
    
    List<GridCoverage> coverages = new ArrayList<GridCoverage>();

    static {
        final Map<String, String> namespaceMap = new HashMap<String, String>() {
            {
                put("http://www.opengis.net/wcs/2.0", "./src/main/resources/schemas/wcs/2.0/");
                put("http://www.opengis.net/gmlcov/1.0", "./src/main/resources/schemas/gmlcov/1.0/");
                put("http://www.opengis.net/gml/3.2", "./src/main/resources/schemas/gml/3.2.1/");
                put("http://www.w3.org/1999/xlink", "./src/test/resources/schemas/xlink/");
                put("http://www.w3.org/XML/1998/namespace", "./src/test/resources/schemas/xml/");
                put("http://www.isotc211.org/2005/gmd", "./src/main/resources/schemas/iso/19139/20070417/gmd/");
                put("http://www.isotc211.org/2005/gco", "./src/main/resources/schemas/iso/19139/20070417/gco/");
                put("http://www.isotc211.org/2005/gss", "./src/main/resources/schemas/iso/19139/20070417/gss/");
                put("http://www.isotc211.org/2005/gts", "./src/main/resources/schemas/iso/19139/20070417/gts/");
                put("http://www.isotc211.org/2005/gsr", "./src/main/resources/schemas/iso/19139/20070417/gsr/");
                put("http://www.opengis.net/swe/2.0", "./src/main/resources/schemas/sweCommon/2.0/");
                put("http://www.opengis.net/ows/2.0", "./src/main/resources/schemas/ows/2.0/");
            }
        };

        try {
            final SchemaFactory factory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            factory.setResourceResolver(new LSResourceResolver() {
                @Override
                public LSInput resolveResource(String type, String namespaceURI, String publicId,
                        String systemId, String baseURI) {

                    String localPosition = namespaceMap.get(namespaceURI);
                    if (localPosition != null) {
                        try {
                            if (systemId.contains("/")) {
                                systemId = systemId.substring(systemId.lastIndexOf("/") + 1);
                            }
                            File file = new File(localPosition + systemId);
                            if (file.exists()) {
                                URL url = DataUtilities.fileToURL(file);
                                systemId = url.toURI().toASCIIString();
                                DOMInputImpl input = new DOMInputImpl(publicId, systemId, null);
                                return input;
                            }
                        } catch (Exception e) {
                            return null;
                        }
                    }
                    return null;
                }
            });
            WCS20_SCHEMA = factory.newSchema(new File("./src/main/resources/schemas/wcs/2.0/wcsAll.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the WCS 2.0 schemas", e);
        }
        boolean windows = false;
        try {
            windows = System.getProperty("os.name").matches(".*Windows.*");
        } catch (Exception e) {
            // no os.name? oh well, never mind
        }
        IS_WINDOWS = windows;
    }

    /**
     * @return The global wcs instance from the application context.
     */
    protected WCSInfo getWCS() {
        return getGeoServer().getService(WCSInfo.class);
    }

    /**
     * Only setup coverages
     */
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        testData.setUpWcs10RasterLayers();
        testData.setUpWcs11RasterLayers();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
                System.out.println("---- WCSTestSupport::doSetup ---> " + new Date());
//        System.out.println("---- GeoServerBaseTestSupport::setUpLogging --->  " + new Date());

        super.onSetUp(testData);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();

        System.out.println("---- WCSTestSupport::doSetup ---< " + new Date());
    }

    @Override
    protected boolean isMemoryCleanRequired() {
        return IS_WINDOWS;
    }

    /**
     * Validates a document against the
     * 
     * @param dom
     * @param configuration
     */
    protected void checkValidationErrors(Document dom) throws Exception {
        Parser p = new Parser(new WCSConfiguration());
        p.setValidating(true);
        p.parse(new DOMSource(dom));

        if (!p.getValidationErrors().isEmpty()) {
            for (Iterator e = p.getValidationErrors().iterator(); e.hasNext();) {
                SAXParseException ex = (SAXParseException) e.next();
                System.out.println(ex.getLineNumber() + "," + ex.getColumnNumber() + " -"
                        + ex.toString());
            }
            Assert.fail("Document did not validate.");
        }
    }
    
    /**
     * Marks the coverage to be cleaned when the test ends
     * @param coverage
     */
    protected void scheduleForCleaning(GridCoverage coverage) {
        if(coverage != null) {
            coverages.add(coverage);
        }
    }

    @After
    public void cleanCoverages() {
        for (GridCoverage coverage : coverages) {
            CoverageCleanerCallback.disposeCoverage(coverage);
        }
    }

    
}
