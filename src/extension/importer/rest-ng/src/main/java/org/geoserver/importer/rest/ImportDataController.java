/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.util.NoSuchElementException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.catalog.rest.CatalogController;
import org.geoserver.importer.Directory;
import org.geoserver.importer.FileData;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.converters.ImportContextJSONConverterWriter;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterators;

@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH)
public class ImportDataController extends ImportBaseController {

    public ImportDataController(Importer importer) {
        super(importer);
    }

    protected ImportContextJSONConverterWriter converterWriter;

    @GetMapping(value = { "/imports/{importId}/data",
            "/imports/{importId}/tasks/{taskId}/data", }, produces = {
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
    @GetMapping(value = {"/imports/{importId}/data/files/{fileName:.+}", "/imports/{importId}/data/files/{fileName:.+}",
            "/imports/{importId}/data/files", "/imports/{importId}/tasks/{taskId}/data/files",
            "/imports/{importId}/tasks/{taskId}/data/files/{fileName:\\.+}" }, 
            produces = {MediaType.APPLICATION_JSON_VALUE, CatalogController.TEXT_JSON})
    public ImportData getDirectory(@PathVariable(required = true, name = "importId") Long importId,
            @PathVariable(required = false, name = "taskId") Integer taskId,
            @PathVariable(required = false, name = "fileName") String fileName) throws Exception {
        
        
        return getDataImport(importId, fileName);
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
