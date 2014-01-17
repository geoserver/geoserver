/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Collections;
import java.util.TimeZone;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.DimensionDefaultValueSetting.Strategy;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.testreader.CustomFormat;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureStore;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.type.DateUtil;
import org.geotools.gce.imagemosaic.ImageMosaicFormat;
import org.geotools.io.DefaultFileFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

/**
 * Tests the WMS default value support for a custom dimension for both
 * vector and raster layers.
 * 
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 */
public class CustomDimensionDefaultValueTest extends WMSTestSupport {

    private static final QName TIME_ELEVATION_CUSTOM = new QName(MockData.SF_URI, "TimeElevationCustom",
            MockData.SF_PREFIX);
  
    private static final QName WATTEMP_CUSTOM = new QName(MockData.SF_URI, "watertemp_custom",
            MockData.SF_PREFIX);

    private static final String COVERAGE_DIMENSION_NAME = CustomFormat.CUSTOM_DIMENSION_NAME;
    
    private static final String REFERENCE_TIME_DIMENSION = "REFERENCE_TIME";
    
    WMS wms;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
    }

    @Before
    public void setup() throws Exception {
        wms = getWMS(); //with the initialized application context
        ((SystemTestData)testData).addVectorLayer(TIME_ELEVATION_CUSTOM,Collections.EMPTY_MAP,"TimeElevationCustom.properties",
                WMS.class,getCatalog());        
        ((SystemTestData)testData).addRasterLayer(WATTEMP_CUSTOM, "custwatertemp.zip", null, Collections.EMPTY_MAP, 
                SystemTestData.class, getCatalog());
    }
    
    public void tearDown() throws Exception {
        ((SystemTestData)testData).tearDown();
    }
        
    
    @Test
    public void testExplicitMinCustomDimValueVectorSelector() throws Exception {
        int fid = 1000;
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);

        setupFeatureCustomDimension(defaultValueSetting);

        FeatureTypeInfo timeElevationCustom = getCatalog().getFeatureTypeByName(
                TIME_ELEVATION_CUSTOM.getLocalPart());
        
        //From src/test/resources/org/geoserver/wms/TimeElevationCustom.properties:
        Date originallySmallest = Date.valueOf("2011-04-20");
        
        java.util.Date d = wms.getDefaultCustomDimensionValue(REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);     
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the smallest one", d.getTime() == originallySmallest.getTime());
        
        Date biggest = Date.valueOf("2021-01-01");
        addFeatureWithReferenceTime(fid++, biggest);

        d = wms.getDefaultCustomDimensionValue(REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);     
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the smallest one", d.getTime() == originallySmallest.getTime());
       
        Date smaller = Date.valueOf("2010-01-01");
        addFeatureWithReferenceTime(fid++, smaller);
        
        d = wms.getDefaultCustomDimensionValue(REFERENCE_TIME_DIMENSION, timeElevationCustom, java.util.Date.class);     
        assertTrue("Default value is null", d != null);
        assertTrue("Default value should be the smallest one", d.getTime() == smaller.getTime());
        
    }
    
    //TODO: rest of the tests
    
    protected void setupFeatureCustomDimension(DimensionDefaultValueSetting defaultValue) {
        FeatureTypeInfo info = getCatalog()
                .getFeatureTypeByName(TIME_ELEVATION_CUSTOM.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute("referenceTime");

        di.setDefaultValue(defaultValue);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(ResourceInfo.CUSTOM_DIMENSION_PREFIX+REFERENCE_TIME_DIMENSION, di);
        getCatalog().save(info);
    }
 
    
    protected void setupCoverageElevationDimension(QName name, DimensionDefaultValueSetting defaultValue) {
        CoverageInfo info = getCatalog().getCoverageByName(name.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(DimensionPresentation.LIST);
        di.setDefaultValue(defaultValue);
        info.getMetadata().put(ResourceInfo.CUSTOM_DIMENSION_PREFIX+COVERAGE_DIMENSION_NAME, di);
        getCatalog().save(info);
    }

    protected void addFeature(int id, Date time, Double elevation, Date referenceTime) throws IOException {
        FeatureTypeInfo timeElevationCustom = getCatalog().getFeatureTypeByName(
                TIME_ELEVATION_CUSTOM.getLocalPart());
        FeatureStore fs = (FeatureStore) timeElevationCustom.getFeatureSource(null, null);
        SimpleFeatureType type = (SimpleFeatureType)timeElevationCustom.getFeatureType();
        MemoryFeatureCollection coll = new MemoryFeatureCollection(type);
        StringBuffer content = new StringBuffer();
        content.append(id);
        content.append('|');
        content.append(time.toString());
        content.append('|');
        content.append(elevation);
        content.append('|');
        content.append(referenceTime.toString());
        
        SimpleFeature f = DataUtilities.createFeature(type, content.toString());
        coll.add(f);
        org.geotools.data.Transaction tx = fs.getTransaction();
        fs.addFeatures(coll);
        tx.commit();
    }

  
    private void addFeatureWithReferenceTime(int fid, Date time) throws IOException{
        this.addFeature(fid, Date.valueOf("2013-01-13"), Double.valueOf(0d), time);
    }
    
}
