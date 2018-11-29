/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.reader;

import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.util.CloseableIterator;
import org.opengis.filter.Filter;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemReader;
import org.springframework.core.io.Resource;

/**
 * Concrete Spring Batch {@link ItemReader}.
 *
 * <p>Reads resource items from in memory {@link Catalog}.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogItemReader<T> extends CatalogReader<T> {

    int counter;

    CloseableIterator<T> catalogIterator;

    public CatalogItemReader(Class<T> clazz, Backup backupFacade) {
        super(clazz, backupFacade);
    }

    protected void initialize(StepExecution stepExecution) {}

    @Override
    public void setResource(Resource resource) {
        // Nothing to do
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Nothing to do
    }

    @Override
    protected T doRead() throws Exception {
        try {
            if (catalogIterator != null && catalogIterator.hasNext()) {
                T item = (T) catalogIterator.next();
                this.counter--;
                return item;
            }

            if (catalogIterator != null) {
                catalogIterator.close();
            }

            if (this.counter != 0) {
                throw new Exception(
                        "Not all the Catalog Resources of class ["
                                + this.clazz
                                + "] have been dumped!");
            }
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }

        return null;
    }

    @Override
    @SuppressWarnings("unchecked")
    protected void doOpen() throws Exception {
        this.counter = getCatalog().count(this.clazz, Filter.INCLUDE);
        this.catalogIterator = (CloseableIterator<T>) getCatalog().list(this.clazz, Filter.INCLUDE);
    }

    @Override
    protected void doClose() throws Exception {
        if (catalogIterator != null) {
            catalogIterator.close();
        }
    }
}
