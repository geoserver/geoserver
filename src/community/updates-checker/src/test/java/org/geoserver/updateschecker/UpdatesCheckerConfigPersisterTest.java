/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.updateschecker;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.nio.file.Files;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class UpdatesCheckerConfigPersisterTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private UpdatesCheckerConfigPersister persister() {
        return TestServices.persister(temp.getRoot());
    }

    @Test
    public void defaultsWhenNoFileExists() throws Exception {
        UpdatesCheckerConfig config = persister().getConfig();
        assertTrue("enabled by default", config.isEnabled());
        assertTrue("logging on by default", config.isLogUpdateChecks());
        assertFalse(config.isHideVersionNumbersInLogs());
    }

    @Test
    public void savesAndReloads() throws Exception {
        UpdatesCheckerConfigPersister persister = persister();

        UpdatesCheckerConfig config = persister.getConfig();
        // the opposite of every default, so the values read back can only have come from the saved file
        config.setEnabled(false);
        config.setLogUpdateChecks(false);
        config.setHideVersionNumbersInLogs(true);
        persister.save(config);

        // a second persister over the same data directory has no cache, so this reads the file
        UpdatesCheckerConfig reloaded = persister().getConfig();
        assertFalse(reloaded.isEnabled());
        assertFalse(reloaded.isLogUpdateChecks());
        assertTrue(reloaded.isHideVersionNumbersInLogs());
    }

    @Test
    public void aSaveThatCannotBeWrittenFailsWithARuntimeExceptionTheCallerCanHandle() throws Exception {
        // a regular file where the module's directory should be, so nothing can be created beneath it
        assertTrue(new File(temp.getRoot(), "updates-checker").createNewFile());
        UpdatesCheckerConfigPersister persister = persister();
        try {
            persister.save(new UpdatesCheckerConfig());
            org.junit.Assert.fail("the save should have failed");
        } catch (RuntimeException expected) {
            assertTrue(expected.getMessage().contains("Could not save"));
        }
        // reading never writes, and still works
        assertTrue(persister.getConfig().isEnabled());
    }

    @Test
    public void toleratesFieldsRemovedOrRenamedSinceTheFileWasWritten() throws Exception {
        // simulates an on-disk config written by an older version of this module, before
        // "logNewVersions" was renamed to "logUpdateChecks" - must not fail the whole read
        File configDir = temp.newFolder("updates-checker");
        File configFile = new File(configDir, "updates-checker.xml");
        Files.writeString(
                configFile.toPath(),
                "<updatesChecker>\n"
                        + "  <enabled>true</enabled>\n"
                        + "  <message>Old message</message>\n"
                        + "  <forceOutdatedForTesting>false</forceOutdatedForTesting>\n"
                        + "  <logNewVersions>true</logNewVersions>\n"
                        + "</updatesChecker>\n");

        UpdatesCheckerConfig config = persister().getConfig();
        assertTrue(config.isEnabled());
        // the removed fields (message, logNewVersions) are silently dropped rather than failing the whole read - the
        // new
        // setting it was replaced by simply starts at its default until explicitly saved again
        assertFalse(config.isLogUpdateChecks());
    }
}
