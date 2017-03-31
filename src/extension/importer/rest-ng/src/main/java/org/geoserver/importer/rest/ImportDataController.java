/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.converters.ImportContextJSONConverterWriter;
import org.geoserver.rest.RestBaseController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Created by vickdw on 3/30/17.
 */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class ImportDataController extends BaseController {

    public ImportDataController(Importer importer) {
        super(importer);
    }

    protected ImportContextJSONConverterWriter converterWriter;

    @GetMapping(value = {
            "/imports/{importId}/data",
            "/imports/{importId}/data/files",
            "/imports/{import}/data/files/{fileName}",
            "/imports/{importId}/tasks/{taskId}/data",
            "/imports/{importId}/tasks/{taskId}/data/files",
            "/imports/{importId}/tasks/{taskId}/data/files/{fileName}",
    }, produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE })
    public ImportData getData(@PathVariable(required = false, name = "importId") Long importId,
                              @PathVariable(required = false, name = "taskId") Integer taskId,
                              @PathVariable(required = false, name = "fileName") String fileName,
                              HttpServletRequest request, HttpServletResponse response)
            throws Exception {

        ImportData data = null;

        int taskNumber = 0;
        if (taskId != null) {
            taskNumber = taskId.intValue();
        }

        ImportTask task = task(importId, taskNumber, true);
        if (task != null) {
            data = task.getData();
        }
        else {
            data = context(importId).getData();
        }
        return data;
    }

}
