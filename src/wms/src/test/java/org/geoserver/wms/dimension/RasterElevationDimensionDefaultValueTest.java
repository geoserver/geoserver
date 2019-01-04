/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import static org.junit.Assert.assertEquals;
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
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.Range;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests the WMS default value support for ELEVATION dimension for both vector and raster layers.
 *
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 */
public class RasterElevationDimensionDefaultValueTest extends WMSTestSupport {

    static final QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);

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
                        WATTEMP,
                        "watertemp.zip",
                        null,
                        Collections.EMPTY_MAP,
                        getClass(),
                        getCatalog());
    }

    @Test
    public void testDefaultElevationCoverageSelector() throws Exception {
        // Use default default value strategy:
        setupCoverageElevationDimension(WATTEMP, null);

        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double expected = Double.valueOf(0d);
        Double e = (Double) wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the smallest one",
                Math.abs(e.doubleValue() - expected.doubleValue()) < 0.00001);
    }

    @Test
    public void testExplicitMinElevationCoverageSelector() throws Exception {
        // Use explicit default value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);

        setupCoverageElevationDimension(WATTEMP, defaultValueSetting);

        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double expected = Double.valueOf(0d);
        Double e = (Double) wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the smallest one",
                Math.abs(e.doubleValue() - expected.doubleValue()) < 0.00001);
    }

    @Test
    public void testExplicitMaxElevationCoverageSelector() throws Exception {
        // Use explicit default value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);

        setupCoverageElevationDimension(WATTEMP, defaultValueSetting);

        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double expected = Double.valueOf(100d);
        Double e = (Double) wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the biggest one",
                Math.abs(e.doubleValue() - expected.doubleValue()) < 0.00001);
    }

    @Test
    public void testExplicitFixedElevationCoverageSelector() throws Exception {
        String fixedElevationStr = "550";

        // Use explicit default value strategy:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue(fixedElevationStr);

        Double fixedElevation = Double.parseDouble(fixedElevationStr);

        setupCoverageElevationDimension(WATTEMP, defaultValueSetting);

        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double e = (Double) wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the fixed one",
                Math.abs(e.doubleValue() - fixedElevation.doubleValue()) < 0.00001);
    }

    @Test
    public void testExplicitNearestToGivenTimeCoverageSelector() throws Exception {
        String referenceElevationStr = "55";

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(referenceElevationStr);

        setupCoverageElevationDimension(WATTEMP, defaultValueSetting);

        // From src/test/resources/org/geoserver/wms/watertemp.zip:
        Double expected = Double.valueOf(100d);

        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double e = (Double) wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the fixed one",
                Math.abs(e.doubleValue() - expected.doubleValue()) < 0.00001);
    }

    @Test
    public void testExplicitNearestToGivenTimeCoverageSelector2() throws Exception {
        String referenceElevationStr = "45";

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(referenceElevationStr);

        setupCoverageElevationDimension(WATTEMP, defaultValueSetting);

        // From src/test/resources/org/geoserver/wms/watertemp.zip:
        Double expected = Double.valueOf(0d);

        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double e = (Double) wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the fixed one",
                Math.abs(e.doubleValue() - expected.doubleValue()) < 0.00001);
    }

    @Test
    public void testFixedRangeElevation() throws Exception {
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("-100/0");
        setupCoverageElevationDimension(WATTEMP, defaultValueSetting);

        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());
        Range<Double> defaultRange = (Range<Double>) wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", defaultRange != null);
        assertEquals(-100, defaultRange.getMinValue(), 0d);
        assertEquals(0, defaultRange.getMaxValue(), 0d);
    }

    protected void setupCoverageElevationDimension(
            QName name, DimensionDefaultValueSetting defaultValue) {
        CoverageInfo info = getCatalog().getCoverageByName(name.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(DimensionPresentation.LIST);
        di.setDefaultValue(defaultValue);
        info.getMetadata().put(ResourceInfo.ELEVATION, di);
        getCatalog().save(info);
    }
}
