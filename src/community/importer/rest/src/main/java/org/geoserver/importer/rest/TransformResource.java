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

import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.geoserver.importer.transform.ImportTransform;
import org.geoserver.importer.transform.TransformChain;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class TransformResource extends BaseResource {

    public TransformResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new TransformJSONFormat(MediaType.APPLICATION_JSON),
                new TransformJSONFormat(MediaType.TEXT_HTML));
    }

    public void handleGet() {
        Object result = transform(true);
        if (result == null) {
            result = task().getTransform();
        }
    
        getResponse().setEntity(getFormatGet().toRepresentation(result));
        getResponse().setStatus(Status.SUCCESS_OK);
    }

    @Override
    public boolean allowPost() {
        return getAttribute("transform") == null;
    }

    @Override
    public void handlePost() {
        ImportTransform tx = 
            (ImportTransform) getFormatPostOrPut().toObject(getRequest().getEntity());
        ImportTask task = task();
        task.getTransform().add(tx);

        getResponse().redirectSeeOther(getPageInfo().rootURI(
            String.format(getPageInfo().rootURI(String.format("%s/transforms/%d",
                ImportJSONWriter.pathTo(task), task.getTransform().getTransforms().size()-1)))));
        getResponse().setStatus(Status.SUCCESS_CREATED);
    }

    @Override
    public boolean allowPut() {
        return getAttribute("transform") != null;
    }

    @Override
    public void handlePut() {
        ImportTransform orig = transform(false);
        ImportTransform tx = 
                (ImportTransform) getFormatPostOrPut().toObject(getRequest().getEntity());

        OwsUtils.copy(tx, orig, (Class) orig.getClass());

        getResponse().setEntity(getFormatGet().toRepresentation(orig));
        getResponse().setStatus(Status.SUCCESS_OK);
    }

    @Override
    public boolean allowDelete() {
        return allowPut();
    }

    @Override
    public void handleDelete() {
        ImportTask task = task();
        ImportTransform tx = transform(false);
        boolean result = task.getTransform().remove(tx);

        getResponse().setStatus(result ? Status.SUCCESS_OK : Status.CLIENT_ERROR_NOT_FOUND);
    }

    ImportTransform transform(boolean optional) {
        ImportTask task = task();

        ImportTransform tx = null;
        if (getAttribute("transform") != null) {
            try {
                Integer i = Integer.parseInt(getAttribute("transform"));
                tx = (ImportTransform) task.getTransform().getTransforms().get(i);
            }
            catch(NumberFormatException e) {
            }
            catch(IndexOutOfBoundsException e) {
            }
        }

        if (tx == null && !optional) {
            throw new RestletException("No such transform", Status.CLIENT_ERROR_NOT_FOUND);
        }
        return tx;
    }

    class TransformJSONFormat extends StreamDataFormat {

        TransformJSONFormat(MediaType type) {
            super(type);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return newReader(in).transform();
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            ImportJSONWriter io = newWriter(out);
            if (object instanceof TransformChain) {
                io.transformChain(task(), true, expand(1));
            }
            else {
                ImportTransform tx = (ImportTransform) object;
                ImportTask task = task();
                int index = task.getTransform().getTransforms().indexOf(tx);

                io.transform(tx, index, task(), true, expand(1));
            }
        }
    }
}
