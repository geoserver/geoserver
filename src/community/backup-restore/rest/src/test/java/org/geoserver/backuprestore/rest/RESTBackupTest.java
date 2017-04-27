/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.backuprestore.BackupRestoreTestSupport;
import org.geoserver.backuprestore.utils.BackupUtils;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestBaseController;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.Assert;

import net.sf.json.JSONObject;

/**
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class RESTBackupTest extends BackupRestoreTestSupport {

    @Test
    public void testNewBackup() throws Exception {
        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json = "{\"backup\": {" + "   \"archiveFile\": \"" + archiveFilePath + "\", "
                + "   \"overwrite\": true,"
                + "   \"options\": { \"option\": [\"BK_BEST_EFFORT=true\"] }" + "  }" + "}";

        JSONObject backup = postNewBackup(json);

        Assert.notNull(backup);

        JSONObject execution = readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue("STARTED".equals(execution.getString("status"))
                || "STARTING".equals(execution.getString("status")));

        while ("STARTED".equals(execution.getString("status"))
                || "STARTING".equals(execution.getString("status"))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
        }

        assertTrue("COMPLETED".equals(execution.getString("status")));
    }

    @Test
    public void testFilteredBackup() throws Exception {
        Resource tmpDir = BackupUtils.tmpDir();
        String archiveFilePath = Paths.path(tmpDir.path(), "geoserver-backup.zip");

        String json = "{\"backup\": {" + "   \"archiveFile\": \"" + archiveFilePath + "\", "
                + "   \"overwrite\": true,"
                + "   \"options\": { \"option\": [\"BK_BEST_EFFORT=false\"] },"
                + "   \"filter\": \"name IN ('topp','geosolutions-it')\"" + "  }" + "}";

        JSONObject backup = postNewBackup(json);

        Assert.notNull(backup);

        JSONObject execution = readExecutionStatus(backup.getJSONObject("execution").getLong("id"));

        assertTrue(execution.getJSONObject("stepExecutions").getJSONArray("step").getJSONObject(0)
                .getJSONObject("parameters").get("filter")
                .equals("name IN ('topp','geosolutions-it')"));

        assertTrue("STARTED".equals(execution.getString("status"))
                || "STARTING".equals(execution.getString("status")));

        while ("STARTED".equals(execution.getString("status"))
                || "STARTING".equals(execution.getString("status"))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(100);
        }

        assertTrue("COMPLETED".equals(execution.getString("status")));
    }

    JSONObject postNewBackup(String body) throws Exception {
        MockHttpServletResponse resp =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/br/backup", body, 
                        MediaType.APPLICATION_JSON_VALUE);

        assertEquals(201, resp.getStatus());
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);

        JSONObject execution = json.getJSONObject("backup");

        assertNotNull(execution);

        return execution;
    }

    JSONObject readExecutionStatus(long executionId) throws Exception {
        JSONObject json = (JSONObject) getAsJSON("/rest/br/backup/" + executionId + ".json");

        JSONObject backup = json.getJSONObject("backup");

        assertNotNull(backup);

        JSONObject execution = backup.getJSONObject("execution");

        assertNotNull(execution);

        return execution;
    }
}
