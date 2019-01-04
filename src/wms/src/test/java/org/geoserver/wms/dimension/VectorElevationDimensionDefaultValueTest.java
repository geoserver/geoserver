/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.Date;
import java.util.Collections;
import javax.xml.namespace.QName;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.util.Range;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Tests the WMS default value support for ELEVATION dimension for both vector and raster layers.
 *
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 */
public class VectorElevationDimensionDefaultValueTest extends WMSTestSupport {

    static final QName ELEVATION_WITH_START_END =
            new QName(MockData.SF_URI, "ElevationWithStartEnd", MockData.SF_PREFIX);

    WMS wms;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    @Before
    public void setup() throws Exception {
        wms = getWMS(); // with the initialized application context
        ((SystemTestData) testData)
                .addVectorLayer(
                        ELEVATION_WITH_START_END,
                        Collections.EMPTY_MAP,
                        "TimeElevationWithStartEnd.properties",
                        getClass(),
                        getCatalog());
    }

    @Test
    public void testExplicitMinElevationVectorSelector() throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);

        setupFeatureElevationDimension(defaultValueSetting);

        FeatureTypeInfo elevationWithStartEnd =
                getCatalog().getFeatureTypeByName(ELEVATION_WITH_START_END.getLocalPart());

        Double originallySmallest = Double.valueOf(1d);
        Double e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the smallest one",
                Math.abs(e.doubleValue() - originallySmallest.doubleValue()) < 0.00001);

        addFeatureWithElevation(fid++, 10d);

        e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the smallest one",
                Math.abs(e.doubleValue() - originallySmallest.doubleValue()) < 0.00001);

        Double smaller = Double.valueOf(originallySmallest.doubleValue() - 1);

        addFeatureWithElevation(fid++, smaller.doubleValue());

        e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the smallest one",
                Math.abs(e.doubleValue() - smaller.doubleValue()) < 0.00001);
    }

    @Test
    public void testExplicitMaxElevationVectorSelector() throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);

        setupFeatureElevationDimension(defaultValueSetting);

        FeatureTypeInfo elevationWithStartEnd =
                getCatalog().getFeatureTypeByName(ELEVATION_WITH_START_END.getLocalPart());

        Double originallyBiggest = Double.valueOf(2d);
        Double e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the biggest one",
                Math.abs(e.doubleValue() - originallyBiggest.doubleValue()) < 0.00001);

        Double smaller = Double.valueOf(originallyBiggest.doubleValue() - 1);

        addFeatureWithElevation(fid++, smaller.doubleValue());

        e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the biggest one",
                Math.abs(e.doubleValue() - originallyBiggest.doubleValue()) < 0.00001);

        Double bigger = Double.valueOf(originallyBiggest.doubleValue() + 1);

        addFeatureWithElevation(fid++, bigger.doubleValue());

        e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the biggest one",
                Math.abs(e.doubleValue() - bigger.doubleValue()) < 0.00001);
    }

    @Test
    public void testExplicitFixedElevationVectorSelector() throws Exception {
        int fid = 1000;
        String fixedElevationStr = "550";

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue(fixedElevationStr);

        Double fixedElevation = Double.parseDouble(fixedElevationStr);
        setupFeatureElevationDimension(defaultValueSetting);

        FeatureTypeInfo elevationWithStartEnd =
                getCatalog().getFeatureTypeByName(ELEVATION_WITH_START_END.getLocalPart());

        Double originallyBiggest = Double.valueOf(3d);
        Double e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the fixed one",
                Math.abs(e.doubleValue() - fixedElevation.doubleValue()) < 0.00001);

        Double smaller = Double.valueOf(originallyBiggest.doubleValue() - 1);

        addFeatureWithElevation(fid++, smaller.doubleValue());

        e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the fixed one",
                Math.abs(e.doubleValue() - fixedElevation.doubleValue()) < 0.00001);

        Double bigger = Double.valueOf(originallyBiggest.doubleValue() + 1);

        addFeatureWithElevation(fid++, bigger.doubleValue());

        e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the fixed one",
                Math.abs(e.doubleValue() - fixedElevation.doubleValue()) < 0.00001);
    }

    @Test
    public void testExplicitNearestToGivenElevationVectorSelector() throws Exception {
        int fid = 1000;
        String referenceElevationStr = "1.6";

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(referenceElevationStr);

        Double referenceElevation = Double.parseDouble(referenceElevationStr);
        setupFeatureElevationDimension(defaultValueSetting);

        FeatureTypeInfo elevationWithStartEnd =
                getCatalog().getFeatureTypeByName(ELEVATION_WITH_START_END.getLocalPart());
        Double expected = Double.valueOf(2d);

        Double e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the nearest one to "
                        + referenceElevation.doubleValue(),
                Math.abs(e.doubleValue() - expected.doubleValue()) < 0.00001);

        expected = Double.valueOf(1.8d);
        addFeatureWithElevation(fid++, expected);
        e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the nearest one to "
                        + referenceElevation.doubleValue(),
                Math.abs(e.doubleValue() - expected.doubleValue()) < 0.00001);

        addFeatureWithElevation(fid++, 1.3d);
        e = (Double) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue(
                "Default elevation should be the nearest one to "
                        + referenceElevation.doubleValue(),
                Math.abs(e.doubleValue() - expected.doubleValue()) < 0.00001);
    }

    @Test
    public void testFixedRangeElevation() throws Exception {
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue("-100/0");
        setupFeatureElevationDimension(defaultValueSetting);

        FeatureTypeInfo elevationWithStartEnd =
                getCatalog().getFeatureTypeByName(ELEVATION_WITH_START_END.getLocalPart());
        Range<Double> defaultRange = (Range<Double>) wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", defaultRange != null);
        assertEquals(-100, defaultRange.getMinValue(), 0d);
        assertEquals(0, defaultRange.getMaxValue(), 0d);
    }

    protected void setupFeatureElevationDimension(DimensionDefaultValueSetting defaultValue) {
        FeatureTypeInfo info =
                getCatalog().getFeatureTypeByName(ELEVATION_WITH_START_END.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute("startElevation");

        di.setDefaultValue(defaultValue);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(ResourceInfo.ELEVATION, di);
        getCatalog().save(info);
    }

    protected void addFeature(int id, Date time, Double elevation) throws IOException {
        FeatureTypeInfo timeWithStartEnd =
                getCatalog().getFeatureTypeByName(ELEVATION_WITH_START_END.getLocalPart());
        FeatureStore fs = (FeatureStore) timeWithStartEnd.getFeatureSource(null, null);
        SimpleFeatureType type = (SimpleFeatureType) timeWithStartEnd.getFeatureType();
        MemoryFeatureCollection coll = new MemoryFeatureCollection(type);
        StringBuffer content = new StringBuffer();
        content.append(id);
        content.append('|');
        content.append(time.toString());
        content.append("||");
        content.append(elevation);
        content.append('|');

        SimpleFeature f = DataUtilities.createFeature(type, content.toString());
        coll.add(f);
        org.geotools.data.Transaction tx = fs.getTransaction();
        fs.addFeatures(coll);
        tx.commit();
    }

    private void addFeatureWithElevation(int fid, double value) throws IOException {
        this.addFeature(fid, Date.valueOf("2013-01-13"), value);
    }
}
