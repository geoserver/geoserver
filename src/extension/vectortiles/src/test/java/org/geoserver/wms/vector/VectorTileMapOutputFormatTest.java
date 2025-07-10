/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.common.collect.ImmutableSet;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.apache.wicket.spring.test.ApplicationContextMock;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.StyleQueryUtil;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
import org.geotools.api.data.Query;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.style.NamedLayer;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyledLayerDescriptor;
import org.geotools.data.DataUtilities;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.memory.MemoryFeatureSource;
import org.geotools.data.store.ContentEntry;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.geometry.jts.WKTReader2;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.map.MapContent;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.io.ParseException;
import org.mockito.Mockito;

public class VectorTileMapOutputFormatTest {

    private static CoordinateReferenceSystem WEB_MERCATOR;

    private static CoordinateReferenceSystem WGS84;

    private static Style defaultPointStyle,
            defaultPolygonStyle,
            scaleDependentPolygonStyle,
            labelPolygonStyle,
            attributesPolygonStyle,
            coalescePolygonStyle;

    private VectorTileMapOutputFormat outputFormat;

    private VectorTileBuilder tileBuilderMock;

    private FeatureLayer pointLayer, scaleDependentPolygonLayer, labelPolygonLayer, attributesPolygonLayer;
    private List<MapContent> mapContents = new ArrayList<>();
    private MemoryDataStore ds;

    @BeforeClass
    public static void beforeClass() throws Exception {
        defaultPointStyle = parseStyle("default_point.sld");
        defaultPolygonStyle = parseStyle("default_polygon.sld");
        scaleDependentPolygonStyle = parseStyle("scaleDependentPolygonStyle.sld");
        labelPolygonStyle = parseStyle("labelsPolygonStyle.sld");
        attributesPolygonStyle = parseStyle("attributesPolygonStyle.sld");
        coalescePolygonStyle = parseStyle("coalescePolygonStyle.sld");

        // avoid lots of application context unset warnings in the console
        GeoServerExtensionsHelper.init(new ApplicationContextMock());

        WEB_MERCATOR = CRS.decode("EPSG:3857");
        WGS84 = CRS.decode("urn:x-ogc:def:crs:EPSG:4326");
    }

    @Before
    public void before() throws Exception {
        tileBuilderMock = mock(VectorTileBuilder.class);

        VectorTileBuilderFactory tileBuilderFactory = mock(VectorTileBuilderFactory.class);
        when(tileBuilderFactory.getMimeType()).thenReturn("testMime");
        when(tileBuilderFactory.getOutputFormats()).thenReturn(ImmutableSet.of("testMime", "testFormat"));

        when(tileBuilderFactory.newBuilder(any(Rectangle.class), any(ReferencedEnvelope.class)))
                .thenReturn(tileBuilderMock);

        outputFormat = new VectorTileMapOutputFormat(tileBuilderFactory);
        outputFormat.setClipToMapBounds(true);

        this.ds = new MemoryDataStore();

        final String pointsTypeSpec = "sp:String,ip:Integer,geom:Point:srid=4326";
        final String linesTypeSpec = "sp:String,ip:Integer,geom:LineString:srid=4326";
        final String polyTypeSpec = "sp:String,ip:Integer,geom:Polygon:srid=4326";

        SimpleFeatureType pointType = DataUtilities.createType("points", pointsTypeSpec);
        SimpleFeatureType lineType = DataUtilities.createType("lines", linesTypeSpec);
        SimpleFeatureType polyType = DataUtilities.createType("polygons", polyTypeSpec);

        ds.addFeature(feature(pointType, "point1", "StringProp1_1", 1000, "POINT(1 1)"));
        ds.addFeature(feature(pointType, "point2", "StringProp1_2", 2000, "POINT(2 2)"));
        ds.addFeature(feature(pointType, "point3", "StringProp1_3", 3000, "POINT(3 3)"));
        double bufferBoundary = -180.0 / 256 * 32;
        ds.addFeature(feature(
                pointType, "pointNear", "StringProp1_4", 3000, String.format("POINT(3 %s)", bufferBoundary + 0.1)));
        ds.addFeature(feature(
                pointType, "pointFar", "StringProp1_5", 3000, String.format("POINT(3 %s)", bufferBoundary - 1.0)));

        ds.addFeature(feature(lineType, "line1", "StringProp2_1", 1000, "LINESTRING (1 1, 2 2)"));
        ds.addFeature(feature(lineType, "line1", "StringProp2_2", 2000, "LINESTRING (3 3, 4 4)"));
        ds.addFeature(feature(lineType, "line1", "StringProp2_3", 3000, "LINESTRING (5 5, 6 6)"));

        ds.addFeature(feature(polyType, "polygon1", "StringProp3_1", 1000, "POLYGON ((1 0, 2 0, 2 1, 1 1, 1 0))"));
        ds.addFeature(feature(polyType, "polygon2", "StringProp3_2", 2000, "POLYGON ((6 6, 7 6, 7 7, 6 7, 6 6))"));
        ds.addFeature(
                feature(polyType, "polygon3", "StringProp3_3", 3000, "POLYGON ((11 11, 12 11, 12 12, 11 12, 11 11))"));

        pointLayer = new FeatureLayer(ds.getFeatureSource("points"), defaultPointStyle);
        scaleDependentPolygonLayer = new FeatureLayer(ds.getFeatureSource("polygons"), scaleDependentPolygonStyle);
        labelPolygonLayer = new FeatureLayer(ds.getFeatureSource("polygons"), labelPolygonStyle);
        attributesPolygonLayer = new FeatureLayer(ds.getFeatureSource("polygons"), attributesPolygonStyle);
    }

    @After
    public void disposeMapContents() {
        // just to avoid nagging logs
        mapContents.forEach(mc -> mc.dispose());
    }

    // Test case for when a style has no active rules (i.e. when the current map scale is not
    // compatible with the Min/MaxScaleDenominator in the SLD Rule).
    @Test
    public void testNoRulesByScale() throws Exception {
        // ----------- normal case, there is a rule that draws

        // this has map scale denominator of about 1:7,700, rule will draw
        ReferencedEnvelope mapBounds = new ReferencedEnvelope(0, 0.005, 0, 0.005, WGS84);
        Rectangle renderingArea = new Rectangle(256, 256);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 0, scaleDependentPolygonLayer);

        Query q = StyleQueryUtil.getStyleQuery(scaleDependentPolygonLayer, mapContent);
        assertNotSame(q.getFilter(), Filter.EXCLUDE);

        // ------------------- abnormal case, there are no rules in the sld that will draw

        // this has map scale denominator of about 1:77k, rule will NOT draw
        mapBounds = new ReferencedEnvelope(0, 0.05, 0, 0.05, WGS84);
        renderingArea = new Rectangle(256, 256);

        mapContent = createMapContent(mapBounds, renderingArea, 0, scaleDependentPolygonLayer);

        q = StyleQueryUtil.getStyleQuery(scaleDependentPolygonLayer, mapContent);
        assertSame(q.getFilter(), Filter.EXCLUDE);
    }

    // the calculated style buffer must account for oversampling
    @Test
    public void testBufferOversample() throws Exception {
        ReferencedEnvelope mapBounds = new ReferencedEnvelope(-90, 90, 0, 180, WGS84);
        Rectangle renderingArea = new Rectangle(256, 256);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 32, pointLayer);

        MapBoxTileBuilderFactory mbbf = new MapBoxTileBuilderFactory();
        VectorTileMapOutputFormat vtof = new VectorTileMapOutputFormat(mbbf);

        VectorTileMapOutputFormat vtof_spy = Mockito.spy(vtof);

        // here's the test - the buffer (from style) is 32 pixels
        // however, since there is 16 * oversampling, it will really be a 512 pixel buffer
        int expectedBuffer = 32 * mbbf.getOversampleX();

        // verify that this buffer is send down to the Pipeline
        vtof_spy.produceMap(mapContent);
        Mockito.verify(vtof_spy)
                .getPipeline(
                        any(WMSMapContent.class),
                        any(ReferencedEnvelope.class),
                        any(Rectangle.class),
                        any(CoordinateReferenceSystem.class),
                        anyKeySet(),
                        any(Hints.class),
                        eq(expectedBuffer));
    }

    @SuppressWarnings("unchecked")
    private static Set<RenderingHints.Key> anyKeySet() {
        return (Set<RenderingHints.Key>) any(Set.class);
    }

    @Test
    public void testBuffer() throws Exception {

        ReferencedEnvelope mapBounds = new ReferencedEnvelope(-90, 90, 0, 180, WGS84);
        Rectangle renderingArea = new Rectangle(256, 256);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 32, pointLayer);

        WebMap mockMap = mock(WebMap.class);
        when(tileBuilderMock.build(same(mapContent))).thenReturn(mockMap);

        assertSame(mockMap, outputFormat.produceMap(mapContent));

        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point1"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point2"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point3"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, never())
                .addFeature(eq("points"), eq("pointFar"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("pointNear"), eq("geom"), any(Geometry.class), anyProperties());
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> anyProperties() {
        return any(Map.class);
    }

    @Test
    public void testBufferProject() throws Exception {

        ReferencedEnvelope mapBounds = new ReferencedEnvelope(0, 20_037_508.34, 0, 20_037_508.34, WEB_MERCATOR);
        Rectangle renderingArea = new Rectangle(256, 256);

        ReferencedEnvelope qbounds = new ReferencedEnvelope(mapBounds);
        qbounds.expandBy(20_037_508.34 / 256 * 32);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 32, pointLayer);

        WebMap mockMap = mock(WebMap.class);
        when(tileBuilderMock.build(same(mapContent))).thenReturn(mockMap);

        assertSame(mockMap, outputFormat.produceMap(mapContent));

        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point1"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point2"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point3"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, never())
                .addFeature(eq("points"), eq("pointFar"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("pointNear"), eq("geom"), any(Geometry.class), anyProperties());
    }

    @Test
    public void testSimple() throws Exception {

        ReferencedEnvelope mapBounds = new ReferencedEnvelope(-90, 90, 0, 180, WGS84);
        Rectangle renderingArea = new Rectangle(256, 256);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, null, pointLayer);

        WebMap mockMap = mock(WebMap.class);
        when(tileBuilderMock.build(same(mapContent))).thenReturn(mockMap);

        assertSame(mockMap, outputFormat.produceMap(mapContent));

        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point1"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point2"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point3"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, never())
                .addFeature(eq("points"), eq("pointFar"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, never())
                .addFeature(eq("points"), eq("pointNear"), eq("geom"), any(Geometry.class), anyProperties());
    }

    @Test
    public void testCQLfilter() throws Exception {
        ReferencedEnvelope mapBounds = new ReferencedEnvelope(-90, 90, 0, 180, WGS84);
        Rectangle renderingArea = new Rectangle(256, 256);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, null, pointLayer);
        FeatureLayer layer = (FeatureLayer) mapContent.layers().get(0);
        layer.setQuery(new Query(null, ECQL.toFilter("sp = 'StringProp1_2'")));

        WebMap mockMap = mock(WebMap.class);
        when(tileBuilderMock.build(same(mapContent))).thenReturn(mockMap);

        assertSame(mockMap, outputFormat.produceMap(mapContent));

        verify(tileBuilderMock, never())
                .addFeature(eq("points"), eq("point1"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, times(1))
                .addFeature(eq("points"), eq("point2"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, never())
                .addFeature(eq("points"), eq("point3"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, never())
                .addFeature(eq("points"), eq("pointFar"), eq("geom"), any(Geometry.class), anyProperties());
        verify(tileBuilderMock, never())
                .addFeature(eq("points"), eq("pointNear"), eq("geom"), any(Geometry.class), anyProperties());
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

        mapContents.add(map);

        return map;
    }

    protected GetMapRequest createGetMapRequest(
            ReferencedEnvelope requestEnvelope, Rectangle renderingArea, Integer buffer) {
        GetMapRequest request = new GetMapRequest();
        request.setBaseUrl("http://localhost:8080/geoserver");

        List<MapLayerInfo> layers = new ArrayList<>();
        List<Style> styles = new ArrayList<>();

        // for (int i = 0; i < layerNames.length; i++) {
        // LayerInfo layerInfo = getCatalog().getLayerByName(layerNames[i].getLocalPart());
        // try {
        // styles.add(layerInfo.getDefaultStyle().getStyle());
        // } catch (IOException e) {
        // throw new RuntimeException(e);
        // }
        // layers.add(new MapLayerInfo(layerInfo));
        // }

        request.setLayers(layers);
        request.setStyles(styles);
        request.setBbox(requestEnvelope);
        request.setCrs(requestEnvelope.getCoordinateReferenceSystem());
        if (requestEnvelope.getCoordinateReferenceSystem() == WGS84) {
            request.setSRS("EPSG:4326");
        } else if (requestEnvelope.getCoordinateReferenceSystem() == WEB_MERCATOR) {
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

    @Test
    public void testPregeneralized() throws Exception {
        // Simple class to MOCK a Datastore supporting PreGeneralized features
        final class PregenDataStore extends MemoryDataStore {

            static final class _FeatureSource extends MemoryFeatureSource {
                public _FeatureSource(ContentEntry entry, Query q) {
                    super(entry, q);
                }

                @Override
                protected void addHints(Set<org.geotools.util.factory.Hints.Key> hints) {
                    hints.add(Hints.GEOMETRY_DISTANCE);
                }
            }

            /** Creates the new feature store. */
            public PregenDataStore() throws IOException {
                super();
            }

            @Override
            protected ContentFeatureSource createFeatureSource(ContentEntry entry, Query query) {
                return new _FeatureSource(entry, query);
            }
        }

        final String polyTypeSpec = "sp:String,ip:Integer,geom:Polygon:srid=4326";
        final SimpleFeatureType pregenPolyType = DataUtilities.createType("pregenPolygon", polyTypeSpec);

        final MemoryDataStore _ds = new PregenDataStore();
        _ds.addFeature(feature(
                pregenPolyType, "pregenPolygon1", "StringPropX_1", 1000, "POLYGON ((1 1, 2 2, 3 3, 4 4, 1 1))"));

        final SimpleFeatureSource fs = _ds.getFeatureSource("pregenPolygon");

        final FeatureLayer pregeneralizedLayer = new FeatureLayer(fs, defaultPolygonStyle);

        final ReferencedEnvelope mapBounds = new ReferencedEnvelope(-90, 90, 0, 180, WGS84);
        final Rectangle renderingArea = new Rectangle(256, 256);

        final WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 0, pregeneralizedLayer);

        // ensure that the FeatureSource supports GEOMETRY_DISTANCE
        Assert.assertTrue(
                pregeneralizedLayer.getSimpleFeatureSource().getSupportedHints().contains(Hints.GEOMETRY_DISTANCE));

        MapBoxTileBuilderFactory mbbf = new MapBoxTileBuilderFactory();
        VectorTileMapOutputFormat vtof = new VectorTileMapOutputFormat(mbbf);
        VectorTileMapOutputFormat vtof_spy = Mockito.spy(vtof);

        // lets produce a map
        vtof_spy.produceMap(mapContent);

        // verify that the Pipeline recognize the Hint support adding the hint to the query
        Mockito.verify(vtof_spy)
                .getPipeline(
                        any(WMSMapContent.class),
                        any(ReferencedEnvelope.class),
                        any(Rectangle.class),
                        any(CoordinateReferenceSystem.class),
                        anyKeySet(),
                        argThat((Hints qH) -> qH.containsKey(Hints.GEOMETRY_DISTANCE)),
                        any(Integer.class));
    }

    // @Test
    // public void testMapBoxTileBuilder() throws Exception {
    //
    // Envelope mapBounds = new Envelope(-92.8, -93.2, 4.5, 4.6);
    // mapBounds = JTS.transform(mapBounds,
    // CRS.findMathTransform(CRS.decode("EPSG:4326"), CRS.decode("EPSG:900913"), true));
    //
    // WMSMapContent mapContent = createMapContent("EPSG:900913", mapBounds, POINTS, POLYGONS);
    // mapContent.setMapHeight(256);
    // mapContent.setMapWidth(256);
    //
    // MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();
    //
    // VectorTileMapOutputFormat outputFormat = new VectorTileMapOutputFormat(getWMS(),
    // builderFact);
    // outputFormat.setTransformToScreenCoordinates(true);
    //
    // RawMap map = (RawMap) outputFormat.produceMap(mapContent);
    // ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // map.writeTo(bos);
    // VectorTileDecoder decoder = new VectorTileDecoder();
    // decoder.setAutoScale(false);
    //
    // FeatureIterable feats = decoder.decode(bos.toByteArray());
    //
    // for (Feature feat : feats) {
    // System.out.println(feat.getLayerName() + ": ");
    // System.out.print(feat.getAttributes());
    // System.out.println(feat.getGeometry());
    // }
    //
    // bos.close();
    //
    // List<Feature> featList = feats.asList();
    // assertEquals(2, featList.size());
    // assertEquals("Points", featList.get(0).getLayerName());
    // assertTrue(featList.get(0).getGeometry() instanceof Point);
    // assertEquals(new Coordinate(641, 973),
    // ((Point) featList.get(0).getGeometry()).getCoordinate());
    // assertEquals("Polygons", featList.get(1).getLayerName());
    // assertTrue(featList.get(1).getGeometry() instanceof Polygon);
    // assertEquals(new Coordinate(646, 976),
    // (((Polygon) featList.get(1).getGeometry()).getCoordinates()[0]));
    // }
    //
    // @Test
    // public void testMapBoxTileBuilderOtherCrs() throws Exception {
    // WMSMapContent mapContent = createMapContent("EPSG:4326", new Envelope(-92.8, -93.2, 4.5,
    // 4.6), POINTS, POLYGONS);
    //
    // mapContent.setMapHeight(256);
    // mapContent.setMapWidth(256);
    //
    // MapBoxTileBuilderFactory builderFact = new MapBoxTileBuilderFactory();
    //
    // VectorTileMapOutputFormat outputFormat = new VectorTileMapOutputFormat(getWMS(),
    // builderFact);
    // outputFormat.setTransformToScreenCoordinates(true);
    //
    // RawMap map = (RawMap) outputFormat.produceMap(mapContent);
    // ByteArrayOutputStream bos = new ByteArrayOutputStream();
    // map.writeTo(bos);
    // VectorTileDecoder decoder = new VectorTileDecoder();
    // decoder.setAutoScale(false);
    //
    // FeatureIterable feats = decoder.decode(bos.toByteArray());
    //
    // for (Feature feat : feats) {
    // System.out.println(feat.getLayerName() + ": ");
    // System.out.print(feat.getAttributes());
    // System.out.println(feat.getGeometry());
    // }
    //
    // bos.close();
    //
    // List<Feature> featList = feats.asList();
    // assertEquals(2, featList.size());
    // assertEquals("Points", featList.get(0).getLayerName());
    // assertTrue(featList.get(0).getGeometry() instanceof Point);
    // assertEquals(new Coordinate(641, 973),
    // ((Point) featList.get(0).getGeometry()).getCoordinate());
    // assertEquals("Polygons", featList.get(1).getLayerName());
    // assertTrue(featList.get(1).getGeometry() instanceof Polygon);
    // assertEquals(new Coordinate(646, 976),
    // (((Polygon) featList.get(1).getGeometry()).getCoordinates()[0]));
    // }

    /**
     * Builds a feature based on feature type, identifier, and values. The geometry can be specified as a WKT string, it
     * will be parsed
     */
    public static SimpleFeature feature(SimpleFeatureType type, String id, Object... values) throws ParseException {

        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);

        for (int i = 0; i < values.length; i++) {
            Object value = values[i];
            if (type.getDescriptor(i) instanceof GeometryDescriptor) {
                if (value instanceof String) {
                    value = parseGeometry((String) value);
                }
            }
            builder.set(i, value);
        }
        return builder.buildFeature(id);
    }

    private static Geometry parseGeometry(String value) throws ParseException {
        return new WKTReader2().read(value);
    }

    private static Style parseStyle(String styleResource) throws IOException {
        try (InputStream in = GeoServerLoader.class.getResourceAsStream(styleResource)) {
            StyledLayerDescriptor sld = new SLDHandler().parse(in, null, null, null);
            return ((NamedLayer) sld.getStyledLayers()[0]).getStyles()[0];
        }
    }

    @Test
    public void testLabelPoints() throws Exception {
        ReferencedEnvelope mapBounds = new ReferencedEnvelope(0, 15, 0, 15, WGS84);
        Rectangle renderingArea = new Rectangle(256, 256);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 0, labelPolygonLayer);

        WebMap mockMap = mock(WebMap.class);
        when(tileBuilderMock.build(same(mapContent))).thenReturn(mockMap);

        assertSame(mockMap, outputFormat.produceMap(mapContent));

        verify(tileBuilderMock, times(1))
                .addFeature(
                        eq("polygons_labels"),
                        eq("polygon1"),
                        eq("geom"),
                        eq(parseGeometry("POINT (1.5 0.5)")),
                        eq(Map.of("sp", (Object) "StringProp3_1")));
        verify(tileBuilderMock, times(1))
                .addFeature(
                        eq("polygons_labels"),
                        eq("polygon2"),
                        eq("geom"),
                        eq(parseGeometry("POINT (6.5 6.5)")),
                        eq(Map.of("sp", (Object) "StringProp3_2")));
        verify(tileBuilderMock, times(1))
                .addFeature(
                        eq("polygons_labels"),
                        eq("polygon3"),
                        eq("geom"),
                        eq(parseGeometry("POINT (11.5 11.5)")),
                        eq(Map.of("sp", (Object) "StringProp3_3")));
    }

    @Test
    public void testAttributeSelection() throws Exception {
        ReferencedEnvelope mapBounds = new ReferencedEnvelope(0, 15, 0, 15, WGS84);
        Rectangle renderingArea = new Rectangle(256, 256);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 0, attributesPolygonLayer);

        WebMap mockMap = mock(WebMap.class);
        when(tileBuilderMock.build(same(mapContent))).thenReturn(mockMap);

        assertSame(mockMap, outputFormat.produceMap(mapContent));

        verify(tileBuilderMock, times(1))
                .addFeature(eq("polygons"), eq("polygon1"), eq("geom"), any(Polygon.class), eq(Map.of("ip", 1000)));
        verify(tileBuilderMock, times(1))
                .addFeature(eq("polygons"), eq("polygon2"), eq("geom"), any(Polygon.class), eq(Map.of("ip", 2000)));
        verify(tileBuilderMock, times(1))
                .addFeature(eq("polygons"), eq("polygon3"), eq("geom"), any(Polygon.class), eq(Map.of("ip", 3000)));
    }

    @Test
    public void testCoalesce() throws Exception {
        // add one feature that can be merged with the others
        ds.addFeature(feature(
                ds.getSchema("polygons"),
                "polygon4",
                "StringProp3_3",
                3000,
                "POLYGON ((15 15, 16 15, 16 16, 15 16, 15 15))"));
        // a feature layer with only two features having the same attributes, for simplicity
        FeatureLayer coalescePolygonLayer = new FeatureLayer(ds.getFeatureSource("polygons"), coalescePolygonStyle);
        coalescePolygonLayer.setQuery(new Query("polygons", ECQL.toFilter("sp = 'StringProp3_3'")));

        ReferencedEnvelope mapBounds = new ReferencedEnvelope(0, 15, 0, 15, WGS84);
        Rectangle renderingArea = new Rectangle(256, 256);

        WMSMapContent mapContent = createMapContent(mapBounds, renderingArea, 0, coalescePolygonLayer);

        WebMap mockMap = mock(WebMap.class);
        when(tileBuilderMock.build(same(mapContent))).thenReturn(mockMap);

        assertSame(mockMap, outputFormat.produceMap(mapContent));

        // only one invocation total
        verify(tileBuilderMock, times(1)).addFeature(any(), any(), any(), any(), any());
        // with the merged feature (geometry is a multi-polygon)
        verify(tileBuilderMock, times(1))
                .addFeature(
                        eq("polygons"),
                        eq("polygon3"),
                        eq("geom"),
                        any(MultiPolygon.class),
                        eq(Map.of("sp", "StringProp3_3", "ip", 3000)));
    }
}
