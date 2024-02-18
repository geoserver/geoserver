/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import static org.geoserver.mapml.MapMLConstants.MAPML_USE_FEATURES;
import static org.geoserver.mapml.MapMLConstants.MAPML_USE_TILES;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.mapml.xml.Mapml;
import org.geoserver.mapml.xml.MultiPolygon;
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

public class MapMLWMSFeatureTest extends MapMLTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Catalog catalog = getCatalog();
        testData.addStyle("polygonFilter", "polygonFilter.sld", getClass(), catalog);
        testData.addStyle("polygonElseFilter", "polygonElseFilter.sld", getClass(), catalog);
        String points = MockData.POINTS.getLocalPart();
        String lines = MockData.LINES.getLocalPart();
        String polygons = MockData.POLYGONS.getLocalPart();
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName("layerGroup");
        lg.getLayers().add(catalog.getLayerByName(points));
        lg.getLayers().add(catalog.getLayerByName(lines));
        lg.getLayers().add(catalog.getLayerByName(polygons));
        CatalogBuilder builder = new CatalogBuilder(catalog);
        builder.calculateLayerGroupBounds(lg, DefaultGeographicCRS.WGS84);
        catalog.add(lg);
    }

    @After
    public void tearDown() {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.POLYGONS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(li);

        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(lgi);

        LayerInfo liRaster = cat.getLayerByName(MockData.WORLD.getLocalPart());
        liRaster.getResource().getMetadata().put(MAPML_USE_FEATURES, false);
        cat.save(liRaster);
    }

    @Test
    public void testMapMLUseFeatures() throws Exception {

        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);

        Mapml mapmlFeatures =
                getWMSAsMapML(
                        MockData.BASIC_POLYGONS.getLocalPart(),
                        null,
                        null,
                        "-180,-90,180,90",
                        "EPSG:4326",
                        null,
                        true);

        assertEquals(
                "Basic Polygons layer has three features, so one should show up in the conversion",
                3,
                mapmlFeatures.getBody().getFeatures().size());
        assertEquals(
                "Polygons layer coordinates should match original feature's coordinates",
                "0,-1,1,0,0,1,-1,0,0,-1",
                ((MultiPolygon)
                                mapmlFeatures
                                        .getBody()
                                        .getFeatures()
                                        .get(0)
                                        .getGeometry()
                                        .getGeometryContent()
                                        .getValue())
                        .getPolygon().get(0).getThreeOrMoreCoordinatePairs().get(0).getValue()
                                .stream()
                                .collect(Collectors.joining(",")));
    }

    @Test
    public void testMapMLUseFeaturesWithSLDFilter() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BUILDINGS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        li.getStyles().add(cat.getStyleByName("polygonFilter"));
        li.getStyles().add(cat.getStyleByName("polygonElseFilter"));
        li.setDefaultStyle(cat.getStyleByName("polygonFilter"));
        cat.save(li);
        Mapml mapmlFeatures =
                getWMSAsMapML(
                        MockData.BUILDINGS.getLocalPart(),
                        null,
                        null,
                        null,
                        "EPSG:4326",
                        "polygonFilter",
                        true);

        assertEquals(
                "Buildings layer has two features, only one should show up after the SLD is applied",
                1,
                mapmlFeatures.getBody().getFeatures().size());

        Mapml mapmlFeaturesElse =
                getWMSAsMapML(
                        MockData.BUILDINGS.getLocalPart(),
                        null,
                        null,
                        null,
                        "EPSG:4326",
                        "polygonElseFilter",
                        true);

        assertEquals(
                "Buildings layer has two features, both should show up after the SLD with elseFilter is applied",
                2,
                mapmlFeaturesElse.getBody().getFeatures().size());

        Map<String, String> kvp = new HashMap<>();
        kvp.put("CQL_FILTER", "ADDRESS = '123 Main Street'");
        kvp.put("srs", "EPSG:4326");
        kvp.put("styles", "polygonElseFilter");
        kvp.put("format_options", MapMLConstants.MAPML_FEATURE_FORMAT_OPTIONS);
        kvp.put("layers", MockData.BUILDINGS.getLocalPart());
        kvp.put("request", "GetMap");
        kvp.put("format", MapMLConstants.MAPML_MIME_TYPE);
        kvp.put("width", "256");
        kvp.put("height", "256");
        kvp.put("BBOX", "-180,-90,180,90");
        Mapml mapmlFeaturesCQL = getWMSAsMapML(null, kvp, null, null, null, null, true);

        assertEquals(
                "SLD filters yield two features, only one should show up after the CQL filter is applied",
                1,
                mapmlFeaturesCQL.getBody().getFeatures().size());

        kvp.put("CQL_FILTER", "ADDRESS = '99 Minor Street'");
        Mapml mapmlNoFeaturesCQL = getWMSAsMapML(null, kvp, null, null, null, null, true);
        assertEquals(
                "SLD filters yield two features, none should show up after the CQL filter is applied",
                0,
                mapmlNoFeaturesCQL.getBody().getFeatures().size());
    }

    @Test
    public void testMapMLGetStyleQuery() throws Exception {
        Catalog cat = getCatalog();
        final String polyTypeSpec = "ADDRESS:String,ip:Integer,geom:Polygon:srid=4326";
        SimpleFeatureType polyType = DataUtilities.createType("polygons", polyTypeSpec);
        DataStore ds = DataUtilities.dataStore(new EmptyFeatureCollection(polyType));
        ReferencedEnvelope mapBounds =
                new ReferencedEnvelope(
                        0, 0.005, 0, 0.005, CRS.decode("urn:x-ogc:def:crs:EPSG:4326"));
        Rectangle renderingArea = new Rectangle(256, 256);

        FeatureLayer layer =
                new FeatureLayer(
                        ds.getFeatureSource("polygons"),
                        cat.getStyleByName("polygonFilter").getStyle());

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 0, layer);
        Query q = StyleQueryUtil.getStyleQuery(layer, mapContent);
        assertTrue(
                "Query filter should include the SLD filter",
                q.getFilter().toString().contains("ADDRESS = 123 Main Street"));

        FeatureLayer layerElse =
                new FeatureLayer(
                        ds.getFeatureSource("polygons"),
                        cat.getStyleByName("polygonElseFilter").getStyle());
        WMSMapContent mapContentElse = createMapContent(mapBounds, renderingArea, 0, layerElse);
        Query qElse = StyleQueryUtil.getStyleQuery(layerElse, mapContentElse);
        assertFalse(
                "Query filter does not include the SLD filter because the else clause is used",
                qElse.getFilter().toString().contains("ADDRESS = 123 Main Street"));
    }

    @Test
    public void testExceptionBecauseMoreThanOneFeatureType() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo li = cat.getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        li.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        li.getResource().getMetadata().put(MAPML_USE_TILES, false);
        cat.save(li);
        LayerGroupInfo lgi = cat.getLayerGroupByName("layerGroup");
        lgi.getMetadata().put(MAPML_USE_FEATURES, true);
        lgi.getMetadata().put(MAPML_USE_TILES, false);
        cat.save(lgi);
        String response =
                getWMSAsMapMLString(
                        "layerGroup" + "," + MockData.BASIC_POLYGONS.getLocalPart(),
                        null,
                        null,
                        null,
                        "EPSG:4326",
                        null,
                        true);

        assertTrue(
                "MapML response contains an exception due to multiple feature types",
                response.contains(
                        "MapML WMS Feature format does not currently support Multiple Feature Type output."));
    }

    @Test
    public void testExceptionBecauseBecauseRaster() throws Exception {
        Catalog cat = getCatalog();
        LayerInfo liRaster = cat.getLayerByName(MockData.WORLD.getLocalPart());
        liRaster.getResource().getMetadata().put(MAPML_USE_FEATURES, true);
        liRaster.getResource().getMetadata().put(MAPML_USE_TILES, false);
        cat.save(liRaster);
        String response =
                getWMSAsMapMLString(
                        MockData.WORLD.getLocalPart(), null, null, null, "EPSG:3857", null, true);

        assertTrue(
                "MapML response contains an exception due to non-vector type",
                response.contains(
                        "MapML WMS Feature format does not currently support non-vector layers."));
    }

    protected static SimpleFeature feature(SimpleFeatureType type, String id, Object... values)
            throws ParseException {

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
            ReferencedEnvelope mapBounds, Rectangle renderingArea, Integer buffer, Layer... layers)
            throws Exception {

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
            ReferencedEnvelope requestEnvelope, Rectangle renderingArea, Integer buffer)
            throws FactoryException {
        GetMapRequest request = new GetMapRequest();
        request.setBaseUrl("http://localhost:8080/geoserver");

        List<MapLayerInfo> layers = new ArrayList<>();
        List<Style> styles = new ArrayList<>();

        request.setLayers(layers);
        request.setStyles(styles);
        request.setBbox(requestEnvelope);
        request.setCrs(requestEnvelope.getCoordinateReferenceSystem());
        if (requestEnvelope.getCoordinateReferenceSystem()
                == CRS.decode("urn:x-ogc:def:crs:EPSG:4326")) {
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
