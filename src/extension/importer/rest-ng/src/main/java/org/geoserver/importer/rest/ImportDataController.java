/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;
import org.geoserver.rest.catalog.CatalogController;
import org.geoserver.importer.*;
import org.geoserver.importer.rest.converters.ImportJSONWriter;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class ImportDataController extends ImportBaseController {

    public ImportDataController(Importer importer) {
        super(importer);
    }

    protected ImportJSONWriter converterWriter;

    @GetMapping(value = { "/imports/{importId}/data", "/imports/{importId}/tasks/{taskId}/data", }, produces = {
                    MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON,
                    MediaType.APPLICATION_XML_VALUE, MediaType.TEXT_HTML_VALUE })
    public ImportData getData(@PathVariable(required = true, name = "importId") Long importId,
            @PathVariable(required = false, name = "taskId") Integer taskId/*,
            @PathVariable(required = false, name = "fileName:.+") String fileName,
            HttpServletRequest request, HttpServletResponse response*/) throws Exception {

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

    //We need to force spring to ignore the .shp here (we don't want a .shp encoded response!
    @GetMapping(value = {"/imports/{importId}/data/files",
            "/imports/{importId}/tasks/{taskId}/data/files",
            "/imports/{importId}/data/files/{fileName:.+}",
            "/imports/{importId}/tasks/{taskId}/data/files/{fileName:\\.+}" },
            produces = {MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON})
    public ImportData getDirectory(@PathVariable(required = true, name = "importId") Long importId,
                                   @PathVariable(required = false, name = "taskId") Integer taskId,
                                   @PathVariable(required = false, name = "fileName") String fileName) throws Exception {


        return getDataImport(importId, fileName);
    }
            
    
    //We need to force spring to ignore the .shp here (we don't want a .shp encoded response!
    @DeleteMapping(value = {"/imports/{importId}/data/files",
            "/imports/{importId}/tasks/{taskId}/data/files",
            "/imports/{importId}/data/files/{fileName:.+}",
            "/imports/{importId}/tasks/{taskId}/data/files/{fileName:\\.+}" })
    public ResponseEntity deleteDirectory(@PathVariable(required = true, name = "importId") Long importId,
            @PathVariable(required = false, name = "taskId") Integer taskId,
            @PathVariable(required = false, name = "fileName") String fileName) throws Exception {
        
        Directory dir = lookupDirectory(importId);
        ImportData file = lookupFile(fileName, dir);

        if (dir.getFiles().remove(file)) {
            return new ResponseEntity("", new HttpHeaders(), HttpStatus.NO_CONTENT);
        } else {
            throw new RestException("Unable to remove file: " + file.getName(), HttpStatus.BAD_REQUEST);
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
                return Iterators.find(dir.getFiles().iterator(), new Predicate<FileData>() {
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
