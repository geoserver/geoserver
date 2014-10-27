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

import org.apache.commons.beanutils.BeanComparator;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geotools.data.Query;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * In memory implementation of the {@link ProcessStatusStore} interface
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class MemoryProcessStatusStore implements ProcessStatusStore {

    Map<String, ExecutionStatus> statuses = new ConcurrentHashMap<String, ExecutionStatus>();

    @Override
    public void save(ExecutionStatus status) {
        statuses.put(status.getExecutionId(), new ExecutionStatus(status));

    }

    @Override
    public int remove(Filter filter) {
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
        if (query.getStartIndex() != null && query.getStartIndex() > 0) {
            result = result.subList(query.getStartIndex(),
                    Math.min(result.size() - query.getStartIndex(), query.getMaxFeatures()));
        } else if (result.size() > query.getMaxFeatures()) {
            result = result.subList(0, query.getMaxFeatures());
        }

        return result;
    }

    @Override
    public ExecutionStatus get(String executionId) {
        return statuses.get(executionId);
    }

    private static class CompositeComparator<T> implements Comparator<T> {
        List<Comparator<T>> comparators;

        public CompositeComparator(List<Comparator<T>> comparators) {
            this.comparators = comparators;
        }

        @Override
        public int compare(T o1, T o2) {
            for (Comparator<T> comparator : comparators) {
                int result = comparator.compare(o1, o2);
                if (result != 0) {
                    return result;
                }
            }

            return 0;
        }
    }

}
