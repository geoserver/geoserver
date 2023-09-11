/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static java.util.Map.entry;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import javax.imageio.metadata.IIOMetadataNode;
import javax.mail.MessagingException;
import javax.mail.Multipart;
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
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.wcs.CoverageCleanerCallback;
import org.geoserver.wcs.WCSInfo;
import org.geotools.api.coverage.Coverage;
import org.geotools.api.coverage.grid.GridCoverage;
import org.geotools.api.coverage.grid.GridGeometry;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.coverage.grid.GridGeometry2D;
import org.geotools.coverage.grid.io.imageio.geotiff.GeoTiffConstants;
import org.geotools.geometry.GeneralBounds;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.operation.matrix.XAffineTransform;
import org.geotools.wcs.v2_0.WCSConfiguration;
import org.geotools.xsd.Parser;
import org.junit.After;
import org.locationtech.jts.geom.CoordinateXY;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
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

    List<GridCoverage> coverages = new ArrayList<>();

    protected static final String VERSION = WCS20Const.CUR_VERSION;

    protected static final QName UTM11 = new QName(MockData.WCS_URI, "utm11", MockData.WCS_PREFIX);

    protected static final QName NO_NATIVE_SRS =
            new QName(MockData.WCS_URI, "no_native_srs", MockData.WCS_PREFIX);

    /**
     * Small dataset that sits slightly across the dateline, enough to trigger the "across the
     * dateline" machinery
     */
    protected static final QName DATELINE_CROSS =
            new QName(MockData.WCS_URI, "dateline_cross", MockData.WCS_PREFIX);

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
                    Map.ofEntries(
                            entry("http://www.opengis.net/wcs/2.0", "/schemas/wcs/2.0/"),
                            entry("http://www.opengis.net/gmlcov/1.0", "/schemas/gmlcov/1.0/"),
                            entry("http://www.opengis.net/gml/3.2", "/schemas/gml/3.2.1/"),
                            entry("http://www.w3.org/1999/xlink", "/schemas/xlink/"),
                            entry("http://www.w3.org/XML/1998/namespace", "/schemas/xml/"),
                            entry(
                                    "http://www.isotc211.org/2005/gmd",
                                    "/schemas/iso/19139/20070417/gmd/"),
                            entry(
                                    "http://www.isotc211.org/2005/gco",
                                    "/schemas/iso/19139/20070417/gco/"),
                            entry(
                                    "http://www.isotc211.org/2005/gss",
                                    "/schemas/iso/19139/20070417/gss/"),
                            entry(
                                    "http://www.isotc211.org/2005/gts",
                                    "/schemas/iso/19139/20070417/gts/"),
                            entry(
                                    "http://www.isotc211.org/2005/gsr",
                                    "/schemas/iso/19139/20070417/gsr/"),
                            entry("http://www.opengis.net/swe/2.0", "/schemas/sweCommon/2.0/"),
                            entry("http://www.opengis.net/ows/2.0", "/schemas/ows/2.0/"),
                            entry("http://www.geoserver.org/wcsgs/2.0", "/schemas/wcs/2.0/"));

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
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        testData.setUpWcs10RasterLayers();
        testData.setUpWcs11RasterLayers();
        testData.setUpRasterLayer(UTM11, "/utm11-2.tiff", null, null, WCSTestSupport.class);
        testData.setUpRasterLayer(
                DATELINE_CROSS, "/datelinecross.tif", null, null, WCSTestSupport.class);
        testData.setupIAULayers(true, false);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {

        super.onSetUp(testData);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<>();
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

        // alter the native BBOX of one raster for at least a pixel to make sure the declared
        // bounds are used, but also fitted to the grid to avoid resampling.
        // Compared to the native bbox we are expanding by almost one pixel on both the west and
        // east sides, and shrinking by one pixel on both the north and south ones:
        //          1 pixel
        //             ↓
        //  1 pixel ←-   -→ 1 pixel
        //             ↑
        //          1 pixel
        CoverageInfo utm11 = getCatalog().getCoverageByName(getLayerId(UTM11));
        if (utm11 != null) {
            utm11.setNativeBoundingBox(
                    new ReferencedEnvelope(
                            440600.0, 471700.0, 3720700.0, 3751000.0, utm11.getNativeCRS()));
            getCatalog().save(utm11);
        }

        // not reprojected, but rotated
        CoverageInfo cad = getCatalog().getCoverageByName(getLayerId(MockData.ROTATED_CAD));
        if (cad != null) {
            cad.setNativeBoundingBox(
                    new ReferencedEnvelope(1402800, 1402900, 5000000, 5000100, cad.getNativeCRS()));
            getCatalog().save(cad);
        }

        // not reprojected, originally crossing the dateline
        CoverageInfo dateline = getCatalog().getCoverageByName(getLayerId(DATELINE_CROSS));
        if (dateline != null) {
            dateline.setNativeBoundingBox(
                    new ReferencedEnvelope(179.5, 180, -84.272, -82.217, dateline.getNativeCRS()));
            getCatalog().save(dateline);
        }

        // forcing envelope + reprojection (changes the envelope, grid to world)
        CoverageInfo usa = getCatalog().getCoverageByName(getLayerId(MockData.USA_WORLDIMG));
        if (usa != null) {
            usa.setSRS("EPSG:3857");
            usa.setProjectionPolicy(ProjectionPolicy.REPROJECT_TO_DECLARED);
            getCatalog().save(usa);
        }
    }

    @Override
    protected boolean isMemoryCleanRequired() {
        return IS_WINDOWS;
    }

    /** Validates a document against the */
    protected void checkValidationErrors(Document dom) throws Exception {
        Parser p = new Parser(new WCSConfiguration());
        p.setValidating(true);
        p.parse(new DOMSource(dom));

        if (!p.getValidationErrors().isEmpty()) {
            for (Exception exception : p.getValidationErrors()) {
                SAXParseException ex = (SAXParseException) exception;
                LOGGER.warning(
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

        // check that the bbox in the utm11 layer is reported as configured
        String utm11Bbox =
                "//wcs:Contents/wcs:CoverageSummary[wcs:CoverageId='wcs__utm11']/ows:BoundingBox";

        assertXpathCoordinate(
                new CoordinateXY(440562.0, 3720758.0), utm11Bbox + "/ows:LowerCorner", dom);
        assertXpathCoordinate(
                new CoordinateXY(471794.0, 3750966.0), utm11Bbox + "/ows:UpperCorner", dom);

        // check that the bbox in the cad layer is reported as configured
        String cadPath =
                "//wcs:Contents/wcs:CoverageSummary[wcs:CoverageId='wcs__RotatedCad']/ows:BoundingBox";
        assertXpathCoordinate(
                new CoordinateXY(1402800.0, 5000000.0), cadPath + "/ows:LowerCorner", dom);
        assertXpathCoordinate(
                new CoordinateXY(1402900.0, 5000100.0), cadPath + "/ows:UpperCorner", dom);

        // check that the bbox in the usa layer has been reprojected
        String usaPath =
                "//wcs:Contents/wcs:CoverageSummary[wcs:CoverageId='cdf__usa']/ows:BoundingBox";
        assertXpathCoordinate(
                new CoordinateXY(-1.457024062347863E7, 6199732.713729635),
                usaPath + "/ows:LowerCorner",
                dom);
        assertXpathCoordinate(
                new CoordinateXY(-1.3790593336628266E7, 7197101.83024677),
                usaPath + "/ows:UpperCorner",
                dom);

        // check the CRSs
        assertCRSReference(dom, "EPSG", "4326");
        assertCRSReference(dom, "EPSG", "32632");
        // custom GeoServer extensions
        assertCRSReference(dom, "EPSG", "900913");
        assertCRSReference(dom, "EPSG", "404000");
        // IAU codes (added in the classpath for tests only)
        assertCRSReference(dom, "IAU", "1000");

        // check the size of supported codes
        final Set<String> supportedCodes = getCodes("EPSG");
        supportedCodes.addAll(getCodes("IAU"));
        NodeList allCrsCodes = xpath.getMatchingNodes("//crs:crsSupported", dom);

        assertEquals(supportedCodes.size(), allCrsCodes.getLength());

        // check the viking raster is there with its CRS
        String vikingPath = "//wcs:Contents/wcs:CoverageSummary[wcs:CoverageId='iau__Viking']";
        assertXpathEvaluatesTo(
                "http://www.opengis.net/def/crs/IAU/0/49900",
                vikingPath + "/ows:BoundingBox/@crs",
                dom);
    }

    /**
     * Need to add prefixes here because some ids are duplicated amongst EPSG and IAU and to filter
     * out WGS84(DD) because it shows up in all authorities
     */
    private static Set<String> getCodes(String authority) {
        return CRS.getSupportedCodes(authority).stream()
                .filter(c -> !"WGS84(DD)".equals(c))
                .map(c -> "http://www.opengis.net/def/crs/" + authority + "/0/" + c)
                .collect(Collectors.toSet());
    }

    private static void assertCRSReference(Document dom, String authority, String code)
            throws XpathException {
        assertXpathExists(
                "//crs:crsSupported[text()='http://www.opengis.net/def/crs/"
                        + authority
                        + "/0/"
                        + code
                        + "']",
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
                (GeneralBounds) expected.getEnvelope(),
                scaleA,
                (GeneralBounds) actual.getEnvelope(),
                scaleB);
    }

    @SuppressWarnings("PMD.SimplifiableTestAssertion") // equality with tolerance
    protected static void assertEnvelopeEquals(
            GeneralBounds expected,
            double scaleExpected,
            GeneralBounds actual,
            double scaleActual) {
        final double tolerance;
        if (scaleExpected <= scaleActual) {
            tolerance = scaleExpected * 1E-1;
        } else if (!Double.isNaN(scaleActual)) {
            tolerance = scaleActual * 1E-1;
        } else {
            tolerance = EPS;
        }
        assertTrue(
                "The 2 envelopes aren't equal, expected " + expected + " but got " + actual,
                expected.equals(actual, tolerance, false));
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
                    gridToCRS = geometry.getGridToCRS();
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
        MimeMessage body = new MimeMessage(null, getBinaryInputStream(response));
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
            di.setResolution(BigDecimal.valueOf(resolution));
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
            di.setResolution(BigDecimal.valueOf(resolution));
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
