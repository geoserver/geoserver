/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo;

import static org.junit.Assert.*;

import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GetCoverageTest extends WCSEOTestSupport {

    @Test
    public void testInvalidGranule() throws Exception {
        // ask for a granule that's not there, but whose coverage is not there
        MockHttpServletResponse response =
                getAsServletResponse(
                        "wcs?service=WCS&version=2.0.1&request=GetCoverage&coverageId=sf__notthere_granule_time_domainsRanges.4");
        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");

        // ask for the right coverage, but the granule is not there
        response =
                getAsServletResponse(
                        "wcs?service=WCS&version=2.0.1&request=GetCoverage&coverageId=sf__timeranges_granule_time_domainsRanges.2000");
        checkOws20Exception(response, 404, "NoSuchCoverage", "coverageId");
    }

    @Test
    public void testNoCoverageId() throws Exception {
        // ask for a granule that's not there, but whose coverage is not there
        MockHttpServletResponse response =
                getAsServletResponse("wcs?service=WCS&version=2.0.1&request=GetCoverage");
        checkOws20Exception(response, 400, "MissingParameterValue", "coverageId");
    }

    @Test
    public void testSimpleSelection() throws Exception {
        // ask for the right coverage, and the granule is there
        Document dom =
                getAsDOM(
                        "wcs?service=WCS&version=2.0.1&request=GetCoverage&coverageId=sf__timeranges_granule_time_domainsRanges.4&outputFormat=&format=application%2Fgml%2Bxml");
        // print(dom);

        assertEquals("1", xpath.evaluate("count(/gml:RectifiedGridCoverage)", dom));
    }
}
