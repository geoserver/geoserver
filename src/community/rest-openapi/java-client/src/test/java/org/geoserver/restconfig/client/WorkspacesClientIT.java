package org.geoserver.restconfig.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.security.SecureRandom;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.geoserver.openapi.model.catalog.WorkspaceInfo;
import org.geoserver.openapi.v1.model.WorkspaceSummary;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestName;
import org.mockito.internal.util.collections.Sets;

/** Integration test suite for {@link WorkspacesClient} */
public class WorkspacesClientIT {

    public @Rule IntegrationTestSupport support = new IntegrationTestSupport();

    private WorkspacesClient client;

    private WorkspaceInfo info;

    public @Rule TestName testName = new TestName();
    private SecureRandom rnd = new SecureRandom();

    public @Before void before() {
        Assume.assumeTrue(support.isAlive());
        this.client = support.client().workspaces();
        this.info = new WorkspaceInfo();
    }

    public @Test void testDefaultWorkspace() {
        Optional<WorkspaceSummary> defaultWorkspace = client.getDefaultWorkspace();
        assertTrue(defaultWorkspace.isPresent());
        assertNotNull(defaultWorkspace.get().getName());

        final String currentDefault = defaultWorkspace.get().getName();
        final String newDefault =
                client.findAllNames().stream()
                        .filter(name -> !currentDefault.equals(name))
                        .findFirst()
                        .get();

        client.setDeafultWorkspace(newDefault);

        defaultWorkspace = client.getDefaultWorkspace();
        assertTrue(defaultWorkspace.isPresent());
        assertEquals(newDefault, defaultWorkspace.get().getName());
    }

    public @Test void testFindAll() {
        Set<String> names = new HashSet<String>(client.findAllNames());
        Set<String> expectedNamespaces = Sets.newSet("cite", "topp");
        assertTrue(names.containsAll(expectedNamespaces));
    }

    public @Test void testFindByName() {
        assertTrue(client.findByName("default").isPresent());
        assertTrue(client.findByName("tiger").isPresent());
        assertFalse(client.findByName("non_exisging_ws_123").isPresent());
    }

    public @Test void testGet() {
        WorkspaceSummary wss = client.get("topp");
        assertNotNull(wss);
        assertEquals("topp", wss.getName());
        assertEquals(Boolean.FALSE, wss.getIsolated());
        assertNotNull(wss.getCoverageStores());
        assertNotNull(wss.getDataStores());
        assertNotNull(wss.getWmsStores());
    }

    public @Test void testCRUD() {
        String name = String.format("%s-%d", testName.getMethodName(), rnd.nextInt((int) 1e6));
        testCRUD(name);
    }

    public @Test void testCRUDWithUUID() {
        String name = UUID.randomUUID().toString();
        testCRUD(name);
    }

    public @Test void createIsolatedWorkspace() {
        String name = UUID.randomUUID().toString();
        WorkspaceInfo requestBody = new WorkspaceInfo().name(name).isolated(true);
        WorkspaceInfo created = client.create(requestBody);
        assertNotNull(created);
        assertEquals(name, created.getName());
        assertEquals(Boolean.TRUE, created.getIsolated());
    }

    private void testCRUD(String name) {
        client.create(info.name(name));
        assertTrue(client.findByName(name).isPresent());
        assertTrue(client.findAllNames().contains(name));

        WorkspaceSummary summary = client.get(name);
        assertNotNull(summary);
        assertEquals(name, summary.getName());
        assertNotNull(summary.getIsolated());
        assertFalse(summary.getIsolated().booleanValue());

        assertNotNull(summary.getCoverageStores());
        assertNotNull(summary.getDataStores());
        assertNotNull(summary.getWmsStores());

        client.update(name, client.toInfo(summary).isolated(true));
        assertTrue(client.get(name).getIsolated().booleanValue());

        client.delete(name);
        assertFalse(client.findAllNames().contains(name));
    }
}
