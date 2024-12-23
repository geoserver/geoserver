/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import it.geosolutions.imageio.pam.PAMDataset;
import java.util.Arrays;
import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.CoverageView;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;

/** Test for the {@link CoverageView} and {@link PAMDataset} integration */
public class RATViewTest extends GeoServerSystemTestSupport {
    protected static QName IR_RGB = new QName(MockData.SF_URI, "ir-rgb", MockData.SF_PREFIX);
    private static final String RGB_IR_VIEW = "RgbIrView";

    static CoordinateReferenceSystem UTM32N;

    @Before
    public void cleanupCatalog() {
        // attempt to solve intermittent build failure
        getGeoServer().reset();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpRasterLayer(IR_RGB, "ir-rgb-rat.zip", null, null, TestData.class);

        UTM32N = CRS.decode("EPSG:32632", true);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // setup the coverage view
        final Catalog cat = getCatalog();
        configureIROnCatalog(cat);
    }

    private void configureIROnCatalog(Catalog cat) throws Exception {
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("ir-rgb");
        final CoverageView coverageView = buildRgbIRView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(RGB_IR_VIEW, storeInfo, builder);
        coverageInfo.getParameters().put("USE_JAI_IMAGEREAD", "false");
        coverageInfo.getDimensions().get(0).setName("Red");
        coverageInfo.getDimensions().get(1).setName("Green");
        coverageInfo.getDimensions().get(2).setName("Blue");
        coverageInfo.getDimensions().get(3).setName("Infrared");
        cat.add(coverageInfo);
    }

    private CoverageView buildRgbIRView() {
        final CoverageView.CoverageBand rBand = new CoverageView.CoverageBand(
                Arrays.asList(new CoverageView.InputCoverageBand("rgb", "0")),
                "rband",
                0,
                CoverageView.CompositionType.BAND_SELECT);
        final CoverageView.CoverageBand gBand = new CoverageView.CoverageBand(
                Arrays.asList(new CoverageView.InputCoverageBand("rgb", "1")),
                "gband",
                1,
                CoverageView.CompositionType.BAND_SELECT);
        final CoverageView.CoverageBand bBand = new CoverageView.CoverageBand(
                Arrays.asList(new CoverageView.InputCoverageBand("rgb", "2")),
                "bband",
                2,
                CoverageView.CompositionType.BAND_SELECT);
        final CoverageView.CoverageBand irBand = new CoverageView.CoverageBand(
                Collections.singletonList(new CoverageView.InputCoverageBand("ir", "0")),
                "irband",
                3,
                CoverageView.CompositionType.BAND_SELECT);
        final CoverageView coverageView = new CoverageView(RGB_IR_VIEW, Arrays.asList(rBand, gBand, bBand, irBand));
        // old coverage views deserialize with null in these fields, force it to test backwards
        // compatibility
        coverageView.setEnvelopeCompositionType(null);
        coverageView.setSelectedResolution(null);
        return coverageView;
    }

    @Test
    public void testGetPAMFromCoverageView() throws Exception {
        final Catalog cat = getCatalog();
        final CoverageStoreInfo storeInfo = cat.getCoverageStoreByName("ir-rgb");
        final CoverageView coverageView = buildRgbIRView();
        final CatalogBuilder builder = new CatalogBuilder(cat);
        builder.setStore(storeInfo);

        final CoverageInfo coverageInfo = coverageView.createCoverageInfo(RGB_IR_VIEW, storeInfo, builder);
        CoverageRATs coverageRATs = new CoverageRATs(cat, coverageInfo);
        PAMDataset pamDataset = coverageRATs.getPAMDataset();
        assertNotNull(pamDataset);
        assertEquals(
                String.valueOf(-1257.032451250814),
                pamDataset
                        .getPAMRasterBand()
                        .get(0)
                        .getHistograms()
                        .getHistItem()
                        .getHistMin()
                        .toString());
    }
}
