/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.sql.Date;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import javax.xml.namespace.QName;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.geotools.data.FeatureSource;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.util.DateRange;
import org.geotools.util.NumberRange;
import org.junit.Before;
import org.junit.Test;
import org.opengis.filter.Filter;

import static org.easymock.classextension.EasyMock.*;
import static org.easymock.EasyMock.*;

/**
 *
 * @author Ian Schneider <ischneider@opengeo.org>
 */
public class WMSTest extends WMSTestSupport {
    
    static final QName TIME_WITH_START_END = new QName(MockData.SF_URI, "TimeWithStartEnd", MockData.SF_PREFIX);

    static final Double ELEVATION_DOUBLE_1 = 0.0d;

    static final Double ELEVATION_DOUBLE_2 = 100.0d;

    static final NumberRange<Double> ELEVATION_RANGE_1 = new NumberRange<Double>(Double.class, ELEVATION_DOUBLE_1, 10.0d);

    static final NumberRange<Double> ELEVATION_RANGE_2 = new NumberRange<Double>(Double.class, 90.0d, ELEVATION_DOUBLE_2);

    static final Double ELEVATION_DOUBLE_NOT_IN_DOMAIN = 50.0d;

    /**
     * epsilon for floating point equals comparisons.
     */
    static final Double EPSILON = 0.000001d;
    
    WMS wms;
    
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addVectorLayer(TIME_WITH_START_END,Collections.EMPTY_MAP,"TimeElevationWithStartEnd.properties",
                getClass(),getCatalog());
    }

    protected void setupStartEndTimeDimension(String featureTypeName, String dimension, String start, String end) {
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(featureTypeName);
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(start);
        di.setEndAttribute(end);
        di.setPresentation(DimensionPresentation.LIST);
        info.getMetadata().put(dimension, di);
        getCatalog().save(info);
    }

    @Before
    public  void setWMS() throws Exception {
        wms = new WMS(getGeoServer());
    }

    @Test
    public void testGetTimeElevationToFilterStartEndDate() throws Exception {
        
        setupStartEndTimeDimension(TIME_WITH_START_END.getLocalPart(), "time", "startTime", "endTime");
        setupStartEndTimeDimension(TIME_WITH_START_END.getLocalPart(), "elevation", "startElevation", "endElevation");
        
        /* Reference for test assertions
        TimeElevation.0=0|2012-02-11|2012-02-12|1|2
        TimeElevation.1=1|2012-02-12|2012-02-13|2|3
        TimeElevation.2=2|2012-02-11|2011-05-13|1|3
         */
        
        doTimeElevationFilter( Date.valueOf("2012-02-10"), null);
        doTimeElevationFilter( Date.valueOf("2012-02-11"), null, 0, 2);
        doTimeElevationFilter( Date.valueOf("2012-02-12"), null, 0, 1, 2);
        doTimeElevationFilter( Date.valueOf("2012-02-13"), null, 1, 2);
        doTimeElevationFilter( Date.valueOf("2012-02-14"), null);
        
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-10")), null
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-11")), null,
                0, 2
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-11"), Date.valueOf("2012-02-13")), null,
                0, 1, 2
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-09"), Date.valueOf("2012-02-14")), null,
                0, 1, 2
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-13"), Date.valueOf("2012-02-14")), null,
                1, 2
        );
        doTimeElevationFilter( 
                new DateRange(Date.valueOf("2012-02-14"), Date.valueOf("2012-02-15")), null
        );
        
        doTimeElevationFilter( null, 0);
        doTimeElevationFilter( null, 1, 0 , 2);
        doTimeElevationFilter( null, 2, 0 , 1, 2);
        doTimeElevationFilter( null, 3, 1 , 2);
        doTimeElevationFilter( null, 4);
        
        doTimeElevationFilter( null, new NumberRange(Integer.class,-1,0));
        doTimeElevationFilter( null, new NumberRange(Integer.class,-1,1),0,2);
        doTimeElevationFilter( null, new NumberRange(Integer.class,1,3),0,1,2);
        doTimeElevationFilter( null, new NumberRange(Integer.class,-1,4),0,1,2);
        doTimeElevationFilter( null, new NumberRange(Integer.class,3,4),1,2);
        doTimeElevationFilter( null, new NumberRange(Integer.class,4,5));
        
        // combined date/elevation - this should be an 'and' filter
        doTimeElevationFilter( Date.valueOf("2012-02-12"), 2, 0, 1, 2);
        // disjunct verification
        doTimeElevationFilter( Date.valueOf("2012-02-11"), 3);
    }
    
    public void doTimeElevationFilter( Object time, Object elevation, Integer... expectedIds) throws Exception {
        
        FeatureTypeInfo timeWithStartEnd = getCatalog().getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        FeatureSource fs = timeWithStartEnd.getFeatureSource(null, null);
        
        List times = time == null ? null : Arrays.asList(time);
        List elevations = elevation == null ? null : Arrays.asList(elevation);
                
        Filter filter = wms.getTimeElevationToFilter(times, elevations, timeWithStartEnd);
        FeatureCollection features = fs.getFeatures(filter);
        
        Set<Integer> results = new HashSet<Integer>();
        FeatureIterator it = features.features();
        while (it.hasNext()) {
            results.add( (Integer) it.next().getProperty("id").getValue());
        }
        assertTrue("expected " + Arrays.toString(expectedIds) + " but got " + results,
                results.containsAll(Arrays.asList(expectedIds)));
    }

    @Test
    public void testGetDefaultElevation_CoverageInfo_DefaultNull() throws Exception {
        String defaultElevation = null;
        CoverageInfo ciDelegate = getCoverageInfoDelegate(defaultElevation);

        ReaderDimensionsAccessor rdaDelegate = getReaderDimensionsAccessorDelegate(ELEVATION_DOUBLE_1, getElevationDomainDouble());

        Double testResult = wms.getDefaultElevation(ciDelegate, rdaDelegate);
        assertEquals(ELEVATION_DOUBLE_1, testResult, EPSILON);
        verify(rdaDelegate);
        verify(ciDelegate);
    }

    @Test
    public void testGetDefaultElevation_CoverageInfo_DefaultValid_DomainDouble() throws Exception {
        String defaultElevation = ELEVATION_DOUBLE_2.toString();
        CoverageInfo ciDelegate = getCoverageInfoDelegate(defaultElevation);

        ReaderDimensionsAccessor rdaDelegate = getReaderDimensionsAccessorDelegate(ELEVATION_DOUBLE_1, getElevationDomainDouble());

        Double testResult = wms.getDefaultElevation(ciDelegate, rdaDelegate);
        assertEquals(ELEVATION_DOUBLE_2, testResult, EPSILON);
        verify(rdaDelegate);
        verify(ciDelegate);
    }

    @Test
    public void testGetDefaultElevation_CoverageInfo_DefaultValid_DomainRange() throws Exception {
        String defaultElevation = ELEVATION_DOUBLE_2.toString();
        CoverageInfo ciDelegate = getCoverageInfoDelegate(defaultElevation);
        
        ReaderDimensionsAccessor rdaDelegate = getReaderDimensionsAccessorDelegate(ELEVATION_DOUBLE_1, getElevationDomainRange());
        
        Double testResult = wms.getDefaultElevation(ciDelegate, rdaDelegate);
        assertEquals(ELEVATION_DOUBLE_2, testResult, EPSILON);
        verify(rdaDelegate);
        verify(ciDelegate);
    }

    @Test
    public void testGetDefaultElevation_CoverageInfo_DefaultNotParseableAsDouble() throws Exception {
        String defaultElevation = "a100.0";
        CoverageInfo ciDelegate = getCoverageInfoDelegate(defaultElevation);

        ReaderDimensionsAccessor rdaDelegate = getReaderDimensionsAccessorDelegate(ELEVATION_DOUBLE_1, getElevationDomainDouble());

        try {
            wms.getDefaultElevation(ciDelegate, rdaDelegate);
            fail("Expected ServiceException did not occur");
        } catch ( ServiceException se){
            assertTrue("Unexpected ServiceException msg.", se.getMessage().contains(WMS.DEFAULT_ELEVATION_NOT_DOUBLE_MSG));
        }
        verify(rdaDelegate);
        verify(ciDelegate);
    }

    @Test
    public void testGetDefaultElevation_CoverageInfo_DefaultParseableAsDoubleNotValid_DomainDouble() throws Exception {
        String defaultElevation = ELEVATION_DOUBLE_NOT_IN_DOMAIN.toString();
        CoverageInfo ciDelegate = getCoverageInfoDelegate(defaultElevation);

        ReaderDimensionsAccessor rdaDelegate = getReaderDimensionsAccessorDelegate(ELEVATION_DOUBLE_1, getElevationDomainDouble());

        try {
            wms.getDefaultElevation(ciDelegate, rdaDelegate);
            fail("Expected ServiceException did not occur");
        } catch ( ServiceException se){
            assertTrue("Unexpected ServiceException msg.", se.getMessage().contains(WMS.DEFAULT_ELEVATION_NOT_IN_DOMAIN_MSG));
        }
        verify(rdaDelegate);
        verify(ciDelegate);
    }

    @Test
    public void testGetDefaultElevation_CoverageInfo_DefaultParseableAsDoubleNotValid_DomainRange() throws Exception {
        String defaultElevation = ELEVATION_DOUBLE_NOT_IN_DOMAIN.toString();
        CoverageInfo ciDelegate = getCoverageInfoDelegate(defaultElevation);
        
        ReaderDimensionsAccessor rdaDelegate = getReaderDimensionsAccessorDelegate(ELEVATION_DOUBLE_1, getElevationDomainRange());

        try {
            wms.getDefaultElevation(ciDelegate, rdaDelegate);
            fail("Expected ServiceException did not occur");
        } catch ( ServiceException se){
            assertTrue("Unexpected ServiceException msg.", se.getMessage().contains(WMS.DEFAULT_ELEVATION_NOT_IN_DOMAIN_MSG));
        }
        verify(rdaDelegate);
        verify(ciDelegate);
    }

    private CoverageInfo getCoverageInfoDelegate(String defaultElevation) throws Exception {
        CoverageInfo ciDelegate = org.easymock.EasyMock.createMock(CoverageInfo.class);
        expect(ciDelegate.isEnabled()).andReturn(true).once();
        expect(ciDelegate.getPrefixedName()).andReturn("testPrefixedName").times(0, 1);

        MetadataMap mmDelegate = org.easymock.classextension.EasyMock.createMock(MetadataMap.class);
        expect(ciDelegate.getMetadata()).andReturn(mmDelegate).once();
        replay(ciDelegate);

        DimensionInfo di = new DimensionInfoImpl();
        di.setDefaultValue(defaultElevation);
        expect(mmDelegate.get(ResourceInfo.ELEVATION, DimensionInfo.class)).andReturn(di).once();
        replay(mmDelegate);

        return ciDelegate;
    }

    private ReaderDimensionsAccessor getReaderDimensionsAccessorDelegate(Double minElevation, TreeSet<Object> elevationDomain) throws Exception {
        ReaderDimensionsAccessor rdaDelegate = org.easymock.classextension.EasyMock.createMock(ReaderDimensionsAccessor.class);
        expect(rdaDelegate.getMinElevation()).andReturn(minElevation).times(0, 1);
        expect(rdaDelegate.getElevationDomain()).andReturn(elevationDomain).times(0, 1);
        replay(rdaDelegate);

        return rdaDelegate;
    }

    /**
     * @parameter elevation1 Double or NumberRange
     * @parameter elevation2 Double or NumberRange.  object type needs to be same as elevation1.
     */
    private TreeSet<Object> getElevationDomain(Object elevation1, Object elevation2) throws Exception {
        TreeSet<Object> elevationDomain = new TreeSet<Object>(ReaderDimensionsAccessor.ELEVATION_COMPARATOR);
        elevationDomain.add(elevation1);
        elevationDomain.add(elevation2);

        return elevationDomain;
    }

    private TreeSet<Object> getElevationDomainDouble() throws Exception {
        return getElevationDomain(ELEVATION_DOUBLE_1, ELEVATION_DOUBLE_2);
    }

    private TreeSet<Object> getElevationDomainRange() throws Exception {
        return getElevationDomain(ELEVATION_RANGE_1, ELEVATION_RANGE_2);
    }

}