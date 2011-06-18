package org.geoserver.security.impl;

import junit.framework.TestCase;

import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.SecureTreeNode;
import org.springframework.security.authentication.TestingAuthenticationToken;

public class SecureTreeNodeTest extends TestCase {

    private TestingAuthenticationToken anonymous;

    @Override
    protected void setUp() throws Exception {
        anonymous = new TestingAuthenticationToken("anonymous", null);
    }

    public void testEmptyRoot() {
        SecureTreeNode root = new SecureTreeNode();

        // smoke tests
        assertNull(root.getChild("NotThere"));
        assertEquals(SecureTreeNode.EVERYBODY, root.getAuthorizedRoles(AccessMode.READ));
        assertEquals(SecureTreeNode.EVERYBODY, root.getAuthorizedRoles(AccessMode.WRITE));

        // empty, deepest node is itself
        SecureTreeNode node = root.getDeepestNode(new String[] { "a", "b" });
        assertSame(root, node);

        // allows access to everyone
        assertTrue(root.canAccess(anonymous, AccessMode.WRITE));
        assertTrue(root.canAccess(anonymous, AccessMode.READ));
        
        // make sure this includes not having a current user as well
        assertTrue(root.canAccess(null, AccessMode.WRITE));
        assertTrue(root.canAccess(null, AccessMode.READ));
    }
    
    
}
