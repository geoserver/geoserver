/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.executor;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Logger;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wps.MemoryProcessStatusStore;
import org.geoserver.wps.ProcessEvent;
import org.geoserver.wps.ProcessListener;
import org.geoserver.wps.ProcessStatusStore;
import org.geoserver.wps.WPSException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.Not;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * A listener that tracks the evolution of process execution and stores it in a {@link
 * ProcessStatusStore}
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ProcessStatusTracker
        implements ApplicationContextAware, ProcessListener, ExtensionPriority {

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    static final Logger LOGGER = Logging.getLogger(ProcessStatusTracker.class);

    ProcessStatusStore store;

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        ProcessStatusStore store =
                GeoServerExtensions.bean(ProcessStatusStore.class, applicationContext);
        if (store == null) {
            store = new MemoryProcessStatusStore();
        }

        this.store = store;
    }

    @Override
    public void submitted(ProcessEvent event) throws WPSException {
        if (store == null) {
            return;
        }

        store.save(event.getStatus());
    }

    /**
     * Custom method that updates the status last updated field without touching anything else, to
     * make sure we let the cluster know the process is still running
     */
    public void touch(String executionId) throws WPSException {
        ExecutionStatus status = store.get(executionId);
        if (status != null) {
            status.setLastUpdated(new Date());
            store.save(status);
        }
    }

    @Override
    public void succeeded(ProcessEvent event) throws WPSException {
        ExecutionStatus newStatus = event.getStatus();
        ExecutionStatus original = store.get(newStatus.getExecutionId());
        newStatus.setLastUpdated(new Date());
        store.save(newStatus);

        // update the status in the event to let the process know it has been cancelled
        if (original.getPhase() == ProcessState.DISMISSING) {
            event.getStatus().setPhase(ProcessState.FAILED);
        }
    }

    @Override
    public void dismissing(ProcessEvent event) throws WPSException {
        ExecutionStatus status = event.getStatus();
        status.setLastUpdated(new Date());
        store.save(status);
    }

    @Override
    public void dismissed(ProcessEvent event) throws WPSException {
        ExecutionStatus status = event.getStatus();
        store.remove(status.getExecutionId());
    }

    @Override
    public void failed(ProcessEvent event) {
        ExecutionStatus status = event.getStatus();
        status.setLastUpdated(new Date());
        store.save(status);
    }

    @Override
    public void progress(ProcessEvent event) throws WPSException {
        ExecutionStatus original = store.get(event.getStatus().getExecutionId());
        if (original.getPhase() == ProcessState.DISMISSING) {
            event.getStatus().setPhase(ProcessState.DISMISSING);
        } else {
            ExecutionStatus newStatus = event.getStatus();
            newStatus.setLastUpdated(new Date());
            store.save(newStatus);
        }
    }

    public ExecutionStatus getStatus(String executionId) {
        return store.get(executionId);
    }

    public void cleanExpiredStatuses(long expirationThreshold) {
        Date date = new Date(expirationThreshold);
        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
        Not completionTimenotNull = FF.not(FF.isNull(FF.property("completionTime")));
        Filter completionTimeExpired =
                FF.before(FF.property("completionTime"), FF.literal(format.format(date)));
        Filter completionTimeFilter = FF.and(completionTimenotNull, completionTimeExpired);
        Not lastUpdatedNotNull = FF.not(FF.isNull(FF.property("lastUpdated")));
        Filter lastUpdatedExpired =
                FF.before(FF.property("lastUpdated"), FF.literal(format.format(date)));
        Filter lastUpdatedFilter = FF.and(lastUpdatedNotNull, lastUpdatedExpired);
        And filter = FF.and(completionTimeFilter, lastUpdatedFilter);
        store.remove(filter);
    }

    public ProcessStatusStore getStore() {
        return store;
    }

    /**
     * Removes the execution status for the given id, and returns its value, if found, or null, if
     * not found
     */
    public ExecutionStatus remove(String executionId) {
        return store.remove(executionId);
    }

    @Override
    public int getPriority() {
        // we want status tracking to be the last bit in the status tracking chain,
        // to make sure that when a status changes, all other listener has done its job
        return ExtensionPriority.LOWEST;
    }
}
