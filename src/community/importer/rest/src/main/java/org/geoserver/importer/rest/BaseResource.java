/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.rest.format.StreamDataFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Preference;
import org.restlet.data.Request;
import org.restlet.data.Status;

public abstract class BaseResource extends AbstractResource {

    protected Importer importer;

    protected BaseResource(Importer importer) {
        this.importer = importer;
    }

    protected ImportContext context() {
        return context(false);
    }

    protected ImportContext context(boolean optional) {
        long i = Long.parseLong(getAttribute("import"));

        ImportContext context = importer.getContext(i);
        if (!optional && context == null) {
            throw new RestletException("No such import: " + i, Status.CLIENT_ERROR_NOT_FOUND);
        }
        return context;
    }

    protected ImportTask task() {
        return task(false);
    }

    protected ImportTask task(boolean optional) {
        ImportContext context = context();
        ImportTask task = null;

        String t = getAttribute("task");
        if (t != null) {
            int id = Integer.parseInt(t);
            task = context.task(id);
        }
        if (t != null && task == null) {
            throw new RestletException("No such task: " + t + " for import: " + context.getId(),
                    Status.CLIENT_ERROR_NOT_FOUND);
        }

        if (task == null && !optional) {
            throw new RestletException("No task specified", 
                    
                    Status.CLIENT_ERROR_NOT_FOUND);
        }

        return task;
    }

    protected int expand(int def) {
        String ex = getRequest().getResourceRef().getQueryAsForm().getFirstValue("expand");
        if (ex == null) {
            return def;
        }

        try {
            return "self".equalsIgnoreCase(ex) ? 1
                 : "all".equalsIgnoreCase(ex) ? Integer.MAX_VALUE 
                 : "none".equalsIgnoreCase(ex) ? 0 
                 : Integer.parseInt(ex);
        }
        catch(NumberFormatException e) {
            return def;
        }
    }

    protected ImportJSONReader newReader(InputStream input) throws IOException {
        return new ImportJSONReader(importer, input);
    }

    protected ImportJSONWriter newWriter(OutputStream output) throws IOException {
        return new ImportJSONWriter(importer, getPageInfo(), output);
    }
}
