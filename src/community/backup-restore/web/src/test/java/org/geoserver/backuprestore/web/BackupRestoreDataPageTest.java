/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.util.factory.Hints;
import org.junit.Test;

/**
 * Render smoke test for {@link BackupRestoreDataPage}.
 *
 * <p>The page wires every option checkbox in Java and declares the matching {@code wicket:id} in the HTML; a mismatch
 * (a component without markup, or vice versa) only surfaces at render time. This test renders the page and asserts the
 * full set of option checkboxes is present, and that the default-{@code true} options (skip security / settings /
 * purge) are pre-checked, guarding both the markup pairing and the documented default states.
 */
public class BackupRestoreDataPageTest extends GeoServerWicketTestSupport {

    @Test
    public void testPageRendersWithAllOptionCheckboxes() {
        login();
        tester.startPage(BackupRestoreDataPage.class);
        tester.assertRenderedPage(BackupRestoreDataPage.class);

        // Backup options
        tester.assertComponent("backupForm:backupOptOverwirte", CheckBox.class);
        tester.assertComponent("backupForm:backupOptBestEffort", CheckBox.class);
        tester.assertComponent("backupForm:backupOptCleanTemp", CheckBox.class);
        tester.assertComponent("backupForm:backupOptSkipGWC", CheckBox.class);
        tester.assertComponent("backupForm:backupOptParamPasswords", CheckBox.class);
        tester.assertComponent("backupForm:backupOptPreserveIds", CheckBox.class);
        tester.assertComponent("backupForm:backupOptSkipSecurity", CheckBox.class);
        tester.assertComponent("backupForm:backupOptSkipSettings", CheckBox.class);

        // Restore options
        tester.assertComponent("restoreForm:restoreOptDryRun", CheckBox.class);
        tester.assertComponent("restoreForm:restoreOptBestEffort", CheckBox.class);
        tester.assertComponent("restoreForm:restoreOptCleanTemp", CheckBox.class);
        tester.assertComponent("restoreForm:restoreOptSkipGWC", CheckBox.class);
        tester.assertComponent("restoreForm:restoreOptSkipSecurity", CheckBox.class);
        tester.assertComponent("restoreForm:restoreOptSkipSettings", CheckBox.class);
        tester.assertComponent("restoreForm:restoreOptPurgeResources", CheckBox.class);
    }

    @Test
    public void testDefaultTrueOptionsArePreChecked() {
        login();
        tester.startPage(BackupRestoreDataPage.class);
        tester.assertRenderedPage(BackupRestoreDataPage.class);

        // These mirror the documented REST defaults (BK_SKIP_SECURITY / BK_SKIP_SETTINGS / BK_PURGE_RESOURCES = true).
        assertChecked("backupForm:backupOptSkipSecurity");
        assertChecked("backupForm:backupOptSkipSettings");
        assertChecked("restoreForm:restoreOptSkipSecurity");
        assertChecked("restoreForm:restoreOptSkipSettings");
        assertChecked("restoreForm:restoreOptPurgeResources");
    }

    private void assertChecked(String path) {
        CheckBox cb = (CheckBox) tester.getComponentFromLastRenderedPage(path);
        assertEquals("Expected " + path + " to be checked by default", Boolean.TRUE, cb.getModelObject());
    }

    @Test
    public void testInlineGuidanceIsRendered() {
        login();
        tester.startPage(BackupRestoreDataPage.class);
        tester.assertRenderedPage(BackupRestoreDataPage.class);

        String html = tester.getLastResponseAsString();
        // Intro paragraph
        assertTrue(
                "Expected the intro guidance paragraph to be rendered",
                html.contains("saves or reloads the GeoServer catalog and configuration"));
        // A representative per-option tooltip (rendered as a title attribute via wicket:message)
        assertTrue(
                "Expected the Preserve Catalog IDs tooltip to be rendered",
                html.contains("migrate the archive into another"));
        assertTrue(
                "Expected the Dry-Run tooltip to be rendered",
                html.contains("Validate the archive and report what would happen"));
    }

    /**
     * Locks the submit-time option marshalling that previously crashed: the default-true skip flags are passed as an
     * explicit {@code Boolean.toString(...)} value, which a plain {@link Hints.OptionKey} rejects with an
     * {@link IllegalArgumentException}. {@link BackupRestoreDataPage#buildBackupHints} must construct the key with the
     * {@code "*"} wildcard. Exercising both {@code true} and {@code false} guards against a regression on either path.
     */
    @Test
    public void testBackupOptionHintsAcceptExplicitBooleans() {
        Hints allOn = BackupRestoreDataPage.buildBackupHints(true, true, true, true, true, true, true);
        assertNotNull(allOn);
        assertFalse(allOn.isEmpty());

        // Even with every box unchecked, the default-true skip flags are still emitted (as explicit "false").
        Hints allOff = BackupRestoreDataPage.buildBackupHints(false, false, false, false, false, false, false);
        assertNotNull(allOff);
        assertFalse(allOff.isEmpty());
    }

    /** Restore counterpart of {@link #testBackupOptionHintsAcceptExplicitBooleans} (adds the purge-resources flag). */
    @Test
    public void testRestoreOptionHintsAcceptExplicitBooleans() {
        Hints allOn = BackupRestoreDataPage.buildRestoreHints(true, true, true, true, true, true, true);
        assertNotNull(allOn);
        assertFalse(allOn.isEmpty());

        Hints allOff = BackupRestoreDataPage.buildRestoreHints(false, false, false, false, false, false, false);
        assertNotNull(allOff);
        assertFalse(allOff.isEmpty());
    }
}
