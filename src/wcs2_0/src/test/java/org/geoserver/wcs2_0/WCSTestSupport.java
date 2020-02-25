/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import javax.imageio.metadata.IIOMetadataNode;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;
import javax.xml.XMLConstants;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.Source;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffConstants;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.wcs.v2_0.WCSConfiguration;
import org.geotools.xsd.Parser;
import org.junit.After;
import org.opengis.coverage.Coverage;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridGeometry;
import org.opengis.referencing.operation.MathTransform;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.ls.DOMImplementationLS;
import org.w3c.dom.ls.LSInput;
import org.w3c.dom.ls.LSResourceResolver;
import org.xml.sax.SAXParseException;

/**
 * Base support class for wcs tests.
 *
 * @author Andrea Aime, GeoSolutions
 */
@SuppressWarnings("serial")
public abstract class WCSTestSupport extends GeoServerSystemTestSupport {
    protected static XpathEngine xpath;

    protected static final boolean IS_WINDOWS;

    private static Schema WCS20_SCHEMA;

    List<GridCoverage> coverages = new ArrayList<GridCoverage>();

    protected static final String VERSION = WCS20Const.CUR_VERSION;

    protected static final QName UTM11 = new QName(MockData.WCS_URI, "utm11", MockData.WCS_PREFIX);

    /**
     * Small value for comparaison of sample values. Since most grid coverage implementations in
     * Geotools 2 store geophysics values as {@code float} numbers, this {@code EPS} value must be
     * of the order of {@code float} relative precision, not {@code double}.
     */
    static final float EPS = 1E-5f;

    static {
        boolean windows = false;
        try {
            windows = System.getProperty("os.name").matches(".*Windows.*");
        } catch (Exception e) {
            // no os.name? oh well, never mind
        }
        IS_WINDOWS = windows;
    }

    protected static Schema getWcs20Schema() {
        if (WCS20_SCHEMA == null) {
            final Map<String, String> namespaceMap =
                    new HashMap<String, String>() {
                        {
                            put("http://www.opengis.net/wcs/2.0", "/schemas/wcs/2.0/");
                            put("http://www.opengis.net/gmlcov/1.0", "/schemas/gmlcov/1.0/");
                            put("http://www.opengis.net/gml/3.2", "/schemas/gml/3.2.1/");
                            put("http://www.w3.org/1999/xlink", "/schemas/xlink/");
                            put("http://www.w3.org/XML/1998/namespace", "/schemas/xml/");
                            put(
                                    "http://www.isotc211.org/2005/gmd",
                                    "/schemas/iso/19139/20070417/gmd/");
                            put(
                                    "http://www.isotc211.org/2005/gco",
                                    "/schemas/iso/19139/20070417/gco/");
                            put(
                                    "http://www.isotc211.org/2005/gss",
                                    "/schemas/iso/19139/20070417/gss/");
                            put(
                                    "http://www.isotc211.org/2005/gts",
                                    "/schemas/iso/19139/20070417/gts/");
                            put(
                                    "http://www.isotc211.org/2005/gsr",
                                    "/schemas/iso/19139/20070417/gsr/");
                            put("http://www.opengis.net/swe/2.0", "/schemas/sweCommon/2.0/");
                            put("http://www.opengis.net/ows/2.0", "/schemas/ows/2.0/");
                            put("http://www.geoserver.org/wcsgs/2.0", "/schemas/wcs/2.0/");
                        }
                    };

            try {
                final SchemaFactory factory =
                        SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);

                factory.setResourceResolver(
                        new LSResourceResolver() {

                            DOMImplementationLS dom;

                            {
                                try {
                                    // ok, this is ugly.. the only way I've found to create an
                                    // InputLS
                                    // without
                                    // having to really implement every bit of it is to create a
                                    // DOMImplementationLS
                                    DocumentBuilderFactory builderFactory =
                                            DocumentBuilderFactory.newInstance();
                                    builderFactory.setNamespaceAware(true);

                                    DocumentBuilder builder = builderFactory.newDocumentBuilder();
                                    // fake xml to parse
                                    String xml =
                                            "<?xml version=\"1.0\" encoding=\"UTF-8\"?><empty></empty>";
                                    dom =
                                            (DOMImplementationLS)
                                                    builder.parse(
                                                                    new ByteArrayInputStream(
                                                                            xml.getBytes()))
                                                            .getImplementation();
                                } catch (Exception e) {
                                    throw new RuntimeException(e);
                                }
                            }

                            @Override
                            public LSInput resolveResource(
                                    String type,
                                    String namespaceURI,
                                    String publicId,
                                    String systemId,
                                    String baseURI) {

                                String localPosition = namespaceMap.get(namespaceURI);
                                if (localPosition != null) {
                                    try {
                                        if (systemId.contains("/")) {
                                            systemId =
                                                    systemId.substring(
                                                            systemId.lastIndexOf("/") + 1);
                                        }
                                        final URL resource =
                                                WCSTestSupport.class.getResource(
                                                        localPosition + "/" + systemId);
                                        if (resource != null) {
                                            systemId = resource.toURI().toASCIIString();
                                            LSInput input = dom.createLSInput();
                                            input.setPublicId(publicId);
                                            input.setSystemId(systemId);
                                            return input;
                                        }
                                    } catch (Exception e) {
                                        return null;
                                    }
                                }
                                return null;
                            }
                        });
                WCS20_SCHEMA =
                        factory.newSchema(
                                new Source[] {
                                    new StreamSource(
                                            WCSTestSupport.class
                                                    .getResource("/schemas/wcs/2.0/wcsAll.xsd")
                                                    .toExternalForm()),
                                    new StreamSource(
                                            WCSTestSupport.class
                                                    .getResource("/schemas/wcs/2.0/wcsgs.xsd")
                                                    .toExternalForm())
                                });
            } catch (Exception e) {
                throw new RuntimeException("Could not parse the WCS 2.0 schemas", e);
            }
        }

        return WCS20_SCHEMA;
    }

    /** @return The global wcs instance from the application context. */
    protected WCSInfo getWCS() {
        return getGeoServer().getService(WCSInfo.class);
    }

    /** Only setup coverages */
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        testData.setUpWcs10RasterLayers();
        testData.setUpWcs11RasterLayers();
        testData.setUpRasterLayer(UTM11, "/utm11-2.tiff", null, null, WCSTestSupport.class);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {

        super.onSetUp(testData);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wcs", "http://www.opengis.net/wcs/2.0");
        namespaces.put("crs", "http://www.opengis.net/wcs/crs/1.0");
        namespaces.put("ows", "http://www.opengis.net/ows/2.0");
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("int", "http://www.opengis.net/WCS_service-extension_interpolation/1.0");
        namespaces.put("gmlcov", "http://www.opengis.net/gmlcov/1.0");
        namespaces.put("swe", "http://www.opengis.net/swe/2.0");
        namespaces.put("gml", "http://www.opengis.net/gml/3.2");
        namespaces.put("wcsgs", "http://www.geoserver.org/wcsgs/2.0");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected boolean isMemoryCleanRequired() {
        return IS_WINDOWS;
    }

    /** Validates a document against the */
    @SuppressWarnings("rawtypes")
    protected void checkValidationErrors(Document dom) throws Exception {
        Parser p = new Parser(new WCSConfiguration());
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

    /** Marks the coverage to be cleaned when the test ends */
    protected void scheduleForCleaning(GridCoverage coverage) {
        if (coverage != null) {
            coverages.add(coverage);
        }
    }

    @After
    public void cleanCoverages() {
        for (GridCoverage coverage : coverages) {
            if (coverage != null) {
                CoverageCleanerCallback.disposeCoverage(coverage);
            }
        }
    }

    protected void checkFullCapabilitiesDocument(Document dom) throws Exception {
        checkValidationErrors(dom, getWcs20Schema());

        // TODO: check all the layers are here, the profiles, and so on

        // check that we have the crs extension
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_service-extension_crs/1.0/conf/crs'])",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//wcs:ServiceMetadata/wcs:Extension/crs:CrsMetadata[crs:crsSupported = 'http://www.opengis.net/def/crs/EPSG/0/4326'])",
                dom);

        // check the interpolation extension
        assertXpathEvaluatesTo(
                "1",
                "count(//ows:ServiceIdentification[ows:Profile='http://www.opengis.net/spec/WCS_service-extension_interpolation/1.0/conf/interpolation'])",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//wcs:ServiceMetadata/wcs:Extension[int:interpolationSupported='http://www.opengis.net/def/interpolation/OGC/1/nearest-neighbor'])",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//wcs:ServiceMetadata/wcs:Extension[int:interpolationSupported='http://www.opengis.net/def/interpolation/OGC/1/linear'])",
                dom);
        assertXpathEvaluatesTo(
                "1",
                "count(//wcs:ServiceMetadata/wcs:Extension[int:interpolationSupported='http://www.opengis.net/def/interpolation/OGC/1/cubic'])",
                dom);
    }

    /**
     * Gets a TIFFField node with the given tag number. This is done by searching for a TIFFField
     * with attribute number whose value is the specified tag value.
     */
    protected IIOMetadataNode getTiffField(Node rootNode, final int tag) {
        Node node = rootNode.getFirstChild();
        if (node != null) {
            node = node.getFirstChild();
            for (; node != null; node = node.getNextSibling()) {
                Node number = node.getAttributes().getNamedItem(GeoTiffConstants.NUMBER_ATTRIBUTE);
                if (number != null && tag == Integer.parseInt(number.getNodeValue())) {
                    return (IIOMetadataNode) node;
                }
            }
        }
        return null;
    }

    protected void setInputLimit(int kbytes) {
        GeoServer gs = getGeoServer();
        WCSInfo info = gs.getService(WCSInfo.class);
        info.setMaxInputMemory(kbytes);
        gs.save(info);
    }

    protected void setOutputLimit(int kbytes) {
        GeoServer gs = getGeoServer();
        WCSInfo info = gs.getService(WCSInfo.class);
        info.setMaxOutputMemory(kbytes);
        gs.save(info);
    }

    /**
     * Compares the envelopes of two coverages for equality using the smallest scale factor of their
     * "grid to world" transform as the tolerance.
     *
     * @param expected The coverage having the expected envelope.
     * @param actual The coverage having the actual envelope.
     */
    protected static void assertEnvelopeEquals(Coverage expected, Coverage actual) {
        final double scaleA = getScale(expected);
        final double scaleB = getScale(actual);

        assertEnvelopeEquals(
                (GeneralEnvelope) expected.getEnvelope(),
                scaleA,
                (GeneralEnvelope) actual.getEnvelope(),
                scaleB);
    }

    protected static void assertEnvelopeEquals(
            GeneralEnvelope expected,
            double scaleExpected,
            GeneralEnvelope actual,
            double scaleActual) {
        final double tolerance;
        if (scaleExpected <= scaleActual) {
            tolerance = scaleExpected * 1E-1;
        } else if (!Double.isNaN(scaleActual)) {
            tolerance = scaleActual * 1E-1;
        } else {
            tolerance = EPS;
        }
        assertTrue(expected.equals(actual, tolerance, false));
    }

    /**
     * Returns the "Sample to geophysics" transform as an affine transform, or {@code null} if none.
     * Note that the returned instance may be an immutable one, not necessarly the default Java2D
     * implementation.
     *
     * @param coverage The coverage for which to get the "grid to CRS" affine transform.
     * @return The "grid to CRS" affine transform of the given coverage, or {@code null} if none or
     *     if the transform is not affine.
     */
    protected static AffineTransform getAffineTransform(final Coverage coverage) {
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
     * @param coverage The coverage for which to get the "grid to CRS" scale, or {@code null}.
     * @return The "grid to CRS" scale, or {@code NaN} if none or if the transform is not affine.
     */
    protected static double getScale(final Coverage coverage) {
        final AffineTransform gridToCRS = getAffineTransform(coverage);
        return (gridToCRS != null) ? XAffineTransform.getScale(gridToCRS) : Double.NaN;
    }

    /** Parses a multipart message from the response */
    protected Multipart getMultipart(MockHttpServletResponse response)
            throws MessagingException, IOException {
        MimeMessage body = new MimeMessage((Session) null, getBinaryInputStream(response));
        Multipart multipart = (Multipart) body.getContent();
        return multipart;
    }

    /** Configures the specified dimension for a coverage */
    protected void setupRasterDimension(
            String coverageName,
            String metadataKey,
            DimensionPresentation presentation,
            Double resolution) {
        CoverageInfo info = getCatalog().getCoverageByName(coverageName);
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        if (resolution != null) {
            di.setResolution(new BigDecimal(resolution));
        }
        info.getMetadata().put(metadataKey, di);
        getCatalog().save(info);
    }

    /** Configures the specified dimension for a coverage */
    protected void setupRasterDimension(
            String coverageName,
            String metadataKey,
            DimensionPresentation presentation,
            Double resolution,
            String unitSymbol) {
        CoverageInfo info = getCatalog().getCoverageByName(coverageName);
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(presentation);
        if (resolution != null) {
            di.setResolution(new BigDecimal(resolution));
        }
        if (unitSymbol != null) {
            di.setUnitSymbol(unitSymbol);
        }
        info.getMetadata().put(metadataKey, di);
        getCatalog().save(info);
    }

    /** Clears dimension information from the specified coverage */
    protected void clearDimensions(String coverageName) {
        CoverageInfo info = getCatalog().getCoverageByName(coverageName);
        info.getMetadata().remove(ResourceInfo.TIME);
        info.getMetadata().remove(ResourceInfo.ELEVATION);
        getCatalog().save(info);
    }
}
