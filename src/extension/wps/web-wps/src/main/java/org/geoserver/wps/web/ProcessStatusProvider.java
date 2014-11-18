/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import java.util.Arrays;
import java.util.List;

import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geotools.data.Query;

/**
 * Provides a filtered, sorted view over the running/recently completed processes
 * 
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class ProcessStatusProvider extends GeoServerDataProvider<ExecutionStatus> {
    static final Property<ExecutionStatus> TYPE = new AbstractProperty<ExecutionStatus>("type") {

        @Override
        public Object getPropertyValue(ExecutionStatus item) {
            // we might want to have a "C" state for the chained processes
            return item.isAsynchronous() ? "A" : "S";
        }

    };

    static final Property<ExecutionStatus> NODE = new BeanProperty<ExecutionStatus>("node",
            "nodeId");

    static final Property<ExecutionStatus> USER = new BeanProperty<ExecutionStatus>("user",
            "userName");

    static final Property<ExecutionStatus> PROCESS = new BeanProperty<ExecutionStatus>(
            "processName", "processName");

    static final Property<ExecutionStatus> CREATED = new BeanProperty<ExecutionStatus>(
            "creationTime", "creationTime");

    static final Property<ExecutionStatus> PHASE = new BeanProperty<ExecutionStatus>("phase",
            "phase");

    static final Property<ExecutionStatus> PROGRESS = new BeanProperty<ExecutionStatus>("progress",
            "progress");

    static final Property<ExecutionStatus> TASK = new BeanProperty<ExecutionStatus>("task", "task");

    static final List<Property<ExecutionStatus>> PROPERTIES = Arrays.asList(TYPE, NODE, USER,
            PROCESS, CREATED, PHASE, PROGRESS, TASK);

    @Override
    protected List<ExecutionStatus> getItems() {
        ProcessStatusTracker tracker = GeoServerApplication.get().getBeanOfType(
                ProcessStatusTracker.class);
        return tracker.getStore().list(Query.ALL);
    }

    @Override
    protected List<Property<ExecutionStatus>> getProperties() {
        return PROPERTIES;
    }

}
