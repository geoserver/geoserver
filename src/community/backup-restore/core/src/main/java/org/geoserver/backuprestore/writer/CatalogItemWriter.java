/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import java.util.List;
import org.geoserver.backuprestore.Backup;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ItemWriter;
import org.springframework.core.io.Resource;

/**
 * Concrete Spring Batch {@link ItemWriter}.
 *
 * <p>Writes unmarshalled items into the temporary {@link Catalog} in memory.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogItemWriter<T> extends CatalogWriter<T> {

    public CatalogItemWriter(
            Class<T> clazz, Backup backupFacade, XStreamPersisterFactory xStreamPersisterFactory) {
        super(clazz, backupFacade, xStreamPersisterFactory);
    }

    @Override
    protected void initialize(StepExecution stepExecution) {
        if (this.getXp() == null) {
            setXp(this.xstream.getXStream());
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(List<? extends T> items) {
        for (T item : items) {
            try {
                if (item instanceof WorkspaceInfo) {
                    getCatalog().add((WorkspaceInfo) item);
                } else if (item instanceof NamespaceInfo) {
                    getCatalog().add((NamespaceInfo) item);
                } else if (item instanceof DataStoreInfo) {
                    getCatalog().add((DataStoreInfo) item);
                } else if (item instanceof CoverageStoreInfo) {
                    getCatalog().add((CoverageStoreInfo) item);
                } else if (item instanceof ResourceInfo) {
                    getCatalog().add((ResourceInfo) item);
                } else if (item instanceof LayerInfo) {
                    getCatalog().add((LayerInfo) item);
                } else if (item instanceof StyleInfo) {
                    getCatalog().add((StyleInfo) item);
                } else if (item instanceof LayerGroupInfo) {
                    getCatalog().add((LayerGroupInfo) item);
                }
            } catch (Exception e) {
                logValidationExceptions((T) null, e);
            }
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        // Nothing to do.
    }

    /**
     * Setter for resource. Represents a file that can be written.
     *
     * @param resource
     */
    @Override
    public void setResource(Resource resource) {
        // Nothing to do
    }
}
