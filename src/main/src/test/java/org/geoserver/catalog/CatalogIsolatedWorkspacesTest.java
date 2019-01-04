/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.geoserver.ows.LocalWorkspace;
import org.junit.Test;

/**
 * Contains tests related to isolated workspaces, this tests exercise catalog operations. An
 * workspace in GeoServer is composed of the workspace information and a namespace.
 */
public final class CatalogIsolatedWorkspacesTest extends IsolatedWorkspacesTest {

    @Test
    public void addWorkspacesWithSameNamespace() {
        // adding two times the same prefix and namespace should fail
        createWorkspace("test_a1", "https://www.test_a.com", false);
        executeAndValidateException(
                () -> createWorkspace("test_a1", "https://www.test_a.com", false),
                IllegalArgumentException.class,
                "Namespace with prefix 'test_a1' already exists");
        // adding a non isolated workspace with the same namespace but different prefix should fail
        executeAndValidateException(
                () -> createWorkspace("test_a2", "https://www.test_a.com", false),
                IllegalArgumentException.class,
                "Namespace with URI 'https://www.test_a.com' already exists.");
        // adding an isolated workspace with the same namespace but different prefix should succeed
        createWorkspace("test_a3", "https://www.test_a.com", true);
    }

    @Test
    public void retrievingNamespacesFromCatalog() {
        Catalog catalog = getCatalog();
        // create isolated workspace
        createWorkspace("test_b1", "https://www.test_b.com", true);
        checkNamespace(
                catalog.getNamespaceByPrefix("test_b1"), "test_b1", "https://www.test_b.com", true);
        assertThat(catalog.getNamespaceByURI("https://www.test_b.com"), nullValue());
        // create non isolated workspace
        createWorkspace("test_b2", "https://www.test_b.com", false);
        // get the workspaces information object
        checkWorkspace(catalog.getWorkspaceByName("test_b1"), "test_b1", true);
        checkWorkspace(catalog.getWorkspaceByName("test_b2"), "test_b2", false);
        // get namespaces by prefix
        checkNamespace(
                catalog.getNamespaceByPrefix("test_b1"), "test_b1", "https://www.test_b.com", true);
        checkNamespace(
                catalog.getNamespaceByPrefix("test_b2"),
                "test_b2",
                "https://www.test_b.com",
                false);
        // get namespaces by URI
        checkNamespace(
                catalog.getNamespaceByURI("https://www.test_b.com"),
                "test_b2",
                "https://www.test_b.com",
                false);
    }

    @Test
    public void retrievingNamespacesFromCatalogInVirtualService() {
        Catalog catalog = getCatalog();
        // create isolated workspace
        createWorkspace("test_c1", "https://www.test_c.com", true);
        createWorkspace("test_c2", "https://www.test_c.com", false);
        // get namespace by URI
        checkNamespace(
                catalog.getNamespaceByURI("https://www.test_c.com"),
                "test_c2",
                "https://www.test_c.com",
                false);
        // set isolated workspace as the local workspace
        WorkspaceInfo workspace = catalog.getWorkspaceByName("test_c1");
        assertThat(workspace, notNullValue());
        LocalWorkspace.set(workspace);
        // get the namespace by URI, we should get the isolated workspace one
        checkNamespace(
                catalog.getNamespaceByURI("https://www.test_c.com"),
                "test_c1",
                "https://www.test_c.com",
                true);
    }

    @Test
    public void updateWorkspacesIsolation() {
        // create two workspaces with the same namespace, both isolated
        createWorkspace("test_d1", "https://www.test_d.com", true);
        createWorkspace("test_d2", "https://www.test_d.com", true);
        // update the first workspace to a non isolated state, this should be successful
        updateWorkspaceIsolationState("test_d1", false);
        // try to make the second workspace non isolated, this should not be successful
        executeAndValidateException(
                () -> updateWorkspaceIsolationState("test_d2", false),
                IllegalArgumentException.class,
                "Namespace with URI 'https://www.test_d.com' already exists");
        // make the first workspace isolated again
        updateWorkspaceIsolationState("test_d1", true);
        // make the second workspace non isolated, this should be successful
        updateWorkspaceIsolationState("test_d2", false);
    }

    @Test
    public void retrieveLayersFromIsolatedWorkspace() {
        Catalog catalog = getCatalog();
        // create two workspaces with the same namespace, one of them is isolated
        createWorkspace("test_e1", "https://www.test_e.com", false);
        createWorkspace("test_e2", "https://www.test_e.com", true);
        WorkspaceInfo workspace1 = catalog.getWorkspaceByName("test_e1");
        NamespaceInfo namespace1 = catalog.getNamespaceByPrefix("test_e1");
        WorkspaceInfo workspace2 = catalog.getWorkspaceByName("test_e2");
        NamespaceInfo namespace2 = catalog.getNamespaceByPrefix("test_e2");
        // add a layer with the same name to both workspaces, layers have different content
        LayerInfo clonedLayer1 =
                cloneVectorLayerIntoWorkspace(workspace1, namespace1, "Lines", "layer_e");
        LayerInfo clonedLayer2 =
                cloneVectorLayerIntoWorkspace(workspace2, namespace2, "Points", "layer_e");
        assertThat(clonedLayer1.getId(), not(clonedLayer2.getId()));
        // retrieving the layer from the isolated workspace, should fail
        LayerInfo layer1 = catalog.getLayerByName("test_e1:layer_e");
        assertThat(layer1, notNullValue());
        assertThat(layer1.getId(), is(clonedLayer1.getId()));
        LayerInfo layer2 = catalog.getLayerByName("test_e3:layer_e");
        assertThat(layer2, nullValue());
    }
}
