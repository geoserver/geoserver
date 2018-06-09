/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import static org.junit.Assert.*;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * Test for 3D BBOXes in App-schema Support for offline as well as online testing
 *
 * @author Niels Charlier
 */
public class Reprojection3DTest extends AbstractAppSchemaTestSupport {

    @Override
    protected BBox3DMockData createTestData() {
        return new BBox3DMockData();
    }

    /** Tests re-projection of NonFeatureTypeProxy. */
    @Test
    public void testReprojection() {
        Document doc =
                getAsDOM(
                        "wfs?request=GetFeature&version=1.1.0&typename=gsml:MappedFeature&srsName=EPSG:4891");
        LOGGER.info("WFS GetFeature&typename=gsml:MappedFeature response:\n" + prettyString(doc));

        assertEquals(
                "3",
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsDimension",
                        doc));
        assertEquals(
                "http://www.opengis.net/gml/srs/epsg.xml#4891",
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/@srsName",
                        doc));

        String point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf1']/gsml:shape/gml:Point/gml:pos",
                        doc);
        assertTrue(Double.parseDouble(point.split(" ")[2]) - (112 - 7.91243825667) < 0.00001);

        point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf2']/gsml:shape/gml:Point/gml:pos",
                        doc);
        assertTrue(Double.parseDouble(point.split(" ")[2]) - (7 - 7.87193142436) < 0.00001);

        point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf3']/gsml:shape/gml:Point/gml:pos",
                        doc);
        assertTrue(Double.parseDouble(point.split(" ")[2]) - (5 - 7.97579399217) < 0.00001);

        point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf4']/gsml:shape/gml:Point/gml:pos",
                        doc);
        assertTrue(Double.parseDouble(point.split(" ")[2]) - (88 - 7.82161881682) < 0.00001);

        point =
                evaluate(
                        "//gsml:MappedFeature[@gml:id='gsml.mappedfeature.mf5']/gsml:shape/gml:LineString/gml:posList",
                        doc);
        String[] coords = point.split(" ");
        assertTrue(Double.parseDouble(coords[2]) - (112 - 7.91243825667) < 0.00001);
        assertTrue(Double.parseDouble(coords[5]) - (7 - 7.87193142436) < 0.00001);
        assertTrue(Double.parseDouble(coords[8]) - (5 - 7.97579399217) < 0.00001);
        assertTrue(Double.parseDouble(coords[11]) - (88 - 7.82161881682) < 0.00001);
    }
}
