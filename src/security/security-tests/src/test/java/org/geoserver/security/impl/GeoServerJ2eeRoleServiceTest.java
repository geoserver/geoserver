/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.AbstractSecurityServiceTest;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.config.J2eeRoleServiceConfig;
import org.geoserver.util.IOUtils;
import org.junit.Test;

public class GeoServerJ2eeRoleServiceTest extends AbstractSecurityServiceTest {

    @Override
    public GeoServerRoleService createRoleService(String name) throws Exception {
        J2eeRoleServiceConfig config = new J2eeRoleServiceConfig();
        config.setName(name);
        config.setClassName(GeoServerJ2eeRoleService.class.getName());
        getSecurityManager().saveRoleService(config);
        // System.out.println(getDataDirectory().root().getAbsoluteFile());
        return null;
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        createRoleService("test1");
        createRoleService("test2");
    }

    @Test
    public void testNoRoles() throws Exception {
        copyWebXML("web1.xml");
        GeoServerRoleService service = getSecurityManager().loadRoleService("test1");
        checkEmpty(service);
    }

    @Test
    public void testRoles() throws Exception {
        copyWebXML("web2.xml");
        GeoServerRoleService service = getSecurityManager().loadRoleService("test2");
        assertEquals(4, service.getRoleCount());
        assertTrue(service.getRoles().contains(new GeoServerRole("role1")));
        assertTrue(service.getRoles().contains(new GeoServerRole("role2")));
        assertTrue(service.getRoles().contains(new GeoServerRole("employee")));
        assertTrue(service.getRoles().contains(new GeoServerRole("MGR")));
    }

    protected void copyWebXML(String name) throws IOException {
        File dataDir = getDataDirectory().root();
        File to = new File(dataDir, "WEB-INF");
        to = new File(to, "web.xml");
        IOUtils.copy(getClass().getResourceAsStream(name), to);
    }
}
