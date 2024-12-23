/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.geoserver.security.impl.DefaultFileAccessManager.GEOSERVER_DATA_SANDBOX;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Properties;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Resource;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;

public class AbstractFileAccessTest extends GeoServerSystemTestSupport {
    private static final String MISSING = "missing";
    protected static final String ROLE_CITE = "role_" + MockData.CITE_PREFIX;
    protected static final String ROLE_CGF = "role_" + MockData.CGF_PREFIX;
    protected static final String ROLE_CDF = "role_" + MockData.CDF_PREFIX;
    protected static final String ROLE_MISSING = "role_" + MISSING;
    protected File sandbox;
    protected File citeFolder;
    protected File cgfFolder;
    protected File cdfFolder;
    protected File missingFolder;
    protected DefaultFileAccessManager fileAccessManager;

    @Before
    public void lookupFileAccessManager() {
        fileAccessManager = GeoServerExtensions.bean(DefaultFileAccessManager.class, applicationContext);
    }

    @Before
    public void cleanupRestrictions() throws Exception {
        // clean up the security restrictions
        GeoServerDataDirectory dd = getDataDirectory();
        Resource layerSecurity = dd.get("security/layers.properties");
        Properties properties = new Properties();
        properties.put("*.*.r", "*");
        properties.put("*.*.w", "*");
        try (OutputStream os = layerSecurity.out()) {
            properties.store(os, "everyone can read and write");
        }

        // clear the system sandbox
        System.clearProperty(GEOSERVER_DATA_SANDBOX);

        // force reloading definitions
        DefaultFileAccessManager fam = GeoServerExtensions.bean(DefaultFileAccessManager.class, applicationContext);
        fam.reload();
    }

    @Before
    public void setupDirectories() throws IOException {
        sandbox = new File("./target/sandbox").getCanonicalFile();
        citeFolder = new File(sandbox, MockData.CITE_PREFIX);
        cgfFolder = new File(sandbox, MockData.CGF_PREFIX);
        cdfFolder = new File(sandbox, MockData.CDF_PREFIX);
        missingFolder = new File(sandbox, MISSING);
        if (!citeFolder.exists()) assertTrue(citeFolder.mkdirs());
        if (!cgfFolder.exists()) assertTrue(cgfFolder.mkdirs());
        if (!cdfFolder.exists()) assertTrue(cdfFolder.mkdirs());
    }

    @Before
    @Override
    public void logout() {
        super.logout();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // configure a workspace for the "missing" directory (that's not going to be created)
        Catalog catalog = getCatalog();
        WorkspaceInfo missingWs = catalog.getFactory().createWorkspace();
        missingWs.setName(MISSING);
        catalog.add(missingWs);
        NamespaceInfo missingNs = catalog.getFactory().createNamespace();
        missingNs.setPrefix(MISSING);
        missingNs.setURI("http://www.geoserver.org/" + MISSING);
        catalog.add(missingNs);
    }

    protected void configureCiteCgfMissingAccess() throws IOException {
        // setup a workspace sandbox
        Resource layerSecurity = getDataDirectory().get("security/layers.properties");
        Properties properties = new Properties();
        properties.put("filesystemSandbox", sandbox.getAbsolutePath());
        properties.put("cite.*.a", ROLE_CGF);
        properties.put("cgf.*.a", ROLE_CGF);
        properties.put("missing.*.a", ROLE_MISSING);
        try (OutputStream os = layerSecurity.out()) {
            properties.store(os, "sandbox");
            os.flush();
        }

        // force reloading definitions
        fileAccessManager.reload();
    }

    protected void configureCiteAccess() throws IOException {
        // setup a workspace sandbox
        Resource layerSecurity = getDataDirectory().get("security/layers.properties");
        Properties properties = new Properties();
        properties.put("filesystemSandbox", sandbox.getAbsolutePath());
        properties.put("cite.*.a", ROLE_CITE);
        try (OutputStream os = layerSecurity.out()) {
            properties.store(os, "sandbox");
            os.flush();
        }

        // force reloading definitions
        fileAccessManager.reload();
    }

    /** Login as a user with ROLE_CITE and ROLE_CGF */
    protected void loginCiteCgfMissing() {
        login("cite", "pwd", ROLE_CITE, ROLE_CGF, ROLE_MISSING);
    }

    /** Login as a full administrator */
    protected void loginAdmin() {
        login("admin", "geoserver", GeoServerRole.ADMIN_ROLE.getAuthority());
    }

    /** Login with ROLE_CITE */
    protected void loginCite() {
        login("cite", "pwd", ROLE_CITE);
    }
}
