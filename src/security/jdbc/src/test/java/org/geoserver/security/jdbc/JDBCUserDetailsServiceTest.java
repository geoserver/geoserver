/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import java.io.IOException;
import java.sql.SQLException;

import junit.framework.Assert;

import org.geoserver.data.test.TestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.AbstractUserDetailsServiceTest;

public abstract class JDBCUserDetailsServiceTest extends AbstractUserDetailsServiceTest {

    protected abstract String getFixtureId();
        
    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {
        
        return JDBCTestSupport.createUserGroupService(getFixtureId(), 
            (LiveDbmsDataSecurity)getTestData(), getSecurityManager());
    }

    @Override
    public GeoServerRoleService createRoleService(String serviceName) throws Exception {    
        return JDBCTestSupport.createRoleService(getFixtureId(),
            (LiveDbmsDataSecurity)getTestData(), getSecurityManager());
    }

    @Override
    public GeoServerRoleStore createStore(GeoServerRoleService service) throws IOException {
        JDBCRoleStore store = 
            (JDBCRoleStore) super.createStore(service);
        try {
            JDBCTestSupport.dropExistingTables(store,store.getConnection());
        } catch (SQLException e) {
            throw new IOException(e);
        }
        store.createTables();
        store.store();
        
        return store;        
    }

    @Override
    public GeoServerUserGroupStore createStore(GeoServerUserGroupService service) throws IOException {
        JDBCUserGroupStore store = 
            (JDBCUserGroupStore) super.createStore(service);
        try {
            JDBCTestSupport.dropExistingTables(store,store.getConnection());
        } catch (SQLException e) {
            throw new IOException(e);
        }
        store.createTables();
        store.store();
        return store;        
    }
    
    
    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        if (roleStore!=null) {
            JDBCRoleStore jdbcStore1 =(JDBCRoleStore) roleStore;
            JDBCTestSupport.dropExistingTables(jdbcStore1,jdbcStore1.getConnection());
            roleStore.store();
        }
        
        if (usergroupStore!=null) {
            JDBCUserGroupStore jdbcStore2 =(JDBCUserGroupStore) usergroupStore;
            JDBCTestSupport.dropExistingTables(jdbcStore2,jdbcStore2.getConnection());
            usergroupStore.store();
        }
    }

    @Override
    protected void setUpInternal() throws Exception {
        if (getTestData().isTestDataAvailable())
            super.setUpInternal();
    }

    
    @Override
    protected boolean isJDBCTest() {
        return true;
    }

    @Override
    protected TestData buildTestData() throws Exception {
        if ("h2".equalsIgnoreCase(getFixtureId()))
            return super.buildTestData();
        return new LiveDbmsDataSecurity(getFixtureId());
    }

    public void testConfiguration() {
        try {
            setServices("config");
            assertEquals(roleService,getSecurityManager().getActiveRoleService());
            //assertEquals(usergroupService,getSecurityManager().getActiveUserGroupService());
            assertEquals(usergroupService.getName(),
                    getSecurityManager().loadUserGroupService(getFixtureId()).getName());
            assertTrue(roleService.canCreateStore());
            assertTrue(usergroupService.canCreateStore());
        } catch (Exception ex) {
            Assert.fail(ex.getMessage());
        }
    }

}
