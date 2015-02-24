/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;
import static org.junit.Assert.*;

import org.springframework.security.core.Authentication;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.DataAccessManager;
import org.junit.Test;


public class DefaultDataAccessManagerAuthTest extends AbstractAuthorizationTest {

    @Test 
    public void testWideOpen() throws Exception {
        DataAccessManager manager = buildLegacyAccessManager("wideOpen.properties");
        checkUserAccessFlat(manager, anonymous, true, true);
    }

    @Test
    public void testLockedDown() throws Exception {
        DataAccessManager manager = buildLegacyAccessManager("lockedDown.properties");
        checkUserAccessFlat(manager, anonymous, false, false);
        checkUserAccessFlat(manager, roUser, false, false);
        checkUserAccessFlat(manager, rwUser, true, true);
        checkUserAccessFlat(manager, root, true, true);
    }
    
    @Test
    public void testPublicRead() throws Exception {
        DataAccessManager manager = buildLegacyAccessManager("publicRead.properties");
        checkUserAccessFlat(manager, anonymous, true, false);
        checkUserAccessFlat(manager, roUser, true, false);
        checkUserAccessFlat(manager, rwUser, true, true);
        checkUserAccessFlat(manager, root, true, true);
    }
    
    private void checkUserAccessFlat(DataAccessManager manager, Authentication user, boolean expectedRead, boolean expectedWrite) {
        // states as a layer
        assertEquals(expectedRead, manager.canAccess(user, statesLayer, AccessMode.READ));
        assertEquals(expectedWrite, manager.canAccess(user, statesLayer, AccessMode.WRITE));
        // states as a resource
        final ResourceInfo resource = statesLayer.getResource();
        assertEquals(expectedRead, manager.canAccess(user, resource, AccessMode.READ));
        assertEquals(expectedWrite, manager.canAccess(user, resource, AccessMode.WRITE));
        // the topp ws
        assertEquals(expectedRead, manager.canAccess(user, toppWs, AccessMode.READ));
        assertEquals(expectedWrite, manager.canAccess(user, toppWs, AccessMode.WRITE));
    }
    
    @Test
    public void testComplex() throws Exception {
        DataAccessManager wo = buildLegacyAccessManager("complex.properties");
        
        // check non configured ws inherits root configuration, auth read, nobody write
        assertFalse(wo.canAccess(anonymous, nurcWs, AccessMode.READ));
        assertFalse(wo.canAccess(anonymous, nurcWs, AccessMode.WRITE));
        assertTrue(wo.canAccess(roUser, nurcWs, AccessMode.READ));
        assertFalse(wo.canAccess(rwUser, nurcWs, AccessMode.WRITE));
        assertTrue(wo.canAccess(root, nurcWs, AccessMode.WRITE));
        
        // check access to the topp workspace (everybody read, nobody for write)
        assertTrue(wo.canAccess(anonymous, toppWs, AccessMode.READ));
        assertFalse(wo.canAccess(anonymous, toppWs, AccessMode.WRITE));
        assertTrue(wo.canAccess(roUser, toppWs, AccessMode.READ));
        assertFalse(wo.canAccess(rwUser, toppWs, AccessMode.WRITE));
        
        // check non configured layer in topp ws inherits topp security attributes
        assertTrue(wo.canAccess(anonymous, roadsLayer, AccessMode.READ));
        assertFalse(wo.canAccess(anonymous, roadsLayer, AccessMode.WRITE));
        assertTrue(wo.canAccess(roUser, roadsLayer, AccessMode.READ));
        assertFalse(wo.canAccess(rwUser, roadsLayer, AccessMode.WRITE));
        
        // check states uses its own config (auth for read, auth for write)
        assertFalse(wo.canAccess(anonymous, statesLayer, AccessMode.READ));
        assertFalse(wo.canAccess(anonymous, statesLayer, AccessMode.WRITE));
        assertTrue(wo.canAccess(roUser, statesLayer, AccessMode.READ));
        assertFalse(wo.canAccess(roUser, statesLayer, AccessMode.WRITE));
        assertTrue(wo.canAccess(rwUser, statesLayer, AccessMode.WRITE));
        assertTrue(wo.canAccess(rwUser, statesLayer, AccessMode.WRITE));
        
        // check landmarks uses its own config (all can for read, auth for write)
        assertTrue(wo.canAccess(anonymous, landmarksLayer, AccessMode.READ));
        assertFalse(wo.canAccess(anonymous, landmarksLayer, AccessMode.WRITE));
        assertTrue(wo.canAccess(roUser, landmarksLayer, AccessMode.READ));
        assertFalse(wo.canAccess(roUser, landmarksLayer, AccessMode.WRITE));
        assertTrue(wo.canAccess(rwUser, landmarksLayer, AccessMode.READ));
        assertTrue(wo.canAccess(rwUser, statesLayer, AccessMode.WRITE));
        
        // check military is off limits for anyone but the military users
        assertFalse(wo.canAccess(anonymous, basesLayer, AccessMode.READ));
        assertFalse(wo.canAccess(anonymous, basesLayer, AccessMode.WRITE));
        assertFalse(wo.canAccess(roUser, basesLayer, AccessMode.READ));
        assertFalse(wo.canAccess(roUser, basesLayer, AccessMode.WRITE));
        assertFalse(wo.canAccess(rwUser, basesLayer, AccessMode.READ));
        assertFalse(wo.canAccess(rwUser, basesLayer, AccessMode.WRITE));
        assertTrue(wo.canAccess(milUser, basesLayer, AccessMode.READ));
        assertTrue(wo.canAccess(milUser, basesLayer, AccessMode.WRITE));
        
        // check the layer with dots
        assertFalse(wo.canAccess(anonymous, arcGridLayer, AccessMode.READ));
        assertFalse(wo.canAccess(anonymous, arcGridLayer, AccessMode.WRITE));
        assertFalse(wo.canAccess(roUser, arcGridLayer, AccessMode.READ));
        assertFalse(wo.canAccess(roUser, arcGridLayer, AccessMode.WRITE));
        assertFalse(wo.canAccess(rwUser, arcGridLayer, AccessMode.READ));
        assertFalse(wo.canAccess(rwUser, arcGridLayer, AccessMode.WRITE));
        assertTrue(wo.canAccess(milUser, arcGridLayer, AccessMode.READ));
        assertTrue(wo.canAccess(milUser, arcGridLayer, AccessMode.WRITE));
    }
    
    @Test
    public void testDefaultMode() throws Exception {
        DataAccessManager wo = buildLegacyAccessManager("lockedDown.properties");
        assertEquals(CatalogMode.HIDE, wo.getMode());
    }
    
    @Test
    public void testHideMode() throws Exception {
        DataAccessManager wo = buildLegacyAccessManager("lockedDownHide.properties");
        assertEquals(CatalogMode.HIDE, wo.getMode());
    }
    
    @Test
    public void testChallengeMode() throws Exception {
        DataAccessManager wo = buildLegacyAccessManager("lockedDownChallenge.properties");
        assertEquals(CatalogMode.CHALLENGE, wo.getMode());
    }
    
    @Test
    public void testMixedMode() throws Exception {
        DataAccessManager wo = buildLegacyAccessManager("lockedDownMixed.properties");
        assertEquals(CatalogMode.MIXED, wo.getMode());
    }
    
    @Test
    public void testUnknownMode() throws Exception {
        DataAccessManager wo = buildLegacyAccessManager("lockedDownUnknown.properties");
        // should fall back on the default and complain in the logger
        assertEquals(CatalogMode.HIDE, wo.getMode());
    }
    
}
