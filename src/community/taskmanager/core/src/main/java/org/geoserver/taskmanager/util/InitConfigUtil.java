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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** @author Niels Charlier */
@Service
public class InitConfigUtil {

    @Autowired TaskManagerDataUtil dataUtil;

    private static final String INIT_BATCH = "@Initialize";

    public boolean isInitConfig(Configuration config) {
        if (config.isTemplate()) {
            return false;
        }
        Batch batch = config.getBatches().get(INIT_BATCH);
        if (batch != null) {
            if (batch.getId() != null) {
                batch = dataUtil.init(batch);
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

    public static Configuration wrap(Configuration config) {
        if (!(config instanceof ConfigurationWrapper)) {
            return new ConfigurationWrapper(config);
        } else {
            return config;
        }
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

    private static class ConfigurationWrapper implements Configuration {

        private static final long serialVersionUID = 8073599284694547987L;

        private Configuration delegate;

        public ConfigurationWrapper(Configuration delegate) {
            this.delegate = delegate;
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
            Map<String, Task> tasks = new HashMap<String, Task>();
            Batch batch = delegate.getBatches().get(INIT_BATCH);
            if (batch != null) {
                for (BatchElement element : batch.getElements()) {
                    tasks.put(element.getTask().getName(), element.getTask());
                }
            }
            return Collections.unmodifiableMap(tasks);
        }

        @Override
        public Map<String, Batch> getBatches() {
            Batch batch = getInitBatch(delegate);
            if (batch != null) {
                return Collections.singletonMap(batch.getName(), batch);
            }
            return Collections.emptyMap();
        }
    }
}
