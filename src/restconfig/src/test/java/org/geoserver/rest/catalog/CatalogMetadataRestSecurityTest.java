/* (c) 2026 Open Source Geospatial Foundation
 * This code is licensed under the GPL 2.0 license.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.assertEquals;

import jakarta.servlet.Filter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.RESTfulDefinitionSource;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.RESTAccessRuleDAO;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Integration tests for REST catalog metadata authorization.
 *
 * <p>Exercises GeoServer's HTTP security filter chain and catalog security independently.
 *
 * <p>Also verifies that users with REST authorization cannot retrieve metadata for layers they are not authorized to
 * read.
 */
public class CatalogMetadataRestSecurityTest extends GeoServerSystemTestSupport {

    private static final String METADATA_PATH = RestBaseController.ROOT_PATH + "/metadata/layers.json";

    private static final String BRIDGES_METADATA_PATH =
            RestBaseController.ROOT_PATH + "/metadata/workspaces/cite/featuretypes/Bridges.json";

    private static final String BUILDINGS_METADATA_PATH =
            RestBaseController.ROOT_PATH + "/metadata/workspaces/cite/featuretypes/Buildings.json";

    private static final String METADATA_RULE = "/rest/metadata/**:GET=ROLE_METADATA_READER";

    private static final String METADATA_RULE_EXPANDED =
            "/rest/metadata/**:GET=ROLE_METADATA_READER,ROLE_SHARED_LAYER_READER";

    private static final String ADMIN_GET_RULE = "/**:GET=ROLE_ADMINISTRATOR";

    private static final String ADMIN_WRITE_RULE = "/**:POST,PUT,DELETE=ROLE_ADMINISTRATOR";

    @Override
    protected List<Filter> getFilters() {
        return List.of(GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class));
    }

    /** Save REST rules and refresh the active authorization mappings. */
    private void storeAndReloadRestRules(RESTAccessRuleDAO dao) throws IOException {

        dao.storeRules();

        RESTfulDefinitionSource definitionSource =
                (RESTfulDefinitionSource) GeoServerExtensions.bean("restFilterDefinitionMap");

        definitionSource.reload();
    }

    /**
     * Configure REST authorization with the specific metadata rule first.
     *
     * <p>The general administrator wildcard rules must follow it.
     */
    private void configureRestRules(RESTAccessRuleDAO dao, String metadataRule) throws IOException {

        dao.clear();

        dao.addRule(metadataRule);
        dao.addRule(ADMIN_GET_RULE);
        dao.addRule(ADMIN_WRITE_RULE);

        storeAndReloadRestRules(dao);
    }

    /**
     * Configure restrictive catalog READ permissions.
     *
     * <p>ROLE_SHARED_LAYER_READER can read cite:Bridges.
     *
     * <p>ROLE_BUILDINGS_READER can read cite:Buildings.
     *
     * <p>All other resources require administrator privileges.
     *
     * <p>Both restricted layers belong to the same workspace.
     *
     * <p>This replaces any inherited wildcard READ permission.
     */
    private void configureLayerRules(DataAccessRuleDAO dao) throws IOException {

        dao.clear();

        dao.setCatalogMode(CatalogMode.HIDE);

        /*
         * Default: administrator access only.
         */
        dao.addRule(new DataAccessRule("*", "*", AccessMode.READ, "ROLE_ADMINISTRATOR"));

        dao.addRule(new DataAccessRule("*", "*", AccessMode.WRITE, "ROLE_ADMINISTRATOR"));

        /*
         * Explicit READ permission for cite:Bridges.
         */
        dao.addRule(new DataAccessRule(
                "cite", "Bridges", AccessMode.READ, "ROLE_SHARED_LAYER_READER", "ROLE_ADMINISTRATOR"));

        /*
         * Explicit READ permission for cite:Buildings.
         *
         * A different role is required.
         */
        dao.addRule(new DataAccessRule(
                "cite", "Buildings", AccessMode.READ, "ROLE_BUILDINGS_READER", "ROLE_ADMINISTRATOR"));

        dao.storeRules();
    }

    /** Verify that a user sees only cite:Bridges. */
    private void assertOnlyBridges(MockHttpServletResponse response) throws Exception {

        assertEquals(200, response.getStatus());

        assertEquals("{\"layers\":[\"cite:Bridges\"]}", response.getContentAsString());
    }

    /** Verify that a user sees only cite:Buildings. */
    private void assertOnlyBuildings(MockHttpServletResponse response) throws Exception {

        assertEquals(200, response.getStatus());

        assertEquals("{\"layers\":[\"cite:Buildings\"]}", response.getContentAsString());
    }

    /** Verify that direct feature type metadata access is allowed. */
    private void assertFeatureTypeAllowed(String path) throws Exception {

        MockHttpServletResponse response = getAsServletResponse(path);

        assertEquals(200, response.getStatus());
    }

    /**
     * Verify that direct feature type metadata access is hidden.
     *
     * <p>CatalogMode.HIDE should return 404 rather than expose information about a restricted resource.
     */
    private void assertFeatureTypeHidden(String path) throws Exception {

        MockHttpServletResponse response = getAsServletResponse(path);

        assertEquals(404, response.getStatus());
    }

    /**
     * Verify REST authorization and catalog READ authorization are enforced independently.
     *
     * <p>Also verify that REST access cannot bypass layer-level restrictions within the same workspace.
     */
    @Test
    public void testMetadataRestRoleAuthorization() throws Exception {

        /*
         * User A:
         *
         * REST metadata permission.
         * READ permission for cite:Bridges.
         */
        addUser("metadata_user_a", "password_a", null, List.of("ROLE_SHARED_LAYER_READER", "ROLE_METADATA_READER"));

        /*
         * User B:
         *
         * READ permission for cite:Bridges.
         * No initial REST metadata permission.
         */
        addUser("metadata_user_b", "password_b", null, List.of("ROLE_SHARED_LAYER_READER"));

        /*
         * User C:
         *
         * REST metadata permission.
         * No catalog READ permissions.
         *
         * This user proves REST authorization alone cannot
         * bypass catalog security.
         */
        addUser("metadata_user_c", "password_c", null, List.of("ROLE_METADATA_READER"));

        /*
         * User D:
         *
         * REST metadata permission.
         * READ permission for cite:Buildings only.
         *
         * This user proves two REST-authorized users can
         * have different layer permissions in the same workspace.
         */
        addUser("metadata_user_d", "password_d", null, List.of("ROLE_BUILDINGS_READER", "ROLE_METADATA_READER"));

        RESTAccessRuleDAO restDao = RESTAccessRuleDAO.get();
        DataAccessRuleDAO dataDao = DataAccessRuleDAO.get();

        /*
         * Preserve original REST authorization rules.
         */
        List<String> originalRestRules = new ArrayList<>(restDao.getRules());

        /*
         * Preserve original catalog security rules.
         */
        List<DataAccessRule> originalDataRules = new ArrayList<>(dataDao.getRules());

        CatalogMode originalCatalogMode = dataDao.getMode();

        try {

            /*
             * ==================================================
             * CONFIGURE CATALOG SECURITY
             * ==================================================
             */
            configureLayerRules(dataDao);

            /*
             * ==================================================
             * PHASE 1
             *
             * REST requires ROLE_METADATA_READER.
             * ==================================================
             */
            configureRestRules(restDao, METADATA_RULE);

            /*
             * --------------------------------------------------
             * User A
             *
             * REST permission: YES
             * Bridges READ:   YES
             * Buildings READ: NO
             *
             * Expected: only cite:Bridges.
             * --------------------------------------------------
             */
            setRequestAuth("metadata_user_a", "password_a");

            MockHttpServletResponse response = getAsServletResponse(METADATA_PATH);

            assertOnlyBridges(response);

            /*
             * Direct feature type metadata requests.
             */
            assertFeatureTypeAllowed(BRIDGES_METADATA_PATH);
            assertFeatureTypeHidden(BUILDINGS_METADATA_PATH);

            /*
             * --------------------------------------------------
             * User B
             *
             * REST permission: NO
             * Bridges READ:   YES
             *
             * Expected: 403.
             * --------------------------------------------------
             */
            setRequestAuth("metadata_user_b", "password_b");

            response = getAsServletResponse(METADATA_PATH);

            assertEquals(403, response.getStatus());

            /*
             * Direct feature type metadata must also be denied
             * because REST authorization is missing.
             */
            response = getAsServletResponse(BRIDGES_METADATA_PATH);

            assertEquals(403, response.getStatus());

            /*
             * --------------------------------------------------
             * User C
             *
             * REST permission: YES
             * Bridges READ:   NO
             * Buildings READ: NO
             *
             * Expected: 200 with an empty layer listing.
             * --------------------------------------------------
             */
            setRequestAuth("metadata_user_c", "password_c");

            response = getAsServletResponse(METADATA_PATH);

            assertEquals(200, response.getStatus());

            assertEquals("{\"layers\":[]}", response.getContentAsString());

            /*
             * User C cannot retrieve metadata for either layer.
             */
            assertFeatureTypeHidden(BRIDGES_METADATA_PATH);
            assertFeatureTypeHidden(BUILDINGS_METADATA_PATH);

            /*
             * --------------------------------------------------
             * User D
             *
             * REST permission: YES
             * Bridges READ:   NO
             * Buildings READ: YES
             *
             * Expected: only cite:Buildings.
             *
             * This verifies that Buildings exists and is
             * accessible to an appropriately authorized user.
             * --------------------------------------------------
             */
            setRequestAuth("metadata_user_d", "password_d");

            response = getAsServletResponse(METADATA_PATH);

            assertOnlyBuildings(response);

            /*
             * Direct feature type metadata requests.
             *
             * Buildings must be accessible.
             * Bridges must be hidden.
             */
            assertFeatureTypeAllowed(BUILDINGS_METADATA_PATH);
            assertFeatureTypeHidden(BRIDGES_METADATA_PATH);

            /*
             * ==================================================
             * PHASE 2
             *
             * Grant ROLE_SHARED_LAYER_READER REST access.
             * ==================================================
             */
            configureRestRules(restDao, METADATA_RULE_EXPANDED);

            /*
             * --------------------------------------------------
             * User B
             *
             * REST permission: YES
             * Bridges READ:   YES
             * Buildings READ: NO
             *
             * Expected: only cite:Bridges.
             * --------------------------------------------------
             */
            setRequestAuth("metadata_user_b", "password_b");

            response = getAsServletResponse(METADATA_PATH);

            assertOnlyBridges(response);

            /*
             * This is the critical layer security regression test.
             *
             * Even though User B now has REST authorization,
             * catalog security must still restrict metadata
             * access to the permitted layer.
             */
            assertFeatureTypeAllowed(BRIDGES_METADATA_PATH);
            assertFeatureTypeHidden(BUILDINGS_METADATA_PATH);

            /*
             * ==================================================
             * PHASE 3
             *
             * Revoke REST access from ROLE_SHARED_LAYER_READER.
             * ==================================================
             */
            configureRestRules(restDao, METADATA_RULE);

            /*
             * --------------------------------------------------
             * User B
             *
             * REST permission: NO
             * Bridges READ:   YES
             *
             * Expected: 403.
             * --------------------------------------------------
             */
            setRequestAuth("metadata_user_b", "password_b");

            response = getAsServletResponse(METADATA_PATH);

            assertEquals(403, response.getStatus());

            /*
             * The direct feature type endpoint must also
             * reject the request.
             */
            response = getAsServletResponse(BRIDGES_METADATA_PATH);

            assertEquals(403, response.getStatus());

        } finally {

            /*
             * ==================================================
             * CLEANUP
             *
             * Restore original REST and catalog security.
             * ==================================================
             */
            try {

                /*
                 * Restore REST authorization rules.
                 */
                restDao.clear();

                for (String rule : originalRestRules) {
                    restDao.addRule(rule);
                }

                storeAndReloadRestRules(restDao);

            } finally {

                try {

                    /*
                     * Restore catalog security rules.
                     */
                    dataDao.clear();

                    for (DataAccessRule rule : originalDataRules) {
                        dataDao.addRule(rule);
                    }

                    dataDao.setCatalogMode(originalCatalogMode);

                    dataDao.storeRules();

                } finally {

                    /*
                     * Clear request authentication.
                     */
                    setRequestAuth(null, null);
                }
            }
        }
    }
}
