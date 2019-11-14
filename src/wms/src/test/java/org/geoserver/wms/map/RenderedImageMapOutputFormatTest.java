/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.geoserver.data.test.CiteTestData.STREAMS;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints.Key;
import java.awt.Shape;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.xml.namespace.QName;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.LayerInfo.WMSInterpolation;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geoserver.wms.CachedGridReaderLayer;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSPartialMapException;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.util.FeatureUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.feature.SchemaException;
import org.geotools.filter.IllegalFilterException;
import org.geotools.gce.imagemosaic.ImageMosaicReader;
import org.geotools.geometry.Envelope2D;
import org.geotools.geometry.GeneralEnvelope;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.test.ImageAssert;
import org.geotools.image.util.ImageUtilities;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.renderer.lite.LabelCache;
import org.geotools.renderer.lite.StreamingRenderer;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ChannelSelectionImpl;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.SelectedChannelTypeImpl;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.TextSymbolizer;
import org.geotools.util.NumberRange;
import org.geotools.util.URLs;
import org.geotools.util.factory.FactoryRegistryException;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.LineString;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.opengis.coverage.grid.GridEnvelope;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.mock.web.MockHttpServletResponse;

public class RenderedImageMapOutputFormatTest extends WMSTestSupport {

    public static QName TAZ_BYTE = new QName(MockData.WCS_URI, "tazbyte", MockData.WCS_PREFIX);

    public static QName SIX_BANDS = new QName(MockData.WCS_URI, "sixbands", MockData.WCS_PREFIX);

    static final QName STRAIGHT_VERTICAL_LINE =
            new QName(MockData.CITE_URI, "STRAIGHT_VERTICAL_LINE", MockData.CITE_PREFIX);

    static final String STRAIGHT_VERTICAL_LINE_STYLE = "verticalline";

    static final QName NORMALIZED =
            new QName(MockData.CITE_URI, "NORMALIZED", MockData.CITE_PREFIX);

    static final String NORMALIZED_STYLE = "normalized";

    static final QName CROSS_DATELINE =
            new QName(MockData.CITE_URI, "CROSS_DATELINE", MockData.CITE_PREFIX);

    static final String CROSS_DATELINE_STYLE = "crossline";

    static final QName TIFF_3035 = new QName(MockData.SF_URI, "3035", MockData.SF_PREFIX);

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(
                    RenderedImageMapOutputFormatTest.class.getPackage().getName());

    private RenderedImageMapOutputFormat rasterMapProducer;

    private String mapFormat = "image/gif";

    private static final ThreadLocal<Boolean> usedCustomLabelCache = new ThreadLocal<Boolean>();

    public static class CustomLabelCache implements LabelCache {
        public CustomLabelCache() {}

        @Override
        public void clear() {}

        @Override
        public void clear(String arg0) {}

        @Override
        public void disableLayer(String arg0) {}

        @Override
        public void enableLayer(String arg0) {}

        @Override
        public void end(Graphics2D arg0, Rectangle arg1) {
            usedCustomLabelCache.set(true);
        }

        @Override
        public void endLayer(String arg0, Graphics2D arg1, Rectangle arg2) {}

        @Override
        public List orderedLabels() {
            return null;
        }

        @Override
        public void put(Rectangle2D arg0) {}

        @Override
        public void put(
                String arg0,
                TextSymbolizer arg1,
                Feature arg2,
                LiteShape2 arg3,
                NumberRange<Double> arg4) {}

        @Override
        public void start() {}

        @Override
        public void startLayer(String arg0) {}

        @Override
        public void stop() {}
    };

    @Before
    public void setRasterMapProducer() throws Exception {
        Logging.getLogger("org.geotools.rendering").setLevel(Level.OFF);
        this.rasterMapProducer = getProducerInstance();

        getTestData().addDefaultRasterLayer(SystemTestData.MULTIBAND, getCatalog());
    }

    protected RenderedImageMapOutputFormat getProducerInstance() {
        return new DummyRasterMapProducer(getWMS());
    }

    @After
    public void unsetRasterMapProducer() throws Exception {
        this.rasterMapProducer = null;
    }

    public String getMapFormat() {
        return this.mapFormat;
    }

    @Test
    public void testSimpleGetMapQuery() throws Exception {

        Catalog catalog = getCatalog();
        final FeatureSource fs =
                catalog.getFeatureTypeByName(
                                MockData.BASIC_POLYGONS.getPrefix(),
                                MockData.BASIC_POLYGONS.getLocalPart())
                        .getFeatureSource(null, null);

        final Envelope env = fs.getBounds();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName("Default");
        Style basicStyle = styleByName.getStyle();
        map.addLayer(new FeatureLayer(fs, basicStyle));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("testSimpleGetMapQuery", image);
    }

    @Test
    public void testAdvancedProjectionDensification() throws Exception {
        WMS wms = getWMS();
        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.ADVANCED_PROJECTION_DENSIFICATION_KEY, true);
        getGeoServer().save(info);
        Graphics2D graphics = Mockito.mock(Graphics2D.class);
        this.rasterMapProducer = new DummyRasterMapProducer(wms);
        ((DummyRasterMapProducer) this.rasterMapProducer).setGraphics(graphics);

        Catalog catalog = getCatalog();
        final FeatureSource fs =
                catalog.getFeatureTypeByName(
                                STRAIGHT_VERTICAL_LINE.getPrefix(),
                                STRAIGHT_VERTICAL_LINE.getLocalPart())
                        .getFeatureSource(null, null);

        final ReferencedEnvelope env =
                new ReferencedEnvelope(10, 15, 0, 50, DefaultGeographicCRS.WGS84);
        CoordinateReferenceSystem utm32 = CRS.decode("EPSG:32632");
        ReferencedEnvelope targetEnv = env.transform(utm32, true);

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(targetEnv);
        map.getViewport().setCoordinateReferenceSystem(utm32);
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName(STRAIGHT_VERTICAL_LINE_STYLE);
        Style basicStyle = styleByName.getStyle();
        map.addLayer(new FeatureLayer(fs, basicStyle));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("densify", image);
        ArgumentCaptor<Shape> shape = ArgumentCaptor.forClass(Shape.class);
        Mockito.verify(graphics).draw(shape.capture());
        LiteShape2 drawnShape = (LiteShape2) shape.getValue();
        assertEquals(64, drawnShape.getGeometry().getCoordinates().length);
    }

    @Test
    public void testAdvancedProjectionDensificationWithFormatOption() throws Exception {
        Graphics2D graphics = Mockito.mock(Graphics2D.class);
        this.rasterMapProducer = new DummyRasterMapProducer(getWMS());
        ((DummyRasterMapProducer) this.rasterMapProducer).setGraphics(graphics);

        Catalog catalog = getCatalog();
        final FeatureSource fs =
                catalog.getFeatureTypeByName(
                                STRAIGHT_VERTICAL_LINE.getPrefix(),
                                STRAIGHT_VERTICAL_LINE.getLocalPart())
                        .getFeatureSource(null, null);

        final ReferencedEnvelope env =
                new ReferencedEnvelope(10, 15, 0, 50, DefaultGeographicCRS.WGS84);
        CoordinateReferenceSystem utm32 = CRS.decode("EPSG:32632");
        ReferencedEnvelope targetEnv = env.transform(utm32, true);

        GetMapRequest request = new GetMapRequest();
        request.getFormatOptions().put("advancedProjectionHandlingDensification", "true");
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(targetEnv);
        map.getViewport().setCoordinateReferenceSystem(utm32);
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName(STRAIGHT_VERTICAL_LINE_STYLE);
        Style basicStyle = styleByName.getStyle();
        map.addLayer(new FeatureLayer(fs, basicStyle));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("densify", image);
        ArgumentCaptor<Shape> shape = ArgumentCaptor.forClass(Shape.class);
        Mockito.verify(graphics).draw(shape.capture());
        LiteShape2 drawnShape = (LiteShape2) shape.getValue();
        assertEquals(64, drawnShape.getGeometry().getCoordinates().length);
    }

    @Test
    public void testWrappingHeuristic() throws Exception {
        WMS wms = getWMS();
        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.DATELINE_WRAPPING_HEURISTIC_KEY, false);
        getGeoServer().save(info);
        Graphics2D graphics = Mockito.mock(Graphics2D.class);
        this.rasterMapProducer = new DummyRasterMapProducer(wms);
        ((DummyRasterMapProducer) this.rasterMapProducer).setGraphics(graphics);

        Catalog catalog = getCatalog();
        final FeatureSource fs =
                catalog.getFeatureTypeByName(
                                CROSS_DATELINE.getPrefix(), CROSS_DATELINE.getLocalPart())
                        .getFeatureSource(null, null);

        final ReferencedEnvelope env =
                new ReferencedEnvelope(-150, 150, -30, 30, DefaultGeographicCRS.WGS84);

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName(STRAIGHT_VERTICAL_LINE_STYLE);
        Style basicStyle = styleByName.getStyle();
        map.addLayer(new FeatureLayer(fs, basicStyle));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("dateline_heuristic", image);
        ArgumentCaptor<Shape> shape = ArgumentCaptor.forClass(Shape.class);
        Mockito.verify(graphics).draw(shape.capture());
        LiteShape2 drawnShape = (LiteShape2) shape.getValue();
        // used to become a multilinestring due to a wrapping bug that has been solved,
        // but now it's a LineString...
        // assertTrue(drawnShape.getGeometry() instanceof MultiLineString);
        assertTrue(drawnShape.getGeometry() instanceof LineString);
    }

    @Test
    public void testDisabledWrappingHeuristic() throws Exception {
        WMS wms = getWMS();
        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.DATELINE_WRAPPING_HEURISTIC_KEY, true);
        getGeoServer().save(info);
        Graphics2D graphics = Mockito.mock(Graphics2D.class);
        this.rasterMapProducer = new DummyRasterMapProducer(wms);
        ((DummyRasterMapProducer) this.rasterMapProducer).setGraphics(graphics);

        Catalog catalog = getCatalog();
        final FeatureSource fs =
                catalog.getFeatureTypeByName(
                                CROSS_DATELINE.getPrefix(), CROSS_DATELINE.getLocalPart())
                        .getFeatureSource(null, null);

        final ReferencedEnvelope env =
                new ReferencedEnvelope(-150, 150, -30, 30, DefaultGeographicCRS.WGS84);

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName(STRAIGHT_VERTICAL_LINE_STYLE);
        Style basicStyle = styleByName.getStyle();
        map.addLayer(new FeatureLayer(fs, basicStyle));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("dateline_heuristic", image);
        ArgumentCaptor<Shape> shape = ArgumentCaptor.forClass(Shape.class);
        Mockito.verify(graphics).draw(shape.capture());
        LiteShape2 drawnShape = (LiteShape2) shape.getValue();
        assertTrue(drawnShape.getGeometry() instanceof LineString);
    }

    @Test
    public void testDisabledWrappingHeuristicWithFormatOption() throws Exception {
        Graphics2D graphics = Mockito.mock(Graphics2D.class);
        this.rasterMapProducer = new DummyRasterMapProducer(getWMS());
        ((DummyRasterMapProducer) this.rasterMapProducer).setGraphics(graphics);

        Catalog catalog = getCatalog();
        final FeatureSource fs =
                catalog.getFeatureTypeByName(
                                CROSS_DATELINE.getPrefix(), CROSS_DATELINE.getLocalPart())
                        .getFeatureSource(null, null);

        final ReferencedEnvelope env =
                new ReferencedEnvelope(-150, 150, -30, 30, DefaultGeographicCRS.WGS84);

        GetMapRequest request = new GetMapRequest();
        request.getFormatOptions().put("disableDatelineWrappingHeuristic", "true");
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(env);
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName(STRAIGHT_VERTICAL_LINE_STYLE);
        Style basicStyle = styleByName.getStyle();
        map.addLayer(new FeatureLayer(fs, basicStyle));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("dateline_heuristic", image);
        ArgumentCaptor<Shape> shape = ArgumentCaptor.forClass(Shape.class);
        Mockito.verify(graphics).draw(shape.capture());
        LiteShape2 drawnShape = (LiteShape2) shape.getValue();
        assertTrue(drawnShape.getGeometry() instanceof LineString);
    }

    @Test
    public void testAdvancedProjectionWithoutDensification() throws Exception {
        WMS wms = getWMS();
        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.ADVANCED_PROJECTION_DENSIFICATION_KEY, false);
        getGeoServer().save(info);
        Graphics2D graphics = Mockito.mock(Graphics2D.class);
        this.rasterMapProducer = new DummyRasterMapProducer(wms);
        ((DummyRasterMapProducer) this.rasterMapProducer).setGraphics(graphics);

        Catalog catalog = getCatalog();
        final FeatureSource fs =
                catalog.getFeatureTypeByName(
                                STRAIGHT_VERTICAL_LINE.getPrefix(),
                                STRAIGHT_VERTICAL_LINE.getLocalPart())
                        .getFeatureSource(null, null);

        final ReferencedEnvelope env =
                new ReferencedEnvelope(10, 15, 0, 50, DefaultGeographicCRS.WGS84);
        CoordinateReferenceSystem utm32 = CRS.decode("EPSG:32632");
        ReferencedEnvelope targetEnv = env.transform(utm32, true);

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.getViewport().setBounds(targetEnv);
        map.getViewport().setCoordinateReferenceSystem(utm32);
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.setRequest(request);

        StyleInfo styleByName = catalog.getStyleByName(STRAIGHT_VERTICAL_LINE_STYLE);
        Style basicStyle = styleByName.getStyle();
        map.addLayer(new FeatureLayer(fs, basicStyle));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("densify", image);
        ArgumentCaptor<Shape> shape = ArgumentCaptor.forClass(Shape.class);
        Mockito.verify(graphics).draw(shape.capture());
        LiteShape2 drawnShape = (LiteShape2) shape.getValue();
        assertEquals(2, drawnShape.getGeometry().getCoordinates().length);
    }

    @Test
    public void testDisableGutter() throws Exception {
        setDisableGutter(true);
        WMS wms = getWMS();
        WMSInfo info = wms.getServiceInfo();
        info.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, false);
        getGeoServer().save(info);
        Catalog catalog = getCatalog();
        CoverageInfo ci =
                catalog.getCoverageByName(
                        SystemTestData.WORLD.getPrefix(), SystemTestData.WORLD.getLocalPart());

        GetMapRequest request = new GetMapRequest();
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857");
        CoordinateReferenceSystem wgs84 = DefaultGeographicCRS.WGS84;
        GeneralEnvelope env =
                GeneralEnvelope.toGeneralEnvelope(new Envelope2D(wgs84, -40, 0, 40, 80));
        MathTransform transform = CRS.findMathTransform(wgs84, crs);
        env = CRS.transform(transform, env);
        ReferencedEnvelope bbox =
                new ReferencedEnvelope(
                        env.getMinimum(0),
                        env.getMaximum(0),
                        env.getMinimum(1),
                        env.getMaximum(1),
                        crs);
        request.setBbox(bbox);
        request.setInterpolations(
                Collections.singletonList(
                        Interpolation.getInstance(Interpolation.INTERP_BILINEAR)));
        request.setSRS("EPSG:3857");
        request.setFormat("image/png");

        final WMSMapContent map = new WMSMapContent(request);
        final int width = 300;
        final int height = 300;
        map.setMapWidth(width);
        map.setMapHeight(height);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.getViewport().setBounds(bbox);

        StyleBuilder builder = new StyleBuilder();
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        reader.getCoordinateReferenceSystem();
        Layer l =
                new CachedGridReaderLayer(
                        reader, builder.createStyle(builder.createRasterSymbolizer()));
        map.addLayer(l);

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        RenderedImage image = imageMap.getImage();
        RenderedImage warp[] = new RenderedImage[1];
        lookForOp("Warp", image, warp);
        // No Gutter has been done
        assertEquals(width, image.getWidth());
        assertEquals(height, image.getHeight());

        imageMap.dispose();
        setDisableGutter(false);
        wms = getWMS();
        info = wms.getServiceInfo();
        info.getMetadata().put(WMS.ADVANCED_PROJECTION_KEY, true);
        getGeoServer().save(info);
    }

    private void lookForOp(String opName, RenderedImage image, RenderedImage[] returnedOp) {
        if (image instanceof RenderedOp) {
            RenderedOp op = (RenderedOp) image;
            String operationName = op.getOperationName();
            if (opName.equalsIgnoreCase(operationName)) {
                returnedOp[0] = op;
                return;
            } else {
                Vector sources = op.getSources();
                if (sources != null && !sources.isEmpty()) {
                    Iterator iterator = sources.iterator();
                    while (iterator.hasNext() && returnedOp[0] == null) {
                        Object next = iterator.next();
                        if (next instanceof RenderedImage) {
                            lookForOp(opName, (RenderedImage) next, returnedOp);
                        }
                    }
                }
            }
        }
    }

    private void setDisableGutter(boolean value)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException,
                    IllegalAccessException {
        Field field = RenderedImageMapOutputFormat.class.getDeclaredField("DISABLE_GUTTER");
        field.setAccessible(true);
        field.set(null, value);
    }

    /**
     * Test to make sure the "direct" raster path and the "nondirect" raster path produce matching
     * results. This test was originally created after fixes to GEOS-7270 where there were issues
     * with images generated during the direct raster path but not in the normal path, stemming from
     * not setting the background color the same way
     */
    @Test
    public void testDirectVsNonDirectRasterRender() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci =
                catalog.getCoverageByName(
                        SystemTestData.MULTIBAND.getPrefix(),
                        SystemTestData.MULTIBAND.getLocalPart());

        final Envelope env = ci.boundingBox();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        GetMapRequest request = new GetMapRequest();
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        ReferencedEnvelope bbox =
                new ReferencedEnvelope(
                        new Envelope(
                                -116.90673461649858211,
                                -114.30988665660261461,
                                32.07093728218402617,
                                33.89032847348440214),
                        crs);
        request.setBbox(bbox);
        request.setSRS("urn:x-ogc:def:crs:EPSG:4326");
        request.setFormat("image/png");

        final WMSMapContent map = new WMSMapContent(request);
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.getViewport().setBounds(bbox);

        StyleBuilder builder = new StyleBuilder();
        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        reader.getCoordinateReferenceSystem();
        Layer l =
                new CachedGridReaderLayer(
                        reader, builder.createStyle(builder.createRasterSymbolizer()));
        map.addLayer(l);

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        ImageAssert.assertEquals(
                new File("src/test/resources/org/geoserver/wms/map/direct-raster-expected.tif"),
                imageMap.getImage(),
                0);
        imageMap.dispose();
    }

    @Test
    public void testTimeoutOption() throws Exception {
        Catalog catalog = getCatalog();
        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();

        StyleInfo styleByName = catalog.getStyleByName("Default");
        Style basicStyle = styleByName.getStyle();

        // Build up a complex map so that we can reasonably guarantee a 1 ms timout
        SimpleFeatureSource fs =
                (SimpleFeatureSource)
                        catalog.getFeatureTypeByName(
                                        MockData.BASIC_POLYGONS.getPrefix(),
                                        MockData.BASIC_POLYGONS.getLocalPart())
                                .getFeatureSource(null, null);
        Envelope env = fs.getBounds();
        SimpleFeatureCollection features = fs.getFeatures();
        SimpleFeatureCollection delayedCollection = new DelayedFeatureCollection(features, 50);
        map.addLayer(new FeatureLayer(delayedCollection, basicStyle));

        LOGGER.info(
                "about to create map ctx for "
                        + map.layers().size()
                        + " layers with bounds "
                        + env);

        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(1000);
        map.setMapHeight(1000);
        map.setRequest(request);

        request.setFormat(getMapFormat());
        Map formatOptions = new HashMap();
        // 1 ms timeout
        formatOptions.put("timeout", 1);
        request.setFormatOptions(formatOptions);
        try {
            RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
            fail("Timeout was not reached");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().startsWith("This request used more time than allowed"));
        }

        // Test partial image exception format
        Map rawKvp = new HashMap();
        rawKvp.put("EXCEPTIONS", "PARTIALMAP");
        request.setRawKvp(rawKvp);

        try {
            RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
            fail("Timeout was not reached");
        } catch (ServiceException e) {
            assertTrue(e instanceof WMSPartialMapException);
            assertTrue(
                    e.getCause()
                            .getMessage()
                            .startsWith("This request used more time than allowed"));
            RenderedImageMap partialMap = (RenderedImageMap) ((WMSPartialMapException) e).getMap();
            assertNotNull(partialMap);
            assertNotNull(partialMap.getImage());
        }
    }

    @Test
    public void testDefaultStyle() throws Exception {
        List<org.geoserver.catalog.FeatureTypeInfo> typeInfos = getCatalog().getFeatureTypes();

        for (org.geoserver.catalog.FeatureTypeInfo info : typeInfos) {
            if (info.getQualifiedName().getNamespaceURI().equals(MockData.CITE_URI)
                    && info.getFeatureType().getGeometryDescriptor() != null)
                testDefaultStyle(info.getFeatureSource(null, null));
        }
    }

    @Test
    public void testBlueLake() throws IOException, IllegalFilterException, Exception {
        final Catalog catalog = getCatalog();
        org.geoserver.catalog.FeatureTypeInfo typeInfo =
                catalog.getFeatureTypeByName(
                        MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        Envelope env = typeInfo.getFeatureSource(null, null).getBounds();
        double shift = env.getWidth() / 6;

        env =
                new Envelope(
                        env.getMinX() - shift,
                        env.getMaxX() + shift,
                        env.getMinY() - shift,
                        env.getMaxY() + shift);

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        int w = 400;
        int h = (int) Math.round((env.getHeight() * w) / env.getWidth());
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        map.setRequest(request);

        addToMap(map, MockData.FORESTS);
        addToMap(map, MockData.LAKES);
        addToMap(map, MockData.STREAMS);
        addToMap(map, MockData.NAMED_PLACES);
        addToMap(map, MockData.ROAD_SEGMENTS);
        addToMap(map, MockData.PONDS);
        addToMap(map, MockData.BUILDINGS);
        addToMap(map, MockData.DIVIDED_ROUTES);
        addToMap(map, MockData.BRIDGES);
        addToMap(map, MockData.MAP_NEATLINE);

        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));

        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertNotBlank("testBlueLake", image);
    }

    @Test
    public void testCustomLabelCache() throws IOException {
        final Catalog catalog = getCatalog();
        org.geoserver.catalog.FeatureTypeInfo typeInfo =
                catalog.getFeatureTypeByName(
                        MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        Envelope env = typeInfo.getFeatureSource(null, null).getBounds();
        double shift = env.getWidth() / 6;

        env =
                new Envelope(
                        env.getMinX() - shift,
                        env.getMaxX() + shift,
                        env.getMinY() - shift,
                        env.getMaxY() + shift);

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        int w = 400;
        int h = (int) Math.round((env.getHeight() * w) / env.getWidth());
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        map.setRequest(request);

        addToMap(map, MockData.FORESTS);
        addToMap(map, MockData.LAKES);
        addToMap(map, MockData.STREAMS);
        addToMap(map, MockData.NAMED_PLACES);
        addToMap(map, MockData.ROAD_SEGMENTS);
        addToMap(map, MockData.PONDS);
        addToMap(map, MockData.BUILDINGS);
        addToMap(map, MockData.DIVIDED_ROUTES);
        addToMap(map, MockData.BRIDGES);
        addToMap(map, MockData.MAP_NEATLINE);

        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));

        request.setFormat(getMapFormat());

        this.rasterMapProducer.setLabelCache(
                new Function<WMSMapContent, LabelCache>() {
                    @Override
                    public LabelCache apply(WMSMapContent mapContent) {
                        return new CustomLabelCache();
                    }
                });
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();
        assertTrue(usedCustomLabelCache.get());
        assertNotBlank("testBlueLake", image);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addDefaultRasterLayer(MockData.TASMANIA_DEM, getCatalog());
        testData.addRasterLayer(TAZ_BYTE, "tazbyte.tiff", null, getCatalog());
        testData.addRasterLayer(SIX_BANDS, "6b.tiff", null, getCatalog());
        testData.addStyle(NORMALIZED_STYLE, "normalized.sld", getClass(), getCatalog());
        testData.addRasterLayer(
                TIFF_3035,
                "3035.zip",
                "tif",
                null,
                RenderedImageMapOutputFormatTest.class,
                getCatalog());

        testData.addStyle(
                STRAIGHT_VERTICAL_LINE_STYLE, "verticalline.sld", getClass(), getCatalog());
        Map properties = new HashMap();
        properties.put(MockData.KEY_STYLE, STRAIGHT_VERTICAL_LINE_STYLE);
        testData.addVectorLayer(
                STRAIGHT_VERTICAL_LINE,
                properties,
                "VerticalLine.properties",
                getClass(),
                getCatalog());

        testData.addStyle(CROSS_DATELINE_STYLE, "crossline.sld", getClass(), getCatalog());
        properties = new HashMap();
        properties.put(MockData.KEY_STYLE, CROSS_DATELINE_STYLE);
        testData.addVectorLayer(
                CROSS_DATELINE, properties, "CrossLine.properties", getClass(), getCatalog());
    }

    @Test
    public void testInterpolations() throws IOException, IllegalFilterException, Exception {
        final Catalog catalog = getCatalog();
        CoverageInfo coverageInfo =
                catalog.getCoverageByName(
                        MockData.TASMANIA_DEM.getNamespaceURI(),
                        MockData.TASMANIA_DEM.getLocalPart());

        Envelope env = coverageInfo.boundingBox();
        double shift = env.getWidth() / 6;

        env =
                new Envelope(
                        env.getMinX() - shift,
                        env.getMaxX() + shift,
                        env.getMinY() - shift,
                        env.getMaxY() + shift);

        GetMapRequest request = new GetMapRequest();
        WMSMapContent map = new WMSMapContent();
        int w = 400;
        int h = (int) Math.round((env.getHeight() * w) / env.getWidth());
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        map.setRequest(request);
        addRasterToMap(map, MockData.TASMANIA_DEM);

        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        request.setInterpolations(
                Arrays.asList(Interpolation.getInstance(Interpolation.INTERP_NEAREST)));
        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        RenderedOp op =
                (RenderedOp) ((RenderedImageTimeDecorator) imageMap.getImage()).getDelegate();
        BufferedImage imageNearest = op.getAsBufferedImage();
        imageMap.dispose();
        assertNotBlank("testInterpolationsNearest", imageNearest);

        request = new GetMapRequest();
        map = new WMSMapContent();
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        map.setRequest(request);
        addRasterToMap(map, MockData.TASMANIA_DEM);

        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        request.setInterpolations(
                Arrays.asList(Interpolation.getInstance(Interpolation.INTERP_BICUBIC)));
        request.setFormat(getMapFormat());

        imageMap = this.rasterMapProducer.produceMap(map);
        op = (RenderedOp) ((RenderedImageTimeDecorator) imageMap.getImage()).getDelegate();
        BufferedImage imageBicubic = op.getAsBufferedImage();
        imageMap.dispose();
        assertNotBlank("testInterpolationsBicubic", imageBicubic);
        // test some sample pixels to check rendering is different using different interpolations
        assertNotEquals(
                getPixelColor(imageNearest, 160, 160).getRGB(),
                getPixelColor(imageBicubic, 160, 160).getRGB());
        assertNotEquals(
                getPixelColor(imageNearest, 300, 450).getRGB(),
                getPixelColor(imageBicubic, 300, 450).getRGB());
    }

    @Test
    public void testInterpolationFromLayerConfig()
            throws IOException, IllegalFilterException, Exception {
        final Catalog catalog = getCatalog();

        LayerInfo layerInfo = catalog.getLayerByName(MockData.TASMANIA_DEM.getLocalPart());

        MapLayerInfo mapLayer = new MapLayerInfo(layerInfo);
        assertNull(layerInfo.getDefaultWMSInterpolationMethod());

        Envelope env = layerInfo.getResource().boundingBox();
        double shift = env.getWidth() / 6;

        env =
                new Envelope(
                        env.getMinX() - shift,
                        env.getMaxX() + shift,
                        env.getMinY() - shift,
                        env.getMaxY() + shift);

        // set Nearest Neighbor interpolation on layer
        GetMapRequest request = new GetMapRequest();
        request.setFormat(getMapFormat());
        request.setLayers(Arrays.asList(mapLayer));

        layerInfo.setDefaultWMSInterpolationMethod(WMSInterpolation.Nearest);
        assertEquals(
                WMSInterpolation.Nearest,
                request.getLayers().get(0).getLayerInfo().getDefaultWMSInterpolationMethod());
        assertTrue(request.getInterpolations().isEmpty());

        WMSMapContent map = createWMSMap(env);
        map.setRequest(request);

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        RenderedOp op =
                (RenderedOp) ((RenderedImageTimeDecorator) imageMap.getImage()).getDelegate();
        BufferedImage imageNearest = op.getAsBufferedImage();
        imageMap.dispose();
        assertNotBlank("testInterpolationsNearest", imageNearest);

        // set Bicubic interpolation on layer
        request = new GetMapRequest();
        request.setFormat(getMapFormat());
        request.setLayers(Arrays.asList(mapLayer));

        layerInfo.setDefaultWMSInterpolationMethod(WMSInterpolation.Bicubic);
        assertEquals(
                WMSInterpolation.Bicubic,
                request.getLayers().get(0).getLayerInfo().getDefaultWMSInterpolationMethod());
        assertTrue(request.getInterpolations().isEmpty());

        map = createWMSMap(env);
        map.setRequest(request);

        imageMap = this.rasterMapProducer.produceMap(map);
        op = (RenderedOp) ((RenderedImageTimeDecorator) imageMap.getImage()).getDelegate();
        BufferedImage imageBicubic = op.getAsBufferedImage();
        imageMap.dispose();
        assertNotBlank("testInterpolationsBicubic", imageBicubic);
        // test some sample pixels to check rendering is different using different interpolations
        assertNotEquals(
                getPixelColor(imageNearest, 160, 160).getRGB(),
                getPixelColor(imageBicubic, 160, 160).getRGB());
        assertNotEquals(
                getPixelColor(imageNearest, 300, 450).getRGB(),
                getPixelColor(imageBicubic, 300, 450).getRGB());

        // check also the *non* direct raster render path
        request = new GetMapRequest();
        request.setFormat(getMapFormat());
        // adding layer twice on purpose to disable direct raster render
        request.setLayers(Arrays.asList(mapLayer, mapLayer));

        layerInfo.setDefaultWMSInterpolationMethod(WMSInterpolation.Bicubic);
        assertEquals(
                WMSInterpolation.Bicubic,
                request.getLayers().get(0).getLayerInfo().getDefaultWMSInterpolationMethod());
        assertTrue(request.getInterpolations().isEmpty());

        map = createWMSMap(env);
        map.setRequest(request);
        // adding layer twice on purpose to disable direct raster render
        addRasterToMap(map, MockData.TASMANIA_DEM);

        imageMap = this.rasterMapProducer.produceMap(map);
        checkByLayerInterpolation(
                imageMap, Interpolation.getInstance(Interpolation.INTERP_BICUBIC));

        // interpolation method specified in the request overrides service and layer configuration
        request = new GetMapRequest();
        // request says "Bicubic"
        request.setInterpolations(
                Arrays.asList(Interpolation.getInstance(Interpolation.INTERP_BICUBIC)));
        request.setFormat(getMapFormat());
        // adding layer twice on purpose to disable direct raster render
        request.setLayers(Arrays.asList(mapLayer, mapLayer));

        // layer config says "Bilinear"
        layerInfo.setDefaultWMSInterpolationMethod(WMSInterpolation.Bilinear);
        assertEquals(
                WMSInterpolation.Bilinear,
                request.getLayers().get(0).getLayerInfo().getDefaultWMSInterpolationMethod());

        // service config says "Nearest"
        assertEquals(
                WMSInfo.WMSInterpolation.Nearest, getWMS().getServiceInfo().getInterpolation());

        map = createWMSMap(env);
        map.setRequest(request);
        // adding layer twice on purpose to disable direct raster render
        addRasterToMap(map, MockData.TASMANIA_DEM);

        imageMap = this.rasterMapProducer.produceMap(map);
        checkByLayerInterpolation(
                imageMap, Interpolation.getInstance(Interpolation.INTERP_BICUBIC));

        // if default interpolation method is not specified, service default is used
        request = new GetMapRequest();
        request.setFormat(getMapFormat());
        request.setLayers(Arrays.asList(mapLayer));

        layerInfo.setDefaultWMSInterpolationMethod(null);
        assertEquals(
                null, request.getLayers().get(0).getLayerInfo().getDefaultWMSInterpolationMethod());
        assertTrue(request.getInterpolations().isEmpty());

        assertEquals(
                WMSInfo.WMSInterpolation.Nearest, getWMS().getServiceInfo().getInterpolation());

        map = createWMSMap(env);
        map.setRequest(request);

        imageMap = this.rasterMapProducer.produceMap(map);
        op = (RenderedOp) ((RenderedImageTimeDecorator) imageMap.getImage()).getDelegate();
        BufferedImage imageServiceDefault = op.getAsBufferedImage();
        imageMap.dispose();
        assertNotBlank("testInterpolationServiceDefault", imageServiceDefault);
        // test produced image is equal to imageNearest
        assertEquals(
                getPixelColor(imageNearest, 200, 200).getRGB(),
                getPixelColor(imageServiceDefault, 200, 200).getRGB());
        assertEquals(
                getPixelColor(imageNearest, 300, 300).getRGB(),
                getPixelColor(imageServiceDefault, 300, 300).getRGB());
        assertEquals(
                getPixelColor(imageNearest, 250, 250).getRGB(),
                getPixelColor(imageServiceDefault, 250, 250).getRGB());
        assertEquals(
                getPixelColor(imageNearest, 150, 150).getRGB(),
                getPixelColor(imageServiceDefault, 150, 150).getRGB());
    }

    /*
     * NOTE: this check is valid only if the direct raster render path is *not* taken
     */
    private void checkByLayerInterpolation(RenderedImageMap imageMap, Interpolation expected) {
        Layer layer = imageMap.getMapContext().layers().get(0);
        Interpolation actual =
                (Interpolation) layer.getUserData().get(StreamingRenderer.BYLAYER_INTERPOLATION);
        assertEquals(expected, actual);
    }

    private WMSMapContent createWMSMap(Envelope bounds) throws Exception {
        WMSMapContent map = new WMSMapContent();

        int w = 400;
        int h = (int) Math.round((bounds.getHeight() * w) / bounds.getWidth());
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        addRasterToMap(map, MockData.TASMANIA_DEM);

        map.getViewport().setBounds(new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84));

        return map;
    }

    private void addRasterToMap(final WMSMapContent map, final QName typeName)
            throws IOException, FactoryRegistryException, TransformException, SchemaException {
        addRasterToMap(map, typeName, null);
    }

    private void addRasterToMap(final WMSMapContent map, final QName typeName, Style style)
            throws IOException, FactoryRegistryException, TransformException, SchemaException {
        final CoverageInfo coverageInfo =
                getCatalog().getCoverageByName(typeName.getNamespaceURI(), typeName.getLocalPart());

        List<LayerInfo> layers = getCatalog().getLayers(coverageInfo);
        if (style == null) {
            StyleInfo defaultStyle = layers.get(0).getDefaultStyle();
            style = defaultStyle.getStyle();
        }

        SimpleFeatureCollection fc =
                FeatureUtilities.wrapGridCoverageReader(
                        (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null),
                        new GeneralParameterValue[] {});
        map.addLayer(new FeatureLayer(fc, style));
    }

    private void addToMap(final WMSMapContent map, final QName typeName) throws IOException {
        final FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(typeName.getNamespaceURI(), typeName.getLocalPart());

        List<LayerInfo> layers = getCatalog().getLayers(ftInfo);
        StyleInfo defaultStyle = layers.get(0).getDefaultStyle();
        Style style = defaultStyle.getStyle();

        map.addLayer(new FeatureLayer(ftInfo.getFeatureSource(null, null), style));
    }

    private void testDefaultStyle(FeatureSource fSource) throws Exception {
        Catalog catalog = getCatalog();
        Style style = catalog.getStyleByName("Default").getStyle();

        FeatureTypeInfo typeInfo =
                catalog.getFeatureTypeByName(
                        MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        Envelope env = typeInfo.getFeatureSource(null, null).getBounds();
        env.expandToInclude(fSource.getBounds());

        int w = 400;
        int h = (int) Math.round((env.getHeight() * w) / env.getWidth());

        double shift = env.getWidth() / 6;

        env =
                new Envelope(
                        env.getMinX() - shift,
                        env.getMaxX() + shift,
                        env.getMinY() - shift,
                        env.getMaxY() + shift);

        WMSMapContent map = new WMSMapContent();
        GetMapRequest request = new GetMapRequest();
        map.setRequest(request);
        map.addLayer(new FeatureLayer(fSource, style));
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(w);
        map.setMapHeight(h);
        map.setBgColor(BG_COLOR);
        map.setTransparent(false);

        // this.rasterMapProducer.setOutputFormat(getMapFormat());
        // this.rasterMapProducer.setMapContext(map);
        // this.rasterMapProducer.produceMap();
        request.setFormat(getMapFormat());

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);

        RenderedImage image = imageMap.getImage();
        imageMap.dispose();
        assertNotNull(image);
        String typeName = fSource.getSchema().getName().getLocalPart();
        assertNotBlank("testDefaultStyle " + typeName, (BufferedImage) image);
    }

    /**
     * Checks {@link RenderedImageMapOutputFormat} makes good use of {@link RenderExceptionStrategy}
     */
    @SuppressWarnings("deprecation")
    @Test
    public void testRenderingErrorsHandling() throws Exception {

        // the ones that are ignorable by the renderer
        assertNotNull(forceRenderingError(new TransformException("fake transform exception")));
        assertNotNull(
                forceRenderingError(
                        new NoninvertibleTransformException("fake non invertible exception")));
        assertNotNull(
                forceRenderingError(
                        new IllegalAttributeException("non illegal attribute exception")));
        assertNotNull(forceRenderingError(new FactoryException("fake factory exception")));

        // any other one should make the map producer fail
        try {
            forceRenderingError(new RuntimeException("fake runtime exception"));
            fail("Expected WMSException");
        } catch (ServiceException e) {
            assertTrue(true);
        }

        try {
            forceRenderingError(new IOException("fake IO exception"));
            fail("Expected WMSException");
        } catch (ServiceException e) {
            assertTrue(true);
        }

        try {
            forceRenderingError(new IllegalArgumentException("fake IAE exception"));
            fail("Expected WMSException");
        } catch (ServiceException e) {
            assertTrue(true);
        }
    }

    /**
     * Test to check if we can successfully create a direct rendered image by using a coverage view
     * as a source, and a symbolizer defining which three bands of the input coverage view can be
     * used for RGB coloring, and with what order.
     */
    @Test
    public void testStyleUsingChannelsFromCoverageView() throws Exception {

        GetMapRequest request = new GetMapRequest();
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        ReferencedEnvelope bbox =
                new ReferencedEnvelope(
                        new Envelope(
                                -116.90673461649858211,
                                -114.30988665660261461,
                                32.07093728218402617,
                                33.89032847348440214),
                        crs);
        request.setBbox(bbox);
        request.setSRS("urn:x-ogc:def:crs:EPSG:4326");
        request.setFormat("image/png");

        final WMSMapContent map = new WMSMapContent(request);
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setTransparent(false);
        map.getViewport().setBounds(bbox);

        StyleBuilder styleBuilder = new StyleBuilder();

        Catalog catalog = getCatalog();

        // Source image
        CoverageInfo ci =
                catalog.getCoverageByName(
                        SystemTestData.MULTIBAND.getPrefix(),
                        SystemTestData.MULTIBAND.getLocalPart());

        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        reader.getCoordinateReferenceSystem();

        Layer sl =
                new CachedGridReaderLayer(
                        reader, styleBuilder.createStyle(styleBuilder.createRasterSymbolizer()));
        map.addLayer(sl);

        RenderedImageMap srcImageMap = this.rasterMapProducer.produceMap(map);
        RenderedImage srcImage = srcImageMap.getImage();

        // CoverageView band creation. We create a coverage view with 6 bands, using
        // the original bands from the multiband coverage

        // Note that first three bands are int reverse order of the bands of the source coverage
        final InputCoverageBand ib0 = new InputCoverageBand("multiband", "2");
        final CoverageBand b0 =
                new CoverageBand(
                        Collections.singletonList(ib0),
                        "multiband@2",
                        0,
                        CompositionType.BAND_SELECT);

        final InputCoverageBand ib1 = new InputCoverageBand("multiband", "1");
        final CoverageBand b1 =
                new CoverageBand(
                        Collections.singletonList(ib1),
                        "multiband@1",
                        1,
                        CompositionType.BAND_SELECT);

        final InputCoverageBand ib2 = new InputCoverageBand("multiband", "0");
        final CoverageBand b2 =
                new CoverageBand(
                        Collections.singletonList(ib2),
                        "multiband@0",
                        2,
                        CompositionType.BAND_SELECT);

        final InputCoverageBand ib3 = new InputCoverageBand("multiband", "0");
        final CoverageBand b3 =
                new CoverageBand(
                        Collections.singletonList(ib3),
                        "multiband@0",
                        0,
                        CompositionType.BAND_SELECT);

        final InputCoverageBand ib4 = new InputCoverageBand("multiband", "1");
        final CoverageBand b4 =
                new CoverageBand(
                        Collections.singletonList(ib4),
                        "multiband@1",
                        1,
                        CompositionType.BAND_SELECT);

        final InputCoverageBand ib5 = new InputCoverageBand("multiband", "2");
        final CoverageBand b5 =
                new CoverageBand(
                        Collections.singletonList(ib5),
                        "multiband@2",
                        2,
                        CompositionType.BAND_SELECT);

        final List<CoverageBand> coverageBands = new ArrayList<CoverageBand>(1);
        coverageBands.add(b0);
        coverageBands.add(b1);
        coverageBands.add(b2);

        coverageBands.add(b3);
        coverageBands.add(b4);
        coverageBands.add(b5);

        CoverageView multiBandCoverageView = new CoverageView("multiband_select", coverageBands);

        CoverageStoreInfo storeInfo = catalog.getCoverageStoreByName("multiband");
        CatalogBuilder builder = new CatalogBuilder(catalog);

        // Reordered bands coverage
        CoverageInfo coverageInfo =
                multiBandCoverageView.createCoverageInfo("multiband_select", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        catalog.add(coverageInfo);
        final LayerInfo layerInfoView = builder.buildLayer(coverageInfo);
        catalog.add(layerInfoView);

        final Envelope env = ci.boundingBox();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        RasterSymbolizer symbolizer = styleBuilder.createRasterSymbolizer();
        ChannelSelection cs = new ChannelSelectionImpl();
        SelectedChannelType red = new SelectedChannelTypeImpl();
        SelectedChannelType green = new SelectedChannelTypeImpl();
        SelectedChannelType blue = new SelectedChannelTypeImpl();

        // We want to create an image where the RGB channels are in reverse order
        // regarding the band order of the input coverage view
        // Note that channel names start with index "1"
        red.setChannelName("3");
        green.setChannelName("2");
        blue.setChannelName("1");

        cs.setRGBChannels(new SelectedChannelType[] {red, green, blue});
        symbolizer.setChannelSelection(cs);

        reader = (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
        reader.getCoordinateReferenceSystem();
        Layer dl = new CachedGridReaderLayer(reader, styleBuilder.createStyle(symbolizer));
        map.removeLayer(sl);
        map.addLayer(dl);

        RenderedImageMap dstImageMap = this.rasterMapProducer.produceMap(map);
        RenderedImage destImage = dstImageMap.getImage();

        int dWidth = destImage.getWidth();
        int dHeight = destImage.getHeight();

        int[] destImageRowBand0 = new int[dWidth * dHeight];
        int[] destImageRowBand1 = new int[destImageRowBand0.length];
        int[] destImageRowBand2 = new int[destImageRowBand0.length];
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 0, destImageRowBand0);
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 1, destImageRowBand1);
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 2, destImageRowBand2);

        int sWidth = srcImage.getWidth();
        int sHeight = srcImage.getHeight();

        int[] srcImageRowBand0 = new int[sWidth * sHeight];
        int[] srcImageRowBand2 = new int[srcImageRowBand0.length];

        srcImage.getData().getSamples(0, 0, sWidth, sHeight, 0, srcImageRowBand0);

        // Source and result image first bands should be the same. We have reversed the order
        // of the three first bands of the source coverage and then we re-reversed the three
        // first bands using channel selection on the raster symbolizer used for rendering.
        Assert.assertTrue(Arrays.equals(destImageRowBand0, srcImageRowBand0));
        // Result band 0 should not be equal to source image band 2
        Assert.assertFalse(Arrays.equals(destImageRowBand0, srcImageRowBand2));

        srcImageMap.dispose();
        dstImageMap.dispose();

        map.dispose();
    }

    /**
     * Test to check the case where a {@link org.geotools.coverage.grid.io.AbstractGridFormat#BANDS}
     * reading parameter is passed to a coverage reader that does not support it. Reader should
     * ignore it, resulting coverage should not be affected.
     */
    @Test
    public void testBandSelectionToNormalCoverage() throws Exception {

        GetMapRequest request = new GetMapRequest();
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        ReferencedEnvelope bbox =
                new ReferencedEnvelope(
                        new Envelope(
                                -116.90673461649858211,
                                -114.30988665660261461,
                                32.07093728218402617,
                                33.89032847348440214),
                        crs);
        request.setBbox(bbox);
        request.setSRS("urn:x-ogc:def:crs:EPSG:4326");
        request.setFormat("image/png");

        final WMSMapContent map = new WMSMapContent(request);
        map.setMapWidth(300);
        map.setMapHeight(300);
        map.setBgColor(Color.red);
        map.setTransparent(false);
        map.getViewport().setBounds(bbox);

        StyleBuilder styleBuilder = new StyleBuilder();
        Catalog catalog = getCatalog();

        CoverageInfo ci =
                catalog.getCoverageByName(
                        SystemTestData.MULTIBAND.getPrefix(),
                        SystemTestData.MULTIBAND.getLocalPart());

        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        reader.getCoordinateReferenceSystem();

        final Envelope env = ci.boundingBox();

        final int[] bandIndices = new int[] {1, 2, 0, 2, 1};
        // Inject bandIndices read param
        Parameter<int[]> bandIndicesParam =
                (Parameter<int[]>) AbstractGridFormat.BANDS.createValue();
        bandIndicesParam.setValue(bandIndices);
        List<GeneralParameterValue> paramList = new ArrayList<GeneralParameterValue>();
        paramList.add(bandIndicesParam);
        GeneralParameterValue[] readParams =
                paramList.toArray(new GeneralParameterValue[paramList.size()]);

        Layer sl =
                new CachedGridReaderLayer(
                        reader,
                        styleBuilder.createStyle(styleBuilder.createRasterSymbolizer()),
                        readParams);
        map.addLayer(sl);

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        ImageAssert.assertEquals(
                new File("src/test/resources/org/geoserver/wms/map/direct-raster-expected.tif"),
                imageMap.getImage(),
                0);
        imageMap.dispose();
    }

    @Test
    public void testGetMapOnByteNodataGrayScale() throws Exception {

        GetMapRequest request = new GetMapRequest();
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        ReferencedEnvelope bbox = new ReferencedEnvelope(new Envelope(145, 146, -43, -41), crs);
        request.setBbox(bbox);
        request.setHeight(768);
        request.setWidth(384);
        request.setSRS("urn:x-ogc:def:crs:EPSG:4326");
        request.setFormat("image/png");
        request.setTransparent(true);

        final WMSMapContent map = new WMSMapContent(request);
        map.setMapHeight(768);
        map.setMapWidth(384);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        map.getViewport().setBounds(bbox);
        addRasterToMap(map, TAZ_BYTE);

        map.getViewport().setBounds(bbox);

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        RenderedOp op =
                (RenderedOp) ((RenderedImageTimeDecorator) imageMap.getImage()).getDelegate();
        BufferedImage image = op.getAsBufferedImage();
        imageMap.dispose();

        // check that a pixel in nodata area is transparent
        assertEquals(0, image.getRaster().getSample(40, 400, 0));
        assertEquals(0, image.getRaster().getSample(40, 400, 1));
    }

    @Test
    public void testGetMapOnByteNodataChannelSelectAndContrastEnhancement() throws Exception {
        GetMapRequest request = new GetMapRequest();
        CoverageInfo coverageInfo =
                getCatalog().getCoverageByName(SIX_BANDS.getPrefix(), SIX_BANDS.getLocalPart());
        ReferencedEnvelope bbox = coverageInfo.getNativeBoundingBox();
        GridEnvelope gridRange = coverageInfo.getGrid().getGridRange();

        final int width = gridRange.getHigh(0) + 1;
        final int height = gridRange.getHigh(1) + 1;
        request.setBbox(bbox);
        request.setWidth(width);
        request.setHeight(height);
        request.setSRS("urn:x-ogc:def:crs:EPSG:4326");
        request.setFormat("image/png");
        request.setTransparent(true);

        final WMSMapContent map = new WMSMapContent(request);
        map.setMapWidth(width);
        map.setMapHeight(height);
        map.setTransparent(true);
        map.getViewport().setBounds(bbox);
        Style normalized = getCatalog().getStyleByName(NORMALIZED_STYLE).getStyle();
        addRasterToMap(map, SIX_BANDS, normalized);
        map.getViewport().setBounds(bbox);

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        RenderedOp op =
                (RenderedOp) ((RenderedImageTimeDecorator) imageMap.getImage()).getDelegate();
        BufferedImage image = op.getAsBufferedImage();
        imageMap.dispose();

        // check that a pixel in nodata area is transparent
        int[] pixel = new int[4];
        WritableRaster raster = image.getRaster();
        raster.getPixel(0, 0, pixel);
        assertArrayEquals(new int[] {0, 0, 0, 0}, pixel);
        raster.getPixel(6, 7, pixel);

        // Checking a valid pixel is opaque
        assertEquals(255, pixel[3]);
    }

    /**
     * Sets up a rendering loop and throws {@code renderExceptionToThrow} wrapped to a
     * RuntimeException when the renderer tries to get a Feature to render.
     *
     * <p>If the rendering succeeded returns the image, which is going to be a blank one but means
     * the renderer didn't complain about the exception caught. Otherwise throws back the exception
     * thrown by {@link RenderedImageMapOutputFormat#produceMap()}
     */
    @SuppressWarnings("unchecked")
    private RenderedImage forceRenderingError(final Exception renderExceptionToThrow)
            throws Exception {

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.setMapWidth(100);
        map.setMapHeight(100);
        map.setRequest(request);
        final ReferencedEnvelope bounds =
                new ReferencedEnvelope(-180, 180, -90, 90, DefaultGeographicCRS.WGS84);
        map.getViewport().setBounds(bounds);

        final FeatureTypeInfo ftInfo =
                getCatalog()
                        .getFeatureTypeByName(STREAMS.getNamespaceURI(), STREAMS.getLocalPart());

        final SimpleFeatureSource featureSource =
                (SimpleFeatureSource) ftInfo.getFeatureSource(null, null);

        DecoratingFeatureSource source;
        // This source should make the renderer fail when asking for the features
        source =
                new DecoratingFeatureSource(featureSource) {
                    @Override
                    public SimpleFeatureCollection getFeatures(Query query) throws IOException {
                        throw new RuntimeException(renderExceptionToThrow);
                        // return delegate.getFeatures(query);
                    }
                };

        StyleInfo someStyle = getCatalog().getStyleByName("line");
        map.addLayer(new FeatureLayer(source, someStyle.getStyle()));
        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        BufferedImage image = (BufferedImage) imageMap.getImage();
        imageMap.dispose();

        return image;
    }

    /**
     * Test to make sure the rendering does not skip on unmatched original envelope and tries
     * anyways to render an output
     */
    @Test
    public void testMosaicExpansion() throws Exception {
        File red1 = URLs.urlToFile(this.getClass().getResource("red_footprint_test/red1.tif"));
        File source = red1.getParentFile();
        File testDataDir = getResourceLoader().getBaseDirectory();
        File directory1 = new File(testDataDir, "redHarvest1");
        File directory2 = new File(testDataDir, "redHarvest2");
        if (directory1.exists()) {
            FileUtils.deleteDirectory(directory1);
        }
        FileUtils.copyDirectory(source, directory1);
        // move all files except red3 to the second dir
        directory2.mkdirs();
        for (File file : FileUtils.listFiles(directory1, new RegexFileFilter("red[^3].*"), null)) {
            assertTrue(file.renameTo(new File(directory2, file.getName())));
        }

        // create the first reader
        URL harvestSingleURL = URLs.fileToUrl(directory1);
        ImageMosaicReader reader = new ImageMosaicReader(directory1, null);

        // now create a second reader that won't be informed of the harvesting changes
        // (simulating changes over a cluster, where the bbox information won't be updated from one
        // node to the other)
        ImageMosaicReader reader2 = new ImageMosaicReader(directory1, null);

        try {
            // harvest the other files with the first reader
            for (File file : directory2.listFiles()) {
                assertTrue(file.renameTo(new File(directory1, file.getName())));
            }
            reader.harvest(null, directory1, null);

            // now use the render to paint a map not hitting the original envelope of reader2
            ReferencedEnvelope renderEnvelope =
                    new ReferencedEnvelope(
                            991000, 992000, 216000, 217000, reader2.getCoordinateReferenceSystem());
            Rectangle rasterArea = new Rectangle(0, 0, 10, 10);
            GetMapRequest request = new GetMapRequest();
            request.setBbox(renderEnvelope);
            request.setSRS("EPSG:6539");
            request.setFormat("image/png");

            final WMSMapContent map = new WMSMapContent(request);
            map.setMapWidth(10);
            map.setMapHeight(10);
            map.setBgColor(Color.BLACK);
            map.setTransparent(false);
            map.getViewport().setBounds(renderEnvelope);

            StyleBuilder builder = new StyleBuilder();
            Style style = builder.createStyle(builder.createRasterSymbolizer());
            Layer l = new CachedGridReaderLayer(reader2, style);
            map.addLayer(l);

            RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
            File reference = new File("src/test/resources/org/geoserver/wms/map/red10.png");
            ImageAssert.assertEquals(reference, imageMap.getImage(), 0);

            // now again, but with a rendering transformation, different code path
            style.featureTypeStyles().get(0).setTransformation(new IdentityCoverageFunction());
            RenderedImageMap imageMap2 = this.rasterMapProducer.produceMap(map);
            ImageAssert.assertEquals(reference, imageMap2.getImage(), 0);
            imageMap.dispose();

        } finally {
            reader.dispose();
            reader2.dispose();
        }
    }

    @Test
    public void testGetMapUntiledBigSize() throws Exception {
        final int mapWidth = 8192;
        final int mapHeight = 8192;
        GetMapRequest request = new GetMapRequest();
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        ReferencedEnvelope bbox = new ReferencedEnvelope(new Envelope(145, 146, -43, -41), crs);
        request.setBbox(bbox);
        request.setHeight(mapHeight);
        request.setWidth(mapWidth);
        request.setSRS("urn:x-ogc:def:crs:EPSG:4326");
        request.setFormat("image/png");
        request.setTransparent(true);

        final WMSMapContent map = new WMSMapContent(request);
        map.setMapHeight(mapHeight);
        map.setMapWidth(mapWidth);
        map.setBgColor(BG_COLOR);
        map.setTransparent(true);
        map.getViewport().setBounds(bbox);
        addRasterToMap(map, TAZ_BYTE);
        map.getViewport().setBounds(bbox);

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        RenderedOp op =
                (RenderedOp) ((RenderedImageTimeDecorator) imageMap.getImage()).getDelegate();
        Point[] tileIndices = op.getTileIndices(new Rectangle(0, 0, mapWidth, mapHeight));

        // Assert we are getting more than a single huge tile.
        assertTrue(tileIndices.length > 1);

        Raster tile = op.getTile(0, 0);
        assertNotNull(tile);

        // check that inner tiling has not be set to mapWidth * mapHeight
        assertTrue(tile.getWidth() < mapWidth);
        assertTrue(tile.getHeight() < mapHeight);

        ImageUtilities.disposePlanarImageChain(op);
        imageMap.dispose();
    }

    @Test
    public void testReprojectionHasNoWhiteLine()
            throws IOException, IllegalFilterException, Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?BBOX=-1137115.798172220821,2084204.190127906622,6183796.336430944502,4720584.184010294266"
                                + "&styles=&layers=sf:3035&Format=image/png"
                                + "&request=GetMap"
                                + "&width=1943"
                                + "&height=700"
                                + "&srs=EPSG:3857");

        BufferedImage image = ImageIO.read(getBinaryInputStream(response));
        assertNotNull(image);

        // Before the fix, the image was containing a vertical stripe on this area
        final int minX = 1025;
        final int minY = 0;
        final int width = 2;
        final int height = 256;
        Raster r = image.getData(new Rectangle(minX, minY, width, height));

        final int validPixel = 237;
        int validPixelsCount = 0;
        for (int i = minX; i < minX + width; i++) {
            for (int j = minY; j < minY + height; j++) {
                if (r.getSample(i, j, 0) == validPixel) {
                    validPixelsCount++;
                }
            }
        }
        // Make sure that stripe contains valid data
        // Before the fix validPixelCount was 0
        assertEquals(width * height, validPixelsCount);
    }

    /**
     * This dummy producer adds no functionality to DefaultRasterMapOutputFormat, just implements a
     * void formatImageOutputStream to have a concrete class over which test that
     * DefaultRasterMapOutputFormat correctly generates the BufferedImage.
     *
     * @author Gabriel Roldan
     * @version $Id: DefaultRasterMapOutputFormatTest.java 6797 2007-05-16 10:23:50Z aaime $
     */
    static class DummyRasterMapProducer extends RenderedImageMapOutputFormat {

        Graphics2D graphics = null;

        public DummyRasterMapProducer(WMS wms) {
            super("image/gif", new String[] {"image/gif"}, wms);
        }

        public void setGraphics(Graphics2D graphics) {
            this.graphics = graphics;
        }

        @Override
        protected Graphics2D getGraphics(
                boolean transparent,
                Color bgColor,
                RenderedImage preparedImage,
                Map<Key, Object> hintsMap) {
            if (this.graphics != null) {
                return this.graphics;
            }
            return super.getGraphics(transparent, bgColor, preparedImage, hintsMap);
        }
    }
}
