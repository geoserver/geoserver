/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.svg;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.style.Style;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.FeatureLayer;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;

public class SVGMapProducerTest extends WMSTestSupport {

    private static String EXPECTED_DOC =
            "<?xml version=\"1.0\" standalone=\"no\"?>"
                    + "<svg xmlns=\"http://www.w3.org/2000/svg\""
                    + "    xmlns:xlink=\"http://www.w3.org/1999/xlink\""
                    + "    stroke=\"green\""
                    + "    fill=\"none\""
                    + "    stroke-width=\"0.1%\""
                    + "    stroke-linecap=\"round\""
                    + "    stroke-linejoin=\"round\""
                    + "    width=\"300\""
                    + "    height=\"300\""
                    + "    viewBox=\"-250.0 -250.0 500.0 500.0\""
                    + "    preserveAspectRatio=\"xMidYMid meet\">"
                    + "  <g id=\"LAYER\" class=\"STYLE\">"
                    + "    <use x=\"10\" y=\"-10\" xlink:href=\"#point\"/>"
                    + "    <path d=\"M50 -50l50 -50 \"/>"
                    + "    <path d=\"M0 0l0 -200 200 0 0 200 -200 0 Z\"/>"
                    + "  </g>"
                    + "</svg>";

    private void doTestSVGMapProducer(String layer, String style, String expectedDoc)
            throws Exception {
        GeometryFactory gf = new GeometryFactory();
        Point point = gf.createPoint(new Coordinate(10, 10));
        LineString line =
                gf.createLineString(
                        new Coordinate[] {new Coordinate(50, 50), new Coordinate(100, 100)});
        Polygon polygon =
                gf.createPolygon(
                        gf.createLinearRing(
                                new Coordinate[] {
                                    new Coordinate(0, 0),
                                    new Coordinate(0, 200),
                                    new Coordinate(200, 200),
                                    new Coordinate(200, 0),
                                    new Coordinate(0, 0)
                                }),
                        null);

        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        ftb.setName(layer);
        ftb.add("geom", Geometry.class);
        SimpleFeatureType type = ftb.buildFeatureType();

        SimpleFeature f1 = SimpleFeatureBuilder.build(type, new Object[] {point}, null);
        SimpleFeature f2 = SimpleFeatureBuilder.build(type, new Object[] {line}, null);
        SimpleFeature f3 = SimpleFeatureBuilder.build(type, new Object[] {polygon}, null);

        MemoryDataStore ds = new MemoryDataStore();
        ds.createSchema(type);
        ds.addFeatures(f1, f2, f3);

        FeatureSource fs = ds.getFeatureSource(layer);

        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(new ReferencedEnvelope(-250, 250, -250, 250, null));
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);

        Style basicStyle = getCatalog().getStyleByName("Default").getStyle();
        basicStyle.setName(style);
        map.addLayer(new FeatureLayer(fs, basicStyle));

        SVGStreamingMapOutputFormat producer = new SVGStreamingMapOutputFormat();
        StreamingSVGMap encodeSVG = producer.produceMap(map);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        encodeSVG.encode(out);
        // System.out.println(out.toString());

        XMLUnit.setIgnoreWhitespace(true);
        XMLUnit.setIgnoreAttributeOrder(true);
        XMLAssert.assertXMLEqual(expectedDoc, out.toString());
    }

    @Test
    public void testHeterogeneousGeometry() throws Exception {
        String layer = "test";
        String style = "Default";
        String expectedDoc = EXPECTED_DOC.replace("LAYER", layer).replace("STYLE", style);
        doTestSVGMapProducer(layer, style, expectedDoc);
    }

    @Test
    public void testEscaping() throws Exception {
        String unescapedLayer = "layer\"<>";
        String escapedLayer = "layer&quot;&lt;&gt;";
        String unescapedStyle = "style\"<>";
        String escapedStyle = "style&quot;&lt;&gt;";
        String expectedDoc =
                EXPECTED_DOC.replace("LAYER", escapedLayer).replace("STYLE", escapedStyle);
        doTestSVGMapProducer(unescapedLayer, unescapedStyle, expectedDoc);
    }
}
