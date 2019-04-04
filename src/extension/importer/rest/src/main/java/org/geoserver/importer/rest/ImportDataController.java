/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import java.util.NoSuchElementException;
import org.geoserver.importer.*;
import org.geoserver.importer.rest.converters.ImportJSONWriter;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.MediaTypeExtensions;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@ControllerAdvice
@RequestMapping(
    path = RestBaseController.ROOT_PATH + "/imports/{importId}",
    produces = {MediaType.APPLICATION_JSON_VALUE, MediaTypeExtensions.TEXT_JSON_VALUE}
)
public class ImportDataController extends ImportBaseController {

    public ImportDataController(Importer importer) {
        super(importer);
    }

    protected ImportJSONWriter converterWriter;

    @GetMapping(
        value = {
            "/data",
            "/tasks/{taskId}/data",
        }
    )
    public ImportData getData(
            @PathVariable Long importId, @PathVariable(required = false) Integer taskId)
            throws Exception {

        ImportData data = null;

        ImportTask task = task(importId, taskId, true);
        if (task != null) {
            data = task.getData();
            data.setParent(task);

        } else {
            final ImportContext context = context(importId);
            data = context.getData();
            data.setParent(context);
        }

        return data;
    }

    // We need to force spring to ignore the .shp here (we don't want a .shp encoded response!
    @GetMapping(
        value = {
            "/data/files", "/tasks/{taskId}/data/files",
            "/data/files/{fileName:.+}", "/tasks/{taskId}/data/files/{fileName:\\.+}"
        }
    )
    public ImportData getDirectory(
            @PathVariable Long importId,
            @PathVariable(required = false) Integer taskId,
            @PathVariable(required = false) String fileName)
            throws Exception {

        return getDataImport(importId, fileName);
    }

    // We need to force spring to ignore the .shp here (we don't want a .shp encoded response!
    @DeleteMapping(
        value = {"/data/files/{fileName:.+}", "/tasks/{taskId}/data/files/{fileName:\\.+}"}
    )
    public ResponseEntity deleteDirectory(
            @PathVariable Long importId,
            @PathVariable(required = false) Integer taskId,
            @PathVariable(required = false) String fileName)
            throws Exception {

        Directory dir = lookupDirectory(importId);
        ImportData file = lookupFile(fileName, dir);

        if (dir.getFiles().remove(file)) {
            return new ResponseEntity("", new HttpHeaders(), HttpStatus.NO_CONTENT);
        } else {
            throw new RestException(
                    "Unable to remove file: " + file.getName(), HttpStatus.BAD_REQUEST);
        }
    }

    private ImportData getDataImport(Long importId, String fileName) {
        Directory dir = lookupDirectory(importId);
        ImportData response = dir;

        if (fileName != null) {
            response = lookupFile(fileName, dir);
            response.setParent((ImportContext) dir.getParent());
        }
        return (ImportData) response;
    }

    Directory lookupDirectory(Long importId) {
        ImportContext context = context(importId);
        if (!(context.getData() instanceof Directory)) {
            throw new RestException("Data is not a directory", HttpStatus.BAD_REQUEST);
        }

        Directory data = (Directory) context.getData();
        data.setParent(context);
        return data;
    }

    public ImportData lookupFile(String file, Directory dir) {
        try {
            if (file != null) {
                return Iterators.find(
                        dir.getFiles().iterator(),
                        new Predicate<FileData>() {
                            @Override
                            public boolean apply(FileData input) {
                                return input.getFile().getName().equals(file);
                            }
                        });
            }
        } catch (NoSuchElementException e) {

        }
        throw new RestException("No such file: " + file, HttpStatus.NOT_FOUND);
    }
}
