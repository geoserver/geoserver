/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.rest.CatalogController;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.converters.ImportContextJSONConverterWriter;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.io.Writer;
import java.util.List;

@RestController
@RequestMapping(path = RestBaseController.ROOT_PATH+"/imports/{id}/tasks", produces = {
        MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_HTML_VALUE })
public class ImportTaskController extends ImportBaseController {

    @Autowired
    protected ImportTaskController(Importer importer) {
        super(importer);
    }

    @GetMapping(path = "", produces = { MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON , MediaType.TEXT_HTML_VALUE})
    public ImportWrapper tasksGet(@PathVariable Long id) {
        return (writer, converter) -> converter.tasks(context(id).getTasks(), true, converter.expand(0));
    }

    @GetMapping(path = "/{taskId}", produces = { MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON , MediaType.TEXT_HTML_VALUE})
    public ImportTask taskGet(@PathVariable Long id, @PathVariable Integer taskId) {
        return task(id, taskId, false);
    }


    @GetMapping(path = {"/{taskId}/progress"}, produces = { MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON , MediaType.TEXT_HTML_VALUE})
    public ImportJSONWrapper progressGet(@PathVariable Long id, @PathVariable Integer taskId) {

        JSONObject progress = new JSONObject();
        ImportTask inProgress = importer.getCurrentlyProcessingTask(id);
        try {
            if (inProgress != null) {
                progress.put("progress", inProgress.getNumberProcessed());
                progress.put("total", inProgress.getTotalToProcess());
                progress.put("state", inProgress.getState().toString());
            } else {
                ImportTask task = task(id, taskId);
                progress.put("state", task.getState().toString());
                if (task.getState() == ImportTask.State.ERROR) {
                    if (task.getError() != null) {
                        progress.put("message", task.getError().getMessage());
                    }
                }
            }
        } catch (JSONException jex) {
            throw new RestException("Internal Error", HttpStatus.INTERNAL_SERVER_ERROR, jex);
        }
        return new ImportJSONWrapper(progress);
    }

    @GetMapping(path = {"/{taskId}/target"}, produces = { MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON , MediaType.TEXT_HTML_VALUE})
    public ImportWrapper targetGet(@PathVariable Long id, @PathVariable Integer taskId) {
        final ImportTask task = task(id, taskId);
        if (task.getStore() == null) {
            throw new RestException("Task has no target store", HttpStatus.NOT_FOUND);
        }
        return (writer, converter) -> converter.store(task.getStore(), task, true, converter.expand(1));

    }
}
