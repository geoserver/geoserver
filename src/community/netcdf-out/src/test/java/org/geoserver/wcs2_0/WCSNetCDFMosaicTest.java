package org.geoserver.wcs2_0;

import static org.junit.Assert.*;

import java.lang.reflect.Field;
import java.util.Set;

import javax.xml.namespace.QName;

import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.coverage.grid.GridCoverage2D;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WCSNetCDFMosaicTest extends WCSTestSupport {
    
    private static final String MIME_TYPE = "application/custom";
    public static QName LATLONMOSAIC = new QName(CiteTestData.WCS_URI, "2DLatLonCoverage", CiteTestData.WCS_PREFIX);
    
    static{
	System.setProperty("org.geotools.referencing.forceXY", "true");
    }
    @Before
    public void init() {

        // make sure CRS ordering is correct
        System.setProperty("org.geotools.referencing.forceXY", "true");
        System.setProperty("user.timezone", "GMT");                       
    }

    @AfterClass
    public static void close() {
        System.clearProperty("org.geotools.referencing.forceXY");
        System.clearProperty("user.timezone");                
    }
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
	testData.setUpDefaultRasterLayers();
	
	
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
	//workaround to add our custom multi dimensional format
		try {
		    Field field = GetCoverage.class.getDeclaredField("mdFormats");
		    field.setAccessible(true);
		    ((Set<String>)field.get(null)).add(MIME_TYPE);
		} catch (NoSuchFieldException e) {
		} catch (SecurityException e) {
		} catch (IllegalArgumentException e) {
		} catch (IllegalAccessException e) {
		}
		

        super.onSetUp(testData);
        testData.addRasterLayer(LATLONMOSAIC, "2DLatLonCoverage.zip", null, null, this.getClass(), getCatalog());
        setupRasterDimension(getLayerId(LATLONMOSAIC), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(getLayerId(LATLONMOSAIC), ResourceInfo.CUSTOM_DIMENSION_PREFIX + "BANDS", DimensionPresentation.LIST, null);
        
    }
    
    @Test
    public void testRequestCoverage() throws Exception {
	
	
        // http response from the request inside the string
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__2DLatLonCoverage&format=application/custom&subset=time,http://www.opengis.net/def/trs/ISO-8601/0/Gregorian UTC(\"2013-11-01T00:00:00.000Z\")&subset=BANDS(\"MyBand\")");
        assertNotNull(response);
        GridCoverage2D lastResult = applicationContext.getBean(WCSResponseInterceptor.class).getLastResult();
        assertTrue(lastResult instanceof GranuleStack);
        GranuleStack stack = (GranuleStack) lastResult;
        //we expect a single granule which covers the entire mosaic
        for(GridCoverage2D c : stack.getGranules()){
            System.out.println(c.getEnvelope());
            assertEquals(45., c.getEnvelope2D().getHeight(),0.001);
            assertEquals(30., c.getEnvelope2D().getWidth(),0.001);
        }
        assertEquals(1, stack.getGranules().size());
    }

}
