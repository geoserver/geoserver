/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.impl;

import junit.framework.Assert;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;

public abstract class AbstractUserGroupServiceTest extends AbstractSecurityServiceTest {

    protected GeoServerUserGroupService service;
    protected GeoServerUserGroupStore store;
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();        
        service = createUserGroupService("test");
        store  = createStore(service);
    }

    

    public void testInsert() {
        try {
            
            
            // all is empty
            checkEmpty(service);
            checkEmpty(store);
        
            // transaction has values ?
            insertValues(store);
            if (!isJDBCTest())
                    checkEmpty(service);
            checkValuesInserted(store);
            
            // rollback
            store.load();
            checkEmpty(store);
            checkEmpty(service);

            // commit
            insertValues(store);
            store.store();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            
        } catch ( Exception ex) {
            Assert.fail(ex.getMessage());
        }        
    }

    public void testModify() {
        try {
            
            // all is empty
            checkEmpty(service);
            checkEmpty(store);
        
            insertValues(store);
            store.store();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            modifyValues(store);
            if (!isJDBCTest())
                checkValuesInserted(service);
            checkValuesModified(store);
            
            store.load();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            modifyValues(store);
            store.store();
            checkValuesModified(store);
            checkValuesModified(service);
            
                        
        } catch ( Exception ex) {
            Assert.fail(ex.getMessage());
        }        
    }

    public void testRemove() {
        try {

            
            // all is empty
            checkEmpty(service);
            checkEmpty(store);
        
            insertValues(store);
            store.store();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            removeValues(store);
            if (!isJDBCTest())
                checkValuesInserted(service);
            checkValuesRemoved(store);
            
            store.load();
            checkValuesInserted(store);
            checkValuesInserted(service);
            
            removeValues(store);
            store.store();
            checkValuesRemoved(store);
            checkValuesRemoved(service);
            
                        
        } catch ( Exception ex) {
            Assert.fail(ex.getMessage());
        }        
    }

    public void testIsModified() {
        try {
                        
            assertFalse(store.isModified());
            
            insertValues(store);
            assertTrue(store.isModified());
            
            store.load();
            assertFalse(store.isModified());
            
            insertValues(store);
            store.store();
            assertFalse(store.isModified());
            
            GeoServerUser user = 
                store.createUserObject("uuuu", "",true);
            GeoServerUserGroup group = 
                store.createGroupObject("gggg", true);        

            
            assertFalse(store.isModified());
            
            // add,remove,update
            store.addUser(user);
            assertTrue(store.isModified());
            store.store();

            assertFalse(store.isModified());
            store.addGroup(group);
            assertTrue(store.isModified());
            store.store();
            
            assertFalse(store.isModified());
            store.updateUser(user);
            assertTrue(store.isModified());
            store.load();
            
            assertFalse(store.isModified());
            store.updateGroup(group);
            assertTrue(store.isModified());
            store.load();
            
            assertFalse(store.isModified());
            store.removeUser(user);
            assertTrue(store.isModified());
            store.load();

            assertFalse(store.isModified());
            store.removeGroup(group);
            assertTrue(store.isModified());
            store.load();
                        
            assertFalse(store.isModified());
            store.associateUserToGroup(user, group);
            assertTrue(store.isModified());
            store.store();

            assertFalse(store.isModified());
            store.disAssociateUserFromGroup(user, group);
            assertTrue(store.isModified());
            store.load();
            
            assertFalse(store.isModified());
            store.clear();
            assertTrue(store.isModified());
            store.load();

            
        } catch ( Exception ex) {
            Assert.fail(ex.getMessage());
        }        
    }

    public void testEmptyPassword() throws Exception {
        // all is empty
        checkEmpty(service);
        checkEmpty(store);

        GeoServerUser user = store.createUserObject("userNoPasswd", null, true);
        store.addUser(user);
        store.store();

        assertEquals(1, service.getUserCount());
        user = service.getUserByUsername("userNoPasswd");
        assertNull(user.getPassword());

        user = (GeoServerUser) service.loadUserByUsername("userNoPasswd");
        assertNull(user.getPassword());
    }

}
