/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test for MultiGeometries in App-schema
 *
 * @author Niels Charlier
 */
public class MultiGeometryTest extends AbstractAppSchemaTestSupport {

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        System.setProperty("org.geotools.referencing.forceXY", "true");
        return new MultiGeometryMockData();
    }

    @Test
    public void testMultiGeometry() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typename=ex:geomContainer");
        LOGGER.info("WFS GetFeature&typename=ex:geomContainer response:\n" + prettyString(doc));

        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//ex:geomContainer[@gml:id='mf1']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "5.03335814 -1",
                "//ex:geomContainer[@gml:id='mf1']/ex:geom/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4979",
                "//ex:geomContainer[@gml:id='mf1']/ex:shape/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "133.8855 -23.6701 112",
                "//ex:geomContainer[@gml:id='mf1']/ex:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4326",
                "//ex:geomContainer[@gml:id='mf1']/ex:nestedFeature/ex:nestedGeom/ex:geom/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.5 -1.2 52.6 -1.2 52.6 -1.1 52.5 -1.1 52.5 -1.2",
                "//ex:geomContainer[@gml:id='mf1']/ex:nestedFeature/ex:nestedGeom/ex:geom/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);
    }

    @Test
    public void testMultiGeometryReprojected() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=ex:geomContainer&srsName=urn:x-ogc:def:crs:EPSG:4052");
        LOGGER.info("WFS GetFeature&typename=ex:geomContainer response:\n" + prettyString(doc));

        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4052",
                "//ex:geomContainer[@gml:id='mf1']/ex:geom/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "5 -1", "//ex:geomContainer[@gml:id='mf1']/ex:geom/gml:Point/gml:pos", doc);
        assertXpathEvaluatesTo(
                "http://www.opengis.net/gml/srs/epsg.xml#4979",
                "//ex:geomContainer[@gml:id='mf1']/ex:shape/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "133.8855 -23.6701 112",
                "//ex:geomContainer[@gml:id='mf1']/ex:shape/gml:Point/gml:pos",
                doc);
        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4052",
                "//ex:geomContainer[@gml:id='mf1']/ex:nestedFeature/ex:nestedGeom/ex:geom/gml:Polygon/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "52.31400393 -1.2 52.41418008 -1.2 52.41418008 -1.1 52.31400393 -1.1 52.31400393 -1.2",
                "//ex:geomContainer[@gml:id='mf1']/ex:nestedFeature/ex:nestedGeom/ex:geom/gml:Polygon/gml:exterior/gml:LinearRing/gml:posList",
                doc);

        assertXpathEvaluatesTo(
                "urn:x-ogc:def:crs:EPSG:4052",
                "//ex:geomContainer[@gml:id='mf1']/ex:location/gml:Point/@srsName",
                doc);
        assertXpathEvaluatesTo(
                "4.96649296 -1",
                "//ex:geomContainer[@gml:id='mf1']/ex:location/gml:Point/gml:pos",
                doc);
    }
}
