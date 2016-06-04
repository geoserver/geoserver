/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ows.util.CaseInsensitiveMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.wcs2_0.kvp.WCS20GetCoverageRequestReader;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import net.opengis.wcs20.GetCoverageType;
import ucar.nc2.Dimension;
import ucar.nc2.Variable;
import ucar.nc2.dataset.NetcdfDataset;

/**
 * Base support class for NetCDF wcs tests.
 * 
 * @author Daniele Romagnoli, GeoSolutions
 * 
 */
public class WCSNetCDFTest extends WCSNetCDFBaseTest {

    public static QName POLYPHEMUS = new QName(CiteTestData.WCS_URI, "polyphemus", CiteTestData.WCS_PREFIX);
    public static QName NO2 = new QName(CiteTestData.WCS_URI, "NO2", CiteTestData.WCS_PREFIX);
    public static QName TEMPERATURE_SURFACE = new QName(CiteTestData.WCS_URI, "Temperature_surface",
            CiteTestData.WCS_PREFIX);

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
        testData.addRasterLayer(TEMPERATURE_SURFACE, "rotated-pole.nc", null, null, this.getClass(),
                getCatalog());
    }

    /**
     * This test checks if an exception is not thrown when is requested an image with a total size lower than the maximum 
     * geoserver output size.
     * 
     */
    @Test
    public void testOutputMemoryNotExceeded() throws Exception {
     // Setting of the output limit to 40 Kb
        setOutputLimit(40);
        // http response from the request inside the string
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__NO2&format=application/x-netcdf&subset=http://www.opengis.net/def/axis/OGC/0/elevation(450)");
        // The status code should be correct
        assertEquals(200, response.getStatus());
        // The output format should be netcdf
        assertEquals("application/x-netcdf", response.getContentType());
        // Reset output limit
        setOutputLimit(-1);
    }   
    
    /**
     * This test checks if an exception is thrown when is requested an image with a total size greater than the maximum
     * geoserver output memory allowed.
     * 
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
     */
    @Test
    public void testInputMemoryCorrect() throws Exception {
        // Setting of the input limit to 40 Kb
        setInputLimit(40);
        // http response from the request inside the string
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__NO2&format=application/x-netcdf&subset=http://www.opengis.net/def/axis/OGC/0/elevation(450)");
        // The status code should be correct
        assertEquals(200, response.getStatus());
        // The output format should be netcdf
        assertEquals("application/x-netcdf", response.getContentType());
        // Reset input limit
        setInputLimit(-1);
    }   
    
    /**
     * This test checks if an exception is thrown when is requested an image with a total size greater than the maximum
     * geoserver input memory allowed.
     * 
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

    /**
     * Test NetCDF output for a rotated pole projection.
     */
    @Test
    public void testRotatedPole() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "ows?request=GetCoverage&service=WCS&version=2.0.1"
                        + "&coverageid=wcs__Temperature_surface&format=application/x-netcdf");
        assertEquals(200, response.getStatus());
        assertEquals("application/x-netcdf", response.getContentType());
        byte[] responseBytes = getBinary(response);
        File file = File.createTempFile("netcdf-rotated-pole-", "-wcs__Temperature_surface.nc",
                new File("./target"));
        FileUtils.writeByteArrayToFile(file, responseBytes);
        try (NetcdfDataset dataset = NetcdfDataset.openDataset(file.getAbsolutePath())) {
            assertNotNull(dataset);
            // check dimensions
            Dimension rlonDim = dataset.findDimension("rlon");
            assertNotNull(rlonDim);
            assertEquals(7, rlonDim.getLength());
            Dimension rlatDim = dataset.findDimension("rlat");
            assertNotNull(rlatDim);
            assertEquals(5, rlatDim.getLength());
            // check coordinate variables
            Variable rlonVar = dataset.findVariable("rlon");
            assertNotNull(rlonVar);
            assertEquals(1, rlonVar.getDimensions().size());
            assertEquals(rlonDim, rlonVar.getDimensions().get(0));
            assertEquals("grid_longitude", rlonVar.findAttribute("long_name").getStringValue());
            assertEquals("grid_longitude", rlonVar.findAttribute("standard_name").getStringValue());
            assertEquals("degrees", rlonVar.findAttribute("units").getStringValue());
            assertArrayEquals(new float[] { -30, -20, -10, 0, 10, 20, 30 },
                    (float[]) rlonVar.read().copyTo1DJavaArray(), 0.0f);
            Variable rlatVar = dataset.findVariable("rlat");
            assertNotNull(rlatVar);
            assertEquals(1, rlatVar.getDimensions().size());
            assertEquals(rlatDim, rlatVar.getDimensions().get(0));
            assertEquals("grid_latitude", rlatVar.findAttribute("long_name").getStringValue());
            assertEquals("grid_latitude", rlatVar.findAttribute("standard_name").getStringValue());
            assertEquals("degrees", rlatVar.findAttribute("units").getStringValue());
            assertArrayEquals(new float[] { -20, -10, 0, 10, 20 },
                    (float[]) rlatVar.read().copyTo1DJavaArray(), 0.0f);
            // check projection variable
            Variable projVar = dataset.findVariable("rotated_latitude_longitude");
            assertNotNull(projVar);
            assertEquals("rotated_latitude_longitude",
                    projVar.findAttribute("grid_mapping_name").getStringValue());
            assertEquals(74.0,
                    projVar.findAttribute("grid_north_pole_longitude").getNumericValue());
            assertEquals(36.0, projVar.findAttribute("grid_north_pole_latitude").getNumericValue());
            Variable tempVar = dataset.findVariable("Temperature_surface");
            // check Temperature_surface variable
            assertNotNull(tempVar);
            assertEquals("rotated_latitude_longitude",
                    tempVar.findAttribute("grid_mapping").getStringValue());
            assertEquals("K", tempVar.findAttribute("units").getStringValue());
            assertEquals(2, tempVar.getDimensions().size());
            assertEquals(rlatDim, tempVar.getDimensions().get(0));
            assertEquals(rlonDim, tempVar.getDimensions().get(1));
            assertArrayEquals(
                    new float[] { 300, 299, 298, 297, 296, 295, 294, 299, 300, 299, 298, 297, 296,
                            295, 298, 299, 300, 299, 298, 297, 296, 297, 298, 299, 300, 299, 298,
                            297, 296, 297, 298, 299, 300, 299, 298 },
                    (float[]) tempVar.read().copyTo1DJavaArray(), 0.0f);
        } finally {
            FileUtils.deleteQuietly(file);
        }
    }

}
