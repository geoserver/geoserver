/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geoserver.importer.Directory;
import org.geoserver.importer.ImportContext;
import org.geoserver.importer.ImportData;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.rest.ImportResource.ImportContextJSONFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

public class DataResource extends BaseResource {


    public DataResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new ImportDataJSONFormat(MediaType.APPLICATION_JSON),
                new ImportDataJSONFormat(MediaType.TEXT_HTML));
    }

    @Override
    public void handleGet() {
        DataFormat formatGet = getFormatGet();
        if (formatGet == null) {
            formatGet = new ImportDataJSONFormat(MediaType.APPLICATION_JSON);
        }

        ImportData data = null;

        ImportTask task = task(true);
        if (task != null) {
            data = task.getData();
        }
        else {
            data = context().getData();
        }

        getResponse().setEntity(formatGet.toRepresentation(data));
    }

    public class ImportDataJSONFormat extends StreamDataFormat {

        protected ImportDataJSONFormat(MediaType type) {
            super(type);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return newReader(in).data();
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            newWriter(out).data((ImportData)object, task(true) != null ? task() : context(), expand(1));
        }
    
    }
}
