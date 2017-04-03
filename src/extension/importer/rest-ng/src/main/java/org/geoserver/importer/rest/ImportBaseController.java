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

    protected ImportTask task(Long imp, Integer taskNumber) {
        return task(imp, taskNumber, false);
    }

    protected ImportTask task(Long imp, Integer taskNumber, boolean optional) {
        return (ImportTask) task(imp, taskNumber, optional, false);
    }

    protected Object task(Long imp, Integer taskNumber, boolean optional, boolean allowAll) {
        ImportContext context = context(imp);
        ImportTask task = null;

        //handle null taskNumber
        if (taskNumber == null) {
            if (!optional && !allowAll) {
                throw new RestException("No task specified", HttpStatus.NOT_FOUND);
            }
        } else {
            task = context.task(taskNumber);
        }

        //handle no task found
        if (task == null) {
            if (allowAll) {
                return context.getTasks();
            }
            if (!optional) {
                throw new RestException(
                        "No such task: " + taskNumber + " for import: " + context.getId(),
                        HttpStatus.NOT_FOUND);
            }
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
