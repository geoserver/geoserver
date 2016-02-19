/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.junit.Assert.assertEquals;

import java.util.Map;

import javax.xml.namespace.QName;

import net.opengis.wcs20.GetCoverageType;

import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wcs2_0.kvp.WCS20GetCoverageRequestReader;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Base support class for NetCDF wcs tests.
 * 
 * @author Daniele Romagnoli, GeoSolutions
 * 
 */
public class WCSNetCDFTest extends WCSNetCDFBaseTest {

    public static QName POLYPHEMUS = new QName(CiteTestData.WCS_URI, "polyphemus", CiteTestData.WCS_PREFIX);
    public static QName NO2 = new QName(CiteTestData.WCS_URI, "NO2", CiteTestData.WCS_PREFIX);

    /**
     * Only setup coverages
     */
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
    }

    @SuppressWarnings("unchecked")
    protected GetCoverageType parse(String url) throws Exception {
        Map<String, Object> rawKvp = new CaseInsensitiveMap(KvpUtils.parseQueryString(url));
        Map<String, Object> kvp = new CaseInsensitiveMap(parseKvp(rawKvp));
        WCS20GetCoverageRequestReader reader = new WCS20GetCoverageRequestReader();
        GetCoverageType gc = (GetCoverageType) reader.createRequest();
        return (GetCoverageType) reader.read(gc, kvp, rawKvp);
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        testData.addRasterLayer(POLYPHEMUS, "pol.zip", null, null, this.getClass(), getCatalog());
        setupRasterDimension(getLayerId(NO2), ResourceInfo.TIME, DimensionPresentation.LIST, null);
        setupRasterDimension(getLayerId(NO2), ResourceInfo.ELEVATION, DimensionPresentation.LIST, null);
    }

    /**
     * This test checks if an exception is not thrown when is requested an image with a total size lower than the maximum 
     * geoserver output size.
     * 
     * @throws Exception
     */
    @Test
    public void testOutputMemoryNotExceeded() throws Exception {
     // Setting of the output limit to 40 Kb
        setOutputLimit(40);
        // http response from the request inside the string
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__NO2&format=application/x-netcdf&subset=http://www.opengis.net/def/axis/OGC/0/elevation(450)");
        // The status code should be correct
        assertEquals(200, response.getStatusCode());
        // The output format should be netcdf
        assertEquals("application/x-netcdf", response.getContentType());
        // Reset output limit
        setOutputLimit(-1);
    }   
    
    /**
     * This test checks if an exception is thrown when is requested an image with a total size greater than the maximum
     * geoserver output memory allowed.
     * 
     * @throws Exception
     */
    @Test
    public void testOutputMemoryExceeded() throws Exception {
        // Setting of the output limit to 40 Kb
        setOutputLimit(40);
        // http response from the request inside the string
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__NO2&format=application/x-netcdf");
        // The output format should be xml because an exception must be thrown
        assertEquals("application/xml", response.getContentType());
        // Reset output limit
        setOutputLimit(-1);
    }
    
    /**
     * This test checks if an exception is not thrown when is requested an image with a total size lower than the maximum 
     * geoserver input size.
     * 
     * @throws Exception
     */
    @Test
    public void testInputMemoryCorrect() throws Exception {
        // Setting of the input limit to 40 Kb
        setInputLimit(40);
        // http response from the request inside the string
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__NO2&format=application/x-netcdf&subset=http://www.opengis.net/def/axis/OGC/0/elevation(450)");
        // The status code should be correct
        assertEquals(200, response.getStatusCode());
        // The output format should be netcdf
        assertEquals("application/x-netcdf", response.getContentType());
        // Reset input limit
        setInputLimit(-1);
    }   
    
    /**
     * This test checks if an exception is thrown when is requested an image with a total size greater than the maximum
     * geoserver input memory allowed.
     * 
     * @throws Exception
     */
    @Test
    public void testInputMemoryExceeded() throws Exception {
        // Setting of the input limit to 40 Kb
        setInputLimit(40);
        // http response from the request inside the string
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__NO2&format=application/x-netcdf");
        // The output format should be xml because an exception must be thrown
        assertEquals("application/xml", response.getContentType());
        // Reset input limit
        setInputLimit(-1);
    }
}
