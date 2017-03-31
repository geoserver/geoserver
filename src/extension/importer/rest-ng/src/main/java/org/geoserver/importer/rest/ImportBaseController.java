/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.springframework.http.HttpStatus;

public class ImportBaseController extends RestBaseController {
    protected Importer importer;

    protected ImportBaseController(Importer importer) {
        this.importer = importer;
    }

    protected ImportContext context(Long imp) {
        return context(imp, false);
    }

    protected ImportContext context(Long imp, boolean optional) {
        return (ImportContext) context(imp, optional, false);
    }

    Object context(Long imp, boolean optional, boolean allowAll) {
        if (imp == null) {
            if (allowAll) {
                return importer.getAllContexts();
            }
            throw new RestException("No import specified", HttpStatus.BAD_REQUEST);
        } else {
            ImportContext context = null;
            context = importer.getContext(imp);
            if (context == null && !optional) {
                throw new RestException("No such import: " + imp.toString(), HttpStatus.NOT_FOUND);
            }
            return context;
        }
    }

    protected ImportTask task(Long imp, int taskNumber) {
        return task(imp, taskNumber, false);
    }

    protected ImportTask task(Long imp, int taskNumber, boolean optional) {
        ImportContext context = context(imp);
        ImportTask task = null;

        task = context.task(taskNumber);

        if (task == null) {
            throw new RestException(
                    "No such task: " + taskNumber + " for import: " + context.getId(),
                    HttpStatus.NOT_FOUND);
        }

        if (task == null && !optional) {
            throw new RestException("No task specified",

                    HttpStatus.NOT_FOUND);
        }

        return task;
    }

    protected int expand(int def, String ex) {
        
        if (ex == null) {
            return def;
        }

        try {
            return "self".equalsIgnoreCase(ex) ? 1
                    : "all".equalsIgnoreCase(ex) ? Integer.MAX_VALUE
                            : "none".equalsIgnoreCase(ex) ? 0 : Integer.parseInt(ex);
        } catch (NumberFormatException e) {
            return def;
        }
    }

    
/*
    protected ImportJSONReader newReader(InputStream input) throws IOException {
        return new ImportJSONReader(importer, input);
    }

    protected ImportJSONWriter newWriter(OutputStream output) throws IOException {
        return new ImportJSONWriter(importer, getPageInfo(), output);
    }*/
}
