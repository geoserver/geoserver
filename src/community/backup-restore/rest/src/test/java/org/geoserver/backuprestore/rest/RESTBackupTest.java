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
import java.util.Properties;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import net.sf.json.JSONObject;
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
import org.junit.Test;
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

        String json =
                "{\"backup\": {"
                        + "   \"archiveFile\": \""
                        + archiveFilePath
                        + "\", "
                        + "   \"overwrite\": true,"
                        + "   \"options\": { \"option\": [\"BK_BEST_EFFORT=false\"] }"
                        + "  }"
                        + "}";

        JSONObject backup = postNewBackup(json);

        assertNotNull(backup);

        JSONObject execution = readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue(
                "STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")));

        int cnt = 0;
        while (cnt < 100
                && ("STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
            cnt++;
        }

        assertTrue("COMPLETED".equals(execution.getString("status")));
    }

    @Test
    public void testBackupWithProtectedReources() throws Exception {
        // setup security, AggregateGeoFeature allowed to everybody, PrimitiveGeoFeature to military
        // only
        Properties props = new Properties();
        props.load(
                new StringReader(
                        Stream.of("PrimitiveGeoFeature.r=MILITARY", "AggregateGeoFeature.r=*")
                                .collect(Collectors.joining("\n"))));
        DefaultResourceAccessManager manager =
                new DefaultResourceAccessManager(
                        new MemoryDataAccessRuleDAO(createDataDirectoryMock(), catalog, props),
                        catalog);

        SecureCatalogImpl sc =
                new SecureCatalogImpl(catalog, manager) {

                    @Override
                    protected boolean isAdmin(Authentication authentication) {
                        return false;
                    }
                };
        GeoServerExtensionsHelper.singleton("secureCatalog", sc, SecureCatalogImpl.class);

        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json =
                "{\"backup\": {"
                        + "   \"archiveFile\": \""
                        + archiveFilePath
                        + "\", "
                        + "   \"overwrite\": true,"
                        + "   \"options\": { \"option\": [\"BK_BEST_EFFORT=false\"] }"
                        + "  }"
                        + "}";

        JSONObject backup = postNewBackup(json);

        assertNotNull(backup);

        JSONObject execution = readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue(
                "STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")));

        int cnt = 0;
        while (cnt < 100
                && ("STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
            cnt++;
        }

        assertTrue("COMPLETED".equals(execution.getString("status")));
    }

    @Test
    public void testParameterizedBackup() throws Exception {
        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json =
                "{\"backup\": {"
                        + "   \"archiveFile\": \""
                        + archiveFilePath
                        + "\", "
                        + "   \"overwrite\": true,"
                        + "   \"options\": { \"option\": [\"BK_PARAM_PASSWORDS=true\"] }"
                        + "  }"
                        + "}";

        JSONObject backup = postNewBackup(json);

        assertNotNull(backup);

        JSONObject execution = readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue(
                "STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")));

        int cnt = 0;
        while (cnt < 100
                && ("STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
            cnt++;
        }

        assertTrue("COMPLETED".equals(execution.getString("status")));

        ZipFile backupZip = new ZipFile(new File(archiveFilePath));
        ZipEntry entry = backupZip.getEntry("store.dat.1");

        Scanner scanner = new Scanner(backupZip.getInputStream(entry), "UTF-8");
        boolean hasExpectedValue = false;
        while (scanner.hasNextLine() && !hasExpectedValue) {
            String line = scanner.next();
            hasExpectedValue = line.contains("encryptedValue");
        }
        assertTrue("Expected the store output to contain tokenized password", hasExpectedValue);
    }

    @Test
    public void testFilteredBackup() throws Exception {
        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json =
                "{\"backup\": {"
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

        JSONObject execution = readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue(
                execution
                        .getJSONObject("stepExecutions")
                        .getJSONArray("step")
                        .getJSONObject(0)
                        .getJSONObject("parameters")
                        .get("wsFilter")
                        .equals("name IN ('topp','geosolutions-it')"));

        assertTrue(
                "STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")));

        int cnt = 0;
        while (cnt < 100
                && ("STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
            cnt++;
        }

        assertTrue("COMPLETED".equals(execution.getString("status")));
    }

    JSONObject postNewBackup(String body) throws Exception {
        MockHttpServletResponse resp =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/br/backup",
                        body,
                        MediaType.APPLICATION_JSON_VALUE);

        assertEquals(201, resp.getStatus());
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);

        JSONObject execution = json.getJSONObject("backup");

        assertNotNull(execution);

        return execution;
    }

    JSONObject readExecutionStatus(long executionId) throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/br/backup/"
                                        + executionId
                                        + ".json");

        JSONObject backup = json.getJSONObject("backup");

        assertNotNull(backup);

        JSONObject execution = backup.getJSONObject("execution");

        assertNotNull(execution);

        return execution;
    }

    class MemoryDataAccessRuleDAO extends DataAccessRuleDAO {

        public MemoryDataAccessRuleDAO(
                GeoServerDataDirectory dd, Catalog rawCatalog, Properties props)
                throws ConfigurationException, IOException {
            super(dd, rawCatalog);
            loadRules(props);
        }

        @Override
        protected void checkPropertyFile(boolean force) {
            // skip checking
            lastModified = Long.MAX_VALUE;
        }
    }
}
