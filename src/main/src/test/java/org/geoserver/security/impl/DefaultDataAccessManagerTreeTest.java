package org.geoserver.security.impl;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;

import java.util.Arrays;
import java.util.Properties;
import java.util.Set;

import junit.framework.TestCase;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DefaultDataAccessManager;
import org.geoserver.security.impl.SecureTreeNode;

/**
 * Tests parsing of the property file into a security tree, and the
 * functionality of the tree as well (building the tree by hand is tedious)
 * 
 * @author Andrea Aime - TOPP
 * 
 */
public class DefaultDataAccessManagerTreeTest extends TestCase {

    private Catalog catalog;

    private TestingAuthenticationToken rwUser;
    
    private TestingAuthenticationToken milUser;

    private TestingAuthenticationToken roUser;

    private TestingAuthenticationToken anonymous;

    @Override
    protected void setUp() throws Exception {
        catalog = createNiceMock(Catalog.class);
        expect(catalog.getWorkspace((String) anyObject())).andReturn(
                createNiceMock(WorkspaceInfo.class)).anyTimes();
        replay(catalog);

        rwUser = new TestingAuthenticationToken("rw", "supersecret", Arrays.asList(new GrantedAuthority[] {
                new GeoServerRole("READER"), new GeoServerRole("WRITER") }));
        roUser = new TestingAuthenticationToken("ro", "supersecret",
                Arrays.asList(new GrantedAuthority[] { new GeoServerRole("READER") }));
        anonymous = new TestingAuthenticationToken("anonymous", null);
        milUser = new TestingAuthenticationToken("military", "supersecret", Arrays.asList(new GrantedAuthority[] {
                new GeoServerRole("MILITARY") }));

    }

    private SecureTreeNode buildTree(String propertyFile) throws Exception {
        Properties props = new Properties();
        props.load(getClass().getResourceAsStream(propertyFile));
        return new DefaultDataAccessManager(new MemoryDataAccessRuleDAO(catalog, props)).root;
    }

    public void testWideOpen() throws Exception {
        SecureTreeNode root = buildTree("wideOpen.properties");
        assertEquals(0, root.children.size());
        // we have he "*" rules
        assertEquals(1, root.getAuthorizedRoles(AccessMode.READ).size());
        assertEquals(1, root.getAuthorizedRoles(AccessMode.WRITE).size());
        assertTrue(root.canAccess(anonymous, AccessMode.READ));
        assertTrue(root.canAccess(anonymous, AccessMode.WRITE));
    }

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
}
