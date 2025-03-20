/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_USE_FEATURES;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;
import static org.junit.Assert.assertEquals;

import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.mapml.xml.Mapml;
import org.geotools.api.style.Style;
import org.junit.After;
import org.junit.Test;

public class MapMLStyleVisitorTest extends MapMLTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("pointSymbolizer", "pointSymbolizer.sld", getClass(), catalog);
        testData.addStyle("lineSymbolizer", "lineSymbolizer.sld", getClass(), catalog);
        testData.addStyle("polygonSymbolizer", "polygonSymbolizer.sld", getClass(), catalog);
        testData.addStyle("polygonFilterSymbolizer", "polygonFilter.sld", getClass(), catalog);
        testData.addStyle("simpleLineSymbolizer", "simpleLineSymbolizer.sld", getClass(), catalog);
        testData.addStyle("simplePointSymbolizer", "simplePointSymbolizer.sld", getClass(), catalog);
        testData.addStyle("simplePolygonSymbolizer", "simplePolygonSymbolizer.sld", getClass(), catalog);
        testData.addStyle("lakeScale", "lakeScale.sld", getClass(), catalog);
    }

    @After
    public void tearDown() {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BUILDINGS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(li);
    }

    @Test
    public void testStylesWithScaleDenominator() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.LAKES.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        li.getStyles().add(cat.getStyleByName("lakeScale"));
        cat.save(li);

        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.LAKES.getLocalPart())
                .bbox("0,-0.002,0.00451,0")
                .srs("EPSG:4326")
                .styles("lakeScale")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "No features are returned because the scale denominator is not within the range of the style",
                0,
                mapmlFeatures.getBody().getFeatures().size());
        mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.LAKES.getLocalPart())
                .bbox("-50,-50,50,50")
                .srs("EPSG:4326")
                .styles("lakeScale")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "Feature is returned because scale is between minscaledenominator and maxscaledenominator",
                1,
                mapmlFeatures.getBody().getFeatures().size());
        assertEquals(
                "Feature style class rule-1_symbolizer-1 is assigned because scale matches rule scale range, rule2 is excluded because it is outside the scale range",
                "lakeScale-r1-s1",
                mapmlFeatures.getBody().getFeatures().get(0).getStyle());
    }

    @Test
    public void testStyleClasses() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BUILDINGS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        li.getStyles().add(cat.getStyleByName("polygonSymbolizer"));
        cat.save(li);

        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.BUILDINGS.getLocalPart())
                .bbox("0,0,0.01,0.01")
                .srs("EPSG:4326")
                .styles("polygonSymbolizer")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "Style classes string",
                ".bbox {display:none} .polygonSymbolizer-r1-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:2.0; fill:#000080; fill-opacity:0.5; stroke:#FFFFFF; stroke-linecap:butt} .polygonSymbolizer-r2-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:4.0; fill:#033080; fill-opacity:0.74; stroke:#FF66FF; stroke-linecap:butt}",
                mapmlFeatures.getHead().getStyle());
        assertEquals(
                "XML feature count is equals the underlying features source count because multiple symbolizer classes are assigned at the feature level",
                2,
                mapmlFeatures.getBody().getFeatures().size());
        assertEquals(
                "The first feature has a two style classes assigned because two symbolizers are applicable",
                "polygonSymbolizer-r1-s1 polygonSymbolizer-r2-s1",
                mapmlFeatures.getBody().getFeatures().get(0).getStyle());
    }

    @Test
    public void testStyleClassesApplyToCorrectFeatures() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BUILDINGS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        li.getStyles().add(cat.getStyleByName("polygonFilterSymbolizer"));
        cat.save(li);

        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.BUILDINGS.getLocalPart())
                .bbox("0,0,0.01,0.01")
                .srs("EPSG:4326")
                .styles("polygonFilterSymbolizer")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "Style classes string",
                ".bbox {display:none} .polygonFilterSymbolizer-r1-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:0.5; fill:#0033cc; fill-opacity:1.0; stroke:#000000; stroke-linecap:butt} .polygonFilterSymbolizer-r2-s1{stroke-opacity:1.0; stroke-dashoffset:0; stroke-width:0.7; fill:#8833cc; fill-opacity:1.0; stroke:#001200; stroke-linecap:butt}",
                mapmlFeatures.getHead().getStyle());

        assertEquals(
                "The first feature is assigned the second style class because it matches the filter",
                "polygonFilterSymbolizer-r2-s1",
                mapmlFeatures.getBody().getFeatures().get(0).getStyle());
        assertEquals(
                "The second feature is assigned the first style class",
                "polygonFilterSymbolizer-r1-s1",
                mapmlFeatures.getBody().getFeatures().get(1).getStyle());
    }

    @Test
    public void testPolygonToOtherGeomTypes() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BUILDINGS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        li.getStyles().add(cat.getStyleByName("simpleLineSymbolizer"));
        li.getStyles().add(cat.getStyleByName("simplePointSymbolizer"));
        cat.save(li);

        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.BUILDINGS.getLocalPart())
                .srs("EPSG:4326")
                .styles("simpleLineSymbolizer")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "The polygon features are represented as is because line sybolizers can paint a polygon too",
                "class org.geoserver.mapml.xml.Polygon",
                mapmlFeatures
                        .getBody()
                        .getFeatures()
                        .get(0)
                        .getGeometry()
                        .getGeometryContent()
                        .getDeclaredType()
                        .toString());

        mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.BUILDINGS.getLocalPart())
                .srs("EPSG:4326")
                .styles("simplePointSymbolizer")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "The polygon features are represented as points because the style has a point symbolizer",
                "class org.geoserver.mapml.xml.Point",
                mapmlFeatures
                        .getBody()
                        .getFeatures()
                        .get(0)
                        .getGeometry()
                        .getGeometryContent()
                        .getDeclaredType()
                        .toString());
    }

    @Test
    public void testLineToOtherGeomTypes() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.STREAMS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        li.getStyles().add(cat.getStyleByName("simplePointSymbolizer"));
        li.getStyles().add(cat.getStyleByName("simplePolygonSymbolizer"));
        cat.save(li);

        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.STREAMS.getLocalPart())
                .srs("EPSG:4326")
                .styles("simplePolygonSymbolizer")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "The line features are represented as polygons because the style has a polygon symbolizer",
                "class org.geoserver.mapml.xml.Polygon",
                mapmlFeatures
                        .getBody()
                        .getFeatures()
                        .get(0)
                        .getGeometry()
                        .getGeometryContent()
                        .getDeclaredType()
                        .toString());

        mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.STREAMS.getLocalPart())
                .srs("EPSG:4326")
                .styles("simplePointSymbolizer")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "The line features are represented as points because the style has a point symbolizer",
                "class org.geoserver.mapml.xml.Point",
                mapmlFeatures
                        .getBody()
                        .getFeatures()
                        .get(0)
                        .getGeometry()
                        .getGeometryContent()
                        .getDeclaredType()
                        .toString());
    }

    @Test
    public void testPointToOtherGeomTypes() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POINTS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        li.getStyles().add(cat.getStyleByName("simpleLineSymbolizer"));
        li.getStyles().add(cat.getStyleByName("simplePolygonSymbolizer"));
        cat.save(li);

        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.POINTS.getLocalPart())
                .bbox("166021.44,0.0,833978.56,9329005.18")
                .srs("EPSG:32615")
                .styles("simpleLineSymbolizer")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "No features are returned because points are not converted to line representations",
                0,
                mapmlFeatures.getBody().getFeatures().size());

        mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.POINTS.getLocalPart())
                .bbox("166021.44,0.0,833978.56,9329005.18")
                .srs("EPSG:32615")
                .styles("simplePolygonSymbolizer")
                .feature(true)
                .getAsMapML();
        assertEquals(
                "The point features are represented as polygons because the style has a polygon symbolizer",
                "class org.geoserver.mapml.xml.Polygon",
                mapmlFeatures
                        .getBody()
                        .getFeatures()
                        .get(0)
                        .getGeometry()
                        .getGeometryContent()
                        .getDeclaredType()
                        .toString());
    }

    @Test
    public void testPointSymbolizer() throws Exception {
        Catalog catalog = getCatalog();
        StyleInfo styleInfo = catalog.getStyleByName("pointSymbolizer");
        Style style = styleInfo.getStyle();
        MapMLStyleVisitor visitor = new MapMLStyleVisitor();
        style.accept(visitor);
        Map<String, MapMLStyle> styleMap = visitor.getStyles();
        MapMLStyle mapMLStyle = styleMap.get("r1-s1");
        assertEquals("0.5", mapMLStyle.getProperty("opacity"));
        assertEquals("#FF0000", mapMLStyle.getProperty("fill"));
        assertEquals("circle", mapMLStyle.getProperty("well-known-name"));
    }

    @Test
    public void testLineSymbolizer() throws Exception {
        Catalog catalog = getCatalog();
        StyleInfo styleInfo = catalog.getStyleByName("lineSymbolizer");
        Style style = styleInfo.getStyle();
        MapMLStyleVisitor visitor = new MapMLStyleVisitor();
        style.accept(visitor);
        Map<String, MapMLStyle> styleMap = visitor.getStyles();
        MapMLStyle mapMLStyle = styleMap.get("r1-s1");
        assertEquals("0.5", mapMLStyle.getProperty("stroke-opacity"));
        assertEquals("#333333", mapMLStyle.getProperty("stroke"));
        assertEquals("3.0", mapMLStyle.getProperty("stroke-width"));
        assertEquals("round", mapMLStyle.getProperty("stroke-linecap"));
        assertEquals("5.0 2.0", mapMLStyle.getProperty("stroke-dasharray"));
    }

    @Test
    public void testPolygonSymbolizer() throws Exception {
        Catalog catalog = getCatalog();
        StyleInfo styleInfo = catalog.getStyleByName("polygonSymbolizer");
        Style style = styleInfo.getStyle();
        MapMLStyleVisitor visitor = new MapMLStyleVisitor();
        style.accept(visitor);
        Map<String, MapMLStyle> styleMap = visitor.getStyles();
        MapMLStyle mapMLStyle = styleMap.get("r1-s1");
        assertEquals("#000080", mapMLStyle.getProperty("fill"));
        assertEquals("2.0", mapMLStyle.getProperty("stroke-width"));
        assertEquals("0.5", mapMLStyle.getProperty("fill-opacity"));
    }
}
