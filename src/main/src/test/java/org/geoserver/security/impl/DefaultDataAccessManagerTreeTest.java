/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.geoserver.security.AccessMode;
import org.junit.Before;
import org.junit.Test;

/**
 * Tests parsing of the property file into a security tree, and the
 * functionality of the tree as well (building the tree by hand is tedious)
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class DefaultDataAccessManagerTreeTest extends AbstractAuthorizationTest {
    
    @Before
    public void setupCatalog() {
        populateCatalog();
    }

    private SecureTreeNode buildTree(String propertyFile) throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream(propertyFile));
        return new DefaultResourceAccessManager(new MemoryDataAccessRuleDAO(catalog, props), catalog).root;
    }

    @Test
    public void testWideOpen() throws Exception {
        SecureTreeNode root = buildTree("wideOpen.properties");
        assertEquals(0, root.children.size());
        // we have he "*" rules
        assertEquals(1, root.getAuthorizedRoles(AccessMode.READ).size());
        assertEquals(1, root.getAuthorizedRoles(AccessMode.WRITE).size());
        assertTrue(root.canAccess(anonymous, AccessMode.READ));
        assertTrue(root.canAccess(anonymous, AccessMode.WRITE));
    }

    @Test
    public void testLockedDown() throws Exception {
        SecureTreeNode root = buildTree("lockedDown.properties");
        assertEquals(0, root.children.size());
        final Set<String> readRoles = root.getAuthorizedRoles(AccessMode.READ);
        assertEquals(1, readRoles.size());
        assertTrue(readRoles.contains("WRITER"));
        final Set<String> writeRoles = root.getAuthorizedRoles(AccessMode.WRITE);
        assertEquals(1, writeRoles.size());
        assertTrue(writeRoles.contains("WRITER"));
        assertFalse(root.canAccess(anonymous, AccessMode.READ));
        assertFalse(root.canAccess(anonymous, AccessMode.WRITE));
        assertFalse(root.canAccess(roUser, AccessMode.READ));
        assertFalse(root.canAccess(roUser, AccessMode.WRITE));
        assertTrue(root.canAccess(rwUser, AccessMode.READ));
        assertTrue(root.canAccess(rwUser, AccessMode.WRITE));
    }

    @Test
    public void testPublicRead() throws Exception {
        SecureTreeNode root = buildTree("publicRead.properties");
        assertEquals(0, root.children.size());
        assertEquals(SecureTreeNode.EVERYBODY, root.getAuthorizedRoles(AccessMode.READ));
        final Set<String> writeRoles = root.getAuthorizedRoles(AccessMode.WRITE);
        assertEquals(1, writeRoles.size());
        assertTrue(writeRoles.contains("WRITER"));
        assertTrue(root.canAccess(anonymous, AccessMode.READ));
        assertFalse(root.canAccess(anonymous, AccessMode.WRITE));
        assertTrue(root.canAccess(roUser, AccessMode.READ));
        assertFalse(root.canAccess(roUser, AccessMode.WRITE));
        assertTrue(root.canAccess(rwUser, AccessMode.READ));
        assertTrue(root.canAccess(rwUser, AccessMode.WRITE));
    }

    @Test
    public void testComplex() throws Exception {
        SecureTreeNode root = buildTree("complex.properties");

        // first off, evaluate tree structure
        assertEquals(2, root.children.size());
        SecureTreeNode topp = root.getChild("topp");
        assertNotNull(topp);
        assertEquals(3, topp.children.size());
        SecureTreeNode states = topp.getChild("states");
        SecureTreeNode landmarks = topp.getChild("landmarks");
        SecureTreeNode bases = topp.getChild("bases");
        assertNotNull(states);
        assertNotNull(landmarks);
        assertNotNull(bases);

        // perform some checks with anonymous access
        assertFalse(root.canAccess(anonymous, AccessMode.READ));
        assertFalse(root.canAccess(anonymous, AccessMode.WRITE));
        assertTrue(topp.canAccess(anonymous, AccessMode.READ));
        assertFalse(states.canAccess(anonymous, AccessMode.READ));
        assertTrue(landmarks.canAccess(anonymous, AccessMode.READ));
        assertFalse(landmarks.canAccess(anonymous, AccessMode.WRITE));
        assertFalse(bases.canAccess(anonymous, AccessMode.READ));
        
        // perform some checks with read only access
        assertTrue(root.canAccess(roUser, AccessMode.READ));
        assertFalse(root.canAccess(roUser, AccessMode.WRITE));
        assertTrue(topp.canAccess(roUser, AccessMode.READ));
        assertTrue(states.canAccess(roUser, AccessMode.READ));
        assertTrue(landmarks.canAccess(roUser, AccessMode.READ));
        assertFalse(landmarks.canAccess(roUser, AccessMode.WRITE));
        assertFalse(bases.canAccess(roUser, AccessMode.READ));
        
        // perform some checks with read write access
        assertTrue(root.canAccess(rwUser, AccessMode.READ));
        assertFalse(root.canAccess(rwUser, AccessMode.WRITE));
        assertTrue(topp.canAccess(rwUser, AccessMode.READ));
        assertTrue(states.canAccess(rwUser, AccessMode.WRITE));
        assertTrue(landmarks.canAccess(rwUser, AccessMode.READ));
        assertTrue(landmarks.canAccess(rwUser, AccessMode.WRITE));
        assertFalse(bases.canAccess(rwUser, AccessMode.READ));
        
        // military access... just access the one layer, for the rest he's like anonymous
        assertFalse(root.canAccess(milUser, AccessMode.READ));
        assertFalse(root.canAccess(milUser, AccessMode.WRITE));
        assertTrue(topp.canAccess(milUser, AccessMode.READ));
        assertFalse(states.canAccess(milUser, AccessMode.WRITE));
        assertTrue(landmarks.canAccess(milUser, AccessMode.READ));
        assertFalse(landmarks.canAccess(milUser, AccessMode.WRITE));
        assertTrue(bases.canAccess(milUser, AccessMode.READ));
        assertTrue(bases.canAccess(milUser, AccessMode.WRITE));
    }
    
    @Test
    public void testNamedTreeAMilitaryOnly() throws Exception {
        SecureTreeNode root = buildTree("namedTreeAMilitaryOnly.properties");
        assertNamedTreeAMilitary(root);
    }
    
    private void assertNamedTreeAMilitary(SecureTreeNode root) {
        SecureTreeNode lgNode = root.getChild("namedTreeA");
        assertNotNull(lgNode);
        assertTrue(lgNode.isContainerLayerGroup());
        assertEquals(new HashSet<String>(Arrays.asList("states-id", "roads-id", "cities-id")), lgNode.getContainedCatalogIds());
    }
    
    @Test
    public void testContainerGroupBMilitaryOnly() throws Exception {
        SecureTreeNode root = buildTree("containerTreeGroupBMilitaryOnly.properties");
        assertContainerTreeBMilitaryOnly(root);
    }

    private void assertContainerTreeBMilitaryOnly(SecureTreeNode root) {
        SecureTreeNode lgNode = root.getChild("containerTreeB");
        assertNotNull(lgNode);
        assertTrue(lgNode.isContainerLayerGroup());
        assertEquals(new HashSet<String>(Arrays.asList("roads-id", "landmarks-id", "forests-id", "nestedContainerE-id")), lgNode.getContainedCatalogIds());
    }
    
    @Test
    public void testSingleGroupCMilitaryOnly() throws Exception {
        SecureTreeNode root = buildTree("singleGroupCMilitaryOnly.properties");
        SecureTreeNode lgNode = root.getChild("singleGroupC");
        assertNotNull(lgNode);
        assertFalse(lgNode.isContainerLayerGroup());
        assertNull(lgNode.getContainedCatalogIds());
    }
    
    @Test
    public void testWsContainerGroupDMilitaryOnly() throws Exception {
        SecureTreeNode root = buildTree("wsContainerGroupDMilitaryOnly.properties");
        SecureTreeNode lgNode = root.getChild("nurc").getChild("wsContainerD");
        assertNotNull(lgNode);
        assertTrue(lgNode.isContainerLayerGroup());
        assertEquals(Collections.singleton("arc.grid-id"), lgNode.getContainedCatalogIds());
    }
    
    @Test
    public void testBothABMilitaryOnly() throws Exception {
        SecureTreeNode root = buildTree("bothGroupABMilitaryOnly.properties");
        assertNamedTreeAMilitary(root);
        assertContainerTreeBMilitaryOnly(root);
    }

}
