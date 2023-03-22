/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.rest;

import static org.junit.Assert.*;

import java.util.logging.Level;
import net.sf.json.JSONObject;
import org.geoserver.backuprestore.BackupRestoreTestSupport;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestBaseController;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;

/** @author Alessio Fabiani, GeoSolutions */
public class RESTRestoreTest extends BackupRestoreTestSupport {

    @Test
    public void testNewRestore() throws Exception {
        Resource archiveFile = file("geoserver-alfa2-backup.zip");

        if (archiveFile == null) {
            LOGGER.log(Level.WARNING, "Could not find source archive file.");
        } else {
            String json =
                    "{\"restore\": {"
                            + "   \"archiveFile\": \""
                            + archiveFile.path()
                            + "\", "
                            + "   \"options\": { \"option\": [\"BK_DRY_RUN=true\", \"BK_BEST_EFFORT=true\"] }"
                            + "  }"
                            + "}";

            assertRestoreIsValid(json);
        }
    }

    private void assertRestoreIsValid(String json) throws Exception {
        JSONObject restore = postNewRestore(json);

        assertNotNull(restore);

        Thread.sleep(500);

        JSONObject execution =
                readExecutionStatus(restore.getJSONObject("execution").getLong("id"));

        assertTrue(
                "STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")));

        int cnt = 0;
        while (cnt < 100
                && ("STARTED".equals(execution.getString("status"))
                        || "STARTING".equals(execution.getString("status")))) {
            execution = readExecutionStatus(execution.getLong("id"));

            Thread.sleep(1000);
            cnt++;
        }

        if (cnt < 100) {
            assertTrue("COMPLETED".equals(execution.getString("status")));
        }
    }

    JSONObject postNewRestore(String body) throws Exception {
        MockHttpServletResponse resp =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/br/restore",
                        body,
                        MediaType.APPLICATION_JSON_UTF8_VALUE);

        int cnt = 0;
        while (cnt < 100 && resp.getStatus() == 500) {
            LOGGER.info(
                    "Could not start a new Restore Job Execution since there are currently Running jobs.");
            Thread.sleep(1000);
            cnt++;
        }
        assertEquals(201, resp.getStatus());
        assertEquals("application/json", resp.getContentType());

        JSONObject json = (JSONObject) json(resp);
        JSONObject execution = json.getJSONObject("restore");

        assertNotNull(execution);

        return execution;
    }

    JSONObject readExecutionStatus(long executionId) throws Exception {
        JSONObject json =
                (JSONObject)
                        getAsJSON(
                                RestBaseController.ROOT_PATH
                                        + "/br/restore/"
                                        + executionId
                                        + ".json");

        JSONObject restore = json.getJSONObject("restore");

        assertNotNull(restore);

        JSONObject execution = restore.getJSONObject("execution");

        assertNotNull(execution);

        return execution;
    }
}
