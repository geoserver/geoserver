/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.test;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import java.io.File;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Base support class for wcs tests.
 *
 * @author Andrea Aime, TOPP
 */
public abstract class WCSTestSupport extends CoverageTestSupport {
    protected static XpathEngine xpath;

    protected static final boolean IS_WINDOWS;

    protected static final Schema WCS10_GETCAPABILITIES_SCHEMA;

    protected static final Schema WCS10_GETCOVERAGE_SCHEMA;

    protected static final Schema WCS10_DESCRIBECOVERAGE_SCHEMA;

    static {
        try {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            WCS10_GETCAPABILITIES_SCHEMA =
                    factory.newSchema(new File("./schemas/wcs/1.0.0/wcsCapabilities.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the WCS 1.0.0 schemas", e);
        }
        try {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            WCS10_GETCOVERAGE_SCHEMA =
                    factory.newSchema(new File("./schemas/wcs/1.0.0/getCoverage.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the WCS 1.0.0 schemas", e);
        }
        try {
            final SchemaFactory factory =
                    SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            WCS10_DESCRIBECOVERAGE_SCHEMA =
                    factory.newSchema(new File("./schemas/wcs/1.0.0/describeCoverage.xsd"));
        } catch (Exception e) {
            throw new RuntimeException("Could not parse the WCS 1.0.0 schemas", e);
        }
        boolean windows = false;
        try {
            windows = System.getProperty("os.name").matches(".*Windows.*");
        } catch (Exception e) {
            // no os.name? oh well, never mind
        }
        IS_WINDOWS = windows;
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);

        // add a raster mosaic with time and elevation
        testData.setUpRasterLayer(WATTEMP, "watertemp.zip", null, null, TestData.class);
        // a raster layer with time, elevation and custom dimensions as ranges
        testData.setUpRasterLayer(TIMERANGES, "timeranges.zip", null, null, TestData.class);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wcs", "http://www.opengis.net/wcs");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected boolean isMemoryCleanRequired() {
        return IS_WINDOWS;
    }

    protected String checkOws11Exception(Document dom) throws Exception {
        assertEquals("ServiceExceptionReport", dom.getFirstChild().getNodeName());

        assertEquals(
                "1.2.0",
                dom.getFirstChild().getAttributes().getNamedItem("version").getNodeValue());
        assertXpathEvaluatesTo("1.2.0", "/ServiceExceptionReport/@version", dom);

        Node root = xpath.getMatchingNodes("/ServiceExceptionReport", dom).item(0);
        assertNotNull(root);

        NodeList nodes = dom.getElementsByTagName("ows:ExceptionText");
        if (nodes.getLength() > 0) {
            return nodes.item(0).getNodeValue();
        }
        return null;
    }

    protected void setupRasterDimension(
            QName layer, String metadata, DimensionPresentation presentation, Double resolution) {
        CoverageInfo info = getCatalog().getCoverageByName(layer.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        if (resolution != null) {
            di.setResolution(new BigDecimal(resolution));
        }
        info.getMetadata().put(metadata, di);
        getCatalog().save(info);
    }
}
