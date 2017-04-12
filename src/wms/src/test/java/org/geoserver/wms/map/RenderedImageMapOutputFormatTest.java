/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import com.vividsolutions.jts.geom.Envelope;
import org.geoserver.catalog.*;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geoserver.wms.*;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.FactoryRegistryException;
import org.geotools.feature.IllegalAttributeException;
import org.geotools.feature.SchemaException;
import org.geotools.filter.IllegalFilterException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.test.ImageAssert;
import org.geotools.map.FeatureLayer;
import org.geotools.map.Layer;
import org.geotools.parameter.Parameter;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.resources.coverage.FeatureUtilities;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ChannelSelectionImpl;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.SelectedChannelTypeImpl;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.xml.namespace.QName;
import java.awt.*;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.geoserver.data.test.CiteTestData.STREAMS;
import static org.junit.Assert.*;

public class RenderedImageMapOutputFormatTest extends WMSTestSupport {

    private static final Logger LOGGER = org.geotools.util.logging.Logging
            .getLogger(RenderedImageMapOutputFormatTest.class.getPackage().getName());

    private RenderedImageMapOutputFormat rasterMapProducer;

    private String mapFormat = "image/gif";

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
        final FeatureSource fs = catalog.getFeatureTypeByName(MockData.BASIC_POLYGONS.getPrefix(),
                MockData.BASIC_POLYGONS.getLocalPart()).getFeatureSource(null, null);

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

    /**
     * Test to make sure the "direct" raster path and the "nondirect" raster path
     * produce matching results. This test was originally created after fixes to GEOS-7270
     * where there were issues with images generated during the direct raster path but not
     * in the normal path, stemming from not setting the background color the same way
     */
    @Test
    public void testDirectVsNonDirectRasterRender() throws Exception {
        Catalog catalog = getCatalog();
        CoverageInfo ci = catalog.getCoverageByName(
                SystemTestData.MULTIBAND.getPrefix(), SystemTestData.MULTIBAND.getLocalPart());

        final Envelope env = ci.boundingBox();

        LOGGER.info("about to create map ctx for BasicPolygons with bounds " + env);

        GetMapRequest request = new GetMapRequest();
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        ReferencedEnvelope bbox = new ReferencedEnvelope(
                new Envelope(-116.90673461649858211,
                        -114.30988665660261461, 32.07093728218402617, 33.89032847348440214), crs);
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
        Layer l = new CachedGridReaderLayer(
                reader,
                builder.createStyle(builder.createRasterSymbolizer()));
        map.addLayer(l);

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        ImageAssert.assertEquals(new File(
                "src/test/resources/org/geoserver/wms/map/direct-raster-expected.tif"), imageMap.getImage(), 0);
        imageMap.dispose();
    }
    
    @Test
    public void testTimeoutOption() throws Exception {
        Catalog catalog = getCatalog();
        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        
        StyleInfo styleByName = catalog.getStyleByName("Default");
        Style basicStyle = styleByName.getStyle();
        
        //Build up a complex map so that we can reasonably guarantee a 1 ms timout
        SimpleFeatureSource fs = (SimpleFeatureSource) catalog.getFeatureTypeByName(MockData.BASIC_POLYGONS.getPrefix(),
                MockData.BASIC_POLYGONS.getLocalPart()).getFeatureSource(null, null);
        Envelope env = fs.getBounds();
        SimpleFeatureCollection features = fs.getFeatures();
        SimpleFeatureCollection delayedCollection = new DelayedFeatureCollection(features, 10);
        map.addLayer(new FeatureLayer(delayedCollection, basicStyle));
        
        LOGGER.info("about to create map ctx for "+map.layers().size()+" layers with bounds " + env);
        
        map.getViewport().setBounds(new ReferencedEnvelope(env, DefaultGeographicCRS.WGS84));
        map.setMapWidth(1000);
        map.setMapHeight(1000);
        map.setRequest(request);
        
        request.setFormat(getMapFormat());
        Map formatOptions = new HashMap();
        //1 ms timeout
        formatOptions.put("timeout", 1);
        request.setFormatOptions(formatOptions);
        try {
            RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
            fail("Timeout was not reached");
        } catch (ServiceException e) {
            assertTrue(e.getMessage().startsWith("This request used more time than allowed"));
        }
        
        //Test partial image exception format
        Map rawKvp = new HashMap();
        rawKvp.put("EXCEPTIONS", "PARTIALMAP");
        request.setRawKvp(rawKvp);
        
        try {
            RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
            fail("Timeout was not reached");
        } catch (ServiceException e) {
            assertTrue(e instanceof WMSPartialMapException);
            assertTrue(e.getCause().getMessage().startsWith("This request used more time than allowed"));
            RenderedImageMap partialMap = (RenderedImageMap) ((WMSPartialMapException)e).getMap();
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
        org.geoserver.catalog.FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(
                MockData.LAKES.getNamespaceURI(), MockData.LAKES.getLocalPart());
        Envelope env = typeInfo.getFeatureSource(null, null).getBounds();
        double shift = env.getWidth() / 6;

        env = new Envelope(env.getMinX() - shift, env.getMaxX() + shift, env.getMinY() - shift,
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
    
    
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addDefaultRasterLayer(MockData.TASMANIA_DEM, getCatalog());
    }

    
    @Test 
    public void testInterpolations() throws IOException, IllegalFilterException, Exception {
        final Catalog catalog = getCatalog();
        CoverageInfo coverageInfo = catalog.getCoverageByName(MockData.TASMANIA_DEM.getNamespaceURI(),
                MockData.TASMANIA_DEM.getLocalPart());
        
        Envelope env = coverageInfo.boundingBox();
        double shift = env.getWidth() / 6;

        env = new Envelope(env.getMinX() - shift, env.getMaxX() + shift, env.getMinY() - shift,
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
        request.setInterpolations(Arrays.asList(Interpolation
                .getInstance(Interpolation.INTERP_NEAREST)));
        request.setFormat(getMapFormat());
        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        RenderedOp op = (RenderedOp)imageMap.getImage();
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
        request.setInterpolations(Arrays.asList(Interpolation
                .getInstance(Interpolation.INTERP_BICUBIC)));
        request.setFormat(getMapFormat());

        imageMap = this.rasterMapProducer.produceMap(map);
        op = (RenderedOp)imageMap.getImage();
        BufferedImage imageBicubic = op.getAsBufferedImage();
        imageMap.dispose();
        assertNotBlank("testInterpolationsBicubic", imageBicubic);
        // test some sample pixels to check rendering is different using different interpolations
        assertNotEquals(getPixelColor(imageNearest, 200, 200).getRGB(),
                getPixelColor(imageBicubic, 200, 200).getRGB());
        assertNotEquals(getPixelColor(imageNearest, 300, 300).getRGB(),
                getPixelColor(imageBicubic, 300, 300).getRGB());
    }

    private void addRasterToMap(final WMSMapContent map, final QName typeName) throws IOException, FactoryRegistryException, TransformException, SchemaException {
        final CoverageInfo coverageInfo = getCatalog().getCoverageByName(typeName.getNamespaceURI(),
                typeName.getLocalPart());


        List<LayerInfo> layers = getCatalog().getLayers(coverageInfo);
        StyleInfo defaultStyle = layers.get(0).getDefaultStyle();
        Style style = defaultStyle.getStyle();
        
        SimpleFeatureCollection fc = FeatureUtilities.wrapGridCoverageReader(
                (GridCoverage2DReader)coverageInfo.getGridCoverageReader(null, null), new GeneralParameterValue[] {});
        map.addLayer(new FeatureLayer(fc, style));
    }
    
    private void addToMap(final WMSMapContent map, final QName typeName) throws IOException {
        final FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(
                typeName.getNamespaceURI(), typeName.getLocalPart());

        List<LayerInfo> layers = getCatalog().getLayers(ftInfo);
        StyleInfo defaultStyle = layers.get(0).getDefaultStyle();
        Style style = defaultStyle.getStyle();

        map.addLayer(new FeatureLayer(ftInfo.getFeatureSource(null, null), style));
    }

    private void testDefaultStyle(FeatureSource fSource) throws Exception {
        Catalog catalog = getCatalog();
        Style style = catalog.getStyleByName("Default").getStyle();

        FeatureTypeInfo typeInfo = catalog.getFeatureTypeByName(MockData.LAKES.getNamespaceURI(),
                MockData.LAKES.getLocalPart());
        Envelope env = typeInfo.getFeatureSource(null, null).getBounds();
        env.expandToInclude(fSource.getBounds());

        int w = 400;
        int h = (int) Math.round((env.getHeight() * w) / env.getWidth());

        double shift = env.getWidth() / 6;

        env = new Envelope(env.getMinX() - shift, env.getMaxX() + shift, env.getMinY() - shift,
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
        assertNotNull(forceRenderingError(new NoninvertibleTransformException(
                "fake non invertible exception")));
        assertNotNull(forceRenderingError(new IllegalAttributeException(
                "non illegal attribute exception")));
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
     * Test to check if we can successfully create a direct rendered image by using
     * a coverage view as a source, and a symbolizer defining which three bands of the
     * input coverage view can be used for RGB coloring, and with what order.
     */
    @Test
    public void testStyleUsingChannelsFromCoverageView() throws Exception {

        GetMapRequest request = new GetMapRequest();
        CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        ReferencedEnvelope bbox = new ReferencedEnvelope(
                new Envelope(-116.90673461649858211,
                        -114.30988665660261461, 32.07093728218402617, 33.89032847348440214), crs);
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
        CoverageInfo ci = catalog.getCoverageByName(
                SystemTestData.MULTIBAND.getPrefix(), SystemTestData.MULTIBAND.getLocalPart());

        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        reader.getCoordinateReferenceSystem();

        Layer sl = new CachedGridReaderLayer(
                reader,
                styleBuilder.createStyle(styleBuilder.createRasterSymbolizer()));
        map.addLayer(sl);

        RenderedImageMap srcImageMap = this.rasterMapProducer.produceMap(map);
        RenderedImage srcImage = srcImageMap.getImage();

        // CoverageView band creation. We create a coverage view with 6 bands, using
        // the original bands from the multiband coverage

        //Note that first three bands are int reverse order of the bands of the source coverage
        final InputCoverageBand ib0 = new InputCoverageBand("multiband", "2");
        final CoverageBand b0 = new CoverageBand(Collections.singletonList(ib0), "multiband@2",
                0, CompositionType.BAND_SELECT);

        final InputCoverageBand ib1 = new InputCoverageBand("multiband", "1");
        final CoverageBand b1 = new CoverageBand(Collections.singletonList(ib1), "multiband@1",
                1, CompositionType.BAND_SELECT);

        final InputCoverageBand ib2 = new InputCoverageBand("multiband", "0");
        final CoverageBand b2 = new CoverageBand(Collections.singletonList(ib2), "multiband@0",
                2, CompositionType.BAND_SELECT);

        final InputCoverageBand ib3 = new InputCoverageBand("multiband", "0");
        final CoverageBand b3 = new CoverageBand(Collections.singletonList(ib3), "multiband@0",
                0, CompositionType.BAND_SELECT);

        final InputCoverageBand ib4 = new InputCoverageBand("multiband", "1");
        final CoverageBand b4 = new CoverageBand(Collections.singletonList(ib4), "multiband@1",
                1, CompositionType.BAND_SELECT);

        final InputCoverageBand ib5 = new InputCoverageBand("multiband", "2");
        final CoverageBand b5 = new CoverageBand(Collections.singletonList(ib5), "multiband@2",
                2, CompositionType.BAND_SELECT);

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
        CoverageInfo coverageInfo = multiBandCoverageView.createCoverageInfo("multiband_select",
                storeInfo, builder);
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

        cs.setRGBChannels(new SelectedChannelType[]{red, green, blue});
        symbolizer.setChannelSelection(cs);

        reader = (GridCoverage2DReader) coverageInfo.getGridCoverageReader(null, null);
        reader.getCoordinateReferenceSystem();
        Layer dl = new CachedGridReaderLayer(
                reader,
                styleBuilder.createStyle(symbolizer));
        map.removeLayer(sl);
        map.addLayer(dl);

        RenderedImageMap dstImageMap = this.rasterMapProducer.produceMap(map);
        RenderedImage destImage = dstImageMap.getImage();

        int dWidth = destImage.getWidth();
        int dHeight = destImage.getHeight();

        int[] destImageRowBand0 = new int[dWidth*dHeight];
        int[] destImageRowBand1 = new int[destImageRowBand0.length];
        int[] destImageRowBand2 = new int[destImageRowBand0.length];
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 0, destImageRowBand0);
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 1, destImageRowBand1);
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 2, destImageRowBand2);

        int sWidth = srcImage.getWidth();
        int sHeight = srcImage.getHeight();

        int[] srcImageRowBand0 = new int[sWidth*sHeight];
        int[] srcImageRowBand2 = new int[srcImageRowBand0.length];

        srcImage.getData().getSamples(0, 0, sWidth, sHeight, 0, srcImageRowBand0);

        // Source and result image first bands should be the same. We have reversed the order
        // of the three first bands of the source coverage and then we re-reversed the three
        // first bands using channel selection on the raster symbolizer used for rendering.
        Assert.assertTrue(Arrays.equals(destImageRowBand0,srcImageRowBand0));
        //Result band 0 should not be equal to source image band 2
        Assert.assertFalse(Arrays.equals(destImageRowBand0,srcImageRowBand2));

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
        ReferencedEnvelope bbox = new ReferencedEnvelope(
                new Envelope(-116.90673461649858211,
                        -114.30988665660261461, 32.07093728218402617, 33.89032847348440214), crs);
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

        CoverageInfo ci = catalog.getCoverageByName(
                SystemTestData.MULTIBAND.getPrefix(), SystemTestData.MULTIBAND.getLocalPart());

        GridCoverage2DReader reader = (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        reader.getCoordinateReferenceSystem();

        final Envelope env = ci.boundingBox();

        final int[] bandIndices = new int[]{1,2,0,2,1};
        //Inject bandIndices read param
        Parameter<int[]> bandIndicesParam =
        		(Parameter<int[]>) AbstractGridFormat.BANDS.createValue();
        bandIndicesParam.setValue(bandIndices);
        List<GeneralParameterValue> paramList = new ArrayList<GeneralParameterValue>();
        paramList.add(bandIndicesParam);
        GeneralParameterValue[] readParams = paramList.toArray(new GeneralParameterValue[paramList.size()]);

        Layer sl = new CachedGridReaderLayer(
                reader,
                styleBuilder.createStyle(styleBuilder.createRasterSymbolizer()),
                readParams
                );
        map.addLayer(sl);

        RenderedImageMap imageMap = this.rasterMapProducer.produceMap(map);
        ImageAssert.assertEquals(new File(
                "src/test/resources/org/geoserver/wms/map/direct-raster-expected.tif"), imageMap.getImage(), 0);
        imageMap.dispose();
    }

    /**
     * Sets up a rendering loop and throws {@code renderExceptionToThrow} wrapped to a
     * RuntimeException when the renderer tries to get a Feature to render.
     * <p>
     * If the rendering succeeded returns the image, which is going to be a blank one but means the
     * renderer didn't complain about the exception caught. Otherwise throws back the exception
     * thrown by {@link RenderedImageMapOutputFormat#produceMap()}
     * </p>
     */
    @SuppressWarnings("unchecked")
    private RenderedImage forceRenderingError(final Exception renderExceptionToThrow)
            throws Exception {

        GetMapRequest request = new GetMapRequest();
        final WMSMapContent map = new WMSMapContent();
        map.setMapWidth(100);
        map.setMapHeight(100);
        map.setRequest(request);
        final ReferencedEnvelope bounds = new ReferencedEnvelope(-180, 180, -90, 90,
                DefaultGeographicCRS.WGS84);
        map.getViewport().setBounds(bounds);

        final FeatureTypeInfo ftInfo = getCatalog().getFeatureTypeByName(STREAMS.getNamespaceURI(),
                STREAMS.getLocalPart());

        final SimpleFeatureSource featureSource = (SimpleFeatureSource) ftInfo.getFeatureSource(
                null, null);

        DecoratingFeatureSource source;
        // This source should make the renderer fail when asking for the features
        source = new DecoratingFeatureSource(featureSource) {
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
     * This dummy producer adds no functionality to DefaultRasterMapOutputFormat, just implements a
     * void formatImageOutputStream to have a concrete class over which test that
     * DefaultRasterMapOutputFormat correctly generates the BufferedImage.
     * 
     * @author Gabriel Roldan
     * @version $Id: DefaultRasterMapOutputFormatTest.java 6797 2007-05-16 10:23:50Z aaime $
     */
    static class DummyRasterMapProducer extends RenderedImageMapOutputFormat {

        public DummyRasterMapProducer(WMS wms) {
            super("image/gif", new String[] { "image/gif" }, wms);
        }
    }
}
