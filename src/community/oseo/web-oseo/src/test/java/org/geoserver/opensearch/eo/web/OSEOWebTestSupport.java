/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.web;

import java.io.IOException;
import java.util.List;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.geoserver.opensearch.eo.store.OSEOPostGISResource;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;

/**
 * Support class for writing UI tests
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class OSEOWebTestSupport extends GeoServerWicketTestSupport {

    protected List<String> collectionNames;
    protected String openSearchAccessStoreId;

    protected abstract OSEOPostGISResource getOSEOPostGIS();

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data to setup
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        getOSEOPostGIS().setupBasicOpenSearch(getCatalog(), getGeoServer());
        OSEOInfo service = getGeoServer().getService(OSEOInfo.class);
        openSearchAccessStoreId = service.getOpenSearchAccessStoreId();
    }

    @Before
    public void collectCollectionNames() {
        // model returns sorted collection names
        this.collectionNames = new EOCollectionsModel().load();
    }

    protected void ensureRolesAvailable(List<String> roleNames) throws IOException {
        GeoServerSecurityManager securityManager = getGeoServerApplication().getSecurityManager();
        GeoServerRoleService roleService = securityManager.getActiveRoleService();
        GeoServerRoleStore roleStore = roleService.createStore();
        for (String roleName : roleNames) {
            if (roleService.getRoleByName(roleName) == null) roleStore.addRole(new GeoServerRole(roleName));
        }
        roleStore.store();
    }
}
