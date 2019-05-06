/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2019, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 *
 */
package org.geoserver.rest.service;

import static org.junit.Assert.assertEquals;

import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.impl.AbstractUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class ResourceAccessManagerWMSTest extends WMSTestSupport {

    static final Logger LOGGER = Logging.getLogger(ResourceAccessManagerWMSTest.class);

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/wms/ResourceAccessManagerContext.xml");
    }
    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerUserGroupStore ugStore =
                getSecurityManager()
                        .loadUserGroupService(AbstractUserGroupService.DEFAULT_NAME)
                        .createStore();
        ugStore.addUser(ugStore.createUserObject("cite", "cite", true));
        ugStore.store();

        GeoServerRoleStore roleStore = getSecurityManager().getActiveRoleService().createStore();
        GeoServerRole role = roleStore.createRoleObject("ROLE_DUMMY");
        roleStore.addRole(role);
        roleStore.associateRoleToUser(role, "cite");
        roleStore.store();

        prepare();
    }

    public void prepare() throws Exception {
        // populate the access manager
        Catalog catalog = getCatalog();
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");

        tam.putLimits(
                "cite",
                catalog.getWorkspaceByName("cite"),
                new WorkspaceAccessLimits(CatalogMode.MIXED, false, false));
    }

    @Test
    public void testGetCapabilitiesLimitedWorkspace() throws Exception {
        setRequestAuth("cite", "cite");
        MockHttpServletResponse response =
                getAsServletResponse("cite/wms?service=WMS&version=1.1.1&request=GetCapabilities");
        // exception used to be eaten and result be a 200
        assertEquals(403, response.getStatus());
    }
}
