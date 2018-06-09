/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.Date;
import java.util.Collections;
import java.util.TimeZone;
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
import org.geotools.feature.type.DateUtil;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Tests the WMS default value support for a custom dimension for both vector and raster layers.
 *
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 */
public class VectorCustomDimensionDefaultValueTest extends WMSTestSupport {

    private static final QName TIME_ELEVATION_CUSTOM =
            new QName(MockData.SF_URI, "TimeElevationCustom", MockData.SF_PREFIX);

    private static final String REFERENCE_TIME_DIMENSION = "REFERENCE_TIME";

    private static final String SCANNING_ANGLE_DIMENSION = "SCANNING_ANGLE";

    WMS wms;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Before
    public void setup() throws Exception {
        wms = getWMS(); // with the initialized application context
        ((SystemTestData) testData)
                .addVectorLayer(
                        TIME_ELEVATION_CUSTOM,
                        Collections.EMPTY_MAP,
                        "TimeElevationCustom.properties",
                        getClass(),
                        getCatalog());
    }

    @Test
    public void testDefaultCustomDimDateValueVectorSelector() throws Exception {
        int fid = 1000;

        // Use default value DimensionInfo setup, should return the minimum value
        setupFeatureCustomDimension(REFERENCE_TIME_DIMENSION, "referenceTime", null);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        // From src/test/resources/org/geoserver/wms/TimeElevationCustom.properties:
        Date originallySmallest = Date.valueOf("2011-04-20");

        java.util.Date d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                d.getTime() == originallySmallest.getTime());

        Date biggest = Date.valueOf("2021-01-01");
        addFeatureWithReferenceTime(fid++, biggest);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                d.getTime() == originallySmallest.getTime());

        Date smaller = Date.valueOf("2010-01-01");
        addFeatureWithReferenceTime(fid++, smaller);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the smallest one", d.getTime() == smaller.getTime());
    }

    @Test
    public void testDefaultCustomDimDoubleValueVectorSelector() throws Exception {
        int fid = 1000;

        // Use default value DimensionInfo setup, should return the minimum value
        setupFeatureCustomDimension(SCANNING_ANGLE_DIMENSION, "scanningAngle", null);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        // From src/test/resources/org/geoserver/wms/TimeElevationCustom.properties:
        Double originallySmallest = Double.valueOf(0d);

        Double d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - originallySmallest.doubleValue()) < 0.0001);

        Double biggest = Double.valueOf(2.1d);
        addFeatureWithScanningAngle(fid++, biggest);

        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - originallySmallest.doubleValue()) < 0.0001);

        Double smaller = Double.valueOf(-1d);
        addFeatureWithScanningAngle(fid++, smaller);

        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - smaller.doubleValue()) < 0.0001);
    }

    @Test
    public void testExplicitMinCustomDimDateValueVectorSelector() throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);

        setupFeatureCustomDimension(REFERENCE_TIME_DIMENSION, "referenceTime", defaultValueSetting);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        // From src/test/resources/org/geoserver/wms/TimeElevationCustom.properties:
        Date originallySmallest = Date.valueOf("2011-04-20");

        java.util.Date d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                d.getTime() == originallySmallest.getTime());

        Date biggest = Date.valueOf("2021-01-01");
        addFeatureWithReferenceTime(fid++, biggest);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                d.getTime() == originallySmallest.getTime());

        Date smaller = Date.valueOf("2010-01-01");
        addFeatureWithReferenceTime(fid++, smaller);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the smallest one", d.getTime() == smaller.getTime());
    }

    @Test
    public void testExplicitMinCustomDimDoubleValueVectorSelector() throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);

        setupFeatureCustomDimension(SCANNING_ANGLE_DIMENSION, "scanningAngle", defaultValueSetting);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        // From src/test/resources/org/geoserver/wms/TimeElevationCustom.properties:
        Double originallySmallest = Double.valueOf(0d);

        Double d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - originallySmallest.doubleValue()) < 0.0001);

        Double biggest = Double.valueOf(2.1d);
        addFeatureWithScanningAngle(fid++, biggest);

        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - originallySmallest.doubleValue()) < 0.0001);

        Double smaller = Double.valueOf(-1d);
        addFeatureWithScanningAngle(fid++, smaller);

        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - smaller.doubleValue()) < 0.0001);
    }

    @Test
    public void testExplicitMaxCustomDimDateValueVectorSelector() throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);

        setupFeatureCustomDimension(REFERENCE_TIME_DIMENSION, "referenceTime", defaultValueSetting);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        // From src/test/resources/org/geoserver/wms/TimeElevationCustom.properties:
        Date originallyBiggest = Date.valueOf("2011-04-23");

        java.util.Date d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the biggest one",
                d.getTime() == originallyBiggest.getTime());

        Date biggest = Date.valueOf("2021-01-01");
        addFeatureWithReferenceTime(fid++, biggest);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the biggest one", d.getTime() == biggest.getTime());

        Date smaller = Date.valueOf("2014-01-01");
        addFeatureWithReferenceTime(fid++, smaller);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the biggest one", d.getTime() == biggest.getTime());
    }

    @Test
    public void testExplicitMaxCustomDimDoubleValueVectorSelector() throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);

        setupFeatureCustomDimension(SCANNING_ANGLE_DIMENSION, "scanningAngle", defaultValueSetting);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        // From src/test/resources/org/geoserver/wms/TimeElevationCustom.properties:
        Double originallyBiggest = Double.valueOf(1.5d);

        Double d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - originallyBiggest.doubleValue()) < 0.0001);

        Double biggest = Double.valueOf(2.1d);
        addFeatureWithScanningAngle(fid++, biggest);

        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - biggest.doubleValue()) < 0.0001);

        Double smaller = Double.valueOf(1.8);
        addFeatureWithScanningAngle(fid++, smaller);

        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - biggest.doubleValue()) < 0.0001);
    }

    @Test
    public void testExplicitFixedCustomDimDateValueVectorSelector() throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        String fixedStr = "2014-01-20T00:00:00Z";
        defaultValueSetting.setReferenceValue(fixedStr);

        setupFeatureCustomDimension(REFERENCE_TIME_DIMENSION, "referenceTime", defaultValueSetting);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        long fixed = DateUtil.parseDateTime(fixedStr);

        java.util.Date d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the fixed one", d.getTime() == fixed);

        Date biggest = Date.valueOf("2021-01-01");
        addFeatureWithReferenceTime(fid++, biggest);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the fixed one", d.getTime() == fixed);

        Date smaller = Date.valueOf("2010-01-01");
        addFeatureWithReferenceTime(fid++, smaller);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the fixed one", d.getTime() == fixed);
    }

    @Test
    public void testExplicitFixedCustomDimDoubleValueVectorSelector() throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        String fixedStr = "42.1";
        defaultValueSetting.setReferenceValue(fixedStr);

        double fixed = Double.parseDouble(fixedStr);

        setupFeatureCustomDimension(SCANNING_ANGLE_DIMENSION, "scanningAngle", defaultValueSetting);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        Double d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - fixed) < 0.0001);

        Double biggest = Double.valueOf(2.1d);
        addFeatureWithScanningAngle(fid++, biggest);

        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - fixed) < 0.0001);

        Double smaller = Double.valueOf(1.8);
        addFeatureWithScanningAngle(fid++, smaller);

        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the smallest one",
                Math.abs(d.doubleValue() - fixed) < 0.0001);
    }

    @Test
    public void testExplicitNearestToGivenValueCustomDimDateValueVectorSelector() throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        String referenceStr = "2014-01-20T00:00:00Z";
        defaultValueSetting.setReferenceValue(referenceStr);

        setupFeatureCustomDimension(REFERENCE_TIME_DIMENSION, "referenceTime", defaultValueSetting);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        Date originallyBiggest = Date.valueOf("2011-04-23");

        java.util.Date d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the closest one",
                d.getTime() == originallyBiggest.getTime());

        Date biggerThanOriginal = Date.valueOf("2012-01-01");
        addFeatureWithReferenceTime(fid++, biggerThanOriginal);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the closest one",
                d.getTime() == biggerThanOriginal.getTime());

        Date biggerThanReference = Date.valueOf("2014-06-01");
        addFeatureWithReferenceTime(fid++, biggerThanReference);

        d =
                wms.getDefaultCustomDimensionValue(
                        REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the closest one",
                d.getTime() == biggerThanReference.getTime());
    }

    @Test
    public void testExplicitNearestToGivenValueCustomDimDoubleValueVectorSelector()
            throws Exception {
        int fid = 1000;

        // Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        String referenceStr = "2.3";
        defaultValueSetting.setReferenceValue(referenceStr);

        setupFeatureCustomDimension(SCANNING_ANGLE_DIMENSION, "scanningAngle", defaultValueSetting);

        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());

        double originallyBiggest = Double.valueOf(1.5d);
        Double d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the closest one",
                Math.abs(d.doubleValue() - originallyBiggest) < 0.0001);

        double biggerThanOriginal = 1.7d;
        addFeatureWithScanningAngle(fid++, biggerThanOriginal);

        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the closest one",
                Math.abs(d.doubleValue() - biggerThanOriginal) < 0.0001);

        double biggerThanReference = 2.45d;
        addFeatureWithScanningAngle(fid++, biggerThanReference);
        d =
                wms.getDefaultCustomDimensionValue(
                        SCANNING_ANGLE_DIMENSION, timeElevationCustom, Double.class);
        assertTrue("Default value is null", d != null);
        assertTrue(
                "Default value should be the closest one",
                Math.abs(d.doubleValue() - biggerThanReference) < 0.0001);
    }

    protected void setupFeatureCustomDimension(
            String dimensionName, String attrName, DimensionDefaultValueSetting defaultValue) {
        FeatureTypeInfo info =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(attrName);

        di.setDefaultValue(defaultValue);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(ResourceInfo.CUSTOM_DIMENSION_PREFIX + dimensionName, di);
        getCatalog().save(info);
    }

    protected void addFeature(
            int id, Date time, Double elevation, Date referenceTime, Double scanningAngle)
            throws IOException {
        FeatureTypeInfo timeElevationCustom =
                getCatalog().getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());
        FeatureStore fs = (FeatureStore) timeElevationCustom.getFeatureSource(null, null);
        SimpleFeatureType type = (SimpleFeatureType) timeElevationCustom.getFeatureType();
        MemoryFeatureCollection coll = new MemoryFeatureCollection(type);
        StringBuffer content = new StringBuffer();
        content.append(id);
        content.append('|');
        content.append(time.toString());
        content.append('|');
        content.append(elevation);
        content.append('|');
        content.append(referenceTime.toString());
        content.append('|');
        content.append(scanningAngle);

        SimpleFeature f = DataUtilities.createFeature(type, content.toString());
        coll.add(f);
        org.geotools.data.Transaction tx = fs.getTransaction();
        fs.addFeatures(coll);
        tx.commit();
    }

    private void addFeatureWithReferenceTime(int fid, Date time) throws IOException {
        this.addFeature(
                fid, Date.valueOf("2013-01-13"), Double.valueOf(0d), time, Double.valueOf(0d));
    }

    private void addFeatureWithScanningAngle(int fid, Double angle) throws IOException {
        this.addFeature(
                fid,
                Date.valueOf("2013-01-13"),
                Double.valueOf(0d),
                Date.valueOf("2014-01-20"),
                angle);
    }
}
