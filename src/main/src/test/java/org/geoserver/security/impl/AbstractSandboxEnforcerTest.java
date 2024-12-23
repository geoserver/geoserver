/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.geoserver.security.impl.DefaultFileAccessManager.GEOSERVER_DATA_SANDBOX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.io.File;
import java.io.IOException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.impl.FileSandboxEnforcer.SandboxException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public abstract class AbstractSandboxEnforcerTest extends AbstractFileAccessTest {

    protected static final String ADMIN_STORE = "lakesAdmin";
    protected static final String CITE_STORE = "lakesCite";
    protected static final String CGF_STORE = "lakesCgf";
    protected static final String CDF_STORE = "lakesCdf";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // force creation of the FileSanboxEnforcer (beans are lazy loaded in tests, and this
        // one registers itself on the catalog on creation)
        GeoServerExtensions.bean(FileSandboxEnforcer.class, applicationContext);
    }

    @After
    public void clearFileAccessManagerConfiguration() {
        System.clearProperty(GEOSERVER_DATA_SANDBOX);
        Resource layerSecurity = getDataDirectory().get("security/layers.properties");
        layerSecurity.delete();
        fileAccessManager.reload();
    }

    @Before
    public void clearStores() throws Exception {
        Catalog catalog = getCatalog();
        for (StoreInfo ds : catalog.getStores(StoreInfo.class)) {
            String name = ds.getName();
            if (ADMIN_STORE.equals(name) || CITE_STORE.equals(name)) {
                catalog.remove(ds);
            }
        }
    }

    @Test
    public void testNoRestrictions() throws Exception {
        // a real test directory
        File testDirectory = new File("./target/test").getCanonicalFile();

        // now, an administrator should be able to create a store in the test directory
        loginAdmin();
        addStore(ADMIN_STORE, testDirectory);

        // a normal user should be able to do the same (from the test we have no admin restrictions)
        loginCite();
        addStore(CITE_STORE, testDirectory);
    }

    @Test
    public void testSystemSandbox() throws Exception {
        // setup a system sandbox
        File systemSandbox = new File("./target/systemSandbox").getCanonicalFile();
        systemSandbox.mkdirs();
        System.setProperty(GEOSERVER_DATA_SANDBOX, systemSandbox.getAbsolutePath());
        fileAccessManager.reload();

        // a real test directory
        File testDirectory = new File("./target/test").getCanonicalFile();

        //  an administrator should not be able to create a store in the test directory any longer
        loginAdmin();
        Catalog catalog = getCatalog();
        SandboxException se = assertThrows(SandboxException.class, () -> addStore(ADMIN_STORE, testDirectory));
        assertThat(
                se.getMessage(),
                allOf(
                        startsWith("Access to "),
                        containsString(testDirectory.getAbsolutePath()),
                        endsWith(" denied by file sandboxing")));

        // check the store really has not been created
        assertNull(catalog.getDataStoreByName(ADMIN_STORE));
    }

    @Test
    public void testWorkspaceAdminSandbox() throws Exception {
        configureCiteAccess();

        // force reloading definitions
        fileAccessManager.reload();

        // add as a workspace admin
        loginCite();
        addStore(CITE_STORE, citeFolder);
        assertThrows(SandboxException.class, () -> addStore(CGF_STORE, cgfFolder));
        assertThrows(SandboxException.class, () -> addStore(CDF_STORE, cdfFolder));

        // now try to escape the sandbox
        assertThrows(SandboxException.class, () -> modifyStore(CITE_STORE, cgfFolder));

        // check the above save did not work
        StoreInfo citeStore = getCatalog().getStoreByName(CITE_STORE, StoreInfo.class);
        testLocation(citeStore, citeFolder);
    }

    @Test
    public void testMultipleWorkspaceAdminSandbox() throws Exception {
        configureCiteCgfMissingAccess();

        // add as a workspace admin
        loginCiteCgfMissing();
        addStore(CITE_STORE, citeFolder);
        addStore(CGF_STORE, cgfFolder);
        assertThrows(SandboxException.class, () -> addStore(CDF_STORE, cdfFolder));
    }

    /** Checks that the location in the store is the expected one */
    protected abstract void testLocation(StoreInfo store, File location) throws Exception;

    /** Adds a store of the desired name and location */
    protected abstract void addStore(String storeName, File location) throws IOException;

    /** Modifies the store's location to the desired one */
    protected abstract void modifyStore(String storeName, File location);
}
