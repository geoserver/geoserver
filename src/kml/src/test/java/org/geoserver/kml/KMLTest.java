/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.kml;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.custommonkey.xmlunit.exceptions.XpathException;
import org.geoserver.catalog.*;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.platform.resource.Resources;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.*;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class KMLTest extends WMSTestSupport {

    public static QName BOULDER = new QName(MockData.SF_URI, "boulder", MockData.SF_PREFIX);
    private static final QName STORM_OBS =
            new QName(MockData.CITE_URI, "storm_obs", MockData.CITE_PREFIX);
    private static TimeZone oldTimeZone;

    XpathEngine xpath;

    @Before
    public void setUpXpath() {
        xpath = XMLUnit.newXpathEngine();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
    }

    @BeforeClass
    public static void setTimeZone() {
        // System.setProperty("user.timezone", "UTC");
        oldTimeZone = TimeZone.getDefault();
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @AfterClass
    public static void clearTimeZone() {
        // System.clearProperty("user.timezone");
        TimeZone.setDefault(oldTimeZone);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("notthere", "notthere.sld", getClass(), catalog);
        testData.addStyle("scaleRange", "scaleRange.sld", getClass(), catalog);
        testData.addStyle("outputMode", "outputMode.sld", getClass(), catalog);
        testData.addVectorLayer(
                STORM_OBS, Collections.EMPTY_MAP, "storm_obs.properties", getClass(), catalog);

        Map<SystemTestData.LayerProperty, Object> properties =
                new HashMap<SystemTestData.LayerProperty, Object>();
        properties.put(
                LayerProperty.LATLON_ENVELOPE,
                new ReferencedEnvelope(-105.336, -105.112, 39.9, 40.116, CRS.decode("EPSG:4326")));
        properties.put(
                LayerProperty.ENVELOPE,
                new ReferencedEnvelope(
                        3045967, 3108482, 1206627, 1285209, CRS.decode("EPSG:2876")));
        properties.put(LayerProperty.SRS, 2876);
        testData.addVectorLayer(BOULDER, properties, "boulder.properties", getClass(), catalog);

        String points = MockData.POINTS.getLocalPart();
        String lines = MockData.LINES.getLocalPart();
        String polygons = MockData.POLYGONS.getLocalPart();

        setNativeBox(catalog, points);
        setNativeBox(catalog, lines);
        setNativeBox(catalog, polygons);

        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("layerGroup");
        lg.getLayers().add(catalog.getLayerByName(points));
        lg.getStyles().add(catalog.getStyleByName("point"));
        lg.getLayers().add(catalog.getLayerByName(lines));
        lg.getStyles().add(catalog.getStyleByName("line"));
        lg.getLayers().add(catalog.getLayerByName(polygons));
        lg.getStyles().add(catalog.getStyleByName("polygon"));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg);
        catalog.add(lg);
    }

    public void setNativeBox(Catalog catalog, String name) throws Exception {
        FeatureTypeInfo fti = catalog.getFeatureTypeByName(name);
        fti.setNativeBoundingBox(fti.getFeatureSource(null, null).getBounds());
        fti.setLatLonBoundingBox(
                new ReferencedEnvelope(fti.getNativeBoundingBox(), DefaultGeographicCRS.WGS84));
        catalog.save(fti);
    }

    @Test
    public void testLayerGroupWithPoints() throws Exception {

        Catalog cat = getCatalog();
        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");

        List<PublishedInfo> layers = lgi.getLayers();
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + lgi.getName()
                                + "&styles="
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");
        //         print(doc, System.out);

        assertEquals(layers.size(), doc.getElementsByTagName("Folder").getLength());
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/kml/icon/point?0.0.0=",
                "//kml:Folder[1]/kml:Placemark/kml:Style/kml:IconStyle/kml:Icon/kml:href",
                doc);
        assertXpathEvaluatesTo(
                "http://icons.opengeo.org/markers/icon-line.1.png",
                "//kml:Folder[2]/kml:Placemark/kml:Style/kml:IconStyle/kml:Icon/kml:href",
                doc);
    }

    @Test
    public void testVector() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");

        assertEquals(
                getFeatureSource(MockData.BASIC_POLYGONS).getFeatures().size(),
                doc.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testReprojectedVector() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(BOULDER)
                                + "&styles="
                                + "&height=1024&width=1024&bbox=3045967,1206627,3108482,1285209&srs=EPSG:2876");
        // print(doc);

        assertEquals(1, doc.getElementsByTagName("Placemark").getLength());

        assertEquals(
                -105.2243,
                Double.parseDouble(xpath.evaluate("//kml:Document/kml:LookAt/kml:longitude", doc)),
                1E-4);
        assertEquals(
                40.0081,
                Double.parseDouble(xpath.evaluate("//kml:Document/kml:LookAt/kml:latitude", doc)),
                1E-4);
    }

    @Test
    public void testVectorScaleRange() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&styles=scaleRange&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");

        assertEquals(
                getFeatureSource(MockData.BASIC_POLYGONS).getFeatures().size(),
                doc.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testVectorWithFeatureId() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&featureid=BasicPolygons.1107531493643");

        assertEquals(1, doc.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testVectorWithSortByAscending() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.ROAD_SEGMENTS)
                                + "&styles=&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&sortBy=FID A");

        // print(doc);
        assertXpathEvaluatesTo("5", "count(//kml:Placemark)", doc);

        // FID is mapped to the Placemark id attribute in the KML document
        // verify that the features in the KML are sorted by ascending FID
        assertXpathEvaluatesTo("RoadSegments.1107532045088", "//kml:Placemark[1]/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045089", "//kml:Placemark[2]/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045089", "//kml:Placemark[3]/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045090", "//kml:Placemark[4]/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045091", "//kml:Placemark[5]/@id", doc);
    }

    @Test
    public void testVectorWithSortByDescending() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.ROAD_SEGMENTS)
                                + "&styles=&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&sortBy=FID D");

        // print(doc);
        assertXpathEvaluatesTo("5", "count(//kml:Placemark)", doc);

        // FID is mapped to the Placemark id attribute in the KML document
        // verify that the features in the KML are sorted by descending FID
        assertXpathEvaluatesTo("RoadSegments.1107532045091", "//kml:Placemark[1]/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045090", "//kml:Placemark[2]/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045089", "//kml:Placemark[3]/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045089", "//kml:Placemark[4]/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045088", "//kml:Placemark[5]/@id", doc);
    }

    @Test
    public void testBasicVector() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.ROAD_SEGMENTS)
                                + "&styles=&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&featureId=RoadSegments.1107532045088");

        // print(doc);
        assertXpathEvaluatesTo("1", "count(//kml:Placemark)", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045088", "//kml:Placemark/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045088", "//kml:Placemark/kml:name", doc);
        String expectedDescription =
                String.format(
                        "<h4>RoadSegments</h4>%n"
                                + "\n"
                                + "<ul class=\"textattributes\">\n"
                                + "  \n"
                                + "  <li><strong><span class=\"atr-name\">FID</span>:</strong> <span class=\"atr-value\">102</span></li>\n"
                                + "  <li><strong><span class=\"atr-name\">NAME</span>:</strong> <span class=\"atr-value\">Route 5</span></li>\n"
                                + "</ul>\n");
        String actualDescription = xpath.evaluate("//kml:Placemark/kml:description", doc);
        assertEqualsIgnoreNewLineStyle(expectedDescription, actualDescription);
        // check look-at
        assertXpathEvaluatesTo(
                "-0.0020000000000095497", "//kml:Placemark/kml:LookAt/kml:longitude", doc);
        assertXpathEvaluatesTo(
                "5.000000003008154E-5", "//kml:Placemark/kml:LookAt/kml:latitude", doc);
        // check style
        assertXpathEvaluatesTo(
                "00ffffff", "//kml:Placemark/kml:Style/kml:IconStyle/kml:color", doc);
        assertXpathEvaluatesTo("0.4", "//kml:Placemark/kml:Style/kml:IconStyle/kml:scale", doc);
        assertXpathEvaluatesTo(
                "http://icons.opengeo.org/markers/icon-line.1.png",
                "//kml:Placemark/kml:Style/kml:IconStyle/kml:Icon/kml:href",
                doc);
        assertXpathEvaluatesTo(
                "00ffffff", "//kml:Placemark/kml:Style/kml:LabelStyle/kml:color", doc);
        assertXpathEvaluatesTo(
                "ff000000", "//kml:Placemark/kml:Style/kml:LineStyle/kml:color", doc);
        assertXpathEvaluatesTo("4.0", "//kml:Placemark/kml:Style/kml:LineStyle/kml:width", doc);
        // check geometry
        assertXpathEvaluatesTo(
                "-0.002000087662804264,4.997808429893395E-5",
                "//kml:Placemark/kml:MultiGeometry/kml:Point/kml:coordinates",
                doc);
        assertXpathEvaluatesTo(
                "-0.0042,-6.0E-4 -0.0032,-3.0E-4 -0.0026,-1.0E-4 -0.0014,2.0E-4 2.0E-4,7.0E-4",
                "//kml:Placemark/kml:MultiGeometry/kml:LineString/kml:coordinates",
                doc);
    }

    @Test
    public void testNoAttributes() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.ROAD_SEGMENTS)
                                + "&styles=&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&featureId=RoadSegments.1107532045088&kmattr=false");

        // print(doc);
        assertXpathEvaluatesTo("1", "count(//kml:Placemark)", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045088", "//kml:Placemark/@id", doc);
        // no name or description
        assertXpathEvaluatesTo("0", "count(//kml:Placemark/kml:name)", doc);
        assertXpathEvaluatesTo("0", "count(//kml:Placemark/kml:description)", doc);
        // check look-at
        assertXpathEvaluatesTo(
                "-0.0020000000000095497", "//kml:Placemark/kml:LookAt/kml:longitude", doc);
        assertXpathEvaluatesTo(
                "5.000000003008154E-5", "//kml:Placemark/kml:LookAt/kml:latitude", doc);
        // style does not need icon information
        assertXpathEvaluatesTo("0", "count(//kml:Placemark/kml:Style/kml:IconStyle)", doc);
        assertXpathEvaluatesTo("0", "count(//kml:Placemark/kml:Style/kml:LabelStyle)", doc);
        assertXpathEvaluatesTo(
                "ff000000", "//kml:Placemark/kml:Style/kml:LineStyle/kml:color", doc);
        assertXpathEvaluatesTo("4.0", "//kml:Placemark/kml:Style/kml:LineStyle/kml:width", doc);
        // check geometry
        assertXpathEvaluatesTo("0", "count(//kml:Placemark/kml:MultiGeometry)", doc);
        assertXpathEvaluatesTo(
                "-0.0042,-6.0E-4 -0.0032,-3.0E-4 -0.0026,-1.0E-4 -0.0014,2.0E-4 2.0E-4,7.0E-4",
                "//kml:Placemark/kml:LineString/kml:coordinates",
                doc);
    }

    @Test
    public void testTimeTemplate() throws Exception {
        FeatureTypeInfo ftInfo =
                getCatalog().getResourceByName(getLayerId(MockData.OTHER), FeatureTypeInfo.class);
        File resourceDir = Resources.directory(getDataDirectory().get(ftInfo));
        File templateFile = new File(resourceDir, "time.ftl");
        try {
            // create the time template

            FileUtils.writeStringToFile(templateFile, "${dates.value}", "UTF-8");

            Document doc =
                    getAsDOM(
                            "wms?request=getmap&service=wms&version=1.1.1"
                                    + "&format="
                                    + KMLMapOutputFormat.MIME_TYPE
                                    + "&layers="
                                    + getLayerId(MockData.OTHER)
                                    + "&styles=&height=1024&width=1024&bbox= -96.0000,0.0000,-90.0000,84.0000&srs=EPSG:4326");

            // print(doc);
            assertXpathEvaluatesTo("1", "count(//kml:Placemark)", doc);
            assertXpathEvaluatesTo(
                    "2002-12-02T00:00:00Z", "//kml:Placemark/kml:TimeStamp/kml:when", doc);
        } finally {
            assertTrue(templateFile.delete());
        }
    }

    @Test
    public void testTimeInvervalTemplate() throws Exception {
        FeatureTypeInfo ftInfo =
                getCatalog().getResourceByName(getLayerId(MockData.OTHER), FeatureTypeInfo.class);
        File resourceDir = Resources.directory(getDataDirectory().get(ftInfo));
        File templateFile = new File(resourceDir, "time.ftl");
        try {
            // create the time template
            FileUtils.writeStringToFile(templateFile, "${dates.value}||${dates.value}", "UTF-8");

            Document doc =
                    getAsDOM(
                            "wms?request=getmap&service=wms&version=1.1.1"
                                    + "&format="
                                    + KMLMapOutputFormat.MIME_TYPE
                                    + "&layers="
                                    + getLayerId(MockData.OTHER)
                                    + "&styles=&height=1024&width=1024&bbox= -96.0000,0.0000,-90.0000,84.0000&srs=EPSG:4326");

            // print(doc);
            assertXpathEvaluatesTo("1", "count(//kml:Placemark)", doc);
            assertXpathEvaluatesTo(
                    "2002-12-02T00:00:00Z", "//kml:Placemark/kml:TimeSpan/kml:begin", doc);
            assertXpathEvaluatesTo(
                    "2002-12-02T00:00:00Z", "//kml:Placemark/kml:TimeSpan/kml:end", doc);
        } finally {
            assertTrue(templateFile.delete());
        }
    }

    @Test
    public void testHeightTemplate() throws Exception {
        FeatureTypeInfo ftInfo =
                getCatalog().getResourceByName(getLayerId(MockData.OTHER), FeatureTypeInfo.class);
        File resourceDir = Resources.directory(getDataDirectory().get(ftInfo));
        File templateFile = new File(resourceDir, "height.ftl");
        try {
            // create the height template
            FileUtils.writeStringToFile(templateFile, "200", "UTF-8");

            Document doc =
                    getAsDOM(
                            "wms?request=getmap&service=wms&version=1.1.1"
                                    + "&format="
                                    + KMLMapOutputFormat.MIME_TYPE
                                    + "&layers="
                                    + getLayerId(MockData.OTHER)
                                    + "&styles=&height=1024&width=1024&bbox= -96.0000,0.0000,-90.0000,84.0000&srs=EPSG:4326");

            // coordinates are reprojected and we get the height at 200
            assertPointCoordinate(
                    doc,
                    "//kml:Placemark/kml:Point/kml:coordinates",
                    -92.99954926766114,
                    4.52401492058674,
                    200.0);
        } finally {
            assertTrue(templateFile.delete());
        }
    }

    /**
     * Checks that a point's coordinate in the kml doc matches the expected value
     *
     * @param doc The KML doc
     * @param path The xpath leading to the coordinate
     * @param expected The expected ordinate values
     */
    public static void assertPointCoordinate(Document doc, String path, double... expected)
            throws XpathException {
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String coordinates = xpath.evaluate(path, doc);
        assertThat(coordinates, not(isEmptyString()));
        double[] ordinates =
                Arrays.stream(coordinates.split("\\s*,\\s*"))
                        .mapToDouble(Double::parseDouble)
                        .toArray();
        assertEquals(expected.length, ordinates.length);
        for (int i = 0; i < expected.length; i++) {
            assertEquals(expected[i], ordinates[i], 1e-6);
        }
    }

    @Test
    public void testVectorWithRemoteLayer() throws Exception {
        if (!RemoteOWSTestSupport.isRemoteWFSStatesAvailable(LOGGER)) return;

        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers=topp:states"
                                + "&styles=Default"
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                                + "&remote_ows_type=wfs"
                                + "&remote_ows_url="
                                + RemoteOWSTestSupport.WFS_SERVER_URL
                                + "&cql_filter=PERSONS>20000000");
        // print(doc);

        assertEquals(1, doc.getElementsByTagName("Placemark").getLength());
    }

    // see GEOS-1948
    @Test
    public void testMissingGraphic() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BRIDGES)
                                + "&styles=notthere"
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");
        assertEquals(1, doc.getElementsByTagName("Placemark").getLength());
    }

    @Test
    public void testContentDisposition() throws Exception {
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMZMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + MockData.BASIC_POLYGONS.getPrefix()
                                + ":"
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");
        assertEquals(
                "attachment; filename=cite-BasicPolygons.kmz",
                resp.getHeader("Content-Disposition"));
    }

    @Test
    public void testEncodeTime() throws Exception {
        setupTemplate(STORM_OBS, "time.ftl", "${obs_datetime.value}");
        // AA: for the life of me I cannot make xpath work against this output, not sure why, so
        // going
        // to test with strings instead...
        String doc =
                getAsString(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(STORM_OBS)
                                + "&styles=&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&featureId=storm_obs.1321870537475");
        assertTrue(doc.contains("<when>1994-07-0"));
    }

    @Test
    public void testKmltitleFormatOption() throws Exception {
        final String kmlRequest =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + getLayerId(MockData.BRIDGES)
                        + "&styles=notthere"
                        + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                        + "&format_options=kmltitle:myCustomLayerTitle";

        Document doc = getAsDOM(kmlRequest);
        print(doc);
        assertEquals(
                "name",
                doc.getElementsByTagName("Document")
                        .item(0)
                        .getFirstChild()
                        .getNextSibling()
                        .getLocalName());
        assertEquals(
                "myCustomLayerTitle",
                doc.getElementsByTagName("Document")
                        .item(0)
                        .getFirstChild()
                        .getNextSibling()
                        .getTextContent());
    }

    @Test
    public void testKmltitleFormatOptionWithMultipleLayers() throws Exception {
        final String kmlRequest =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + getLayerId(MockData.BRIDGES)
                        + ","
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "&styles=notthere"
                        + ","
                        + MockData.BASIC_POLYGONS.getLocalPart()
                        + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326"
                        + "&format_options=kmltitle:myCustomLayerTitle";

        Document doc = getAsDOM(kmlRequest);
        print(doc);
        assertEquals(
                "name",
                doc.getElementsByTagName("Document")
                        .item(0)
                        .getFirstChild()
                        .getNextSibling()
                        .getLocalName());
        assertEquals(1, doc.getElementsByTagName("Document").getLength());
        assertEquals(2, doc.getElementsByTagName("Folder").getLength());
        assertEquals(
                "myCustomLayerTitle",
                doc.getElementsByTagName("Document")
                        .item(0)
                        .getFirstChild()
                        .getNextSibling()
                        .getTextContent());
        assertXpathEvaluatesTo("cite:Bridges", "//kml:Folder[1]/kml:name", doc);
        assertXpathEvaluatesTo("cite:BasicPolygons", "//kml:Folder[2]/kml:name", doc);
    }

    @Test
    public void testRelativeLinks() throws Exception {
        final String kmlRequest =
                "wms?request=getmap&service=wms&version=1.1.1"
                        + "&format="
                        + KMLMapOutputFormat.MIME_TYPE
                        + "&layers="
                        + getLayerId(MockData.BASIC_POLYGONS)
                        + "&styles="
                        + MockData.BASIC_POLYGONS.getLocalPart()
                        + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&format_options=rellinks:true";

        // first page
        Document dom = getAsDOM(kmlRequest + "&startIndex=0&maxFeatures=1");
        // print(dom);
        // only one link, the "next" one
        assertXpathEvaluatesTo("1", "count(//kml:Folder/kml:NetworkLink)", dom);
        assertXpathEvaluatesTo("next", "//kml:Folder/kml:NetworkLink/@id", dom);
        assertXpathEvaluatesTo("Next page", "//kml:Folder/kml:NetworkLink/kml:description", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/cite/BasicPolygons.kml?startindex=1&maxfeatures=1",
                "//kml:Folder/kml:NetworkLink/kml:Link/kml:href",
                dom);

        // mid page
        dom = getAsDOM(kmlRequest + "&startIndex=1&maxFeatures=1");
        // print(dom);
        // only one link, the "next" one
        assertXpathEvaluatesTo("2", "count(//kml:Folder/kml:NetworkLink)", dom);
        assertXpathEvaluatesTo("prev", "//kml:Folder/kml:NetworkLink[1]/@id", dom);
        assertXpathEvaluatesTo(
                "Previous page", "//kml:Folder/kml:NetworkLink[1]/kml:description", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/cite/BasicPolygons.kml?startindex=0&maxfeatures=1",
                "//kml:Folder/kml:NetworkLink[1]/kml:Link/kml:href",
                dom);
        assertXpathEvaluatesTo("next", "//kml:Folder/kml:NetworkLink[2]/@id", dom);
        assertXpathEvaluatesTo("Next page", "//kml:Folder/kml:NetworkLink[2]/kml:description", dom);
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/rest/cite/BasicPolygons.kml?startindex=2&maxfeatures=1",
                "//kml:Folder/kml:NetworkLink[2]/kml:Link/kml:href",
                dom);

        // the last page is same as the mid one, as the code does not have enough context to know
        // it's hitting
        // the last one squarely
    }

    @Test
    public void testForceGroundOverlay() throws Exception {
        Document dom =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&format_options=mode:refresh;kmscore:0;autofit:true");
        // print(dom);

        assertXpathEvaluatesTo("0", "count(//kml:Placemark)", dom);
        assertXpathEvaluatesTo("1", "count(//kml:GroundOverlay)", dom);
        String pngOverlay =
                "http://localhost:8080/geoserver/wms?service=wms&request=GetMap&version=1.1.1&format=image%2Fpng&layers=cite%3ABasicPolygons&styles=BasicPolygons&height=512&width=1024&transparent=true&bbox=-180.0%2C-90.0%2C180.0%2C90.0&srs=EPSG%3A4326&format_options=AUTOFIT%3Atrue%3BKMSCORE%3A0%3BMODE%3Arefresh";
        assertXpathEvaluatesTo(pngOverlay, "//kml:GroundOverlay/kml:Icon/kml:href", dom);
        assertXpathEvaluatesTo("-180.0", "//kml:GroundOverlay/kml:LatLonBox/kml:west", dom);
        assertXpathEvaluatesTo("180.0", "//kml:GroundOverlay/kml:LatLonBox/kml:east", dom);
        assertXpathEvaluatesTo("90.0", "//kml:GroundOverlay/kml:LatLonBox/kml:north", dom);
        assertXpathEvaluatesTo("-90.0", "//kml:GroundOverlay/kml:LatLonBox/kml:south", dom);
    }

    @Test
    public void testOutputModeVector() throws Exception {
        Document dom =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + "&styles=outputMode"
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&format_options=kmscore:100&featureid=BasicPolygons.1107531493644");
        print(dom);

        // we got a ground overlay
        assertXpathEvaluatesTo("0", "count(//kml:GroundOverlay)", dom);
        assertXpathEvaluatesTo("1", "count(//kml:Placemark)", dom);
        // the point style got activated
        assertXpathEvaluatesTo(
                "http://localhost:8080/geoserver/styles/bridge.png",
                "//kml:Placemark/kml:Style/kml:IconStyle/kml:Icon/kml:href",
                dom);
        // and we extracted the centroid
        assertXpathEvaluatesTo(
                "0.5,3.5", "//kml:Placemark/kml:MultiGeometry/kml:Point/kml:coordinates", dom);
    }

    @Test
    public void testRasterLayer() throws Exception {
        Document dom =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.TASMANIA_DEM)
                                + "&styles=&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326");
        // print(dom);

        assertXpathEvaluatesTo("0", "count(//kml:Placemark)", dom);
        assertXpathEvaluatesTo("1", "count(//kml:GroundOverlay)", dom);
        String pngOverlay =
                "http://localhost:8080/geoserver/wms?service=wms&request=GetMap&version=1.1.1&format=image%2Fpng&layers=wcs%3ADEM&styles=raster&height=1024&width=1024&transparent=true&bbox=-180.0%2C-90.0%2C180.0%2C90.0&srs=EPSG%3A4326";
        assertXpathEvaluatesTo(pngOverlay, "//kml:GroundOverlay/kml:Icon/kml:href", dom);
    }

    @Test
    public void testKMZMixed() throws Exception {
        // force vector layers to be vector dumps (kmscore 100)
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMZMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.BASIC_POLYGONS)
                                + ","
                                + getLayerId(MockData.WORLD)
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + ","
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&format_options=kmscore:100");

        // check the contents of the zip file
        assertEquals(KMZMapOutputFormat.MIME_TYPE, response.getContentType());
        ByteArrayInputStream bis = getBinaryInputStream(response);
        ZipInputStream zis = new ZipInputStream(bis);

        // first entry, the kml document itself
        ZipEntry entry = zis.getNextEntry();
        assertEquals("wms.kml", entry.getName());
        // we need to clone the input stream, as dom(is) closes the stream
        byte[] data = IOUtils.toByteArray(zis);
        Document dom = dom(new ByteArrayInputStream(data));
        // print(dom);
        // we have the placemarks in the first folder (vector), and no ground overlays
        assertXpathEvaluatesTo("3", "count(//kml:Folder[1]/kml:Placemark)", dom);
        assertXpathEvaluatesTo("0", "count(//kml:Folder[1]/kml:GroundOverlay)", dom);
        // we have only the ground overlay in the second folder
        assertXpathEvaluatesTo("0", "count(//kml:Folder[2]/kml:Placemark)", dom);
        assertXpathEvaluatesTo("1", "count(//kml:Folder[2]/kml:GroundOverlay)", dom);
        assertXpathEvaluatesTo(
                "images/layers_1.png", "//kml:Folder[2]/kml:GroundOverlay/kml:Icon/kml:href", dom);
        zis.closeEntry();

        // the images folder
        entry = zis.getNextEntry();
        assertEquals("images/", entry.getName());
        zis.closeEntry();

        // the ground overlay for the raster layer
        entry = zis.getNextEntry();
        assertEquals("images/layers_1.png", entry.getName());
        zis.closeEntry();
        assertNull(zis.getNextEntry());
    }

    @Test
    public void testProjectedGroundOverlayWithPlacemarks() throws Exception {
        // Tests GEOS-7369, the combination of kmscore = 0, kmplacemark = true, and mode = refresh
        // with data in a
        // projected crs results in placemarks being encoded in that projected crs, rather than 4326
        // that is required
        // by KML
        Document dom =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(BOULDER)
                                + "&styles="
                                + MockData.BASIC_POLYGONS.getLocalPart()
                                + ","
                                + "&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&format_options=mode:refresh;kmscore:0;kmplacemark:true");

        // verify that the placemark coordinates are encoded properly
        assertXpathEvaluatesTo("1", "count(//kml:Folder[1]/kml:GroundOverlay)", dom);
        assertXpathEvaluatesTo("1", "count(//kml:Folder[1]/kml:Placemark)", dom);

        Element pm = getFirstElementByTagName(dom, "Placemark");
        assertNotNull(pm);

        Element point = getFirstElementByTagName(pm, "Point");
        assertNotNull(point);

        String[] coords =
                getFirstElementByTagName(point, "coordinates")
                        .getFirstChild()
                        .getTextContent()
                        .split(",");
        double[] p = new double[] {Double.parseDouble(coords[0]), Double.parseDouble(coords[1])};

        assertEquals(-105.2, p[0], 0.1);
        assertEquals(40.0, p[1], 0.1);
    }

    @Test
    public void testPointLayerGroupVector() throws Exception {
        Document doc =
                getAsDOM(
                        "wms?request=getmap&service=wms&version=1.1.1"
                                + "&format="
                                + KMLMapOutputFormat.MIME_TYPE
                                + "&layers="
                                + getLayerId(MockData.ROAD_SEGMENTS)
                                + "&styles=&height=1024&width=1024&bbox=-180,-90,180,90&srs=EPSG:4326&featureId=RoadSegments.1107532045088");

        // print(doc);
        assertXpathEvaluatesTo("1", "count(//kml:Placemark)", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045088", "//kml:Placemark/@id", doc);
        assertXpathEvaluatesTo("RoadSegments.1107532045088", "//kml:Placemark/kml:name", doc);
        String expectedDescription =
                String.format(
                        "<h4>RoadSegments</h4>%n"
                                + "\n"
                                + "<ul class=\"textattributes\">\n"
                                + "  \n"
                                + "  <li><strong><span class=\"atr-name\">FID</span>:</strong> <span class=\"atr-value\">102</span></li>\n"
                                + "  <li><strong><span class=\"atr-name\">NAME</span>:</strong> <span class=\"atr-value\">Route 5</span></li>\n"
                                + "</ul>\n");
        String actualDescription = xpath.evaluate("//kml:Placemark/kml:description", doc);
        assertEqualsIgnoreNewLineStyle(expectedDescription, actualDescription);
        // check look-at
        assertXpathEvaluatesTo(
                "-0.0020000000000095497", "//kml:Placemark/kml:LookAt/kml:longitude", doc);
        assertXpathEvaluatesTo(
                "5.000000003008154E-5", "//kml:Placemark/kml:LookAt/kml:latitude", doc);
        // check style
        assertXpathEvaluatesTo(
                "00ffffff", "//kml:Placemark/kml:Style/kml:IconStyle/kml:color", doc);
        assertXpathEvaluatesTo("0.4", "//kml:Placemark/kml:Style/kml:IconStyle/kml:scale", doc);
        assertXpathEvaluatesTo(
                "http://icons.opengeo.org/markers/icon-line.1.png",
                "//kml:Placemark/kml:Style/kml:IconStyle/kml:Icon/kml:href",
                doc);
        assertXpathEvaluatesTo(
                "00ffffff", "//kml:Placemark/kml:Style/kml:LabelStyle/kml:color", doc);
        assertXpathEvaluatesTo(
                "ff000000", "//kml:Placemark/kml:Style/kml:LineStyle/kml:color", doc);
        assertXpathEvaluatesTo("4.0", "//kml:Placemark/kml:Style/kml:LineStyle/kml:width", doc);
        // check geometry
        assertXpathEvaluatesTo(
                "-0.002000087662804264,4.997808429893395E-5",
                "//kml:Placemark/kml:MultiGeometry/kml:Point/kml:coordinates",
                doc);
        assertXpathEvaluatesTo(
                "-0.0042,-6.0E-4 -0.0032,-3.0E-4 -0.0026,-1.0E-4 -0.0014,2.0E-4 2.0E-4,7.0E-4",
                "//kml:Placemark/kml:MultiGeometry/kml:LineString/kml:coordinates",
                doc);
    }
}
