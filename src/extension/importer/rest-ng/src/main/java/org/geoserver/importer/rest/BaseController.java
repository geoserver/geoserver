package org.geoserver.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.springframework.http.HttpRequest;
import org.springframework.http.HttpStatus;

public class BaseController extends RestBaseController {
    protected Importer importer;

    protected BaseController(Importer importer) {
        this.importer = importer;
    }

    protected ImportContext context(Long imp) {
        return context(imp, false);
    }

    protected ImportContext context(Long imp, boolean optional) {
        long i = imp;

        ImportContext context = importer.getContext(i);
        if (!optional && context == null) {
            throw new RestException("No such import: " + i, HttpStatus.NOT_FOUND);
        }
        return context;
    }

    protected ImportTask task(Long imp, int taskNumber) {
        return task(imp, taskNumber, false);
    }

    protected ImportTask task(Long imp, Integer taskNumber, boolean optional) {
        ImportContext context = context(imp);
        ImportTask task = null;
        
        if(taskNumber == null && optional) {
            return task;
        } 
        
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
