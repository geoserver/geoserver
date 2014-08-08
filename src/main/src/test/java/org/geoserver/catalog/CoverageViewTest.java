/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.xml.namespace.QName;

import org.geoserver.catalog.CoverageView.CompositionType;
import org.geoserver.catalog.CoverageView.CoverageBand;
import org.geoserver.catalog.CoverageView.InputCoverageBand;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;

public class CoverageViewTest extends GeoServerSystemTestSupport {

    protected static QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
        testData.setUpDefaultRasterLayers();
        testData.setUpRasterLayer(WATTEMP, "watertemp.zip", null, null, TestData.class);
    }

    /**
     * @throws Exception
     */
    @Test
    public void testCoverageView() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("watertemp");

        final InputCoverageBand band = new InputCoverageBand("watertemp", "0");
        final CoverageBand outputBand = new CoverageBand(Collections.singletonList(band), "watertemp@0",
                0, CompositionType.BAND_SELECT);
        final CoverageView coverageView = new CoverageView("waterView",
                Collections.singletonList(outputBand));
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo("waterView", storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD","false");
        cat.add(coverageInfo);
        final MetadataMap metadata = coverageInfo.getMetadata();
        final CoverageView metadataCoverageView = (CoverageView) metadata.get(CoverageView.COVERAGE_VIEW);
        assertEquals(metadataCoverageView, coverageView);

        final ResourcePool resPool = cat.getResourcePool();
        final ReferencedEnvelope bbox = coverageInfo.getLatLonBoundingBox();
        final GridCoverage coverage = resPool.getGridCoverage(coverageInfo, "waterView", bbox, null);
        assertEquals(coverage.getNumSampleDimensions(), 1);

        ((GridCoverage2D) coverage).dispose(true);
        final GridCoverageReader reader = resPool.getGridCoverageReader(coverageInfo, null);
        reader.dispose();
    }

    /**
     * @throws Exception
     */
    @Test
    public void testBands() throws Exception {

        // Test input bands
        final InputCoverageBand u = new InputCoverageBand("u-component", "0");
        final InputCoverageBand v = new InputCoverageBand("u-component", "0");
        assertEquals(u,v);

        final InputCoverageBand empty = new InputCoverageBand();
        v.setCoverageName("v-component");
        v.setBand("1");
        assertNotEquals(u,v);
        assertNotEquals(u,empty);

        // Test output bands
        final CoverageBand outputBandU = new CoverageBand(Collections.singletonList(u), "u@1", 0,
                CompositionType.BAND_SELECT);

        final CoverageBand outputBandV = new CoverageBand();
        outputBandV.setInputCoverageBands(Collections.singletonList(v));
        outputBandV.setDefinition("v@0");
        outputBandV.setIndex(1);
        outputBandV.setCompositionType(CompositionType.BAND_SELECT);

        assertNotEquals(outputBandU, outputBandV);

        // Test compositions
        CompositionType defaultComposition = CompositionType.getDefault(); 
        assertEquals("Band Selection", defaultComposition.displayValue());
        assertEquals("BAND_SELECT", defaultComposition.toValue());
        assertEquals(outputBandU.getCompositionType() , defaultComposition);

        // Test coverage views
        final List<CoverageBand> bands = new ArrayList<CoverageBand>();
        bands.add(outputBandU);
        bands.add(outputBandV);

        final CoverageView coverageView = new CoverageView("wind", bands);
        final CoverageView sameViewDifferentName = new CoverageView();
        sameViewDifferentName.setName("winds");
        sameViewDifferentName.setCoverageBands(bands);
        assertNotEquals(coverageView, sameViewDifferentName);

        assertEquals(coverageView.getBand(1), outputBandV);
        assertEquals(outputBandU, coverageView.getBands("u-component").get(0));
        assertEquals(2, coverageView.getSize());
        assertEquals(2, coverageView.getCoverageBands().size());
        assertEquals("wind", coverageView.getName());
    }

}
