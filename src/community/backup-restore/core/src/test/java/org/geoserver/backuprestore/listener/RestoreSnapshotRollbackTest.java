/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.listener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import org.apache.commons.io.FileUtils;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Unit tests for the transactional-restore snapshot / rollback file mechanics
 * ({@link RestoreJobExecutionListener#snapshotDataDirectory(File)} and
 * {@link RestoreJobExecutionListener#rollbackDataDirectory(File, File)}).
 *
 * <p>Exercised against a hand-built data directory so they do not need a running restore job or the live GeoServer /
 * security singletons (the in-memory reload that follows the on-disk rollback is covered separately by the integration
 * path). The central guarantee asserted here is the one the brief calls out: after a rollback, every snapshotted
 * subtree / root file is byte-identical to its pre-restore state.
 */
public class RestoreSnapshotRollbackTest {

    @Rule
    public TemporaryFolder tmp = new TemporaryFolder();

    /** A round trip with no intervening changes must leave the data directory byte-identical. */
    @Test
    public void snapshotThenRollbackIsByteIdenticalWhenNothingChanged() throws Exception {
        File dataDir = newDataDir();
        String before = digest(dataDir);

        File snapshot = RestoreJobExecutionListener.snapshotDataDirectory(dataDir);
        try {
            RestoreJobExecutionListener.rollbackDataDirectory(dataDir, snapshot);
        } finally {
            FileUtils.deleteDirectory(snapshot);
        }

        assertEquals(
                "an unchanged data directory must survive a snapshot+rollback byte-identical", before, digest(dataDir));
    }

    /**
     * Mutations a restore would make to the tracked subtrees and root files — overwriting an existing file, adding a
     * new file inside a tracked subtree, deleting an existing file, and rewriting a root {@code *.xml} — must all be
     * undone by the rollback.
     */
    @Test
    public void rollbackUndoesInPlaceMutations() throws Exception {
        File dataDir = newDataDir();
        String before = digest(dataDir);

        File snapshot = RestoreJobExecutionListener.snapshotDataDirectory(dataDir);
        try {
            // Overwrite an existing tracked file.
            write(new File(dataDir, "workspaces/ws1/store.xml"), "MUTATED");
            // Add a brand-new file inside a tracked subtree (a restore would write extra workspaces/styles).
            write(new File(dataDir, "workspaces/intruder/intruder.xml"), "NEW");
            // Delete an existing tracked file.
            assertTrue(new File(dataDir, "styles/style.sld").delete());
            // Rewrite a root global xml.
            write(new File(dataDir, "global.xml"), "<global>changed</global>");

            // Sanity: the directory really did change.
            assertFalse("precondition: the mutations must change the digest", before.equals(digest(dataDir)));

            RestoreJobExecutionListener.rollbackDataDirectory(dataDir, snapshot);
        } finally {
            FileUtils.deleteDirectory(snapshot);
        }

        assertEquals("rollback must restore every tracked mutation byte-identically", before, digest(dataDir));
        assertFalse(
                "rollback must drop files the restore added inside a tracked subtree",
                new File(dataDir, "workspaces/intruder/intruder.xml").exists());
    }

    /** A whole subtree the restore creates from scratch (absent in the snapshot) must be removed by the rollback. */
    @Test
    public void rollbackRemovesSubtreesCreatedByTheRestore() throws Exception {
        File dataDir = newDataDir();
        // No gwc-layers in the pristine dir.
        assertFalse(new File(dataDir, "gwc-layers").exists());
        String before = digest(dataDir);

        File snapshot = RestoreJobExecutionListener.snapshotDataDirectory(dataDir);
        try {
            // The restore creates the whole gwc-layers tree.
            write(new File(dataDir, "gwc-layers/topp_states.xml"), "<layer/>");
            RestoreJobExecutionListener.rollbackDataDirectory(dataDir, snapshot);
        } finally {
            FileUtils.deleteDirectory(snapshot);
        }

        assertFalse(
                "rollback must remove a tracked subtree that did not exist before the restore",
                new File(dataDir, "gwc-layers").exists());
        assertEquals(before, digest(dataDir));
    }

    /** A root *.xml the restore introduces (absent in the snapshot) must be removed by the rollback. */
    @Test
    public void rollbackRemovesRootXmlCreatedByTheRestore() throws Exception {
        File dataDir = newDataDir();
        assertFalse(new File(dataDir, "wps.xml").exists());
        String before = digest(dataDir);

        File snapshot = RestoreJobExecutionListener.snapshotDataDirectory(dataDir);
        try {
            // The restore writes a brand-new root service descriptor.
            write(new File(dataDir, "wps.xml"), "<wps/>");
            RestoreJobExecutionListener.rollbackDataDirectory(dataDir, snapshot);
        } finally {
            FileUtils.deleteDirectory(snapshot);
        }

        assertFalse(
                "rollback must remove a root *.xml that did not exist before the restore",
                new File(dataDir, "wps.xml").exists());
        assertEquals(before, digest(dataDir));
    }

    /** A tracked subtree the restore wipes (e.g. on a purge restore) must be brought back by the rollback. */
    @Test
    public void rollbackRestoresSubtreesDeletedByTheRestore() throws Exception {
        File dataDir = newDataDir();
        String before = digest(dataDir);

        File snapshot = RestoreJobExecutionListener.snapshotDataDirectory(dataDir);
        try {
            FileUtils.deleteDirectory(new File(dataDir, "workspaces"));
            assertFalse(new File(dataDir, "workspaces").exists());
            RestoreJobExecutionListener.rollbackDataDirectory(dataDir, snapshot);
        } finally {
            FileUtils.deleteDirectory(snapshot);
        }

        assertTrue(
                "rollback must recreate a tracked subtree the restore deleted",
                new File(dataDir, "workspaces/ws1/store.xml").exists());
        assertEquals(before, digest(dataDir));
    }

    /** The snapshot must not capture untracked content (data stores, gwc tile blobs), so rollback leaves it alone. */
    @Test
    public void snapshotIgnoresUntrackedContent() throws Exception {
        File dataDir = newDataDir();
        // An untracked directory and file outside the snapshot set.
        write(new File(dataDir, "data/foo.shp"), "shapefile-bytes");

        File snapshot = RestoreJobExecutionListener.snapshotDataDirectory(dataDir);
        try {
            assertFalse("the snapshot must not copy untracked subtrees", new File(snapshot, "data").exists());
            // The rollback must not touch untracked content.
            String stamp = "still-here";
            write(new File(dataDir, "data/foo.shp"), stamp);
            RestoreJobExecutionListener.rollbackDataDirectory(dataDir, snapshot);
            assertEquals(stamp, read(new File(dataDir, "data/foo.shp")));
        } finally {
            FileUtils.deleteDirectory(snapshot);
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------------------------------------------------------

    /** Builds a minimal data dir with content in several tracked subtrees plus root xml files. */
    private File newDataDir() throws IOException {
        File dir = tmp.newFolder("datadir");
        write(new File(dir, "workspaces/default.xml"), "<workspace>ws1</workspace>");
        write(new File(dir, "workspaces/ws1/workspace.xml"), "<workspace><name>ws1</name></workspace>");
        write(new File(dir, "workspaces/ws1/store.xml"), "<dataStore><name>store</name></dataStore>");
        write(new File(dir, "styles/style.xml"), "<style><name>style</name></style>");
        write(new File(dir, "styles/style.sld"), "<sld/>");
        write(new File(dir, "layergroups/lg.xml"), "<layerGroup/>");
        write(new File(dir, "gwc/gwc-gs.xml"), "<gwcConfiguration/>");
        write(new File(dir, "security/users.properties"), "admin=secret");
        write(new File(dir, "security/masterpw/default/passwd"), "masterpw");
        write(new File(dir, "global.xml"), "<global/>");
        write(new File(dir, "logging.xml"), "<logging/>");
        return dir;
    }

    private static void write(File f, String content) throws IOException {
        f.getParentFile().mkdirs();
        Files.write(f.toPath(), content.getBytes(StandardCharsets.UTF_8));
    }

    private static String read(File f) throws IOException {
        return new String(Files.readAllBytes(f.toPath()), StandardCharsets.UTF_8);
    }

    /**
     * A stable, order-independent digest of every regular file under {@code dir}: relative path + length + content
     * hash, sorted. Two directories with the same digest are byte-identical in their file contents and layout.
     */
    private static String digest(File dir) throws IOException {
        StringBuilder sb = new StringBuilder();
        java.util.List<String> lines = new java.util.ArrayList<>();
        collect(dir, dir, lines);
        java.util.Collections.sort(lines);
        for (String l : lines) {
            sb.append(l).append('\n');
        }
        return sb.toString();
    }

    private static void collect(File root, File f, java.util.List<String> lines) throws IOException {
        File[] children = f.listFiles();
        if (children == null) {
            return;
        }
        for (File c : children) {
            if (c.isDirectory()) {
                collect(root, c, lines);
            } else {
                String rel = root.toPath().relativize(c.toPath()).toString().replace('\\', '/');
                byte[] bytes = Files.readAllBytes(c.toPath());
                lines.add(rel + "|" + bytes.length + "|" + Integer.toHexString(java.util.Arrays.hashCode(bytes)));
            }
        }
    }
}
