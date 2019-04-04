/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.hamcrest.Matchers.equalToIgnoringCase;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GWCDataSecurityChallengeIntegrationTest extends WMSTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        DataAccessRuleDAO dao =
                GeoServerExtensions.bean(DataAccessRuleDAO.class, applicationContext);
        dao.setCatalogMode(CatalogMode.CHALLENGE);

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);
        GWC.get().getConfig().setSecurityEnabled(true);

        addUser("cite", "cite", null, Arrays.asList("ROLE_CITE_LAKES_VIEWER"));
        addUser("other", "other", null, Arrays.asList("OTHER_VIEWER"));

        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");
        addLayerAccessRule("cite", "Lakes", AccessMode.READ, "ROLE_CITE_LAKES_VIEWER");
    }

    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Test
    public void testDirectWMSIntegration() throws Exception {
        String path =
                "wms?service=WMS&request=GetMap&version=1.1.1&format=image/png"
                        + "&layers="
                        + getLayerId(MockData.LAKES)
                        + "&srs=EPSG:4326"
                        + "&width=256&height=256&styles=&bbox=-180.0,-90.0,0.0,90.0&tiled=true";
        MockHttpServletResponse response;

        // Try first as anonymous user, which should be disallowed.
        setRequestAuth(null, null);
        response = getAsServletResponse(path);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());

        // Make initial authorized request to cache the item.
        setRequestAuth("cite", "cite");
        response = getAsServletResponse(path);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("MISS"));

        // Make second authorized request to ensure the item was cached.
        response = getAsServletResponse(path);
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals("image/png", response.getContentType());
        assertThat(response.getHeader("geowebcache-cache-result"), equalToIgnoringCase("HIT"));

        // Ensure other unauthorized users can't access the cached tile.
        setRequestAuth("other", "other");
        response = getAsServletResponse(path);
        assertEquals(HttpServletResponse.SC_FORBIDDEN, response.getStatus());

        // Ensure anonymous users can't access the cached tile.
        setRequestAuth(null, null);
        response = getAsServletResponse(path);
        assertEquals(HttpServletResponse.SC_UNAUTHORIZED, response.getStatus());
    }
}
