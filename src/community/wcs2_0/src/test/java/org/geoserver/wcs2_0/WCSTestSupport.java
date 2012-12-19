/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import java.awt.geom.AffineTransform;
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
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.data.DataUtilities;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.wcs.v2_0.WCSConfiguration;
import org.geotools.xml.Parser;
import org.junit.After;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.referencing.operation.MathTransform;
import org.w3c.dom.Document;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXParseException;

/**
 * Base support class for wcs tests.
 * 
 * @author Andrea Aime, TOPP
 * 
 */
@SuppressWarnings("serial")
public abstract class WCSTestSupport extends GeoServerSystemTestSupport {
    protected static XpathEngine xpath;

    protected static final boolean IS_WINDOWS;

    protected static final Schema WCS20_SCHEMA;
    
    List<GridCoverage> coverages = new ArrayList<GridCoverage>();

    /**
     * Small value for comparaison of sample values. Since most grid coverage implementations in
     * Geotools 2 store geophysics values as {@code float} numbers, this {@code EPS} value must
     * be of the order of {@code float} relative precision, not {@code double}.
     */
    static final float EPS = 1E-5f;

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
          // System.out.println("---- WCSTestSupport::doSetup ---> " + new Date());
//        System.out.println("---- GeoServerBaseTestSupport::setUpLogging --->  " + new Date());

        super.onSetUp(testData);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wcs", "http://www.opengis.net/wcs/2.0");
        namespaces.put("wcscrs", "http://www.opengis.net/wcs/service-extension/crs/1.0");
        namespaces.put("ows", "http://www.opengis.net/ows/2.0");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("int", "http://www.opengis.net/WCS_service-extension_interpolation/1.0");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();

        // System.out.println("---- WCSTestSupport::doSetup ---< " + new Date());
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
    @SuppressWarnings("rawtypes")
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

    /**
     * Compares the envelopes of two coverages for equality using the smallest
     * scale factor of their "grid to world" transform as the tolerance.
     *
     * @param expected The coverage having the expected envelope.
     * @param actual   The coverage having the actual envelope.
     */
    static void assertEnvelopeEquals(Coverage expected, Coverage actual) {
        final double scaleA = getScale(expected);
        final double scaleB = getScale(actual);
    
        assertEnvelopeEquals((GeneralEnvelope)expected.getEnvelope(),scaleA,(GeneralEnvelope)actual.getEnvelope(),scaleB);
    }

    static void assertEnvelopeEquals(GeneralEnvelope expected,double scaleExpected, GeneralEnvelope actual,double scaleActual) {
        final double tolerance;
        if (scaleExpected <= scaleActual) {
            tolerance = scaleExpected*1E-1;
        } else if (!Double.isNaN(scaleActual)) {
            tolerance = scaleActual*1E-1;
        } else {
            tolerance = EPS;
        }
        Assert.assertTrue(expected.equals(actual, tolerance, false));
    }

    /**
     * Returns the "Sample to geophysics" transform as an affine transform, or {@code null}
     * if none. Note that the returned instance may be an immutable one, not necessarly the
     * default Java2D implementation.
     *
     * @param  coverage The coverage for which to get the "grid to CRS" affine transform.
     * @return The "grid to CRS" affine transform of the given coverage, or {@code null}
     *         if none or if the transform is not affine.
     */
    static AffineTransform getAffineTransform(final Coverage coverage) {
        if (coverage instanceof GridCoverage) {
            final GridGeometry geometry = ((GridCoverage) coverage).getGridGeometry();
            if (geometry != null) {
                final MathTransform gridToCRS;
                if (geometry instanceof GridGeometry2D) {
                    gridToCRS = ((GridGeometry2D) geometry).getGridToCRS();
                } else {
                    gridToCRS = geometry.getGridToCRS();
                }
                if (gridToCRS instanceof AffineTransform) {
                    return (AffineTransform) gridToCRS;
                }
            }
        }
        return null;
    }

    /**
     * Returns the scale of the "grid to CRS" transform, or {@link Double#NaN} if unknown.
     *
     * @param  coverage The coverage for which to get the "grid to CRS" scale, or {@code null}.
     * @return The "grid to CRS" scale, or {@code NaN} if none or if the transform is not affine.
     */
    static double getScale(final Coverage coverage) {
        final AffineTransform gridToCRS = getAffineTransform(coverage);
        return (gridToCRS != null) ? XAffineTransform.getScale(gridToCRS) : Double.NaN;
    }

    
}
