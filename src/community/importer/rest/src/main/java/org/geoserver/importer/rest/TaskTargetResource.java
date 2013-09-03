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
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.util.logging.Logging;
import org.geoserver.importer.ImportTask;
import org.geoserver.importer.Importer;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class TaskTargetResource extends BaseResource {

    static Logger LOGGER = Logging.getLogger(TaskTargetResource.class);

    public TaskTargetResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new TaskTargetJSONFormat(MediaType.APPLICATION_JSON),
                new TaskTargetJSONFormat(MediaType.TEXT_HTML));
    }

    @Override
    public void handleGet() {
        ImportTask task = task();
        if (task.getStore() == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Task has no target store");
        }
        else {
            getResponse().setEntity(getFormatGet().toRepresentation(task.getStore()));
        }
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handlePut() {
        ImportTask task = task();
        if (task.getStore() == null) {
            getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "Task has no target store");
        }
        else {
            StoreInfo store = (StoreInfo) getFormatPostOrPut().toObject(getRequest().getEntity());
            updateStoreInfo(task, store, importer);

            importer.changed(task);
            getResponse().setStatus(Status.SUCCESS_NO_CONTENT);
        }
    }

    static void updateStoreInfo(ImportTask task, StoreInfo update, Importer importer) {
        //handle three cases here:
        // 1. current task store is null -> set the update as the new store
        // 2. update is reference to an existing store -> set the update as the new store
        // 3. update is a partial change to the current store -> update the current
        
        // allow an existing store to be referenced as the target
        
        StoreInfo orig = task.getStore();
        
        //check if the update is referencing an existing store
        StoreInfo existing = null;
        if (update.getName() != null) {
            Catalog cat = importer.getCatalog();
            if (update.getWorkspace() != null) {
                existing = cat.getStoreByName(
                    update.getWorkspace(), update.getName(), StoreInfo.class);
            }
            else {
                existing = importer.getCatalog().getStoreByName(update.getName(), StoreInfo.class);
            }
            if (existing == null) {
                throw new RestletException("Unable to find referenced store", Status.CLIENT_ERROR_BAD_REQUEST);
            }
            if (!existing.isEnabled()) {
                throw new RestletException("Proposed target store is not enabled", Status.CLIENT_ERROR_BAD_REQUEST);
            }
        }

        if (existing != null) {
            //JD: not sure why we do this, rather than just task.setStore(existing);
            CatalogBuilder cb = new CatalogBuilder(importer.getCatalog());

            StoreInfo clone;
            if (existing instanceof DataStoreInfo) {
                clone = cb.buildDataStore(existing.getName());
                cb.updateDataStore((DataStoreInfo) clone, (DataStoreInfo) existing);
            }
            else if (existing instanceof CoverageStoreInfo) {
                clone = cb.buildCoverageStore(existing.getName());
                cb.updateCoverageStore((CoverageStoreInfo) clone, (CoverageStoreInfo) existing);
            }
            else {
                throw new RestletException(
                    "Unable to handle existing store: " + update, Status.SERVER_ERROR_INTERNAL);
            }
            
            ((StoreInfoImpl) clone).setId(existing.getId());
            task.setStore(clone);
            task.setDirect(false);
        }
        else if (orig == null){
            task.setStore(update);
        }
        else {
            //update the original
            CatalogBuilder cb = new CatalogBuilder(importer.getCatalog());
            if (orig instanceof DataStoreInfo) {
                cb.updateDataStore((DataStoreInfo)orig, (DataStoreInfo)update);
            }
            else if (orig instanceof CoverageStoreInfo) {
                cb.updateCoverageStore((CoverageStoreInfo)orig, (CoverageStoreInfo)update);
            }
            else {
                throw new RestletException(
                    "Unable to update store with " + update, Status.SERVER_ERROR_INTERNAL);
            }
        }
    }

    class TaskTargetJSONFormat extends StreamDataFormat {

        public TaskTargetJSONFormat(MediaType type) {
            super(type);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return newReader(in).fromJSON(StoreInfo.class);
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            StoreInfo store = (StoreInfo) object;
            newWriter(out).store(store, task(), true, expand(1));
        }
    }
}
