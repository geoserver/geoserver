/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;

import java.awt.image.ColorModel;
import java.awt.image.SampleModel;
import java.util.ArrayList;
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
import org.geotools.coverage.grid.io.GranuleStore;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.StructuredGridCoverage2DReader;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

/**
 * Base support class for CoverageViews based on multiple coverages from the same store.
 * 
 * @author Daniele Romagnoli, GeoSolutions
 * 
 */
public class CoverageViewReaderTest extends GeoServerSystemTestSupport {

    public final static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    protected static QName CURRENT = new QName(MockData.SF_URI, "regional_currents",
            MockData.SF_PREFIX);

    private CoverageView coverageView = null;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpRasterLayer(CURRENT, "currents.zip", null, null, CoverageViewReaderTest.class);

        createCoverageView();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        addViewToCatalog();
    }

    private void addViewToCatalog() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("regional_currents");
       
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo("regional_currents", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD","false");
        cat.add(coverageInfo);
        final LayerInfo layerInfo = builder.buildLayer(coverageInfo);
        cat.add(layerInfo);
    }

    private void createCoverageView() throws Exception {
        final InputCoverageBand band_u = new InputCoverageBand("u-component_of_current_surface", "0");
        final CoverageBand outputBand_u = new CoverageBand(Collections.singletonList(band_u),
                "u-component_of_current_surface@0", 0, CompositionType.BAND_SELECT);

        final InputCoverageBand band_v = new InputCoverageBand("v-component_of_current_surface", "0");
        final CoverageBand outputBand_v = new CoverageBand(Collections.singletonList(band_v),
                "v-component_of_current_surface@0", 1, CompositionType.BAND_SELECT);
        final List<CoverageBand> coverageBands = new ArrayList<CoverageBand>(2);
        coverageBands.add(outputBand_u);
        coverageBands.add(outputBand_v);
        coverageView = new CoverageView("regional_currents", coverageBands);
    }

    /**
     * @throws Exception
     */
    @Test
    public void testCoverageView() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageInfo coverageInfo = cat.getCoverageByName("regional_currents");
        final MetadataMap metadata = coverageInfo.getMetadata();

        final CoverageView metadataCoverageView = (CoverageView) metadata.get(CoverageView.COVERAGE_VIEW);
        assertEquals(metadataCoverageView, coverageView);

        final ResourcePool resPool = cat.getResourcePool();
        final ReferencedEnvelope bbox = coverageInfo.getLatLonBoundingBox();
        final GridCoverage coverage = resPool.getGridCoverage(coverageInfo, "regional_currents", bbox, null);
        assertEquals(coverage.getNumSampleDimensions(), 2);

        ((GridCoverage2D) coverage).dispose(true);
        final GridCoverageReader reader = resPool.getGridCoverageReader(coverageInfo, "regional_currents", null);
        final GranuleStore granules = (GranuleStore) ((StructuredGridCoverage2DReader) reader).getGranules("regional_currents", true);
        SimpleFeatureCollection granulesCollection = granules.getGranules(null);
        assertEquals(1, granulesCollection.size());
        final Filter filter = FF.equal(FF.property("location"), FF.literal("sample.grb2"), true);
        final int removed = granules.removeGranules(filter);
        assertEquals (1, removed);
        granulesCollection = granules.getGranules(null);
        assertEquals(0, granulesCollection.size());

        GridCoverage2DReader myReader = (GridCoverage2DReader) reader;
        ImageLayout layout = myReader.getImageLayout();
        SampleModel sampleModel = layout.getSampleModel(null);
        assertEquals (2, sampleModel.getNumBands());
        ColorModel colorModel = layout.getColorModel(null);
        assertEquals (2, colorModel.getNumComponents());
        reader.dispose();
    }

}
