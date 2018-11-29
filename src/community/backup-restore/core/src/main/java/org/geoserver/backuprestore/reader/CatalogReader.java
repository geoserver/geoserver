/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.backuprestore.reader;

import java.io.IOException;
import java.util.List;
import org.geoserver.backuprestore.Backup;
import org.geoserver.backuprestore.BackupRestoreItem;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemCountAware;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemStream;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.ItemStreamReader;
import org.springframework.batch.item.ParseException;
import org.springframework.batch.item.UnexpectedInputException;
import org.springframework.batch.item.file.ResourceAwareItemReaderItemStream;
import org.springframework.batch.item.util.ExecutionContextUserSupport;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Abstract Spring Batch {@link ItemReader}.
 *
 * <p>Configures the {@link Catalog} and initizializes the {@link XStreamPersister}.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
@SuppressWarnings({"rawtypes"})
public abstract class CatalogReader<T> extends BackupRestoreItem<T>
        implements ItemStream,
                ItemStreamReader<T>,
                ResourceAwareItemReaderItemStream<T>,
                InitializingBean {

    protected Class clazz;

    public CatalogReader(Class<T> clazz, Backup backupFacade) {
        super(backupFacade);
        this.clazz = clazz;

        this.setExecutionContextName(ClassUtils.getShortName(clazz));
    }

    private static final String READ_COUNT = "read.count";

    private static final String READ_COUNT_MAX = "read.count.max";

    private int currentItemCount = 0;

    private int maxItemCount = Integer.MAX_VALUE;

    private boolean saveState = true;

    private final ExecutionContextUserSupport executionContextUserSupport =
            new ExecutionContextUserSupport();

    /**
     * The name of the component which will be used as a stem for keys in the {@link
     * ExecutionContext}. Subclasses should provide a default value, e.g. the short form of the
     * class name.
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

    /**
     * Read next item from input.
     *
     * @return item
     * @throws Exception Allows subclasses to throw checked exceptions for interpretation by the
     *     framework
     */
    protected abstract T doRead() throws Exception;

    /**
     * Open resources necessary to start reading input.
     *
     * @throws Exception Allows subclasses to throw checked exceptions for interpretation by the
     *     framework
     */
    protected abstract void doOpen() throws Exception;

    /**
     * Close the resources opened in {@link #doOpen()}.
     *
     * @throws Exception Allows subclasses to throw checked exceptions for interpretation by the
     *     framework
     */
    protected abstract void doClose() throws Exception;

    /**
     * Move to the given item index. Subclasses should override this method if there is a more
     * efficient way of moving to given index than re-reading the input using {@link #doRead()}.
     *
     * @param itemIndex index of item (0 based) to jump to.
     * @throws Exception Allows subclasses to throw checked exceptions for interpretation by the
     *     framework
     */
    protected void jumpToItem(int itemIndex) throws Exception {
        for (int i = 0; i < itemIndex; i++) {
            read();
        }
    }

    @Override
    public T read() throws Exception, UnexpectedInputException, ParseException {
        if (currentItemCount >= maxItemCount) {
            return null;
        }
        currentItemCount++;
        T item = doRead();
        if (item instanceof ItemCountAware) {
            ((ItemCountAware) item).setItemCount(currentItemCount);
        }
        return item;
    }

    protected int getCurrentItemCount() {
        return currentItemCount;
    }

    /**
     * The index of the item to start reading from. If the {@link ExecutionContext} contains a key
     * <code>[name].read.count</code> (where <code>[name]</code> is the name of this component) the
     * value from the {@link ExecutionContext} will be used in preference.
     *
     * @see #setName(String)
     * @param count the value of the current item count
     */
    public void setCurrentItemCount(int count) {
        this.currentItemCount = count;
    }

    /**
     * The maximum index of the items to be read. If the {@link ExecutionContext} contains a key
     * <code>[name].read.count.max</code> (where <code>[name]</code> is the name of this component)
     * the value from the {@link ExecutionContext} will be used in preference.
     *
     * @see #setName(String)
     * @param count the value of the maximum item count
     */
    public void setMaxItemCount(int count) {
        this.maxItemCount = count;
    }

    @Override
    public void close() throws ItemStreamException {
        currentItemCount = 0;
        try {
            doClose();
        } catch (Exception e) {
            throw new ItemStreamException("Error while closing item reader", e);
        }
    }

    @Override
    public void open(ExecutionContext executionContext) throws ItemStreamException {
        try {
            doOpen();
        } catch (Exception e) {
            return;
        }
        if (!isSaveState()) {
            return;
        }

        if (executionContext.containsKey(getExecutionContextKey(READ_COUNT_MAX))) {
            maxItemCount = executionContext.getInt(getExecutionContextKey(READ_COUNT_MAX));
        }

        int itemCount = 0;
        if (executionContext.containsKey(getExecutionContextKey(READ_COUNT))) {
            itemCount = executionContext.getInt(getExecutionContextKey(READ_COUNT));
        } else if (currentItemCount > 0) {
            itemCount = currentItemCount;
        }

        if (itemCount > 0 && itemCount < maxItemCount) {
            try {
                jumpToItem(itemCount);
            } catch (Exception e) {
                throw new ItemStreamException("Could not move to stored position on restart", e);
            }
        }

        currentItemCount = itemCount;
    }

    @Override
    public void update(ExecutionContext executionContext) throws ItemStreamException {
        if (saveState) {
            Assert.notNull(executionContext, "ExecutionContext must not be null");
            executionContext.putInt(getExecutionContextKey(READ_COUNT), currentItemCount);
            if (maxItemCount < Integer.MAX_VALUE) {
                executionContext.putInt(getExecutionContextKey(READ_COUNT_MAX), maxItemCount);
            }
        }
    }

    @SuppressWarnings({"unchecked"})
    protected void firePostRead(T item, Resource resource) throws IOException {
        List<CatalogAdditionalResourcesReader> additionalResourceReaders =
                GeoServerExtensions.extensions(CatalogAdditionalResourcesReader.class);

        for (CatalogAdditionalResourcesReader rd : additionalResourceReaders) {
            if (rd.canHandle(item)) {
                rd.readAdditionalResources(
                        backupFacade, Files.asResource(resource.getFile()), item);
            }
        }
    }

    /**
     * Set the flag that determines whether to save internal data for {@link ExecutionContext}. Only
     * switch this to false if you don't want to save any state from this stream, and you don't need
     * it to be restartable. Always set it to false if the reader is being used in a concurrent
     * environment.
     *
     * @param saveState flag value (default true).
     */
    public void setSaveState(boolean saveState) {
        this.saveState = saveState;
    }

    /**
     * The flag that determines whether to save internal state for restarts.
     *
     * @return true if the flag was set
     */
    public boolean isSaveState() {
        return saveState;
    }
}
