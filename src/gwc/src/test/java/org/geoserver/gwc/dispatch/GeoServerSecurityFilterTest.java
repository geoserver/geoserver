/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class GeoServerSecurityFilterTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // Don't set up any test data
    }

    @Test
    public void testGwcRootAnonymous() throws Exception {
        doTestHomePage(false, false);
    }

    @Test
    public void testGwcHomeAnonymous() throws Exception {
        doTestHomePage(true, false);
    }

    @Test
    public void testGwcRootUser() throws Exception {
        login("user", "password", "ROLE_USER");
        doTestHomePage(false, false);
    }

    @Test
    public void testGwcHomeUser() throws Exception {
        login("user", "password", "ROLE_USER");
        doTestHomePage(true, false);
    }

    @Test
    public void testGwcRootAdmin() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        doTestHomePage(false, true);
    }

    @Test
    public void testGwcHomeAdmin() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        doTestHomePage(true, true);
    }

    private void doTestHomePage(boolean home, boolean isAdmin) throws Exception {
        String html = getAsString("gwc" + (home ? "/home" : ""));
        assertThat(html, containsString("GWC Home"));
        assertThat(html, containsString("Welcome to GeoWebCache"));
        if (isAdmin) {
            assertThat(html, containsString(" version "));
            assertThat(html, containsString(" build "));
            assertThat(html, containsString("Runtime Statistics"));
            assertThat(html, containsString("Storage Locations"));
        } else {
            assertThat(html, not(containsString(" version ")));
            assertThat(html, not(containsString(" build ")));
            assertThat(html, not(containsString("Runtime Statistics")));
            assertThat(html, not(containsString("Storage Locations")));
        }
    }
}
