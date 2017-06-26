/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.IOException;

import org.apache.commons.io.IOUtils;
import org.geoserver.opensearch.eo.OSEOTestSupport;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.Before;
import org.opengis.filter.FilterFactory2;
import org.springframework.mock.web.MockHttpServletResponse;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;

public class OSEORestTestSupport extends OSEOTestSupport {
    
    protected static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Before
    public void loginAdmin() {
        login("admin", "geoserver", GeoServerRole.ADMIN_ROLE.getAuthority());
    }

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        if (!isQuietTests()) {
            System.out.println(response.getContentAsString());
        }

        assertEquals(expectedHttpCode, response.getStatus());
        assertThat(response.getContentType(), startsWith("application/json"));
        return JsonPath.parse(response.getContentAsString());
    }
    
    protected byte[] getTestData(String location) throws IOException {
        return IOUtils.toByteArray(getClass().getResourceAsStream(location));
    }


}