/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/*
 * Copyright 2006-2007 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.geoserver.backuprestore.writer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import org.geoserver.backuprestore.Backup;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.item.ExecutionContext;
import org.springframework.batch.item.ItemStreamException;
import org.springframework.batch.item.file.MultiResourceItemWriter;
import org.springframework.batch.item.file.ResourceAwareItemWriterItemStream;
import org.springframework.batch.item.file.ResourceSuffixCreator;
import org.springframework.batch.item.file.SimpleResourceSuffixCreator;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.util.Assert;

/**
 * Wraps a {@link ResourceAwareItemWriterItemStream} and creates a new output resource when the
 * count of items written in current resource exceeds {@link #setItemCountLimitPerResource(int)}.
 * Suffix creation can be customized with {@link #setResourceSuffixCreator(ResourceSuffixCreator)}.
 *
 * <p>Note that new resources are created only at chunk boundaries i.e. the number of items written
 * into one resource is between the limit set by {@link #setItemCountLimitPerResource(int)} and
 * (limit + chunk size).
 *
 * <p>Code based on original {@link MultiResourceItemWriter} by Robert Kasanicky.
 *
 * @param <T> item type
 * @author Robert Kasanicky
 * @author Alessio Fabiani, GeoSolutions
 */
public class CatalogMultiResourceItemWriter<T> extends CatalogWriter<T> {

    private static final String RESOURCE_INDEX_KEY = "resource.index";

    private static final String CURRENT_RESOURCE_ITEM_COUNT = "resource.item.count";

    private Resource resource;

    private CatalogWriter<? super T> delegate;

    private int itemCountLimitPerResource = Integer.MAX_VALUE;

    private int currentResourceItemCount = 0;

    private int resourceIndex = 1;

    private ResourceSuffixCreator suffixCreator = new SimpleResourceSuffixCreator();

    private boolean saveState = true;

    private boolean opened = false;

    public CatalogMultiResourceItemWriter(Class<T> clazz, Backup backupFacade) {
        super(clazz, backupFacade);
    }

    protected void initialize(StepExecution stepExecution) {
        delegate.retrieveInterstepData(stepExecution);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void write(List<? extends T> items) throws Exception {
        try {
            if (!opened) {
                File file = setResourceToDelegate();
                // create only if write is called
                file.createNewFile();
                Assert.state(
                        file.canWrite(),
                        "Output resource " + file.getAbsolutePath() + " must be writable");
                delegate.open(new ExecutionContext());
                opened = true;
            }
            delegate.write(items);
            currentResourceItemCount += items.size();
            if (currentResourceItemCount >= itemCountLimitPerResource) {
                delegate.close();
                resourceIndex++;
                currentResourceItemCount = 0;
                setResourceToDelegate();
                opened = false;
            }
        } catch (Exception e) {
            logValidationExceptions((T) null, e);
        }
    }

    /** Allows customization of the suffix of the created resources based on the index. */
    public void setResourceSuffixCreator(ResourceSuffixCreator suffixCreator) {
        this.suffixCreator = suffixCreator;
    }

    /** After this limit is exceeded the next chunk will be written into newly created resource. */
    public void setItemCountLimitPerResource(int itemCountLimitPerResource) {
        this.itemCountLimitPerResource = itemCountLimitPerResource;
    }

    /** Delegate used for actual writing of the output. */
    public void setDelegate(CatalogWriter<? super T> delegate) {
        this.delegate = delegate;
    }

    /**
     * Prototype for output resources. Actual output files will be created in the same directory and
     * use the same name as this prototype with appended suffix (according to {@link
     * #setResourceSuffixCreator(ResourceSuffixCreator)}.
     */
    public void setResource(Resource resource) {
        this.resource = resource;
    }

    public void setSaveState(boolean saveState) {
        this.saveState = saveState;
    }

    @SuppressWarnings("unchecked")
    @Override
    public void close() {
        try {
            super.close();
            resourceIndex = 1;
            currentResourceItemCount = 0;
            if (opened) {
                delegate.close();
            }
        } catch (ItemStreamException e) {
            logValidationExceptions((T) null, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void open(ExecutionContext executionContext) {
        try {
            super.open(executionContext);
            resourceIndex = executionContext.getInt(getExecutionContextKey(RESOURCE_INDEX_KEY), 1);
            currentResourceItemCount =
                    executionContext.getInt(getExecutionContextKey(CURRENT_RESOURCE_ITEM_COUNT), 0);

            try {
                setResourceToDelegate();
            } catch (IOException e) {
                throw new ItemStreamException("Couldn't assign resource", e);
            }

            if (executionContext.containsKey(getExecutionContextKey(CURRENT_RESOURCE_ITEM_COUNT))) {
                // It's a restart
                delegate.open(executionContext);
            } else {
                opened = false;
            }
        } catch (ItemStreamException e) {
            logValidationExceptions((T) null, e);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public void update(ExecutionContext executionContext) {
        try {
            super.update(executionContext);
            if (saveState) {
                if (opened) {
                    delegate.update(executionContext);
                }
                executionContext.putInt(
                        getExecutionContextKey(CURRENT_RESOURCE_ITEM_COUNT),
                        currentResourceItemCount);
                executionContext.putInt(getExecutionContextKey(RESOURCE_INDEX_KEY), resourceIndex);
            }
        } catch (ItemStreamException e) {
            logValidationExceptions((T) null, e);
        }
    }

    /** Create output resource (if necessary) and point the delegate to it. */
    private File setResourceToDelegate() throws IOException {
        String path = resource.getFile().getAbsolutePath() + suffixCreator.getSuffix(resourceIndex);
        File file = new File(path);
        delegate.setResource(new FileSystemResource(file));
        return file;
    }

    @Override
    public void afterPropertiesSet() throws Exception {}
}
