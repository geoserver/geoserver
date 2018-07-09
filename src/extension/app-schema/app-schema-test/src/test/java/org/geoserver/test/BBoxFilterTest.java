/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.geotools.geometry.jts.JTS;
import org.geotools.gml.producer.CoordinateFormatter;
import org.geotools.referencing.CRS;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.w3c.dom.Document;

/**
 * This is to test spatial (bbox) queries for complex features
 *
 * @author Derrick Wong, Curtin University of Technology
 */
public class BBoxFilterTest extends AbstractAppSchemaTestSupport {
    private final String WFS_GET_FEATURE =
            "wfs?request=GetFeature&version=1.1.0&typename=ex:geomContainer";

    private final String WFS_GET_FEATURE_LOG =
            "WFS GetFeature&typename=ex:geomContainerresponse:\n";

    private final String LONGLAT = "&BBOX=130,-29,134,-24";

    private final String LATLONG = "&BBOX=-29,130,-24,134";

    private final String EPSG_4326 = "EPSG:4326";

    private final String EPSG_4283 = "urn:x-ogc:def:crs:EPSG:4283";

    protected BBoxMockData createTestData() {
        return new BBoxMockData();
    }

    /**
     * The following performs a WFS request and obtains all features specified in
     * BBoxTestPropertyfile.properties
     */
    @Test
    public void testQuery() {
        Document doc = getAsDOM(WFS_GET_FEATURE);
        LOGGER.info(WFS_GET_FEATURE_LOG + prettyString(doc));
        assertXpathEvaluatesTo("3", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(3, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering longitude
     * latitude.
     */
    @Test
    public void testQueryBboxLongLat() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LONGLAT);
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));
        assertXpathEvaluatesTo("0", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(0, "//ex:geomContainer", doc);
    }

    /**
     * This uses long lat bbox, with srsName specified in long lat format (EPSG code). This should
     * return the results.
     */
    @Test
    public void testQueryBboxLongLatEPSGCode() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LONGLAT + ",EPSG:4326");
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:geomContainer", doc);
    }

    /**
     * This uses long lat bbox, with srsName specified in lat long format (URN). This should not
     * return the results.
     */
    @Test
    public void testQueryBboxLongLatURN() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LONGLAT + ",urn:x-ogc:def:crs:EPSG:4326");
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));
        assertXpathEvaluatesTo("0", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(0, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering latitude
     * longitude. This test should return features since WFS 1.1.0 defaults to lat long if
     * unspecified.
     */
    @Test
    public void testQueryBboxLatLong() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG);
        LOGGER.info(WFS_GET_FEATURE_LOG + LATLONG + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering latitude
     * longitude and srsName in EPSG code format. This test should not return features if the axis
     * ordering behaves similar to queries to Simple features.
     */
    @Test
    public void testQueryBboxLatLongEPSGCode() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG + ",EPSG:4326");
        LOGGER.info(WFS_GET_FEATURE_LOG + LATLONG + prettyString(doc));
        assertXpathEvaluatesTo("0", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(0, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering latitude
     * longitude and srsName in URN format. This test should return features if the axis ordering
     * behaves similar to queries to Simple features.
     */
    @Test
    public void testQueryBboxLatLongURN() {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG + ",urn:x-ogc:def:crs:EPSG:4326");
        LOGGER.info(WFS_GET_FEATURE_LOG + LATLONG + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering latitude
     * longitude and srsName in URN format using POST request (GEOS-6216). This test should return
     * features if the axis ordering behaves similar to queries to Simple features.
     */
    @Test
    public void testQueryBboxLatLongPost() {

        String xml =
                "<wfs:GetFeature service=\"WFS\" version=\"1.1.0\" " //
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" " //
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " //
                        + "xmlns:gml=\"http://www.opengis.net/gml\" " //
                        + "xmlns:ex=\"http://example.com\" " //
                        + "xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" " //
                        + "xsi:schemaLocation=\"" //
                        + "http://www.opengis.net/wfs http://schemas.opengis.net/wfs/1.1.0/wfs.xsd \">" //
                        + "<wfs:Query typeName=\"ex:geomContainer\">" //
                        + "    <ogc:Filter>" //
                        + "        <ogc:BBOX>" //
                        + "            <ogc:PropertyName>ex:geom</ogc:PropertyName>" //
                        + "            <gml:Envelope srsName=\"urn:x-ogc:def:crs:EPSG:4326\">" //
                        + "                  <gml:lowerCorner>-29 130</gml:lowerCorner>" //
                        + "                  <gml:upperCorner>-24 134</gml:upperCorner>" //
                        + "            </gml:Envelope>" //
                        + "        </ogc:BBOX>" //
                        + "    </ogc:Filter>" //
                        + "</wfs:Query>" //
                        + "</wfs:GetFeature>"; //
        validate(xml);
        Document doc = postAsDOM("wfs", xml);
        LOGGER.info(WFS_GET_FEATURE_LOG + " with POST filter " + prettyString(doc));
        assertXpathEvaluatesTo("2", "/wfs:FeatureCollection/@numberOfFeatures", doc);
        assertXpathCount(2, "//ex:geomContainer", doc);
    }

    /**
     * The following performs a WFS request specifying a BBOX parameter of axis ordering longitude
     * latitude along with srs reprojection.
     */
    @Test
    public void testQueryBboxLatLongSrs4283()
            throws NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException,
                    TransformException {
        Document doc = getAsDOM(WFS_GET_FEATURE + LATLONG + "&srsName=urn:x-ogc:def:crs:EPSG:4283");
        LOGGER.info(WFS_GET_FEATURE_LOG + LONGLAT + prettyString(doc));

        CoordinateReferenceSystem sourceCRS = (CoordinateReferenceSystem) CRS.decode(EPSG_4326);
        CoordinateReferenceSystem targetCRS = (CoordinateReferenceSystem) CRS.decode(EPSG_4283);
        MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
        GeometryFactory factory = new GeometryFactory();
        Point targetPoint =
                (Point)
                        JTS.transform(
                                factory.createPoint(new Coordinate(132.61, -26.98)), transform);
        CoordinateFormatter format = new CoordinateFormatter(8);
        String targetPointCoord1 =
                format.format(targetPoint.getCoordinate().x)
                        + " "
                        + format.format(targetPoint.getCoordinate().y);
        targetPoint =
                (Point)
                        JTS.transform(
                                factory.createPoint(new Coordinate(132.71, -26.46)), transform);
        String targetPointCoord2 =
                format.format(targetPoint.getCoordinate().x)
                        + " "
                        + format.format(targetPoint.getCoordinate().y);

        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='1']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2", "//ex:geomContainer[@gml:id='1']/ex:geom/gml:Point/@srsDimension", doc);
        assertXpathEvaluatesTo(
                targetPointCoord1,
                "//ex:geomContainer[@gml:id='1']/ex:geom/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='1']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.1']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//ex:geomContainer[@gml:id='1']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.1']/ex:geom/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                targetPointCoord1,
                "//ex:geomContainer[@gml:id='1']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.1']/ex:geom/gml:Point/gml:pos",
                doc);

        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='2']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2", "//ex:geomContainer[@gml:id='2']/ex:geom/gml:Point/@srsDimension", doc);
        assertXpathEvaluatesTo(
                targetPointCoord2,
                "//ex:geomContainer[@gml:id='2']/ex:geom/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4283",
                "//ex:geomContainer[@gml:id='2']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.2']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "2",
                "//ex:geomContainer[@gml:id='2']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.2']/ex:geom/gml:Point/@srsDimension",
                doc);
        assertXpathEvaluatesTo(
                targetPointCoord2,
                "//ex:geomContainer[@gml:id='2']/ex:nestedFeature/ex:nestedGeom[@gml:id='nested.2']/ex:geom/gml:Point/gml:pos",
                doc);
    }
}
