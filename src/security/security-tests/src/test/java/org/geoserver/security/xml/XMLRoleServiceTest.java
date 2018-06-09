/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.xml;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.event.RoleLoadedEvent;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.AbstractRoleServiceTest;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.Util;
import org.geoserver.test.SystemTest;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;

@Category(SystemTest.class)
public class XMLRoleServiceTest extends AbstractRoleServiceTest {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.xml");

    @Override
    public GeoServerRoleService createRoleService(String serviceName) throws Exception {
        return createRoleService(serviceName, XMLConstants.FILE_RR);
    }

    @Before
    public void cleraRoleService() throws Exception {
        store.clear();
        store.store();
    }

    protected GeoServerRoleService createRoleService(String serviceName, String xmlFileName)
            throws Exception {

        XMLRoleServiceConfig gaConfig =
                (XMLRoleServiceConfig) getSecurityManager().loadRoleServiceConfig(serviceName);
        if (gaConfig == null) {
            gaConfig = new XMLRoleServiceConfig();
            gaConfig.setName(serviceName);
        }
        gaConfig.setClassName(XMLRoleService.class.getName());
        gaConfig.setCheckInterval(1000);
        gaConfig.setFileName(xmlFileName);
        gaConfig.setValidating(true);
        getSecurityManager().saveRoleService(gaConfig /*,isNewRoleService(serviceName)*/);
        return getSecurityManager().loadRoleService(serviceName);
    }

    @Test
    public void testCopyFrom() throws Exception {
        GeoServerRoleService service1 = createRoleService("copyFrom");
        GeoServerRoleService service2 = createRoleService("copyTo");
        GeoServerRoleStore store1 = createStore(service1);
        GeoServerRoleStore store2 = createStore(service2);

        store1.clear();
        checkEmpty(store1);
        insertValues(store1);
        Util.copyFrom(store1, store2);
        store1.clear();
        checkEmpty(store1);
        checkValuesInserted(store2);
    }

    @Test
    public void testDefault() throws Exception {
        GeoServerRoleService service =
                getSecurityManager().loadRoleService(XMLRoleService.DEFAULT_NAME);

        assertEquals(2, service.getRoles().size());
        GeoServerRole adminRole = service.getRoleByName(XMLRoleService.DEFAULT_LOCAL_ADMIN_ROLE);
        GeoServerRole groupAdminRole =
                service.getRoleByName(XMLRoleService.DEFAULT_LOCAL_GROUP_ADMIN_ROLE);

        assertEquals(0, service.getGroupNamesForRole(adminRole).size());
        assertEquals(0, service.getGroupNamesForRole(groupAdminRole).size());
        assertEquals(1, service.getUserNamesForRole(adminRole).size());
        assertEquals(0, service.getUserNamesForRole(groupAdminRole).size());
        assertEquals(1, service.getRolesForUser(GeoServerUser.ADMIN_USERNAME).size());
        assertTrue(service.getRolesForUser(GeoServerUser.ADMIN_USERNAME).contains(adminRole));
    }

    @Test
    public void testLocking() throws Exception {
        File xmlFile = File.createTempFile("roles", ".xml");
        try {
            FileUtils.copyURLToFile(getClass().getResource("rolesTemplate.xml"), xmlFile);
            GeoServerRoleService service1 =
                    createRoleService("locking1", xmlFile.getCanonicalPath());
            GeoServerRoleService service2 =
                    createRoleService("locking2", xmlFile.getCanonicalPath());
            GeoServerRoleStore store1 = createStore(service1);
            GeoServerRoleStore store2 = createStore(service2);

            GeoServerRole role_test1 = store1.createRoleObject("ROLE_TEST");
            GeoServerRole role_test2 = store2.createRoleObject("ROLE_TEST");

            // obtain a lock
            store1.addRole(role_test1);
            boolean fail;
            String failMessage = "Concurrent lock not allowed";
            fail = true;
            try {
                store2.clear();
            } catch (IOException ex) {
                fail = false;
            }
            if (fail) Assert.fail(failMessage);

            // release lock
            store1.load();
            // get lock
            store2.addRole(role_test1);

            fail = true;
            try {
                store1.clear();
            } catch (IOException ex) {
                fail = false;
            }
            if (fail) Assert.fail(failMessage);

            // release lock
            store2.store();
            store1.clear();
            store1.store();

            // // end of part one, now check all modifying methods

            // obtain a lock
            store1.addRole(role_test1);

            fail = true;
            try {
                store2.associateRoleToGroup(role_test2, "agroup");
            } catch (IOException ex) {
                try {
                    store2.disAssociateRoleFromGroup(role_test2, "agroup");
                } catch (IOException e) {
                    fail = false;
                }
            }
            if (fail) Assert.fail(failMessage);

            fail = true;
            try {
                store2.associateRoleToUser(role_test2, "auser");
            } catch (IOException ex) {
                try {
                    store2.disAssociateRoleFromUser(role_test2, "auser");
                } catch (IOException e) {
                    fail = false;
                }
            }
            if (fail) Assert.fail(failMessage);

            fail = true;
            try {
                store2.updateRole(role_test2);
            } catch (IOException ex) {
                try {
                    store2.removeRole(role_test2);
                } catch (IOException ex1) {
                    try {
                        store2.addRole(role_test2);
                    } catch (IOException ex2) {
                        fail = false;
                    }
                }
            }
            if (fail) Assert.fail(failMessage);

            fail = true;
            try {
                store2.clear();
            } catch (IOException ex) {
                try {
                    store2.store();
                } catch (IOException e) {
                    fail = false;
                }
            }
            if (fail) Assert.fail(failMessage);

            fail = true;
            try {
                store2.setParentRole(role_test1, null);
            } catch (IOException ex) {
                fail = false;
            }
            if (fail) Assert.fail(failMessage);
        } finally {
            xmlFile.delete();
        }
    }

    @Test
    public void testDynamicReload() throws Exception {
        Files.schedule(200, TimeUnit.MILLISECONDS);
        File xmlFile = File.createTempFile("roles", ".xml");
        try {
            FileUtils.copyURLToFile(getClass().getResource("rolesTemplate.xml"), xmlFile);
            GeoServerRoleService service1 =
                    createRoleService("reload1", xmlFile.getCanonicalPath());
            GeoServerRoleService service2 =
                    createRoleService("reload2", xmlFile.getCanonicalPath());

            GeoServerRoleStore store1 = createStore(service1);

            GeoServerRole role_test1 = store1.createRoleObject("ROLE_TEST1");

            checkEmpty(service1);
            checkEmpty(service2);

            // prepare for syncing
            class CheckRoleLoaded implements RoleLoadedListener {
                public int notified = 0;

                @Override
                public void rolesChanged(RoleLoadedEvent event) {
                    synchronized (this) {
                        this.notifyAll();
                        notified++;
                    }
                }
            };
            CheckRoleLoaded listener = new CheckRoleLoaded();
            service2.registerRoleLoadedListener(listener);

            // modifiy store1
            store1.addRole(role_test1);
            store1.store();
            assertTrue(service1.getRoles().size() == 1);

            // increment lastmodified adding a second manually, the test is too fast
            xmlFile.setLastModified(xmlFile.lastModified() + 1000);

            // wait for the listener to unlock when
            // service 2 triggers a load event
            synchronized (listener) {
                if (listener.notified == 0) {
                    listener.wait(); // wait 4 seconds
                }
            }
            assertTrue("notification expected", listener.notified > 0);

            // here comes the magic !!!
            assertTrue(service2.getRoles().size() == 1);
        } finally {
            Files.schedule(10, TimeUnit.SECONDS);
            xmlFile.delete();
        }
    }
}
