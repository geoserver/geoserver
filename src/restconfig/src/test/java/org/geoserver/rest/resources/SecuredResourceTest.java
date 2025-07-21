/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import org.geoserver.platform.resource.Resource;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link SecuredResource}. */
public class SecuredResourceTest {

    private SecuredResource securedResource;
    private Resource delegateResource;
    private SecureResourceStore store;

    @Before
    public void setUp() {
        // Mock dependencies
        delegateResource = mock(Resource.class);
        store = mock(SecureResourceStore.class);

        // Configure delegate resource
        when(delegateResource.path()).thenReturn("workspaces/topp/datastores");
        when(delegateResource.name()).thenReturn("datastores");

        // Create the secured resource
        securedResource = new SecuredResource(delegateResource, store);
    }

    @Test
    public void testGetType() {
        // Setup store to return type
        when(store.getType(securedResource)).thenReturn(Resource.Type.DIRECTORY);

        // Should delegate to store for getting type
        assertEquals(Resource.Type.DIRECTORY, securedResource.getType());
        verify(store).getType(securedResource);
    }

    @Test
    public void testListInternal() {
        // Create mock child resources
        SecuredResource child1 = new SecuredResource(mock(Resource.class), store);
        SecuredResource child2 = new SecuredResource(mock(Resource.class), store);
        List<Resource> children = List.of(child1, child2);

        // Setup store to return children
        when(store.list("workspaces/topp/datastores")).thenReturn(children);
        when(store.wrap(child1)).thenReturn(child1);
        when(store.wrap(child2)).thenReturn(child2);

        List<Resource> result = securedResource.list();
        assertEquals(children, result);
        verify(store).list("workspaces/topp/datastores");
        verify(store).wrap(child1);
        verify(store).wrap(child2);
    }

    @Test
    public void testDelete() {
        // Setup store to allow deletion
        when(store.remove("workspaces/topp/datastores")).thenReturn(true);

        // Should delegate to store for deletion
        assertTrue(securedResource.delete());
        verify(store).remove("workspaces/topp/datastores");

        // Setup store to deny deletion
        when(store.remove("workspaces/topp/datastores")).thenReturn(false);

        // Should return false when deletion is denied
        assertFalse(securedResource.delete());
    }

    @Test
    public void testRenameTo() {
        // Create a target resource
        Resource target = mock(Resource.class);
        when(target.path()).thenReturn("workspaces/topp/newname");

        // Setup store to allow move
        when(store.move("workspaces/topp/datastores", "workspaces/topp/newname"))
                .thenReturn(true);

        // Should delegate to store for move
        assertTrue(securedResource.renameTo(target));
        verify(store).move("workspaces/topp/datastores", "workspaces/topp/newname");

        // Setup store to deny move
        when(store.move("workspaces/topp/datastores", "workspaces/topp/newname"))
                .thenReturn(false);

        // Should return false when move is denied
        assertFalse(securedResource.renameTo(target));
    }

    @Test
    public void testIn() throws IOException {
        // Create an input stream
        ByteArrayInputStream bis = new ByteArrayInputStream("test data".getBytes());

        // Setup store to return the input stream
        when(store.in(securedResource)).thenReturn(bis);

        // Should delegate to store for getting input stream
        try (InputStream result = securedResource.in()) {
            assertSame(bis, result);
            verify(store).in(securedResource);
        }
    }

    @Test
    public void testOut() throws IOException {
        // Create an output stream
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        // Setup store to return the output stream
        when(store.out(securedResource)).thenReturn(bos);

        // Should delegate to store for getting output stream
        try (OutputStream result = securedResource.out()) {
            assertSame(bos, result);
            verify(store).out(securedResource);
        }
    }

    @Test
    public void testFile() {
        // Create a file
        File file = new File("/tmp/test");

        // Setup delegate to return the file
        when(delegateResource.file()).thenReturn(file);

        // Should delegate directly to the delegate resource
        assertEquals(file, securedResource.file());
        verify(delegateResource).file();
    }

    @Test
    public void testDir() {
        // Create a directory
        File dir = new File("/tmp/testdir");

        // Setup delegate to return the directory
        when(delegateResource.dir()).thenReturn(dir);

        // Should delegate directly to the delegate resource
        assertEquals(dir, securedResource.dir());
        verify(delegateResource).dir();
    }

    @Test
    public void testLastModified() {
        // Setup delegate to return a timestamp
        when(delegateResource.lastmodified()).thenReturn(12345L);

        // Should delegate directly to the delegate resource
        assertEquals(12345L, securedResource.lastmodified());
        verify(delegateResource).lastmodified();
    }

    @Test
    public void testParent() {
        // Create a parent resource
        Resource parent = mock(Resource.class);

        // Setup delegate to return the parent
        when(delegateResource.parent()).thenReturn(parent);

        // Setup store to wrap resources
        when(store.wrap(parent)).thenReturn(new SecuredResource(parent, store));

        // Should get the parent from delegate but wrap it as a SecuredResource
        Resource result = securedResource.parent();
        assertNotNull(result);
        assertTrue(result instanceof SecuredResource);
    }

    @Test
    public void testGet() {
        // Create a child resource
        Resource child = mock(Resource.class);

        // Setup delegate to return the child
        when(delegateResource.get("child")).thenReturn(child);

        // Setup store to wrap resources
        when(store.wrap(child)).thenReturn(new SecuredResource(child, store));

        // Should get the child from delegate but wrap it as a SecuredResource
        Resource result = securedResource.get("child");
        assertNotNull(result);
        assertTrue(result instanceof SecuredResource);
    }
}
