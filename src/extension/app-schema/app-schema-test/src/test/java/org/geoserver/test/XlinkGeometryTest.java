/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.test;

import org.junit.Test;
import org.w3c.dom.Document;

/**
 * @author Niels Charlier, Curtin University Of Technology
 *     <p>Tests manual and automatic xlink:href for Geometries
 */
public class XlinkGeometryTest extends AbstractAppSchemaTestSupport {

    @Override
    protected XlinkGeometryMockData createTestData() {
        return new XlinkGeometryMockData();
    }

    /** Tests whether automatic and manual xlink:href is encoded in all Geometry Types */
    @Test
    public void testGeometry() {
        Document doc = getAsDOM("wfs?request=GetFeature&version=1.1.0&typeName=ex:MyTestFeature");
        LOGGER.info("WFS GetFeature response:\n" + prettyString(doc));

        // test manual xlink:href
        assertXpathEvaluatesTo(
                "xlinkvalue1",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:geometryref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue2",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:curveref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue3",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:pointref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue4",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:linestringref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue5",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:surfaceref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue6",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:polygonref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue7",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multicurveref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue8",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multipointref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue9",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multilinestringref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue10",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multisurfaceref/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "xlinkvalue11",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='1']/ex:multipolygonref/@xlink:href",
                doc);

        // test auto xlink:href
        assertXpathEvaluatesTo(
                "#geom1",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:geometry/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom2",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:curve/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom3",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:point/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom4",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:linestring/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom5",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:surface/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom6",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:polygon/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom7",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multicurve/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom8",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipoint/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom9",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multilinestring/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom10",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multisurface/@xlink:href",
                doc);
        assertXpathEvaluatesTo(
                "#geom11",
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipolygon/@xlink:href",
                doc);

        // test if nodes are empty
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:geometry/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:curve/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:point/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:linestring/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:surface/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:polygon/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multicurve/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipoint/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multilinestring/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multisurface/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipolygon/*",
                doc);

        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:geometryref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:curveref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:pointref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:linestringref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:surfaceref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:polygonref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multicurveref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipointref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multilinestringref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multisurfaceref/*",
                doc);
        assertXpathCount(
                0,
                "wfs:FeatureCollection/gml:featureMember/ex:MyTestFeature[@gml:id='2']/ex:multipolygonref/*",
                doc);
    }
}
