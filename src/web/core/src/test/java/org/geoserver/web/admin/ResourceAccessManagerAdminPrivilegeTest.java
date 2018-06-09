/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.WorkspaceAccessLimits;

public class ResourceAccessManagerAdminPrivilegeTest extends AbstractAdminPrivilegeTest {
    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add(
                "classpath:/org/geoserver/web/admin/ResourceAccessManagerContext.xml");
    }

    @Override
    protected void setupAccessRules() {
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        Catalog catalog = getCatalog();

        WorkspaceAccessLimits wsAdminLimits =
                new WorkspaceAccessLimits(CatalogMode.HIDE, true, true, true);
        WorkspaceAccessLimits wsUserLimits =
                new WorkspaceAccessLimits(CatalogMode.HIDE, true, true, false);

        WorkspaceInfo citeWs = catalog.getWorkspaceByName("cite");
        tam.putLimits("cite", citeWs, wsAdminLimits);

        WorkspaceInfo sfWs = catalog.getWorkspaceByName("sf");
        tam.putLimits("sf", sfWs, wsAdminLimits);

        tam.setDefaultWorkspaceAccessLimits(wsUserLimits);
    }
}
