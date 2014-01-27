/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
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
public class WCSNetCDFTest extends WCSTestSupport {

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

    @Test
    public void testCoverageNames() throws Exception {
        MockHttpServletResponse response = getAsServletResponse("ows?request=GetCoverage&service=WCS&version=2.0.1" +
                "&coverageId=wcs__NO2&format=application/x-netcdf&subset=http://www.opengis.net/def/axis/OGC/0/elevation(450)");
        assertEquals(200, response.getStatusCode());
        assertEquals("application/x-netcdf", response.getContentType());
    }
}
