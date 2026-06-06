/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import org.geoserver.backuprestore.BackupRestoreTestSupport;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.security.impl.DefaultResourceAccessManager;
import org.junit.After;
import org.junit.Test;
import org.kordamp.json.JSONArray;
import org.kordamp.json.JSONObject;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.vfny.geoserver.global.ConfigurationException;

/** @author Alessio Fabiani, GeoSolutions */
public class RESTBackupTest extends BackupRestoreTestSupport {

    @Test
    public void testNewBackup() throws Exception {
        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json = "{\"backup\": {"
                + "   \"archiveFile\": \""
                + archiveFilePath
                + "\", "
                + "   \"overwrite\": true,"
                + "   \"options\": { \"option\": [\"BK_BEST_EFFORT=false\"] }"
                + "  }"
                + "}";

        JSONObject backup = postNewBackup(json);

        assertNotNull(backup);

        JSONObject execution =
                readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue("STARTED".equals(execution.getString("status")) || "STARTING".equals(execution.getString("status")));

        int cnt = 0;
        while (cnt < 100
                && ("STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
            cnt++;
        }

        assertEquals("COMPLETED", execution.getString("status"));
    }

    @Test
    public void testBackupWithProtectedReources() throws Exception {
        // setup security, AggregateGeoFeature allowed to everybody, PrimitiveGeoFeature to military
        // only
        Properties props = new Properties();
        props.load(new StringReader(String.join("\n", "PrimitiveGeoFeature.r=MILITARY", "AggregateGeoFeature.r=*")));
        DefaultResourceAccessManager manager = new DefaultResourceAccessManager(
                new MemoryDataAccessRuleDAO(createDataDirectoryMock(), catalog, props), catalog);

        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, manager) {

            @Override
            protected boolean isAdmin(Authentication authentication) {
                return false;
            }
        };
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);

        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json = "{\"backup\": {"
                + "   \"archiveFile\": \""
                + archiveFilePath
                + "\", "
                + "   \"overwrite\": true,"
                + "   \"options\": { \"option\": [\"BK_BEST_EFFORT=false\"] }"
                + "  }"
                + "}";

        JSONObject backup = postNewBackup(json);

        assertNotNull(backup);

        JSONObject execution =
                readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue("STARTED".equals(execution.getString("status")) || "STARTING".equals(execution.getString("status")));

        int cnt = 0;
        while (cnt < 100
                && ("STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
            cnt++;
        }

        assertEquals("COMPLETED", execution.getString("status"));
    }

    @Test
    public void testParameterizedBackup() throws Exception {
        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json = "{\"backup\": {"
                + "   \"archiveFile\": \""
                + archiveFilePath
                + "\", "
                + "   \"overwrite\": true,"
                + "   \"options\": { \"option\": [\"BK_PARAM_PASSWORDS=true\"] }"
                + "  }"
                + "}";

        JSONObject backup = postNewBackup(json);

        assertNotNull(backup);

        JSONObject execution =
                readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue("STARTED".equals(execution.getString("status")) || "STARTING".equals(execution.getString("status")));

        int cnt = 0;
        while (cnt < 100
                && ("STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
            cnt++;
        }

        assertEquals("COMPLETED", execution.getString("status"));

        String storeContent;
        try (ZipFile backupZip = new ZipFile(new File(archiveFilePath))) {
            ZipEntry entry = backupZip.getEntry("store.dat.1");
            // Read the entry while the ZipFile is still open: a prior try-with-resources closed the ZipFile (and thus
            // the entry stream) before the scanner ran, so it always read nothing.
            try (Scanner scanner =
                    new Scanner(backupZip.getInputStream(entry), StandardCharsets.UTF_8).useDelimiter("\\A")) {
                storeContent = scanner.hasNext() ? scanner.next() : "";
            }
        }
        // A parameterized backup (BK_PARAM_PASSWORDS=true) replaces store passwords with ${...} tokens written inside
        // <tokenizedPassword> elements (see BackupRestoreItem.createParameterizingMapConverter).
        assertTrue(
                "Expected the parameterized store output to contain a tokenized password, was:\n" + storeContent,
                storeContent.contains("tokenizedPassword"));
    }

    @Test
    public void testFilteredBackup() throws Exception {
        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json = "{\"backup\": {"
                + "   \"archiveFile\": \""
                + archiveFilePath
                + "\", "
                + "   \"overwrite\": true,"
                + "   \"options\": { \"option\": [\"BK_BEST_EFFORT=false\"] },"
                + "   \"wsFilter\": \"name IN ('topp','geosolutions-it')\""
                + "  }"
                + "}";

        JSONObject backup = postNewBackup(json);

        assertNotNull(backup);

        JSONObject execution =
                readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue("STARTED".equals(execution.getString("status")) || "STARTING".equals(execution.getString("status")));

        int cnt = 0;
        while (cnt < 100
                && ("STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
            cnt++;
        }

        assertEquals("COMPLETED", execution.getString("status"));

        // The wsFilter job parameter is propagated onto the step executions. Read the step list defensively: when it
        // has a single element Jettison serializes it as an object rather than a 1-element array.
        assertEquals(
                "name IN ('topp','geosolutions-it')",
                stepExecutions(execution).getJSONObject(0).getJSONObject("parameters").get("wsFilter"));
    }

    @Test
    public void testAbandonBackupDoesNotError() throws Exception {
        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json = "{\"backup\": {"
                + "   \"archiveFile\": \""
                + archiveFilePath
                + "\", "
                + "   \"overwrite\": true,"
                + "   \"options\": { \"option\": [\"BK_BEST_EFFORT=false\"] }"
                + "  }"
                + "}";

        JSONObject backup = postNewBackup(json);
        long id = backup.getJSONObject("execution").getLong("id");

        // Aborting an execution must not fail with a 500 even if the job is still running: Spring Batch cannot abandon
        // a running execution, so abandonExecution stops it first and then records it ABANDONED. Before the fix, DELETE
        // on a still-running execution surfaced JobExecutionAlreadyRunningException as HTTP 500.
        MockHttpServletResponse response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/br/backup/" + id);
        assertEquals(200, response.getStatus());
    }

    @After
    public void waitForRunningExecutions() throws InterruptedException {
        // A test that fails before its own wait loop can leave an async backup/restore execution running; the backup
        // facade refuses to start a new job while one is running, which would cascade-fail the following tests. Let any
        // in-flight execution finish so each test starts from a clean slate.
        int cnt = 0;
        while (cnt++ < 100
                && (!backupFacade.getBackupRunningExecutions().isEmpty()
                        || !backupFacade.getRestoreRunningExecutions().isEmpty())) {
            Thread.sleep(100);
        }
    }

    /**
     * Returns the {@code stepExecutions/step} list as a {@link JSONArray}, wrapping the single object Jettison emits
     * when the list has exactly one element (a 1-element collection is serialized as an object, not a 1-element array).
     */
    static JSONArray stepExecutions(JSONObject execution) {
        Object steps = execution.getJSONObject("stepExecutions").get("step");
        if (steps instanceof JSONArray) {
            return (JSONArray) steps;
        }
        JSONArray array = new JSONArray();
        array.add(steps);
        return array;
    }

    JSONObject postNewBackup(String body) throws Exception {
        MockHttpServletResponse resp = postAsServletResponse(
                RestBaseController.ROOT_PATH + "/br/backup", body, MediaType.APPLICATION_JSON_VALUE);

        assertEquals(201, resp.getStatus());
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);

        JSONObject execution = json.getJSONObject("backup");

        assertNotNull(execution);

        return execution;
    }

    JSONObject readExecutionStatus(long executionId) throws Exception {
        JSONObject json = (JSONObject) getAsJSON(RestBaseController.ROOT_PATH + "/br/backup/" + executionId + ".json");

        JSONObject backup = json.getJSONObject("backup");

        assertNotNull(backup);

        JSONObject execution = backup.getJSONObject("execution");

        assertNotNull(execution);

        return execution;
    }

    static class MemoryDataAccessRuleDAO extends DataAccessRuleDAO {

        public MemoryDataAccessRuleDAO(GeoServerDataDirectory dd, Catalog rawCatalog, Properties props)
                throws ConfigurationException, IOException {
            super(dd, rawCatalog);
            loadRules(props);
        }

        @Override
        protected void checkPropertyFile(boolean force) {
            // skip checking
            lastModified.set(Long.MAX_VALUE);
        }
    }
}
