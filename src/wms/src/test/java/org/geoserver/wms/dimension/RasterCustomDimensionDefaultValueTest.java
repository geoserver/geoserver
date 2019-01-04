/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import static org.junit.Assert.assertTrue;

import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.testreader.CustomFormat;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the WMS default value support for a custom dimension for both vector and raster layers.
 *
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 */
public class RasterCustomDimensionDefaultValueTest extends WMSTestSupport {

    private static final QName WATTEMP_CUSTOM =
            new QName(MockData.SF_URI, "watertemp_custom", MockData.SF_PREFIX);

    private static final String COVERAGE_DIMENSION_NAME = CustomFormat.CUSTOM_DIMENSION_NAME;

    WMS wms;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Before
    public void setup() throws Exception {
        wms = getWMS(); // with the initialized application context
        ((SystemTestData) testData)
                .addRasterLayer(
                        WATTEMP_CUSTOM,
                        "custwatertemp.zip",
                        null,
                        Collections.EMPTY_MAP,
                        getClass(),
                        getCatalog());
    }

    @Test
    public void testDefaultCustomDimValueVectorSelector() throws Exception {
        // Use default default value strategy:
        setupCoverageMyDimension(WATTEMP_CUSTOM, null);

        CoverageInfo customCoverage = getCatalog().getCoverageByName(WATTEMP_CUSTOM.getLocalPart());

        String expected = "CustomDimValueA";
        String def =
                wms.getDefaultCustomDimensionValue(
                        COVERAGE_DIMENSION_NAME, customCoverage, String.class);
        assertTrue("Default dimension value is null", def != null);
        assertTrue("Default dimension value should be the smallest one", expected.equals(def));
    }

    @Test
    public void testExplicitMinCustomDimValueVectorSelector() throws Exception {
        // Use default explicit value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);
        setupCoverageMyDimension(WATTEMP_CUSTOM, defaultValueSetting);

        CoverageInfo customCoverage = getCatalog().getCoverageByName(WATTEMP_CUSTOM.getLocalPart());

        String expected = "CustomDimValueA";
        String def =
                wms.getDefaultCustomDimensionValue(
                        COVERAGE_DIMENSION_NAME, customCoverage, String.class);
        assertTrue("Default dimension value is null", def != null);
        assertTrue("Default dimension value should be the smallest one", expected.equals(def));
    }

    @Test
    public void testExplicitMaxCustomDimValueVectorSelector() throws Exception {
        // Use default explicit value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);
        setupCoverageMyDimension(WATTEMP_CUSTOM, defaultValueSetting);

        CoverageInfo customCoverage = getCatalog().getCoverageByName(WATTEMP_CUSTOM.getLocalPart());

        String expected = "CustomDimValueC";
        String def =
                wms.getDefaultCustomDimensionValue(
                        COVERAGE_DIMENSION_NAME, customCoverage, String.class);
        assertTrue("Default dimension value is null", def != null);
        assertTrue("Default dimension value should be the biggest one", expected.equals(def));
    }

    @Test
    public void testExplicitNearestToGivenValueCustomDimValueVectorSelector() throws Exception {
        // Use default explicit value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        String referenceValue = "CustomDimValueD";
        defaultValueSetting.setReferenceValue(referenceValue);

        setupCoverageMyDimension(WATTEMP_CUSTOM, defaultValueSetting);

        CoverageInfo customCoverage = getCatalog().getCoverageByName(WATTEMP_CUSTOM.getLocalPart());

        String expected = "CustomDimValueC";
        String def =
                wms.getDefaultCustomDimensionValue(
                        COVERAGE_DIMENSION_NAME, customCoverage, String.class);
        assertTrue("Default dimension value is null", def != null);
        assertTrue("Default dimension value should be the closest one", expected.equals(def));
    }

    protected void setupCoverageMyDimension(QName name, DimensionDefaultValueSetting defaultValue) {
        CoverageInfo info = getCatalog().getCoverageByName(name.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(DimensionPresentation.LIST);
        di.setDefaultValue(defaultValue);
        info.getMetadata().put(ResourceInfo.CUSTOM_DIMENSION_PREFIX + COVERAGE_DIMENSION_NAME, di);
        getCatalog().save(info);
    }
}
