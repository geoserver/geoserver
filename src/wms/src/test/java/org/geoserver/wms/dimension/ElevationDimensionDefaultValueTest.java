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
 * Tests the WMS default value support for ELEVATION dimension for both
 * vector and raster layers.
 * 
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 */
public class ElevationDimensionDefaultValueTest extends WMSTestSupport {

    static final QName ELEVATION_WITH_START_END = new QName(MockData.SF_URI, "ElevationWithStartEnd",
            MockData.SF_PREFIX);
  
    static final QName WATTEMP = new QName(MockData.SF_URI, "watertemp",
            MockData.SF_PREFIX);

    WMS wms;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);        
    }

    @Before
    public void setup() throws Exception {
        wms = getWMS(); //with the initialized application context

        ((SystemTestData)testData).addVectorLayer(ELEVATION_WITH_START_END,Collections.EMPTY_MAP,"TimeElevationWithStartEnd.properties",
                SystemTestData.class, getCatalog());

        ((SystemTestData)testData).addRasterLayer(WATTEMP, "watertemp.zip", null, Collections.EMPTY_MAP, 
                SystemTestData.class, getCatalog());
    }
    
    public void tearDown() throws Exception {
        ((SystemTestData)testData).tearDown();
    }
        
    
    @Test
    public void testExplicitMinElevationVectorSelector() throws Exception {
        int fid = 1000;
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);

        setupFeatureElevationDimension(defaultValueSetting);

        FeatureTypeInfo elevationWithStartEnd = getCatalog().getFeatureTypeByName(
                ELEVATION_WITH_START_END.getLocalPart());
                
        Double originallySmallest = Double.valueOf(1d);
        Double e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the smallest one", Math.abs(e.doubleValue()-originallySmallest.doubleValue()) < 0.00001);
        
        addFeatureWithElevation(fid++, 10d);

        e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the smallest one", Math.abs(e.doubleValue()-originallySmallest.doubleValue()) < 0.00001);
        
        Double smaller = Double.valueOf(originallySmallest.doubleValue()-1);
        
        addFeatureWithElevation(fid++, smaller.doubleValue());
        
        e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the smallest one", Math.abs(e.doubleValue()-smaller.doubleValue()) < 0.00001);
        
    }
    
    
    @Test
    public void testExplicitMaxElevationVectorSelector() throws Exception {
        int fid = 1000;
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);

        setupFeatureElevationDimension(defaultValueSetting);

        FeatureTypeInfo elevationWithStartEnd = getCatalog().getFeatureTypeByName(
                ELEVATION_WITH_START_END.getLocalPart());
                
        Double originallyBiggest = Double.valueOf(2d);
        Double e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the biggest one", Math.abs(e.doubleValue()-originallyBiggest.doubleValue()) < 0.00001);
        
        Double smaller = Double.valueOf(originallyBiggest.doubleValue()-1);
        
        addFeatureWithElevation(fid++, smaller.doubleValue());
        
        e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the biggest one", Math.abs(e.doubleValue()-originallyBiggest.doubleValue()) < 0.00001);
        
        Double bigger = Double.valueOf(originallyBiggest.doubleValue()+1);
        
        addFeatureWithElevation(fid++, bigger.doubleValue());

        e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the biggest one", Math.abs(e.doubleValue()-bigger.doubleValue()) < 0.00001);
        
       
        
    }
   
    
    @Test
    public void testExplicitFixedElevationVectorSelector() throws Exception {
        int fid = 1000;
        String fixedElevationStr = "550";
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue(fixedElevationStr);

        Double fixedElevation = Double.parseDouble(fixedElevationStr);
        setupFeatureElevationDimension(defaultValueSetting);
        
        FeatureTypeInfo elevationWithStartEnd = getCatalog().getFeatureTypeByName(
                ELEVATION_WITH_START_END.getLocalPart());
        
        Double originallyBiggest = Double.valueOf(3d);
        Double e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the fixed one", Math.abs(e.doubleValue()-fixedElevation.doubleValue()) < 0.00001);
        
        Double smaller = Double.valueOf(originallyBiggest.doubleValue()-1);
        
        addFeatureWithElevation(fid++, smaller.doubleValue());
        
        e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the fixed one", Math.abs(e.doubleValue()-fixedElevation.doubleValue()) < 0.00001);
        
        Double bigger = Double.valueOf(originallyBiggest.doubleValue()+1);
        
        addFeatureWithElevation(fid++, bigger.doubleValue());

        e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the fixed one", Math.abs(e.doubleValue()-fixedElevation.doubleValue()) < 0.00001);
    }
    
    @Test
    public void testExplicitNearestToGivenElevationVectorSelector() throws Exception {
        int fid = 1000;
        String referenceElevationStr = "1.6";
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(referenceElevationStr);

        Double referenceElevation = Double.parseDouble(referenceElevationStr);
        setupFeatureElevationDimension(defaultValueSetting);
        
        FeatureTypeInfo elevationWithStartEnd = getCatalog().getFeatureTypeByName(
                ELEVATION_WITH_START_END.getLocalPart());
        Double expected = Double.valueOf(2d);
        
        Double e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the nearest one to "+referenceElevation.doubleValue(), Math.abs(e.doubleValue()-expected.doubleValue()) < 0.00001);
        
        expected = Double.valueOf(1.8d);
        addFeatureWithElevation(fid++, expected);
        e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the nearest one to "+referenceElevation.doubleValue(), Math.abs(e.doubleValue()-expected.doubleValue()) < 0.00001);
        
        addFeatureWithElevation(fid++, 1.3d);
        e = wms.getDefaultElevation(elevationWithStartEnd);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the nearest one to "+referenceElevation.doubleValue(), Math.abs(e.doubleValue()-expected.doubleValue()) < 0.00001);
        
    }
   
    @Test
    public void testDefaultElevationCoverageSelector() throws Exception {
        // Use default default value strategy: 
        setupCoverageElevationDimension(WATTEMP,null);
        
        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double expected = Double.valueOf(0d);
        Double e = wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the smallest one", Math.abs(e.doubleValue()-expected.doubleValue()) < 0.00001);

        
    }
    
    @Test
    public void testExplicitMinElevationCoverageSelector() throws Exception {
        // Use explicit default value strategy: 
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MINIMUM);
        
        setupCoverageElevationDimension(WATTEMP,defaultValueSetting);
        
        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());        
        
        Double expected = Double.valueOf(0d);
        Double e = wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the smallest one", Math.abs(e.doubleValue()-expected.doubleValue()) < 0.00001);
    }
    
    
    @Test
    public void testExplicitMaxElevationCoverageSelector() throws Exception {
        // Use explicit default value strategy: 
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.MAXIMUM);

        setupCoverageElevationDimension(WATTEMP,defaultValueSetting);
        
        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double expected = Double.valueOf(100d);
        Double e = wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the biggest one", Math.abs(e.doubleValue()-expected.doubleValue()) < 0.00001);
    }
    
    @Test
    public void testExplicitFixedElevationCoverageSelector() throws Exception {
        String fixedElevationStr = "550";
        
        // Use explicit default value strategy: 
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.FIXED);
        defaultValueSetting.setReferenceValue(fixedElevationStr);

        Double fixedElevation = Double.parseDouble(fixedElevationStr);

        setupCoverageElevationDimension(WATTEMP,defaultValueSetting);
               
        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double e = wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the fixed one", Math.abs(e.doubleValue()-fixedElevation.doubleValue()) < 0.00001);
    }
    
    @Test
    public void testExplicitNearestToGivenTimeCoverageSelector() throws Exception {        
        String referenceElevationStr = "55";
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(referenceElevationStr);

        setupCoverageElevationDimension(WATTEMP,defaultValueSetting);

        //From src/test/resources/org/geoserver/wms/watertemp.zip:
        Double expected = Double.valueOf(100d);
        
        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double e = wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the fixed one", Math.abs(e.doubleValue()-expected.doubleValue()) < 0.00001);            
                
    }
    
    @Test
    public void testExplicitNearestToGivenTimeCoverageSelector2() throws Exception {        
        String referenceElevationStr = "45";
        
        //Use explicit default value DimensionInfo setup:
        DimensionDefaultValueSetting defaultValueSetting = new DimensionDefaultValueSetting();
        defaultValueSetting.setStrategyType(Strategy.NEAREST);
        defaultValueSetting.setReferenceValue(referenceElevationStr);

        setupCoverageElevationDimension(WATTEMP,defaultValueSetting);

        //From src/test/resources/org/geoserver/wms/watertemp.zip:
        Double expected = Double.valueOf(0d);
        
        CoverageInfo elevatedCoverage = getCatalog().getCoverageByName(WATTEMP.getLocalPart());

        Double e = wms.getDefaultElevation(elevatedCoverage);
        assertTrue("Default elevation is null", e != null);
        assertTrue("Default elevation should be the fixed one", Math.abs(e.doubleValue()-expected.doubleValue()) < 0.00001);                
    }
    
    protected void setupFeatureElevationDimension(DimensionDefaultValueSetting defaultValue) {
        FeatureTypeInfo info = getCatalog()
                .getFeatureTypeByName(ELEVATION_WITH_START_END.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute("startElevation");

        di.setDefaultValue(defaultValue);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(ResourceInfo.ELEVATION, di);
        getCatalog().save(info);
    }
 
    
    protected void setupCoverageElevationDimension(QName name, DimensionDefaultValueSetting defaultValue) {
        CoverageInfo info = getCatalog().getCoverageByName(name.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(DimensionPresentation.LIST);
        di.setDefaultValue(defaultValue);
        info.getMetadata().put(ResourceInfo.ELEVATION, di);
        getCatalog().save(info);
    }

    protected void addFeature(int id, Date time, Double elevation) throws IOException {
        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(
                ELEVATION_WITH_START_END.getLocalPart());
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

  
    private void addFeatureWithElevation(int fid, double value) throws IOException{
        this.addFeature(fid, Date.valueOf("2013-01-13"), value);
    }
    
}
