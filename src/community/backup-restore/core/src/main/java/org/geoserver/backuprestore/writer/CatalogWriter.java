/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.writer;

import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupRestoreItem;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStreamWriter;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.ClassUtils;

/**
 * Abstract Spring Batch {@link ItemReader}.
 * 
 * Configures the {@link Catalog} and initizializes the {@link XStreamPersister}.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
@SuppressWarnings("rawtypes")
public abstract class CatalogWriter<T> extends BackupRestoreItem
        implements ItemStreamWriter<T>, ResourceAwareItemWriterItemStream<T>, InitializingBean {

    protected Class clazz;

    public CatalogWriter(Class<T> clazz, Backup backupFacade,
            XStreamPersisterFactory xStreamPersisterFactory) {
        super(backupFacade, xStreamPersisterFactory);
        this.clazz = clazz;

        this.setExecutionContextName(ClassUtils.getShortName(clazz));
    }

    private final ExecutionContextUserSupport executionContextUserSupport = new ExecutionContextUserSupport();

    /**
     * No-op.
     * 
     * @see org.springframework.batch.item.ItemStream#close()
     */
    @Override
    public void close() {
    }

    /**
     * No-op.
     * 
     * @see org.springframework.batch.item.ItemStream#open(ExecutionContext)
     */
    @Override
    public void open(ExecutionContext executionContext) {
    }

    /**
     * Return empty {@link ExecutionContext}.
     * 
     * @see org.springframework.batch.item.ItemStream#update(ExecutionContext)
     */
    @Override
    public void update(ExecutionContext executionContext) {
    }

    /**
     * The name of the component which will be used as a stem for keys in the {@link ExecutionContext}. Subclasses should provide a default value,
     * e.g. the short form of the class name.
     * 
     * @param name the name for the component
     */
    public void setName(String name) {
        this.setExecutionContextName(name);
    }

    protected void setExecutionContextName(String name) {
        executionContextUserSupport.setName(name);
    }

    public String getExecutionContextKey(String key) {
        return executionContextUserSupport.getKey(key);
    }

    protected String getItemName(XStreamPersister xp) {
        return xp.getClassAliasingMapper().serializedClass(clazz);
    }

}
