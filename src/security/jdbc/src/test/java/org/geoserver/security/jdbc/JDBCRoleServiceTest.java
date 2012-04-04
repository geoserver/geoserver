/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;

import junit.framework.Assert;

import org.geoserver.data.test.TestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.impl.AbstractRoleServiceTest;


public abstract class JDBCRoleServiceTest extends AbstractRoleServiceTest {

    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security.jdbc");
    
    protected abstract String getFixtureId();            
    
    

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();
        if (store!=null) {
            JDBCRoleStore jdbcStore =(JDBCRoleStore)store;
            JDBCTestSupport.dropExistingTables(jdbcStore,jdbcStore.getConnection());
            store.store();
        }

    }

    @Override
    protected void setUpInternal() throws Exception {
        if (getTestData().isTestDataAvailable())
            super.setUpInternal();
    }

    
    
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

    
    public void testRoleDatabaseSetup() {
        try {        
            JDBCRoleStore jdbcStore =  
                (JDBCRoleStore) store;
            assertTrue(jdbcStore.tablesAlreadyCreated());
            jdbcStore.checkDDLStatements();
            jdbcStore.checkDMLStatements();
            jdbcStore.clear();
            jdbcStore.dropTables();            
            jdbcStore.store();
            assertFalse(jdbcStore.tablesAlreadyCreated());
            jdbcStore.load();
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
    }

    @Override
    protected TestData buildTestData() throws Exception {
        if ("h2".equalsIgnoreCase(getFixtureId()))
            return super.buildTestData();
        return new LiveDbmsDataSecurity(getFixtureId());
    }
    
    
    

    @Override
    protected boolean isJDBCTest() {
        return true;
    }




}
