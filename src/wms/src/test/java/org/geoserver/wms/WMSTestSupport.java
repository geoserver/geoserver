/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static junit.framework.TestCase.fail;
import static org.geoserver.data.test.MockData.WORLD;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.*;

import java.awt.Color;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Panel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.dom.DOMSource;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerGroupInfo.Mode;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resources;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.FeatureSource;
import org.geotools.map.FeatureLayer;
import org.geotools.map.GridCoverageLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.Style;
import org.geotools.xml.transform.TransformerBase;
import org.geotools.xsd.Configuration;
import org.geotools.xsd.Parser;
import org.junit.Assert;
import org.locationtech.jts.geom.Envelope;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXParseException;

/**
 * Base support class for wms tests.
 *
 * <p>Deriving from this test class provides the test case with preconfigured geoserver and wms
 * objects.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public abstract class WMSTestSupport extends GeoServerSystemTestSupport {

    protected static final String NATURE_GROUP = "nature";

    protected static final String CONTAINER_GROUP = "containerGroup";

    protected static final String OPAQUE_GROUP = "opaqueGroup";

    protected static final int SHOW_TIMEOUT = 2000;

    protected static final boolean INTERACTIVE = false;

    protected static final Color BG_COLOR = Color.white;

    protected static final Color COLOR_PLACES_GRAY = new Color(170, 170, 170);
    protected static final Color COLOR_LAKES_BLUE = new Color(64, 64, 192);
    /** @return The global wms singleton from the application context. */
    protected WMS getWMS() {
        WMS wms = (WMS) applicationContext.getBean("wms");
        // WMS wms = new WMS(getGeoServer());
        // wms.setApplicationContext(applicationContext);
        return wms;
    }

    /** @return The global web map service singleton from the application context. */
    protected WebMapService getWebMapService() {
        return (WebMapService) applicationContext.getBean("webMapService");
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("xlink", "http://www.w3.org/1999/xlink");
        namespaces.put("xsi", "http://www.w3.org/2001/XMLSchema-instance");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("wcs", "http://www.opengis.net/wcs/1.1.1");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sf", "http://cite.opengeospatial.org/gmlsf");
        namespaces.put("kml", "http://www.opengis.net/kml/2.2");

        testData.registerNamespaces(namespaces);
        registerNamespaces(namespaces);
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        // Add a raster layer
        testData.setUpRasterLayer(WORLD, "world.tiff", null, null, TestData.class);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // setup a layer group
        Catalog catalog = getCatalog();
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        if (lakes != null && forests != null) {
            group.setName(NATURE_GROUP);
            group.getLayers().add(lakes);
            group.getLayers().add(forests);
            group.getStyles().add(null);
            group.getStyles().add(null);
            CatalogBuilder cb = new CatalogBuilder(catalog);
            cb.calculateLayerGroupBounds(group);
            catalog.add(group);
        }
        testData.addStyle("default", "Default.sld", MockData.class, catalog);
        // "default", MockData.class.getResource("Default.sld")

        // create a group containing the other group
        LayerGroupInfo containerGroup = catalog.getFactory().createLayerGroup();
        LayerGroupInfo nature = catalog.getLayerGroupByName(NATURE_GROUP);
        if (nature != null) {
            containerGroup.setName(CONTAINER_GROUP);
            containerGroup.setMode(Mode.CONTAINER);
            containerGroup.getLayers().add(nature);
            containerGroup.getStyles().add(null);
            CatalogBuilder cb = new CatalogBuilder(catalog);
            cb.calculateLayerGroupBounds(containerGroup);
            catalog.add(containerGroup);
        }
    }

    protected void setupOpaqueGroup(Catalog catalog) throws Exception {
        // setup an opaque group too
        LayerGroupInfo opaqueGroup = catalog.getFactory().createLayerGroup();
        LayerInfo roadSegments = catalog.getLayerByName(getLayerId(MockData.ROAD_SEGMENTS));
        LayerInfo neatline = catalog.getLayerByName(getLayerId(MockData.MAP_NEATLINE));
        if (roadSegments != null && neatline != null) {
            opaqueGroup.setName(OPAQUE_GROUP);
            opaqueGroup.setMode(Mode.OPAQUE_CONTAINER);
            opaqueGroup.getLayers().add(roadSegments);
            opaqueGroup.getLayers().add(neatline);
            opaqueGroup.getStyles().add(null);
            opaqueGroup.getStyles().add(null);
            CatalogBuilder cb = new CatalogBuilder(catalog);
            cb.calculateLayerGroupBounds(opaqueGroup);
            catalog.add(opaqueGroup);
        }
    }

    /** subclass hook to register additional namespaces. */
    protected void registerNamespaces(Map<String, String> namespaces) {}

    /**
     * Convenience method for subclasses to create a map layer from a layer name.
     *
     * <p>The map layer is created with the default style for the layer.
     *
     * @param layerName The name of the layer.
     * @return A new map layer.
     */
    protected Layer createMapLayer(QName layerName) throws IOException {
        return createMapLayer(layerName, null);
    }

    /**
     * Convenience method for subclasses to create a map layer from a layer name and a style name.
     *
     * <p>The map layer is created with the default style for the layer.
     *
     * @param layerName The name of the layer.
     * @param a style in the catalog (or null if you want to use the default style)
     * @return A new map layer.
     */
    protected Layer createMapLayer(QName layerName, String styleName) throws IOException {
        Catalog catalog = getCatalog();

        LayerInfo layerInfo = catalog.getLayerByName(layerName.getLocalPart());
        Style style = layerInfo.getDefaultStyle().getStyle();
        if (styleName != null) {
            style = catalog.getStyleByName(styleName).getStyle();
        }

        FeatureTypeInfo info =
                catalog.getFeatureTypeByName(layerName.getPrefix(), layerName.getLocalPart());
        Layer layer = null;
        if (info != null) {
            FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
            featureSource = info.getFeatureSource(null, null);

            layer = new FeatureLayer(featureSource, style);
        } else {
            // try a coverage
            CoverageInfo cinfo =
                    catalog.getCoverageByName(
                            layerName.getNamespaceURI(), layerName.getLocalPart());
            GridCoverage2D cov = (GridCoverage2D) cinfo.getGridCoverage(null, null);

            layer = new GridCoverageLayer(cov, style);
        }

        if (layer == null) {
            throw new IllegalArgumentException("Could not find layer for " + layerName);
        }

        layer.setTitle(layerInfo.getTitle());
        return layer;
    }

    /** Calls through to {@link #createGetMapRequest(QName[])}. */
    protected GetMapRequest createGetMapRequest(QName layerName) {
        return createGetMapRequest(new QName[] {layerName});
    }

    /**
     * Convenience method for subclasses to create a new GetMapRequest object.
     *
     * <p>The returned object has the following properties:
     *
     * <ul>
     *   <li>styles set to default styles for layers specified
     *   <li>bbox set to (-180,-90,180,180 )
     *   <li>crs set to epsg:4326
     * </ul>
     *
     * Caller must set additional parameters of request as need be.
     *
     * @param The layer names of the request.
     * @return A new GetMapRequest object.
     */
    protected GetMapRequest createGetMapRequest(QName[] layerNames) {
        GetMapRequest request = new GetMapRequest();
        request.setBaseUrl("http://localhost:8080/geoserver");

        List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>(layerNames.length);
        List styles = new ArrayList();

        for (int i = 0; i < layerNames.length; i++) {
            LayerInfo layerInfo = getCatalog().getLayerByName(layerNames[i].getLocalPart());
            try {
                styles.add(layerInfo.getDefaultStyle().getStyle());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            layers.add(new MapLayerInfo(layerInfo));
        }

        request.setLayers(layers);
        request.setStyles(styles);
        request.setBbox(new Envelope(-180, -90, 180, 90));
        request.setCrs(DefaultGeographicCRS.WGS84);
        request.setSRS("EPSG:4326");
        request.setRawKvp(new HashMap());
        return request;
    }

    /**
     * Asserts that the image is not blank, in the sense that there must be pixels different from
     * the passed background color.
     *
     * @param testName the name of the test to throw meaningfull messages if something goes wrong
     * @param image the imgage to check it is not "blank"
     * @param bgColor the background color for which differing pixels are looked for
     */
    protected void assertNotBlank(String testName, BufferedImage image, Color bgColor) {
        int pixelsDiffer = countNonBlankPixels(testName, image, bgColor);
        assertTrue(testName + " image is comlpetely blank", 0 < pixelsDiffer);
    }

    /**
     * Asserts that the image is blank, in the sense that all pixels will be equal to the background
     * color
     *
     * @param testName the name of the test to throw meaningful messages if something goes wrong
     * @param image the image to check it is not "blank"
     * @param bgColor the background color for which differing pixels are looked for
     */
    protected void assertBlank(String testName, BufferedImage image, Color bgColor) {
        int pixelsDiffer = countNonBlankPixels(testName, image, bgColor);
        assertEquals(testName + " image is completely blank", 0, pixelsDiffer);
    }

    /** Counts the number of non black pixels */
    protected int countNonBlankPixels(String testName, BufferedImage image, Color bgColor) {
        int pixelsDiffer = 0;

        for (int y = 0; y < image.getHeight(); y++) {
            for (int x = 0; x < image.getWidth(); x++) {
                if (image.getRGB(x, y) != bgColor.getRGB()) {
                    ++pixelsDiffer;
                }
            }
        }

        LOGGER.fine(
                testName
                        + ": pixel count="
                        + (image.getWidth() * image.getHeight())
                        + " non bg pixels: "
                        + pixelsDiffer);
        return pixelsDiffer;
    }

    /**
     * Utility method to run the transformation on tr with the provided request and returns the
     * result as a DOM.
     *
     * <p>Parsing the response is done in a namespace aware way.
     *
     * @param req , the Object to run the xml transformation against with {@code tr}, usually an
     *     instance of a {@link Request} subclass
     * @param tr , the transformer to run the transformation with and produce the result as a DOM
     */
    public static Document transform(Object req, TransformerBase tr) throws Exception {
        return transform(req, tr, true);
    }

    /**
     * Utility method to run the transformation on tr with the provided request and returns the
     * result as a DOM
     *
     * @param req , the Object to run the xml transformation against with {@code tr}, usually an
     *     instance of a {@link Request} subclass
     * @param tr , the transformer to run the transformation with and produce the result as a DOM
     * @param namespaceAware whether to use a namespace aware parser for the response or not
     */
    public static Document transform(Object req, TransformerBase tr, boolean namespaceAware)
            throws Exception {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        tr.transform(req, out);

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(namespaceAware);

        DocumentBuilder db = dbf.newDocumentBuilder();

        /**
         * Resolves everything to an empty xml document, useful for skipping errors due to missing
         * dtds and the like
         *
         * @author Andrea Aime - TOPP
         */
        class EmptyResolver implements org.xml.sax.EntityResolver {
            public InputSource resolveEntity(String publicId, String systemId)
                    throws org.xml.sax.SAXException, IOException {
                StringReader reader =
                        new StringReader("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
                InputSource source = new InputSource(reader);
                source.setPublicId(publicId);
                source.setSystemId(systemId);

                return source;
            }
        }
        db.setEntityResolver(new EmptyResolver());

        // System.out.println(out.toString());

        Document doc = db.parse(new ByteArrayInputStream(out.toByteArray()));
        return doc;
    }

    /** Checks that the image generated by the map producer is not blank. */
    protected void assertNotBlank(String testName, BufferedImage image) {
        assertNotBlank(testName, image, BG_COLOR);
        showImage(testName, image);
    }

    /** Checks that the image generated by the map producer is not blank. */
    protected void assertBlank(String testName, BufferedImage image) {
        assertBlank(testName, image, BG_COLOR);
        showImage(testName, image);
    }

    public static void showImage(String frameName, final BufferedImage image) {
        showImage(frameName, SHOW_TIMEOUT, image);
    }

    /** Shows <code>image</code> in a Frame. */
    public static void showImage(String frameName, long timeOut, final BufferedImage image) {
        int width = image.getWidth();
        int height = image.getHeight();

        if (((System.getProperty("java.awt.headless") == null)
                        || !System.getProperty("java.awt.headless").equals("true"))
                && INTERACTIVE) {
            Frame frame = new Frame(frameName);
            frame.addWindowListener(
                    new WindowAdapter() {
                        public void windowClosing(WindowEvent e) {
                            e.getWindow().dispose();
                        }
                    });

            Panel p = new Panel(null) { // no layout manager so it respects
                        // setSize
                        public void paint(Graphics g) {
                            g.drawImage(image, 0, 0, this);
                        }
                    };

            frame.add(p);
            p.setSize(width, height);
            frame.pack();
            frame.setVisible(true);

            try {
                Thread.sleep(timeOut);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            frame.dispose();
        }
    }

    /**
     * Performs some checks on an image response assuming the image is a png.
     *
     * @see #checkImage(MockHttpServletResponse, String)
     */
    protected void checkImage(MockHttpServletResponse response) {
        checkImage(response, "image/png", -1, -1);
    }

    /**
     * Performs some checks on an image response such as the mime type and attempts to read the
     * actual image into a buffered image.
     */
    protected void checkImage(
            MockHttpServletResponse response, String mimeType, int width, int height) {
        try {
            if (response.getContentType().contains("text")) {
                assertEquals(response.getContentAsString(), mimeType, response.getContentType());
            } else {
                assertEquals(mimeType, response.getContentType());
            }
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        try {
            BufferedImage image = ImageIO.read(getBinaryInputStream(response));
            assertNotNull(image);
            if (width > 0) {
                assertEquals(width, image.getWidth());
            }
            if (height > 0) {
                assertEquals(height, image.getHeight());
            }
        } catch (Throwable t) {
            t.printStackTrace();
            fail("Could not read image returned from GetMap:" + t.getLocalizedMessage());
        }
    }

    /**
     * Sets up a template in a feature type directory.
     *
     * @param featureTypeName The name of the feature type.
     * @param template The name of the template.
     * @param body The content of the template.
     */
    protected void setupTemplate(QName featureTypeName, String template, String body)
            throws IOException {

        ResourceInfo info =
                getCatalog().getResourceByName(toName(featureTypeName), ResourceInfo.class);
        Resources.copy(
                new ByteArrayInputStream(body.getBytes()), getDataDirectory().get(info), template);
    }

    protected LayerGroupInfo createLakesPlacesLayerGroup(
            Catalog catalog, LayerGroupInfo.Mode mode, LayerInfo rootLayer) throws Exception {
        return createLakesPlacesLayerGroup(catalog, "lakes_and_places", mode, rootLayer);
    }

    protected LayerGroupInfo createLakesPlacesLayerGroup(
            Catalog catalog, String name, LayerGroupInfo.Mode mode, LayerInfo rootLayer)
            throws Exception {
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo places = catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES));

        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);

        group.setMode(mode);
        if (rootLayer != null) {
            group.setRootLayer(rootLayer);
            group.setRootLayerStyle(rootLayer.getDefaultStyle());
        }

        group.getLayers().add(lakes);
        group.getLayers().add(places);
        group.getStyles().add(null);
        group.getStyles().add(null);

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.calculateLayerGroupBounds(group);

        catalog.add(group);

        return group;
    }

    protected int getRawTopLayerCount() {
        Catalog rawCatalog = (Catalog) GeoServerExtensions.bean("rawCatalog");
        List<LayerInfo> layers = new ArrayList<LayerInfo>(rawCatalog.getLayers());
        for (ListIterator<LayerInfo> it = layers.listIterator(); it.hasNext(); ) {
            LayerInfo next = it.next();
            if (!next.enabled() || next.getName().equals(MockData.GEOMETRYLESS.getLocalPart())) {
                it.remove();
            }
        }
        List<LayerGroupInfo> groups = rawCatalog.getLayerGroups();
        int opaqueDelta = groups.stream().anyMatch(lg -> OPAQUE_GROUP.equals(lg.getName())) ? 2 : 0;
        int expectedLayerCount =
                layers.size() + groups.size() - 1 /* nested layer group */ - opaqueDelta;
        return expectedLayerCount;
    }

    /** Validates a document against the */
    @SuppressWarnings("rawtypes")
    protected void checkWms13ValidationErrors(Document dom) throws Exception {
        Parser p =
                new Parser(
                        (Configuration)
                                Class.forName("org.geotools.wms.v1_3.WMSConfiguration")
                                        .getDeclaredConstructor()
                                        .newInstance());
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

    /**
     * Check that a number represent by a string is similar to the expected number A number is
     * considered similar to another if the difference between them is inferior or equal to the
     * provided precision.
     *
     * @param rawValue raw value that should contain a number
     * @param expected the expected numeric value
     * @param precision precision that should be used to compare the two values
     */
    public static void checkNumberSimilar(String rawValue, double expected, double precision) {
        // try to extract a double value
        assertThat(rawValue, is(notNullValue()));
        assertThat(rawValue.trim().isEmpty(), is(false));
        double value = 0;
        try {
            value = Double.parseDouble(rawValue);
        } catch (NumberFormatException exception) {
            Assert.fail(String.format("Value '%s' is not a number.", rawValue));
        }
        // compare the parsed double value with the expected one
        double difference = Math.abs(expected - value);
        assertThat(difference <= precision, is(true));
    }
}
