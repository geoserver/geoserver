/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import org.geoserver.catalog.rest.CatalogController;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.rest.RestBaseController;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    public ImportContextCollectionWrapper<ImportTask> tasksGet(@PathVariable Long id) {
        return new ImportContextCollectionWrapper<>(context(id).getTasks(), ImportTask.class);
    }

    @GetMapping(path = "/{taskId}", produces = { MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON , MediaType.TEXT_HTML_VALUE})
    public ImportTask taskGet(@PathVariable Long id, @PathVariable Integer taskId) {
        return task(id, taskId, false);
    }


    @GetMapping(path = {"/progress","/{taskId}/progress"}, produces = { MediaType.APPLICATION_JSON_VALUE,
            CatalogController.TEXT_JSON , MediaType.TEXT_HTML_VALUE})
    public void progressGet(@PathVariable Long id, @PathVariable(required = false) Integer taskId) {
            //getResponse().setEntity(createProgressRepresentation());
    }
}
