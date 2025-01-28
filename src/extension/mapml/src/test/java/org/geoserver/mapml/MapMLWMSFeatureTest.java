/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_USE_FEATURES;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;
import static org.geoserver.mapml.template.MapMLMapTemplate.MAPML_FEATURE_FTL;
import static org.geoserver.mapml.template.MapMLMapTemplate.MAPML_FEATURE_HEAD_FTL;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.mapml.xml.Coordinates;
import org.geoserver.mapml.xml.Feature;
import org.geoserver.mapml.xml.LineString;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.MultiLineString;
import org.geoserver.mapml.xml.MultiPolygon;
import org.geoserver.mapml.xml.Point;
import org.geoserver.mapml.xml.Polygon;
import org.geoserver.mapml.xml.Span;
import org.geoserver.mapml.xml.Tile;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.map.StyleQueryUtil;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.Query;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.style.Style;
import org.geotools.data.DataUtilities;
import org.geotools.data.store.EmptyFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.After;
import org.junit.Test;
import org.locationtech.jts.io.ParseException;
import org.springframework.mock.web.MockHttpServletRequest;

public class MapMLWMSFeatureTest extends MapMLTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("polygonOneFilter", "polygonOneFilter.sld", getClass(), catalog);
        testData.addStyle("polygonElseFilter", "polygonElseFilter.sld", getClass(), catalog);
        String points = MockData.POINTS.getLocalPart();
        String lines = MockData.LINES.getLocalPart();
        String polygons = MockData.POLYGONS.getLocalPart();
        String world = MockData.WORLD.getLocalPart();
        String basicPolygons = MockData.BASIC_POLYGONS.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("layerGroup");
        lg.getLayers().add(catalog.getLayerByName(points));
        lg.getLayers().add(catalog.getLayerByName(lines));
        lg.getLayers().add(catalog.getLayerByName(polygons));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg, DefaultGeographicCRS.WGS84);
        catalog.add(lg);

        LayerGroupInfo lgWithRaster = catalog.getFactory().createLayerGroup();
        lgWithRaster.setName("layerGroupWithRaster2");
        lgWithRaster.getLayers().add(catalog.getLayerByName(basicPolygons));
        lgWithRaster.getLayers().add(catalog.getLayerByName(world));
        builder.calculateLayerGroupBounds(lgWithRaster, DefaultGeographicCRS.WGS84);
        catalog.add(lgWithRaster);
    }

    @After
    public void tearDown() throws IOException {
        revertLayer(MockData.POLYGONS);
        revertLayer(MockData.BUILDINGS);
        revertLayer(MockData.ROAD_SEGMENTS);

        Catalog cat = getCatalog();
        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(lgi);

        LayerInfo liRaster = cat.getLayerByName(MockData.WORLD.getLocalPart());
        liRaster.getResource().getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(liRaster);

        disableTileCaching(MockData.WORLD, cat);
    }

    @Test
    public void testMapMLUseFeatures() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);

        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.BASIC_POLYGONS.getLocalPart())
                .bbox("-180,-90,180,90")
                .srs("EPSG:4326")
                .feature(true)
                .getAsMapML();

        assertEquals(
                "Basic Polygons layer has three features, so one should show up in the conversion",
                3,
                mapmlFeatures.getBody().getFeatures().size());

        Polygon polygon = (Polygon) mapmlFeatures
                .getBody()
                .getFeatures()
                .get(0)
                .getGeometry()
                .getGeometryContent()
                .getValue();
        assertEquals(
                "Polygons layer coordinates should match original feature's coordinates",
                "0 -1 1 0 0 1 -1 0 0 -1",
                polygon.getThreeOrMoreCoordinatePairs().get(0).getCoordinates().get(0));
    }

    @Test
    public void testMapMLUseFeaturesLayerGroupRaster() throws Exception {

        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name("layerGroupWithRaster2")
                .bbox("-180,-90,180,90")
                .srs("EPSG:4326")
                .feature(true)
                .getAsMapML();
        List<Tile> tiles = mapmlFeatures.getBody().getTiles();
        assertEquals("Raster layer world should return tiles", 2, tiles.size());
        MockHttpServletRequest request = createRequest(tiles.get(0).getSrc());
        assertEquals("-180.0,-90.0,0.0,90.0", request.getParameter("BBOX"));
        MockHttpServletRequest request2 = createRequest(tiles.get(1).getSrc());
        assertEquals("0.0,-90.0,180.0,90.0", request2.getParameter("BBOX"));

        Mapml mapmlFeatures2 = new MapMLWMSRequest()
                .name("layerGroupWithRaster2")
                .bbox("-89,-44,-87,-42")
                .srs("EPSG:4326")
                .feature(true)
                .getAsMapML();
        List<Tile> tiles2 = mapmlFeatures2.getBody().getTiles();
        assertEquals("Raster layer world should return 1 tile", 1, tiles2.size());
        MockHttpServletRequest request3 = createRequest(tiles2.get(0).getSrc());
        assertEquals("-90.0,-45.0,-84.375,-39.375", request3.getParameter("BBOX"));
    }

    @Test
    public void testUseFeaturesRasterTileCache() throws Exception {
        Catalog cat = getCatalog();
        enableTileCaching(MockData.WORLD, cat);
        Mapml mapmlFeatures3 = new MapMLWMSRequest()
                .name("layerGroupWithRaster2")
                .bbox("-89,-44,-87,-42")
                .srs("EPSG:4326")
                .feature(true)
                .tile(true)
                .getAsMapML();
        List<Tile> tiles3 = mapmlFeatures3.getBody().getTiles();
        assertEquals("Raster layer world should return 11 tiles", 1, tiles3.size());
    }

    @Test
    public void testMapMLUseFeaturesWithSLDFilter() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BUILDINGS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        li.getStyles().add(cat.getStyleByName("polygonOneFilter"));
        li.getStyles().add(cat.getStyleByName("polygonElseFilter"));
        li.setDefaultStyle(cat.getStyleByName("polygonOneFilter"));
        cat.save(li);
        String bbox = "-0.1,-0.1,0.1,0.1";
        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.BUILDINGS.getLocalPart())
                .bbox(bbox)
                .srs("EPSG:4326")
                .styles("polygonOneFilter")
                .feature(true)
                .getAsMapML();

        assertEquals(
                "Buildings layer has two features, only one should show up after the SLD is applied",
                1,
                mapmlFeatures.getBody().getFeatures().size());

        Mapml mapmlFeaturesElse = new MapMLWMSRequest()
                .name(MockData.BUILDINGS.getLocalPart())
                .bbox(bbox)
                .srs("EPSG:4326")
                .styles("polygonElseFilter")
                .feature(true)
                .getAsMapML();

        assertEquals(
                "Buildings layer has two features, both should show up after the SLD with elseFilter is applied",
                2,
                mapmlFeaturesElse.getBody().getFeatures().size());

        Map<String, String> kvp = new HashMap<>();
        kvp.put("CQL_FILTER", "ADDRESS = '123 Main Street'");
        kvp.put("srs", "EPSG:4326");
        kvp.put("styles", "polygonElseFilter");
        kvp.put("format_options", MapMLConstants.MAPML_FEATURE_FO + ":true");
        kvp.put("layers", MockData.BUILDINGS.getLocalPart());
        kvp.put("request", "GetMap");
        kvp.put("format", MapMLConstants.MAPML_MIME_TYPE);
        kvp.put("width", "256");
        kvp.put("height", "256");
        kvp.put("BBOX", bbox);
        Mapml mapmlFeaturesCQL =
                new MapMLWMSRequest().kvp(kvp).bbox(bbox).feature(true).getAsMapML();

        assertEquals(
                "SLD filters yield two features, only one should show up after the CQL filter is applied",
                1,
                mapmlFeaturesCQL.getBody().getFeatures().size());

        kvp.put("CQL_FILTER", "ADDRESS = '99 Minor Street'");
        Mapml mapmlNoFeaturesCQL =
                new MapMLWMSRequest().kvp(kvp).bbox(bbox).feature(true).getAsMapML();
        assertEquals(
                "SLD filters yield two features, none should show up after the CQL filter is applied",
                0,
                mapmlNoFeaturesCQL.getBody().getFeatures().size());
    }

    @Test
    public void testScreenMapSimplification() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BUILDINGS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);

        // test small bbox, the two features are big enough that they should both be returned
        Mapml mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.BUILDINGS.getLocalPart())
                .bbox("-0.1,-0.1,0.1,0.1")
                .srs("EPSG:4326")
                .feature(true)
                .getAsMapML();
        assertEquals(2, mapmlFeatures.getBody().getFeatures().size());

        // test larger bbox, this time they are smaller than a pixel, only one remains
        mapmlFeatures = new MapMLWMSRequest()
                .name(MockData.BUILDINGS.getLocalPart())
                .bbox("-10,-10,10,10")
                .srs("EPSG:4326")
                .feature(true)
                .getAsMapML();
        assertEquals(1, mapmlFeatures.getBody().getFeatures().size());
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCoordinateSimplification() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);

        // test with a small bbox, that should still lead to a geometric simplification
        Mapml mapml = new MapMLWMSRequest()
                .name(MockData.ROAD_SEGMENTS.getLocalPart())
                .bbox("-0.1,-0.1,0.1,0.1")
                .srs("EPSG:4326")
                .feature(true)
                .getAsMapML();
        List<Feature> features = mapml.getBody().getFeatures();
        assertEquals(5, features.size());
        for (Feature feature : features) {
            Object geometry = feature.getGeometry().getGeometryContent().getValue();
            // all lines are small enough that they are simplified to start/end
            if (geometry instanceof LineString) {
                LineString ls = (LineString) geometry;
                String lscoords =
                        ls.getCoordinates().get(0).getCoordinates().get(0).toString();
                assertEquals(4, lscoords.split(" ").length);
            } else if (geometry instanceof MultiLineString) {
                MultiLineString mls = (MultiLineString) geometry;
                for (Coordinates je : mls.getTwoOrMoreCoordinatePairs()) {
                    String mlscoords = je.getCoordinates().get(0).toString();
                    assertEquals(2, mlscoords.split(" ").length);
                }
            }
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testCoordinatePrecision() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.ROAD_SEGMENTS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);

        // test with a big bbox, we will only need minimal precision
        Mapml mapml = new MapMLWMSRequest()
                .name(MockData.ROAD_SEGMENTS.getLocalPart())
                .bbox("-180,-90,180,90")
                .srs("EPSG:4326")
                .feature(true)
                .getAsMapML();
        List<Feature> features = mapml.getBody().getFeatures();
        //     assertEquals(5, features.size());
        testScale(features, 0);

        // test with smaller bbox, we will need more precision
        mapml = new MapMLWMSRequest()
                .name(MockData.ROAD_SEGMENTS.getLocalPart())
                .bbox("-0.1,-0.1,0.1,0.1")
                .srs("EPSG:4326")
                .feature(true)
                .getAsMapML();
        features = mapml.getBody().getFeatures();
        testScale(features, 4);
    }

    private static void testScale(List<Feature> features, int expectedScale) {
        for (Feature feature : features) {
            Object geometry = feature.getGeometry().getGeometryContent().getValue();
            // all lines are small enough that they are simplified to start/end
            if (geometry instanceof LineString) {
                LineString ls = (LineString) geometry;
                String lscoords =
                        ls.getCoordinates().get(0).getCoordinates().get(0).toString();
                String[] coords = lscoords.split(" ");
                BigDecimal bd = new BigDecimal(coords[0]).stripTrailingZeros();
                int scale = bd.scale();
                assertEquals(expectedScale, scale);
            } else if (geometry instanceof MultiLineString) {
                MultiLineString mls = (MultiLineString) geometry;
                for (Coordinates je : mls.getTwoOrMoreCoordinatePairs()) {
                    String mlscoords = je.getCoordinates().get(0).toString();
                    String[] coords = mlscoords.split(" ");
                    BigDecimal bd = new BigDecimal(coords[0]);
                    int scale = bd.scale();
                    assertEquals(expectedScale, scale);
                }
            }
        }
    }

    @Test
    public void testMapMLGetStyleQuery() throws Exception {
        Catalog cat = getCatalog();
        final String polyTypeSpec = "ADDRESS:String,ip:Integer,geom:Polygon:srid=4326";
        SimpleFeatureType polyType = DataUtilities.createType("polygons", polyTypeSpec);
        DataStore ds = DataUtilities.dataStore(new EmptyFeatureCollection(polyType));
        ReferencedEnvelope mapBounds =
                new ReferencedEnvelope(0, 0.005, 0, 0.005, CRS.decode("urn:x-ogc:def:crs:EPSG:4326"));
        Rectangle renderingArea = new Rectangle(256, 256);

        FeatureLayer layer = new FeatureLayer(
                ds.getFeatureSource("polygons"),
                cat.getStyleByName("polygonOneFilter").getStyle());

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 0, layer);
        Query q = StyleQueryUtil.getStyleQuery(layer, mapContent);
        assertTrue(
                "Query filter should include the SLD filter",
                q.getFilter().toString().contains("ADDRESS = 123 Main Street"));

        FeatureLayer layerElse = new FeatureLayer(
                ds.getFeatureSource("polygons"),
                cat.getStyleByName("polygonElseFilter").getStyle());
        WMSMapContent mapContentElse = createMapContent(mapBounds, renderingArea, 0, layerElse);
        Query qElse = StyleQueryUtil.getStyleQuery(layerElse, mapContentElse);
        assertFalse(
                "Query filter does not include the SLD filter because the else clause is used",
                qElse.getFilter().toString().contains("ADDRESS = 123 Main Street"));
    }

    @Test
    public void testTemplateHeaderStyle() throws Exception {
        File template = null;
        try {
            Catalog cat = getCatalog();
            LayerInfo li = cat.getLayerByName(MockData.BRIDGES.getLocalPart());
            li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
            li.getResource().getMetadata().put(MAPML_USE_TILES, false);
            cat.save(li);
            String layerId = getLayerId(MockData.BRIDGES);
            FeatureTypeInfo resource = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
            File parent = getDataDirectory().get(resource).dir();
            template = new File(parent, MAPML_FEATURE_HEAD_FTL);
            FileUtils.write(
                    template,
                    "<mapml- xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                            + "<map-head>\n"
                            + "  <map-style>.desired {stroke-dashoffset:3}</map-style>\n"
                            + "</map-head>\n"
                            + "</mapml->\n",
                    "UTF-8");
            Mapml mapmlFeatures = new MapMLWMSRequest()
                    .name(MockData.BRIDGES.getLocalPart())
                    .bbox("-180,-90,180,90")
                    .srs("EPSG:4326")
                    .feature(true)
                    .getAsMapML();

            String mapmlStyle = mapmlFeatures.getHead().getStyle();
            assertTrue(mapmlStyle.contains(".desired {stroke-dashoffset:3}"));
        } finally {
            if (template != null) {
                template.delete();
            }
        }
    }

    @Test
    public void testMapMLFeaturePointHasClass() throws Exception {
        File template = null;
        try {
            Catalog cat = getCatalog();
            LayerInfo li = cat.getLayerByName(MockData.BRIDGES.getLocalPart());
            li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
            li.getResource().getMetadata().put(MAPML_USE_TILES, false);
            cat.save(li);
            String layerId = getLayerId(MockData.BRIDGES);
            FeatureTypeInfo resource = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
            File parent = getDataDirectory().get(resource).dir();
            template = new File(parent, MAPML_FEATURE_FTL);
            FileUtils.write(
                    template,
                    "<mapml- xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                            + "<map-head>\n"
                            + "</map-head>\n"
                            + "<map-body>\n"
                            + "<map-feature>\n"
                            + "  <#list attributes as attribute>\n"
                            + "    <#if attribute.name == \"NAME\">\n"
                            + "      <map-properties name=\"UPDATED ${attribute.name}\" value=\"CHANGED ${attribute.value}\"/>\n"
                            + "    </#if>\n"
                            + "  </#list>\n"
                            + "  <#list attributes as gattribute>\n"
                            + "    <#if gattribute.isGeometry>\n"
                            + "      <map-geometry>"
                            + "       <map-point>"
                            + "       <map-coordinates><#list gattribute.rawValue.coordinates as coord>"
                            + "        <#if coord?index == 0><map-span class=\"desired\">${coord.x} ${coord.y}</map-span><#else>${coord.x} ${coord.y}</#if></#list></map-coordinates></map-point>"
                            + "      </map-geometry>"
                            + "    </#if>\n"
                            + "  </#list>\n"
                            + "</map-feature>\n"
                            + "</map-body>\n"
                            + "</mapml->\n",
                    "UTF-8");
            Mapml mapmlFeatures = new MapMLWMSRequest()
                    .name(MockData.BRIDGES.getLocalPart())
                    .bbox("-180,-90,180,90")
                    .srs("EPSG:4326")
                    .feature(true)
                    .getAsMapML();

            Feature feature2 = mapmlFeatures.getBody().getFeatures().get(0); // get the first feature, which has a class
            String attributes = feature2.getProperties().getAnyElement();
            assertTrue(attributes.contains("UPDATED NAME"));
            Point featurePoint =
                    (Point) feature2.getGeometry().getGeometryContent().getValue();
            Span span = ((Span)
                    featurePoint.getCoordinates().get(0).getCoordinates().get(0));
            assertEquals("desired", span.getClazz());
        } finally {
            if (template != null) {
                template.delete();
            }
        }
    }

    @Test
    public void testMapMLFeatureLineHasClass() throws Exception {
        File template = null;
        try {
            Catalog cat = getCatalog();
            LayerInfo li = cat.getLayerByName(MockData.MLINES.getLocalPart());
            li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
            li.getResource().getMetadata().put(MAPML_USE_TILES, false);
            cat.save(li);
            String layerId = getLayerId(MockData.MLINES);
            FeatureTypeInfo resource = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
            File parent = getDataDirectory().get(resource).dir();
            template = new File(parent, MAPML_FEATURE_FTL);
            FileUtils.write(
                    template,
                    "<mapml- xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                            + "<map-head>\n"
                            + "</map-head>\n"
                            + "<map-body>\n"
                            + "<map-feature>\n"
                            + "  <#list attributes as attribute>\n"
                            + "    <#if attribute.isGeometry>\n"
                            + "      <map-geometry><map-linestring><map-coordinates><#list attribute.rawValue.coordinates as coord><#if coord?index == 2> <map-span class=\"desired\">${coord.x} ${coord.y}<#elseif coord?index == 3>${coord.x} ${coord.y}</map-span><#else> ${coord.x} ${coord.y}</#if></#list></map-coordinates></map-linestring></map-geometry>\n"
                            + "    </#if>\n"
                            + "  </#list>\n"
                            + "</map-feature>\n"
                            + "</map-body>\n"
                            + "</mapml->\n",
                    "UTF-8");
            Mapml mapmlFeatures = new MapMLWMSRequest()
                    .name(MockData.MLINES.getLocalPart())
                    .bbox("500000,500000,500999,500999")
                    .srs("EPSG:32615")
                    .feature(true)
                    .getAsMapML();

            Feature feature2 =
                    mapmlFeatures.getBody().getFeatures().get(0); // get the second feature, which has a class
            LineString featureLine =
                    (LineString) feature2.getGeometry().getGeometryContent().getValue();
            Span span =
                    (Span) featureLine.getCoordinates().get(0).getCoordinates().get(1);
            assertEquals("desired", span.getClazz());
        } finally {
            if (template != null) {
                template.delete();
            }
        }
    }

    @Test
    public void testMapMLFeaturePolygonHasClass() throws Exception {
        File template = null;
        try {
            Catalog cat = getCatalog();
            LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
            li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
            li.getResource().getMetadata().put(MAPML_USE_TILES, false);
            cat.save(li);
            String layerId = getLayerId(MockData.POLYGONS);
            FeatureTypeInfo resource = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
            File parent = getDataDirectory().get(resource).dir();
            template = new File(parent, MAPML_FEATURE_FTL);
            FileUtils.write(
                    template,
                    "<mapml- xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                            + "<map-head>\n"
                            + "</map-head>\n"
                            + "<map-body>\n"
                            + "<map-feature>\n"
                            + "  <#list attributes as attribute>\n"
                            + "    <#if attribute.isGeometry>\n"
                            + "      <map-geometry>\n"
                            + "       <map-polygon>"
                            + "       <#assign shell = attribute.rawValue.getExteriorRing()><map-coordinates><#list shell.coordinates as coord><#if coord?index == 0><map-span class=\"desired\">${coord.x} ${coord.y}<#elseif coord?index == 4> ${coord.x} ${coord.y}</map-span><#else> ${coord.x} ${coord.y}</#if></#list></map-coordinates>"
                            + "      <#list 0 ..< attribute.rawValue.getNumInteriorRing() as index>"
                            + "        <#assign hole = attribute.rawValue.getInteriorRingN(index)><map-coordinates><#list hole.coordinates as coord><#if coord?index == 0><map-span class=\"desired\">${coord.x} ${coord.y} <#elseif coord?index == 4> ${coord.x} ${coord.y}</map-span><#else> ${coord.x} ${coord.y}</#if></#list></map-coordinates></#list>"
                            + "       </map-polygon>"
                            + "      </map-geometry>\n"
                            + "    </#if>\n"
                            + "  </#list>\n"
                            + "</map-feature>\n"
                            + "</map-body>\n"
                            + "</mapml- >\n",
                    "UTF-8");
            Mapml mapmlFeatures = new MapMLWMSRequest()
                    .name(MockData.POLYGONS.getLocalPart())
                    .bbox("500000,500000,500999,500999")
                    .srs("EPSG:32615")
                    .feature(true)
                    .getAsMapML();

            Feature feature2 =
                    mapmlFeatures.getBody().getFeatures().get(0); // get the second feature, which has a class
            Polygon featurePolygon =
                    (Polygon) feature2.getGeometry().getGeometryContent().getValue();
            Span span = (Span) featurePolygon
                    .getThreeOrMoreCoordinatePairs()
                    .get(0)
                    .getCoordinates()
                    .get(0);
            assertEquals("desired", span.getClazz());
        } finally {
            if (template != null) {
                template.delete();
            }
        }
    }

    @Test
    public void testMapMLFeatureMultiPolygonHasClass() throws Exception {
        File template = null;
        try {
            Catalog cat = getCatalog();
            LayerInfo li = cat.getLayerByName(MockData.NAMED_PLACES.getLocalPart());
            li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
            li.getResource().getMetadata().put(MAPML_USE_TILES, false);
            cat.save(li);
            String layerId = getLayerId(MockData.NAMED_PLACES);
            FeatureTypeInfo resource = getCatalog().getResourceByName(layerId, FeatureTypeInfo.class);
            File parent = getDataDirectory().get(resource).dir();
            template = new File(parent, MAPML_FEATURE_FTL);
            FileUtils.write(
                    template,
                    "<mapml- xmlns=\"http://www.w3.org/1999/xhtml\">\n"
                            + "<map-head>\n"
                            + "</map-head>\n"
                            + "<map-body>\n"
                            + "<map-feature>\n"
                            + "<#if attributes.FID.value == \"117\">\n"
                            + "  <#list attributes as attribute>\n"
                            + "    <#if attribute.isGeometry>\n"
                            + "      <map-geometry>\n"
                            + "        <map-multipolygon>"
                            + "      <#list 0 ..< attribute.rawValue.getNumGeometries() as index>"
                            + "        <#assign polygon = attribute.rawValue.getGeometryN(index)>"
                            + "       <map-polygon>"
                            + "       <#assign shell = polygon.getExteriorRing()><map-coordinates><#list shell.coordinates as coord><#if coord?index == 0><map-span class=\"desired\">${coord.x} ${coord.y}<#elseif coord?index == 4> ${coord.x} ${coord.y}</map-span><#else> ${coord.x} ${coord.y}</#if></#list></map-coordinates>"
                            + "      <#list 0 ..< polygon.getNumInteriorRing() as index>"
                            + "        <#assign hole = polygon.getInteriorRingN(index)><map-coordinates><#list hole.coordinates as coord><#if coord?index == 0><map-span class=\"desired\">${coord.x} ${coord.y} <#elseif coord?index == 4> ${coord.x} ${coord.y}</map-span><#else> ${coord.x} ${coord.y}</#if></#list></map-coordinates></#list>"
                            + "       </map-polygon>"
                            + "        </#list>"
                            + "       </map-multipolygon>"
                            + "      </map-geometry>\n"
                            + "    </#if>\n"
                            + "  </#list>\n"
                            + "<#else>\n"
                            + "  <#list attributes as attribute>\n"
                            + "    <#if attribute.isGeometry>\n"
                            + "      <map-geometry>\n"
                            + "        <map-multipolygon>"
                            + "      <#list 0 ..< attribute.rawValue.getNumGeometries() as index>"
                            + "        <#assign polygon = attribute.rawValue.getGeometryN(index)>"
                            + "       <map-polygon>"
                            + "       <#assign shell = polygon.getExteriorRing()><map-coordinates><#list shell.coordinates as coord> ${coord.x} ${coord.y} </#list></map-coordinates>"
                            + "      <#list 0 ..< polygon.getNumInteriorRing() as index>"
                            + "        <#assign hole = polygon.getInteriorRingN(index)><map-coordinates><#list hole.coordinates as coord> ${coord.x} ${coord.y} </#list></map-coordinates></#list>"
                            + "       </map-polygon>"
                            + "        </#list>"
                            + "       </map-multipolygon>"
                            + "      </map-geometry>\n"
                            + "    </#if>\n"
                            + "  </#list>\n"
                            + "</#if>\n"
                            + "</map-feature>\n"
                            + "</map-body>\n"
                            + "</mapml- >\n",
                    "UTF-8");
            Mapml mapmlFeatures = new MapMLWMSRequest()
                    .name(MockData.NAMED_PLACES.getLocalPart())
                    .bbox("-180,-90,180,90")
                    .srs("EPSG:4326")
                    .feature(true)
                    .getAsMapML();

            Feature feature2 = mapmlFeatures.getBody().getFeatures().get(0); // get the first feature, which has a class
            MultiPolygon featureMultiPolygon =
                    (MultiPolygon) feature2.getGeometry().getGeometryContent().getValue();
            Span span = (Span) featureMultiPolygon
                    .getPolygon()
                    .get(0)
                    .getThreeOrMoreCoordinatePairs()
                    .get(0)
                    .getCoordinates()
                    .get(0);
            assertEquals("desired", span.getClazz());
        } finally {
            if (template != null) {
                template.delete();
            }
        }
    }

    @Test
    public void testCanHandleRaster() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo liRaster = cat.getLayerByName(MockData.WORLD.getLocalPart());
        liRaster.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        liRaster.getResource().getMetadata().put(MAPML_USE_TILES, false);
        cat.save(liRaster);
        String response = new MapMLWMSRequest()
                .name(MockData.WORLD.getLocalPart())
                .srs("EPSG:3857")
                .feature(true)
                .getAsString();

        assertTrue("MapML response contains a map tile", response.contains("map-tile"));
    }

    protected static SimpleFeature feature(SimpleFeatureType type, String id, Object... values) throws ParseException {

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                if (value instanceof String) {
                    value = new WKTReader2().read((String) value);
                }
            }
            builder.set(i, value);
        }
        return builder.buildFeature(id);
    }

    private WMSMapContent createMapContent(
            ReferencedEnvelope mapBounds, Rectangle renderingArea, Integer buffer, Layer... layers) throws Exception {

        GetMapRequest mapRequest = createGetMapRequest(mapBounds, renderingArea, buffer);

        WMSMapContent map = new WMSMapContent(mapRequest);
        map.getViewport().setBounds(mapBounds);
        if (layers != null) {
            for (Layer l : layers) {
                map.addLayer(l);
            }
        }
        map.setMapWidth(renderingArea.width);
        map.setMapHeight(renderingArea.height);
        if (Objects.nonNull(buffer)) {
            map.setBuffer(buffer);
        }

        return map;
    }

    protected GetMapRequest createGetMapRequest(
            ReferencedEnvelope requestEnvelope, Rectangle renderingArea, Integer buffer) throws FactoryException {
        GetMapRequest request = new GetMapRequest();
        request.setBaseUrl("http://localhost:8080/geoserver");

        List<MapLayerInfo> layers = new ArrayList<>();
        List<Style> styles = new ArrayList<>();

        request.setLayers(layers);
        request.setStyles(styles);
        request.setBbox(requestEnvelope);
        request.setCrs(requestEnvelope.getCoordinateReferenceSystem());
        if (requestEnvelope.getCoordinateReferenceSystem() == CRS.decode("urn:x-ogc:def:crs:EPSG:4326")) {
            request.setSRS("EPSG:4326");
        } else if (requestEnvelope.getCoordinateReferenceSystem() == CRS.decode("EPSG:3857")) {
            request.setSRS("EPSG:3857");
        } else {
            throw new IllegalArgumentException("Please use one of the test CRS's");
        }
        request.setWidth(renderingArea.width);
        request.setHeight(renderingArea.height);
        request.setRawKvp(new HashMap<>());
        request.setBuffer(buffer);
        return request;
    }
}
