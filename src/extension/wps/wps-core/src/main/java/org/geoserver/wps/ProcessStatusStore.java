/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import java.util.List;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geotools.data.Query;
import org.opengis.filter.Filter;

/**
 * Stores and allows retrieval of execution status information for the various running and recently
 * completed processes
 */
public interface ProcessStatusStore {
    /** Saves or updates a given process status */
    void save(ExecutionStatus status);

    /** Retrieves a specific status by id */
    ExecutionStatus get(String executionId);

    /** Removes a specific status by id */
    ExecutionStatus remove(String executionId);

    /**
     * Clears process statuses matching a certain condition, and returns the number of statuses
     * removed
     */
    int remove(Filter filter);

    /**
     * Retrieves process statuses based on the given conditions. The store should honor the filter,
     * sorting and paging conditions, ignoring the others
     */
    List<ExecutionStatus> list(Query query);

    /**
     * Does the underlying store support the use of Predicates like FullText
     *
     * @return true if FULLTEXT searches are supported.
     */
    boolean supportsPredicate();

    /**
     * Does the underlying store support Paging
     *
     * @return true if Paging in searches is supported.
     */
    boolean supportsPaging();
}
