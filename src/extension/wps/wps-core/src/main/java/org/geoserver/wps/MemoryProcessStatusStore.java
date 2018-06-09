/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.BeanComparator;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geotools.data.Query;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * In memory implementation of the {@link ProcessStatusStore} interface
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MemoryProcessStatusStore implements ProcessStatusStore {

    static final Logger LOGGER = Logging.getLogger(MemoryProcessStatusStore.class);

    Map<String, ExecutionStatus> statuses = new ConcurrentHashMap<String, ExecutionStatus>();

    @Override
    public void save(ExecutionStatus status) {
        boolean succeded = false;

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Saving status " + status);
        }

        // use optimistic locking to update the status, and check the phase transition is a valid
        // one
        while (!succeded) {
            ExecutionStatus oldStatus = statuses.get(status.getExecutionId());
            ExecutionStatus newStatus = new ExecutionStatus(status);
            if (oldStatus != null) {
                ProcessState previousPhase = oldStatus.getPhase();
                ProcessState currPhase = status.getPhase();
                if (!currPhase.isValidSuccessor(previousPhase)) {
                    throw new WPSException(
                            "Cannot switch process status from "
                                    + previousPhase
                                    + " to "
                                    + currPhase);
                }
                ExecutionStatus prevInMap = statuses.put(status.getExecutionId(), newStatus);
                succeded = prevInMap == oldStatus;
            } else {
                ExecutionStatus previous = statuses.put(status.getExecutionId(), newStatus);
                succeded = previous == null;
            }
        }
    }

    @Override
    public int remove(Filter filter) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Removing statuses matching " + filter);
        }

        int count = 0;
        for (ExecutionStatus status : statuses.values()) {
            if (filter.evaluate(status)) {
                count++;
                statuses.remove(status.getExecutionId());
            }
        }

        return count;
    }

    @Override
    public List<ExecutionStatus> list(Query query) {
        List<ExecutionStatus> result = new ArrayList<>();

        // extract and filter
        Filter filter = query.getFilter();
        for (ExecutionStatus status : statuses.values()) {
            if (filter.evaluate(status)) {
                result.add(status);
            }
        }

        // sort
        SortBy[] sorts = query.getSortBy();
        if (sorts != null) {
            List<Comparator<ExecutionStatus>> comparators = new ArrayList<>();
            for (SortBy sort : sorts) {
                if (sort == SortBy.NATURAL_ORDER) {
                    comparators.add(new BeanComparator("creationTime"));
                } else if (sort == SortBy.REVERSE_ORDER) {
                    comparators.add(Collections.reverseOrder(new BeanComparator("creationTime")));
                } else {
                    String property = sort.getPropertyName().getPropertyName();
                    // map property to ExecutionStatus values
                    if ("node".equalsIgnoreCase(property)) {
                        property = "nodeId";
                    } else if ("user".equalsIgnoreCase(property)) {
                        property = "userName";
                    } else if ("task".equalsIgnoreCase(property)) {
                        property = "task";
                    }
                    Comparator<ExecutionStatus> comparator = new BeanComparator(property);
                    if (sort.getSortOrder() == SortOrder.DESCENDING) {
                        comparator = Collections.reverseOrder(comparator);
                    }
                    comparators.add(comparator);
                }
            }

            if (comparators.size() > 1) {
                Comparator<ExecutionStatus> comparator = new CompositeComparator<>(comparators);
                Collections.sort(result, comparator);
            } else if (comparators.size() == 1) {
                Collections.sort(result, comparators.get(0));
            }
        }

        // paging
        Integer startIndex = query.getStartIndex();
        if (startIndex != null && startIndex > 0) {
            if (startIndex > result.size()) {
                result.clear();
            } else {
                result = result.subList(startIndex, result.size());
            }
        }
        if (result.size() > query.getMaxFeatures()) {
            result = result.subList(0, query.getMaxFeatures());
        }

        return result;
    }

    @Override
    public ExecutionStatus get(String executionId) {
        return statuses.get(executionId);
    }

    @Override
    public ExecutionStatus remove(String executionId) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Removing status for execution id: " + executionId);
        }

        return statuses.remove(executionId);
    }

    @Override
    public boolean supportsPredicate() {
        //
        return true;
    }

    @Override
    public boolean supportsPaging() {
        return false;
    }
}
