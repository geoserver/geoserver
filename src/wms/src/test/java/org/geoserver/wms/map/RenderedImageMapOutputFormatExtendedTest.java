/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.data.test.TestData;
import org.geoserver.wms.CachedGridReaderLayer;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.map.RenderedImageMapOutputFormatTest.DummyRasterMapProducer;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.IllegalFilterException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.xml.styling.SLDParser;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author Nicola Lagomarsini */
public class RenderedImageMapOutputFormatExtendedTest extends WMSTestSupport {

    private static final QName MOSAIC_HOLES =
            new QName(MockData.SF_URI, "mosaic_holes", MockData.SF_PREFIX);

    private static final String RGB_IR_VIEW = "RgbIrView";
    private static final QName IR_RGB = new QName(MockData.SF_URI, "ir-rgb", MockData.SF_PREFIX);

    private RenderedImageMapOutputFormat rasterMapProducer;

    @Before
    public void setRasterMapProducer() throws Exception {
        this.rasterMapProducer = getProducerInstance();
    }

    protected RenderedImageMapOutputFormat getProducerInstance() {
        return new DummyRasterMapProducer(getWMS());
    }

    @After
    public void unsetRasterMapProducer() throws Exception {
        this.rasterMapProducer = null;
    }

    @BeforeClass
    public static void setup() {
        System.setProperty("ENABLE_ADVANCED_PROJECTION", "false");
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        Map properties = new HashMap();
        properties.put(LayerProperty.STYLE, "raster");
        final Catalog cat = getCatalog();
        testData.addRasterLayer(
                MOSAIC_HOLES,
                "mosaic_holes.zip",
                null,
                properties,
                RenderedImageMapOutputFormatExtendedTest.class,
                cat);

        testData.addRasterLayer(IR_RGB, "ir-rgb.zip", null, null, TestData.class, cat);

        // setup the coverage view
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("ir-rgb");
        final CoverageView coverageView = buildRgbIRView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo(RGB_IR_VIEW, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        coverageInfo.getDimensions().get(0).setName("Red");
        coverageInfo.getDimensions().get(1).setName("Green");
        coverageInfo.getDimensions().get(2).setName("Blue");
        coverageInfo.getDimensions().get(3).setName("Infrared");
        cat.add(coverageInfo);
    }

    private CoverageView buildRgbIRView() {
        final CoverageBand rBand =
                new CoverageBand(
                        Arrays.asList(new InputCoverageBand("rgb", "0")),
                        "rband",
                        0,
                        CompositionType.BAND_SELECT);
        final CoverageBand gBand =
                new CoverageBand(
                        Arrays.asList(new InputCoverageBand("rgb", "1")),
                        "gband",
                        1,
                        CompositionType.BAND_SELECT);
        final CoverageBand bBand =
                new CoverageBand(
                        Arrays.asList(new InputCoverageBand("rgb", "2")),
                        "bband",
                        2,
                        CompositionType.BAND_SELECT);
        final CoverageBand irBand =
                new CoverageBand(
                        Collections.singletonList(new InputCoverageBand("ir", "0")),
                        "irband",
                        3,
                        CompositionType.BAND_SELECT);
        final CoverageView coverageView =
                new CoverageView(RGB_IR_VIEW, Arrays.asList(rBand, gBand, bBand, irBand));
        return coverageView;
    }

    /**
     * Test to check that a channel selection after a renderingTransformation involving an optimized
     * read with underlying BANDS selection will not thrown an exception, by updating the band
     * select accordingly
     */
    @Test
    public void testRenderingTransformationChannelsSelectionFromCoverageView() throws Exception {

        final Catalog catalog = getCatalog();

        // Get the RGB-IR View which is combining an RGB GeoTIFF and an IR GeoTIFF
        final CoverageInfo ci = catalog.getCoverageByName(RGB_IR_VIEW);
        final GridCoverage2DReader reader =
                (GridCoverage2DReader) ci.getGridCoverageReader(null, null);
        final ReferencedEnvelope bbox = new ReferencedEnvelope(reader.getOriginalEnvelope());

        final GetMapRequest request = new GetMapRequest();
        request.setBbox(bbox);
        request.setSRS("urn:x-ogc:def:crs:EPSG:32632");
        request.setFormat("image/png");

        final WMSMapContent map = new WMSMapContent(request);
        map.setMapWidth(20);
        map.setMapHeight(20);
        map.setTransparent(false);
        map.getViewport().setBounds(bbox);

        // Setup a style
        final SLDParser parser = new SLDParser(CommonFactoryFinder.getStyleFactory());
        parser.setInput(
                RasterSymbolizerVisitorTest.class.getResource("CropTransformAndChannelSelect.sld"));
        final StyledLayerDescriptor sld = parser.parseSLD();
        final NamedLayer ul = (NamedLayer) sld.getStyledLayers()[0];
        final Style style = ul.getStyles()[0];

        final Layer dl = new CachedGridReaderLayer(reader, style);
        map.addLayer(dl);

        // Without the symbolizer update fix, the rendering would have thrown a
        // "java.lang.IllegalArgumentException: Band number 4 is not valid."
        // trying to do a band select on the 4th element to setup the gray channel.
        // However, the optimized read performed through the BANDS parameter
        // returned the IR image only so the channel selection
        // should have been updated with a proper index (as part of the fix)
        // as already made in the path not involving a RenderingTransformation.
        RenderedImageMap dstImageMap = this.rasterMapProducer.produceMap(map);
        RenderedImage destImage = dstImageMap.getImage();

        assertNotNull(destImage);
        ColorModel cm = destImage.getColorModel();
        SampleModel sm = destImage.getSampleModel();
        assertTrue(cm.getColorSpace().getNumComponents() == 1);
        assertTrue(sm.getNumBands() == 1);
        dstImageMap.dispose();

        map.dispose();
    }

    /** Test to check that disabling ADVANCED PROJECTION HANDLING will not return a blank image */
    @Test
    public void testMosaicNoProjection() throws IOException, IllegalFilterException, Exception {
        // Request
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?BBOX=6.40284375,36.385494140625,12.189662109375,42.444494140625"
                                + "&styles=&layers=sf:mosaic_holes&Format=image/png"
                                + "&request=GetMap"
                                + "&width=550"
                                + "&height=250"
                                + "&srs=EPSG:4326");
        checkImage(response);
    }

    @AfterClass
    public static void after() {
        System.setProperty("ENABLE_ADVANCED_PROJECTION", "true");
    }
}
