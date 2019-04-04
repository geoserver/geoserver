/* (c) 2017-2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.taskmanager.AbstractTaskManagerTest;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.geoserver.taskmanager.data.TaskManagerFactory;
import org.geoserver.taskmanager.schedule.BatchJobService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class TaskManagerSecurityUtilTest extends AbstractTaskManagerTest {

    @Autowired private DataAccessRuleDAO ruleDao;

    @Autowired private TaskManagerSecurityUtil secUtil;

    @Autowired private TaskManagerDao dao;

    @Autowired private TaskManagerFactory fac;

    @Autowired private BatchJobService bjService;

    @Autowired private Catalog catalog;

    private Configuration config;

    private Batch batch;

    @Before
    public void setup() {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        // workspaces
        CatalogFactory catFac = new CatalogFactoryImpl(catalog);
        WorkspaceInfo wi;
        wi = catFac.createWorkspace();
        wi.setName("cdf");
        catalog.add(wi);
        wi = catFac.createWorkspace();
        wi.setName("cite");
        catalog.add(wi);
        catalog.setDefaultWorkspace(catalog.getWorkspaceByName("cite"));

        // rules
        DataAccessRule rule = new DataAccessRule();
        rule.setRoot("cdf");
        rule.setAccessMode(AccessMode.READ);
        rule.getRoles().add("readcdf");
        ruleDao.addRule(rule);

        rule = new DataAccessRule();
        rule.setRoot("cdf");
        rule.setAccessMode(AccessMode.WRITE);
        rule.getRoles().add("writecdf");
        ruleDao.addRule(rule);

        rule = new DataAccessRule();
        rule.setRoot("cdf");
        rule.setAccessMode(AccessMode.ADMIN);
        rule.getRoles().add("admincdf");
        ruleDao.addRule(rule);

        rule = new DataAccessRule();
        rule.setRoot("cite");
        rule.setAccessMode(AccessMode.READ);
        rule.getRoles().add("readcite");
        ruleDao.addRule(rule);

        rule = new DataAccessRule();
        rule.setRoot("cite");
        rule.setAccessMode(AccessMode.WRITE);
        rule.getRoles().add("writecite");
        ruleDao.addRule(rule);

        rule = new DataAccessRule();
        rule.setRoot("cite");
        rule.setAccessMode(AccessMode.ADMIN);
        rule.getRoles().add("admincite");
        ruleDao.addRule(rule);

        config = fac.createConfiguration();
        config.setName("my_config");
        config.setWorkspace("cdf");
        config = dao.save(config);
        batch = fac.createBatch();
        batch.setName("my_batch");
        batch.setConfiguration(config);
        batch.setWorkspace("cite");
        batch = bjService.saveAndSchedule(batch);
    }

    @After
    public void cleanUp() {
        // taskmanager db
        dao.delete(batch);
        dao.delete(config);

        // clear the rules
        ruleDao.clear();

        // catalog
        catalog.remove(catalog.getWorkspaceByName("cdf"));
        catalog.remove(catalog.getWorkspaceByName("cite"));

        logout();
    }

    @Test
    public void testReadable() {
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken(
                        "jan",
                        "jan",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("readcdf"),
                                    new SimpleGrantedAuthority("readcite")
                                }));
        assertTrue(secUtil.isReadable(user, config));
        assertTrue(secUtil.isReadable(user, batch));

        user =
                new UsernamePasswordAuthenticationToken(
                        "piet",
                        "piet",
                        Arrays.asList(
                                new GrantedAuthority[] {new SimpleGrantedAuthority("readcdf")}));
        assertTrue(secUtil.isReadable(user, config));
        assertFalse(secUtil.isReadable(user, batch));

        user =
                new UsernamePasswordAuthenticationToken(
                        "pol",
                        "pol",
                        Arrays.asList(
                                new GrantedAuthority[] {new SimpleGrantedAuthority("readcite")}));
        assertFalse(secUtil.isReadable(user, config));
        assertFalse(secUtil.isReadable(user, batch));
    }

    @Test
    public void testWritable() {
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken(
                        "jan",
                        "jan",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("writecdf"),
                                    new SimpleGrantedAuthority("writecite")
                                }));
        assertTrue(secUtil.isWritable(user, batch));

        user =
                new UsernamePasswordAuthenticationToken(
                        "piet",
                        "piet",
                        Arrays.asList(
                                new GrantedAuthority[] {new SimpleGrantedAuthority("writecdf")}));
        assertFalse(secUtil.isWritable(user, batch));

        user =
                new UsernamePasswordAuthenticationToken(
                        "pol",
                        "pol",
                        Arrays.asList(
                                new GrantedAuthority[] {new SimpleGrantedAuthority("writecite")}));
        assertFalse(secUtil.isWritable(user, batch));
    }

    @Test
    public void testAdminable() {
        UsernamePasswordAuthenticationToken user =
                new UsernamePasswordAuthenticationToken(
                        "jan",
                        "jan",
                        Arrays.asList(
                                new GrantedAuthority[] {
                                    new SimpleGrantedAuthority("admincdf"),
                                    new SimpleGrantedAuthority("admincite")
                                }));
        assertTrue(secUtil.isAdminable(user, config));
        assertTrue(secUtil.isAdminable(user, batch));

        user =
                new UsernamePasswordAuthenticationToken(
                        "piet",
                        "piet",
                        Arrays.asList(
                                new GrantedAuthority[] {new SimpleGrantedAuthority("admincdf")}));
        assertTrue(secUtil.isAdminable(user, config));
        assertFalse(secUtil.isAdminable(user, batch));

        user =
                new UsernamePasswordAuthenticationToken(
                        "pol",
                        "pol",
                        Arrays.asList(
                                new GrantedAuthority[] {new SimpleGrantedAuthority("admincite")}));
        assertFalse(secUtil.isAdminable(user, config));
        assertFalse(secUtil.isAdminable(user, batch));
    }
}
