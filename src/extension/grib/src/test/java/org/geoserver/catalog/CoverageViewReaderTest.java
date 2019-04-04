/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;

import java.awt.image.ColorModel;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.media.jai.ImageLayout;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.parameter.Parameter;
import org.junit.Assert;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.parameter.GeneralParameterValue;

/**
 * Base support class for CoverageViews based on multiple coverages from the same store.
 *
 * @author Daniele Romagnoli, GeoSolutions
 */
public class CoverageViewReaderTest extends GeoServerSystemTestSupport {

    public static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    protected static QName CURRENT =
            new QName(MockData.SF_URI, "regional_currents", MockData.SF_PREFIX);

    private CoverageView coverageView = null;
    private CoverageView multiBandCoverageView = null;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        testData.setUpRasterLayer(
                CURRENT, "currents.zip", null, null, CoverageViewReaderTest.class);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        createMultiBandCoverageView();
        addMultiBandViewToCatalog();
    }

    private void addViewToCatalog() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("regional_currents");

        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo =
                coverageView.createCoverageInfo("regional_currents", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);
        final LayerInfo layerInfo = builder.buildLayer(coverageInfo);
        cat.add(layerInfo);
    }

    private void addMultiBandViewToCatalog() throws Exception {
        final Catalog cat = getCatalog();
        CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("multiband");

        CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        // Reordered bands coverage
        CoverageInfo coverageInfo =
                multiBandCoverageView.createCoverageInfo("multiband_select", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        cat.add(coverageInfo);
        final LayerInfo layerInfoView = builder.buildLayer(coverageInfo);
        cat.add(layerInfoView);
    }

    private void createCoverageView() throws Exception {
        final InputCoverageBand band_u =
                new InputCoverageBand("u-component_of_current_surface", "0");
        final CoverageBand outputBand_u =
                new CoverageBand(
                        Collections.singletonList(band_u),
                        "u-component_of_current_surface@0",
                        0,
                        CompositionType.BAND_SELECT);

        final InputCoverageBand band_v =
                new InputCoverageBand("v-component_of_current_surface", "0");
        final CoverageBand outputBand_v =
                new CoverageBand(
                        Collections.singletonList(band_v),
                        "v-component_of_current_surface@0",
                        1,
                        CompositionType.BAND_SELECT);
        final List<CoverageBand> coverageBands = new ArrayList<CoverageBand>(2);
        coverageBands.add(outputBand_u);
        coverageBands.add(outputBand_v);
        coverageView = new CoverageView("regional_currents", coverageBands);
    }

    private void createMultiBandCoverageView() throws Exception {
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

        final List<CoverageBand> coverageBands = new ArrayList<CoverageBand>(1);
        coverageBands.add(b0);
        coverageBands.add(b1);
        coverageBands.add(b2);

        coverageBands.add(b3);
        coverageBands.add(b4);

        multiBandCoverageView = new CoverageView("multiband_select", coverageBands);
    }

    /** */
    @Test
    public void testCoverageView() throws Exception {
        createCoverageView();
        addViewToCatalog();

        final Catalog cat = getCatalog();
        final CoverageInfo coverageInfo = cat.getCoverageByName("regional_currents");
        final MetadataMap metadata = coverageInfo.getMetadata();

        final CoverageView metadataCoverageView =
                (CoverageView) metadata.get(CoverageView.COVERAGE_VIEW);
        assertEquals(metadataCoverageView, coverageView);

        final ResourcePool resPool = cat.getResourcePool();
        final ReferencedEnvelope bbox = coverageInfo.getLatLonBoundingBox();
        final GridCoverage coverage =
                resPool.getGridCoverage(coverageInfo, "regional_currents", bbox, null);
        assertEquals(coverage.getNumSampleDimensions(), 2);

        ((GridCoverage2D) coverage).dispose(true);
        final GridCoverageReader reader =
                resPool.getGridCoverageReader(coverageInfo, "regional_currents", null);
        final GranuleStore granules =
                (GranuleStore)
                        ((StructuredGridCoverage2DReader) reader)
                                .getGranules("regional_currents", true);
        SimpleFeatureCollection granulesCollection = granules.getGranules(null);
        // getting the actual phisical granules behind the view,
        assertEquals(2, granulesCollection.size());
        final Filter filter = FF.equal(FF.property("location"), FF.literal("sample.grb2"), true);
        final int removed = granules.removeGranules(filter);
        assertEquals(1, removed);
        granulesCollection = granules.getGranules(null);
        assertEquals(0, granulesCollection.size());

        GridCoverage2DReader myReader = (GridCoverage2DReader) reader;
        ImageLayout layout = myReader.getImageLayout();
        SampleModel sampleModel = layout.getSampleModel(null);
        assertEquals(2, sampleModel.getNumBands());
        ColorModel colorModel = layout.getColorModel(null);
        assertEquals(2, colorModel.getNumComponents());
        reader.dispose();
    }

    /**
     * Test creation of a Coverage from a multi band CoverageView using an {@link
     * org.geotools.coverage.grid.io.AbstractGridFormat#BANDS} reading parameter
     */
    @Test
    public void testBandSelectionOnCoverageView() throws Exception {

        final Catalog cat = getCatalog();
        final CoverageInfo coverageInfo = cat.getCoverageByName("multiband_select");
        final MetadataMap metadata = coverageInfo.getMetadata();

        final ResourcePool resPool = cat.getResourcePool();
        final ReferencedEnvelope bbox = coverageInfo.getLatLonBoundingBox();
        final GridCoverage coverage =
                resPool.getGridCoverage(coverageInfo, "multiband_select", bbox, null);
        RenderedImage srcImage = coverage.getRenderedImage();

        assertEquals(coverage.getNumSampleDimensions(), 5);
        ((GridCoverage2D) coverage).dispose(true);
        final GridCoverageReader reader =
                resPool.getGridCoverageReader(coverageInfo, "multiband_select", null);
        int[] bandIndices = new int[] {2, 0, 1, 0, 2, 2, 2, 3};
        Parameter<int[]> bandIndicesParam = null;

        if (bandIndices != null) {
            bandIndicesParam = (Parameter<int[]>) AbstractGridFormat.BANDS.createValue();
            bandIndicesParam.setValue(bandIndices);
        }

        GridCoverage2DReader myReader = (GridCoverage2DReader) reader;
        ImageLayout layout = myReader.getImageLayout();
        SampleModel sampleModel = layout.getSampleModel(null);
        assertEquals(5, sampleModel.getNumBands());
        reader.dispose();

        List<GeneralParameterValue> paramList = new ArrayList<GeneralParameterValue>();
        paramList.addAll(Arrays.asList(bandIndicesParam));
        GeneralParameterValue[] readParams =
                paramList.toArray(new GeneralParameterValue[paramList.size()]);
        GridCoverage result = reader.read(readParams);
        assertEquals(8, result.getNumSampleDimensions());
        RenderedImage destImage = result.getRenderedImage();

        int dWidth = destImage.getWidth();
        int dHeight = destImage.getHeight();

        int[] destImageRowBand0 = new int[dWidth * dHeight];
        int[] destImageRowBand1 = new int[destImageRowBand0.length];
        int[] destImageRowBand2 = new int[destImageRowBand0.length];
        int[] destImageRowBand3 = new int[destImageRowBand0.length];
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 0, destImageRowBand0);
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 1, destImageRowBand1);
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 2, destImageRowBand2);
        destImage.getData().getSamples(0, 0, dWidth, dHeight, 3, destImageRowBand3);

        int sWidth = srcImage.getWidth();
        int sHeight = srcImage.getHeight();

        int[] srcImageRowBand0 = new int[sWidth * sHeight];
        int[] srcImageRowBand1 = new int[srcImageRowBand0.length];
        int[] srcImageRowBand2 = new int[srcImageRowBand0.length];
        int[] srcImageRowBand3 = new int[srcImageRowBand0.length];

        srcImage.getData().getSamples(0, 0, sWidth, sHeight, 0, srcImageRowBand0);
        srcImage.getData().getSamples(0, 0, sWidth, sHeight, 1, srcImageRowBand1);
        srcImage.getData().getSamples(0, 0, sWidth, sHeight, 2, srcImageRowBand2);

        Assert.assertTrue(Arrays.equals(destImageRowBand0, srcImageRowBand2));
        Assert.assertTrue(Arrays.equals(destImageRowBand1, srcImageRowBand0));
        Assert.assertTrue(Arrays.equals(destImageRowBand2, srcImageRowBand1));
        Assert.assertTrue(Arrays.equals(destImageRowBand3, srcImageRowBand0));
        Assert.assertFalse(Arrays.equals(destImageRowBand0, srcImageRowBand0));
    }

    /**
     * Test creation of a Coverage from a multi band CoverageView which has more bands compared to
     * the input CoverageView
     */
    @Test
    public void testOutputWithMoreBandsThanInputCoverageView() throws Exception {

        final Catalog cat = getCatalog();
        final CoverageInfo coverageInfo = cat.getCoverageByName("multiband_select");
        final MetadataMap metadata = coverageInfo.getMetadata();

        final ResourcePool resPool = cat.getResourcePool();
        final ReferencedEnvelope bbox = coverageInfo.getLatLonBoundingBox();
        final GridCoverage coverage =
                resPool.getGridCoverage(coverageInfo, "multiband_select", bbox, null);
        RenderedImage srcImage = coverage.getRenderedImage();

        assertEquals(coverage.getNumSampleDimensions(), 5);
        ((GridCoverage2D) coverage).dispose(true);
        final GridCoverageReader reader =
                resPool.getGridCoverageReader(coverageInfo, "multiband_select", null);
        int[] bandIndices = new int[] {2, 0, 1, 0, 2, 2, 2, 3, 4, 0, 1, 0, 4, 2, 3};
        Parameter<int[]> bandIndicesParam = null;

        if (bandIndices != null) {
            bandIndicesParam = (Parameter<int[]>) AbstractGridFormat.BANDS.createValue();
            bandIndicesParam.setValue(bandIndices);
        }

        GridCoverage2DReader myReader = (GridCoverage2DReader) reader;
        ImageLayout layout = myReader.getImageLayout();
        SampleModel sampleModel = layout.getSampleModel(null);
        assertEquals(5, sampleModel.getNumBands());
        reader.dispose();

        List<GeneralParameterValue> paramList = new ArrayList<GeneralParameterValue>();
        paramList.addAll(Arrays.asList(bandIndicesParam));
        GeneralParameterValue[] readParams =
                paramList.toArray(new GeneralParameterValue[paramList.size()]);
        GridCoverage result = myReader.read(readParams);
        assertEquals(15, result.getNumSampleDimensions());
    }
}
