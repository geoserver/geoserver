/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestException;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/** Unit tests for {@link SecuredResource}. */
public class SecuredResourceTest {

    @Rule
    public TemporaryFolder tmpDir = new TemporaryFolder();

    private Path root;

    private FileSystemResourceStore delegate;
    private SecureResourceStore store;

    private GeoServerSecurityManager securityManager;
    private WorkspaceAdminAuthorizer authorizer;
    private Authentication authentication;

    @Before
    public void setUp() throws IOException {
        root = tmpDir.getRoot().toPath();
        delegate = new FileSystemResourceStore(tmpDir.getRoot());

        // create directory structure
        tmpDir.newFolder("workspaces", "topp", "datastores");
        tmpDir.newFolder("workspaces", "sf");
        tmpDir.newFolder("styles");

        securityManager = mock(GeoServerSecurityManager.class);
        authorizer = mock(WorkspaceAdminAuthorizer.class);

        GeoServerExtensionsHelper.singleton("securityManager", securityManager, GeoServerSecurityManager.class);
        GeoServerExtensionsHelper.singleton("workspaceAdminAuthorizer", authorizer, WorkspaceAdminAuthorizer.class);

        // default: non-admin workspace admin user for "topp"
        authentication = new TestingAuthenticationToken("wsadmin", "password", "ROLE_USER");
        authentication.setAuthenticated(true);
        setAuthentication(authentication);

        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // "topp" is administrable
        WorkspaceAccessLimits toppLimits = mock(WorkspaceAccessLimits.class);
        when(toppLimits.isAdminable()).thenReturn(true);
        when(authorizer.getWorkspaceAccessLimits(authentication, "topp")).thenReturn(toppLimits);

        // "sf" is not administrable
        WorkspaceAccessLimits sfLimits = mock(WorkspaceAccessLimits.class);
        when(sfLimits.isAdminable()).thenReturn(false);
        when(authorizer.getWorkspaceAccessLimits(authentication, "sf")).thenReturn(sfLimits);

        store = new SecureResourceStore(delegate);
    }

    @After
    public void tearDown() {
        GeoServerExtensionsHelper.init(null);
        SecurityContextHolder.clearContext();
    }

    private void setAuthentication(Authentication auth) {
        SecurityContextImpl ctx = new SecurityContextImpl();
        ctx.setAuthentication(auth);
        SecurityContextHolder.setContext(ctx);
    }

    private SecuredResource securedResource(String path) {
        Resource resource = delegate.get(path);
        return new SecuredResource(resource, store);
    }

    // -- getType --

    @Test
    public void testGetType_readable() {
        SecuredResource resource = securedResource("workspaces/topp/datastores");
        assertEquals(Resource.Type.DIRECTORY, resource.getType());
    }

    @Test
    public void testGetType_notReadable() {
        SecuredResource resource = securedResource("workspaces/sf");
        assertEquals(Resource.Type.UNDEFINED, resource.getType());
    }

    // -- list --

    @Test
    public void testList() {
        SecuredResource resource = securedResource("workspaces/topp");
        List<Resource> children = resource.list();
        assertFalse(children.isEmpty());
        for (Resource child : children) {
            assertTrue(child instanceof SecuredResource);
        }
        assertTrue(children.stream().anyMatch(r -> "datastores".equals(r.name())));
    }

    @Test
    public void testList_notReadable() {
        SecuredResource resource = securedResource("workspaces/sf");
        assertTrue(resource.list().isEmpty());
    }

    // -- delete --

    @Test
    public void testDelete_writable() throws IOException {
        tmpDir.newFile("workspaces/topp/datastores/todelete.txt");
        SecuredResource resource = securedResource("workspaces/topp/datastores/todelete.txt");
        assertTrue(resource.delete());
        assertFalse(Files.isRegularFile(root.resolve("workspaces/topp/datastores/todelete.txt")));
    }

    @Test
    public void testDelete_notWritable() throws IOException {
        tmpDir.newFile("workspaces/sf/protected.txt");
        SecuredResource resource = securedResource("workspaces/sf/protected.txt");
        assertFalse(resource.delete());
        assertTrue(Files.isRegularFile(root.resolve("workspaces/sf/protected.txt")));
    }

    // -- renameTo --

    @Test
    public void testRenameTo_writable() throws IOException {
        tmpDir.newFile("workspaces/topp/datastores/old.txt");
        SecuredResource source = securedResource("workspaces/topp/datastores/old.txt");
        Resource target = delegate.get("workspaces/topp/datastores/new.txt");

        assertTrue(source.renameTo(target));
        assertFalse(Files.exists(root.resolve("workspaces/topp/datastores/old.txt")));
        assertTrue(Files.isRegularFile(root.resolve("workspaces/topp/datastores/new.txt")));
    }

    @Test
    public void testRenameTo_notWritable() throws IOException {
        tmpDir.newFile("workspaces/sf/old.txt");
        SecuredResource source = securedResource("workspaces/sf/old.txt");
        Resource target = delegate.get("workspaces/sf/new.txt");

        assertFalse(source.renameTo(target));
        assertTrue(Files.isRegularFile(root.resolve("workspaces/sf/old.txt")));
    }

    @Test
    public void testIn_readable() throws IOException {
        File f = tmpDir.newFile("workspaces/topp/datastores/data.txt");
        Files.writeString(f.toPath(), "hello");
        SecuredResource resource = securedResource("workspaces/topp/datastores/data.txt");

        try (InputStream in = resource.in()) {
            assertEquals("hello", new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    @Test(expected = RestException.class)
    public void testIn_notReadable() {
        securedResource("workspaces/sf/secret.txt").in();
    }

    @Test
    public void testOut_writable() throws IOException {
        SecuredResource resource = securedResource("workspaces/topp/datastores/output.txt");

        try (OutputStream out = resource.out()) {
            out.write("written".getBytes(StandardCharsets.UTF_8));
        }

        String content = Files.readString(root.resolve("workspaces/topp/datastores/output.txt"));
        assertEquals("written", content);
    }

    @Test(expected = RestException.class)
    public void testOut_notWritable() {
        securedResource("workspaces/sf/blocked.txt").out();
    }

    @Test
    public void testFile_writable() {
        SecuredResource resource = securedResource("workspaces/topp/datastores/test.txt");
        File file = resource.file();
        assertNotNull(file);
        assertEquals(root.resolve("workspaces/topp/datastores/test.txt").toFile(), file);
    }

    @Test(expected = RestException.class)
    public void testFile_notWritable() {
        securedResource("workspaces/sf/blocked.txt").file();
    }

    @Test
    public void testDir_writable() {
        SecuredResource resource = securedResource("workspaces/topp/datastores");
        File dir = resource.dir();
        assertNotNull(dir);
        assertEquals(root.resolve("workspaces/topp/datastores").toFile(), dir);
        assertTrue(dir.isDirectory());
    }

    @Test(expected = RestException.class)
    public void testDir_notWritable() {
        securedResource("workspaces/sf").dir();
    }

    @Test
    public void testLastModified() {
        assertTrue(securedResource("workspaces/topp/datastores").lastmodified() > 0);
    }

    @Test
    public void testParent() {
        Resource parent = securedResource("workspaces/topp/datastores").parent();
        assertNotNull(parent);
        assertTrue(parent instanceof SecuredResource);
        assertEquals("topp", parent.name());
    }

    @Test
    public void testGet() {
        Resource child = securedResource("workspaces/topp").get("datastores");
        assertNotNull(child);
        assertTrue(child instanceof SecuredResource);
        assertEquals("datastores", child.name());
    }
}
