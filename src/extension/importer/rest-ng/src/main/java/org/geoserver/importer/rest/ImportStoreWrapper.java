package org.geoserver.importer.rest;

import org.geoserver.catalog.StoreInfo;
import org.geoserver.importer.ImportTask;

/**
 * Created by tbarsballe on 2017-03-31.
 */
public class ImportStoreWrapper {
    StoreInfo store ;

    ImportTask task;

    public ImportStoreWrapper(StoreInfo store, ImportTask task) {
        this.store = store;
        this.task = task;
    }

    public StoreInfo getStore() {
        return store;
    }

    public ImportTask getTask() {
        return task;
    }
}
