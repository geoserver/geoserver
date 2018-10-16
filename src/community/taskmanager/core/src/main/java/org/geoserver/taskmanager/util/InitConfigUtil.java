/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.taskmanager.data.Attribute;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.data.Run.Status;
import org.geoserver.taskmanager.data.Task;
import org.geoserver.taskmanager.data.TaskManagerDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** @author Niels Charlier */
@Service
public class InitConfigUtil {

    private static final String INIT_BATCH = "@Initialize";

    @Autowired private TaskManagerDao dao;

    public boolean isInitConfig(Configuration config) {
        if (config.isTemplate()) {
            return false;
        }
        Batch batch = getInitBatch(config);
        if (batch != null) {
            if (batch.getId() != null) {
                batch = dao.initHistory(batch);
                for (BatchRun batchRun : batch.getBatchRuns()) {
                    if (batchRun.getStatus() == Status.COMMITTED) {
                        return false;
                    }
                }
            }
            return true;
        } else {
            return false;
        }
    }

    public Configuration wrap(Configuration config) {
        if (!(config instanceof ConfigurationWrapper)) {
            Batch batch = getInitBatch(config);
            if (batch != null) {
                return new ConfigurationWrapper(config, batch);
            }
        }
        return config;
    }

    public static Configuration unwrap(Configuration config) {
        if (config instanceof ConfigurationWrapper) {
            return ((ConfigurationWrapper) config).getDelegate();
        } else {
            return config;
        }
    }

    public static Batch getInitBatch(Configuration config) {
        return config.getBatches().get(INIT_BATCH);
    }

    public static boolean isInitBatch(Batch batch) {
        return batch.getConfiguration() != null && batch.getName().equals(INIT_BATCH);
    }

    private static class ConfigurationWrapper implements Configuration {

        private static final long serialVersionUID = 8073599284694547987L;

        private Configuration delegate;

        private Map<String, Task> tasks = new HashMap<String, Task>();;

        private Map<String, Batch> batches;

        public ConfigurationWrapper(Configuration delegate, Batch initBatch) {
            this.delegate = delegate;

            if (initBatch != null) {
                for (BatchElement element : initBatch.getElements()) {
                    tasks.put(element.getTask().getName(), element.getTask());
                }

                batches = Collections.singletonMap(initBatch.getName(), initBatch);
            } else {
                batches = Collections.emptyMap();
            }
        }

        public Configuration getDelegate() {
            return delegate;
        }

        @Override
        public void setRemoveStamp(long removeStamp) {
            delegate.setRemoveStamp(removeStamp);
        }

        @Override
        public long getRemoveStamp() {
            return delegate.getRemoveStamp();
        }

        @Override
        public Long getId() {
            return delegate.getId();
        }

        @Override
        public boolean isTemplate() {
            return delegate.isTemplate();
        }

        @Override
        public void setTemplate(boolean template) {
            delegate.setTemplate(template);
        }

        @Override
        public String getWorkspace() {
            return delegate.getWorkspace();
        }

        @Override
        public void setWorkspace(String workspace) {
            delegate.setWorkspace(workspace);
        }

        @Override
        public Map<String, Attribute> getAttributes() {
            return delegate.getAttributes();
        }

        @Override
        public String getName() {
            return delegate.getName();
        }

        @Override
        public void setName(String name) {
            delegate.setName(name);
        }

        @Override
        public String getDescription() {
            return delegate.getDescription();
        }

        @Override
        public void setDescription(String name) {
            delegate.setDescription(name);
        }

        @Override
        public boolean isValidated() {
            return delegate.isValidated();
        }

        @Override
        public void setValidated(boolean initMode) {
            delegate.setValidated(initMode);
        }

        @Override
        public Map<String, Task> getTasks() {
            return tasks;
        }

        @Override
        public Map<String, Batch> getBatches() {
            return batches;
        }
    }
}
