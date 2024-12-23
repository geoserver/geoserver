/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.geoserver.security.impl.DefaultFileAccessManager.GEOSERVER_DATA_SANDBOX;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Test;

public class DefaultFileAccessManagerTest extends AbstractFileAccessTest {

    @Test
    public void testNoRestrictions() throws Exception {
        // a real test directory
        File testDirectory = new File("./target/test").getCanonicalFile();

        // no restrictions for admin
        loginAdmin();
        assertNull(fileAccessManager.getAvailableRoots());
        assertTrue(fileAccessManager.checkAccess(testDirectory));

        // but also none for a workspace admin
        login("cite", "pwd", ROLE_CITE);
        assertNull(fileAccessManager.getAvailableRoots());
        assertTrue(fileAccessManager.checkAccess(testDirectory));
    }

    @Test
    public void testSystemSandbox() throws Exception {
        // setup a system sandbox
        File systemSandbox = new File("./target/systemSandbox").getCanonicalFile();
        System.setProperty(GEOSERVER_DATA_SANDBOX, systemSandbox.getAbsolutePath());
        fileAccessManager.reload();

        // a real test directory outside of the sandbox
        File testDirectory = new File("./target/test").getCanonicalFile();

        // the admin is sandboxed
        loginAdmin();
        assertEquals(fileAccessManager.getAvailableRoots(), List.of(systemSandbox));
        assertFalse(fileAccessManager.checkAccess(testDirectory));
        assertTrue(fileAccessManager.checkAccess(new File("./target/systemSandbox/test/a/b/c").getCanonicalFile()));
        // there is no escaping it
        assertFalse(fileAccessManager.checkAccess(new File("./target/systemSandbox/../a/b/c").getCanonicalFile()));
    }

    @Test
    public void testWorkspaceAdminSandbox() throws Exception {
        configureCiteAccess();

        login("cite", "pwd", ROLE_CITE);
        assertEquals(List.of(citeFolder), fileAccessManager.getAvailableRoots());
        assertTrue(fileAccessManager.checkAccess(citeFolder));
        assertFalse(fileAccessManager.checkAccess(cgfFolder));
        assertFalse(fileAccessManager.checkAccess(cdfFolder));
    }

    @Test
    public void testMultipleWorkspaceAdminSandbox() throws Exception {
        configureCiteCgfMissingAccess();

        loginCiteCgfMissing();
        // "missing" is not there as it does not exist (would confuse the file browser if
        // it was returned as a root, but wasn't actually there on the file system)
        assertThat(fileAccessManager.getAvailableRoots(), Matchers.hasItems(citeFolder, cgfFolder));
        assertTrue(fileAccessManager.checkAccess(citeFolder));
        assertTrue(fileAccessManager.checkAccess(cgfFolder));
        assertTrue(fileAccessManager.checkAccess(missingFolder)); // formally allowed
        assertFalse(fileAccessManager.checkAccess(cdfFolder));
    }
}
