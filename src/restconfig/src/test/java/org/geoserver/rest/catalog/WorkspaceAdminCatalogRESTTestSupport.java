/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import jakarta.servlet.Filter;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.AccessMode;

/**
 * Base test support class for workspace administrator REST API integration tests.
 *
 * <p>Enables the Spring Security filter chain and sets up a workspace admin user with admin access to the {@code cite}
 * workspace from the default test data. The {@code sf} workspace is not administrable by this user.
 *
 * <p>Admin authentication is inherited from {@link CatalogRESTTestSupport#login()} which calls {@code loginAsAdmin()}
 * in {@code @Before}. Workspace admin tests should call {@link #setWorkspaceAdminRequestAuth()} to switch to the
 * workspace admin user for HTTP Basic auth.
 *
 * @see WorkspaceAdminRestIntegrationTest
 */
public abstract class WorkspaceAdminCatalogRESTTestSupport extends CatalogRESTTestSupport {

    protected static final String ROLE_WS_ADMIN = "ROLE_CITE_WORKSPACE_ADMIN";

    protected static final String WSADMIN_USER = "wsadmin";
    protected static final String WSADMIN_PASSWORD = "wsadmin";

    /** Workspace the test user can administer (from default test data) */
    protected static final String WS = MockData.CITE_PREFIX;

    /** Workspace the test user cannot administer (from default test data) */
    protected static final String WS_OTHER = MockData.SF_PREFIX;

    /** A layer known to exist in the adminable workspace */
    protected static final String CITE_LAYER = MockData.BUILDINGS.getLocalPart();

    /** Enable spring security */
    @Override
    protected List<Filter> getFilters() {
        List<Filter> filters = new ArrayList<>(super.getFilters());
        filters.add((Filter) GeoServerExtensions.bean("filterChainProxy"));
        return filters;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addUser(WSADMIN_USER, WSADMIN_PASSWORD, null, List.of(ROLE_WS_ADMIN));
        addLayerAccessRule(WS, "*", AccessMode.ADMIN, ROLE_WS_ADMIN);
    }

    protected void setAdminRequestAuth() {
        setRequestAuth("admin", "geoserver");
    }

    protected void setWorkspaceAdminRequestAuth() {
        setRequestAuth(WSADMIN_USER, WSADMIN_PASSWORD);
    }
}
