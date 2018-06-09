/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.ows.LocalWorkspace;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.hamcrest.Matchers;
import org.junit.Before;

/** Contains utility methods useful for testing isolated workspaces. */
public abstract class IsolatedWorkspacesTest extends GeoServerSystemTestSupport {

    // store the name of the workspaces created during a test execution
    protected final List<String> CREATED_WORKSPACES_PREFIXES = new ArrayList<>();

    @Before
    public void beforeTest() {
        // delete create workspaces
        CREATED_WORKSPACES_PREFIXES.forEach(this::removeWorkspace);
        // make sure that no local workspace is set
        LocalWorkspace.set(null);
    }

    /**
     * Helper method that updates the isolation state of an workspace and the corresponding
     * namespace.
     */
    protected void updateWorkspaceIsolationState(String prefix, boolean isolated) {
        Catalog catalog = getCatalog();
        // set the workspace isolation state using the provided one
        WorkspaceInfo workspace = catalog.getWorkspaceByName(prefix);
        workspace.setIsolated(isolated);
        catalog.save(workspace);
        // set the namespace isolation state using the provided one
        NamespaceInfo namespace = catalog.getNamespaceByPrefix(prefix);
        namespace.setIsolated(isolated);
        catalog.save(namespace);
    }

    /** Helper method that checks that the provided workspace has the expected content. */
    protected void checkWorkspace(
            WorkspaceInfo workspace, String expectedPrefix, boolean expectedIsolation) {
        assertThat(workspace, notNullValue());
        assertThat(workspace.getName(), is(expectedPrefix));
        assertThat(workspace.isIsolated(), is(expectedIsolation));
    }

    /** Helper method that checks that the provided namespace has the expected content. */
    protected void checkNamespace(
            NamespaceInfo namespace,
            String expectedPrefix,
            String expectedNamespaceUri,
            boolean expectedIsolation) {
        assertThat(namespace, notNullValue());
        assertThat(namespace.getPrefix(), is(expectedPrefix));
        assertThat(namespace.getName(), is(expectedPrefix));
        assertThat(namespace.getURI(), is(expectedNamespaceUri));
        assertThat(namespace.isIsolated(), is(expectedIsolation));
    }

    /**
     * Helper functional interface to allow passing functions that don't receive anything as input
     * and don't provide anything as output.
     */
    @FunctionalInterface
    protected interface Statement {

        void execute();
    }

    /**
     * Helper method that executes a statement where an exception of a certain type is expected to
     * happen. This method will check hat the obtained exception contains the expected message and
     * is an instance expected type.
     */
    protected void executeAndValidateException(
            Statement statement, Class<?> expectedException, String expectedMessage) {
        boolean exceptionHappen = false;
        try {
            // execute the statement
            statement.execute();
        } catch (Exception exception) {
            // check that obtained exception matches the expectations
            assertThat(exception, instanceOf(expectedException));
            assertThat(exception.getMessage(), Matchers.containsString(expectedMessage));
            exceptionHappen = true;
        }
        // check that an exception actually happen
        assertThat(exceptionHappen, is(true));
    }

    /**
     * Helper method that creates a workspace and add it to the catalog. This method will first
     * create the namespace and then the workspace. The create workspaces prefixes are stored in
     * {@link #CREATED_WORKSPACES_PREFIXES}.
     */
    protected void createWorkspace(String prefix, String namespaceUri, boolean isolated) {
        Catalog catalog = getCatalog();
        // create the namespace
        NamespaceInfoImpl namespace = new NamespaceInfoImpl();
        namespace.setPrefix(prefix);
        namespace.setURI(namespaceUri);
        namespace.setIsolated(isolated);
        catalog.add(namespace);
        // create the workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName(prefix);
        workspace.setIsolated(isolated);
        catalog.add(workspace);
        // store created workspace
        CREATED_WORKSPACES_PREFIXES.add(prefix);
    }
}
