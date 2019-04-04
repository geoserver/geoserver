/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.hz;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.HazelcastInstanceAware;
import com.hazelcast.core.IMap;
import com.hazelcast.map.EntryBackupProcessor;
import com.hazelcast.map.EntryProcessor;
import com.hazelcast.query.PagingPredicate;
import com.hazelcast.query.Predicate;
import com.hazelcast.query.TruePredicate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.beanutils.BeanComparator;
import org.geoserver.wps.CompositeComparator;
import org.geoserver.wps.ProcessStatusStore;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessState;
import org.geotools.data.Query;
import org.geotools.filter.FilterCapabilities;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.filter.visitor.PostPreProcessFilterSplittingVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;
import org.opengis.filter.temporal.After;
import org.opengis.filter.temporal.Before;

/**
 * A Hazelcast based implementation of the {@link ProcessStatusStore} interface
 *
 * @author Andrea Aime - GeoSolutions
 */
public class HazelcastStatusStore implements ProcessStatusStore {

    static final Logger LOGGER = Logging.getLogger(HazelcastStatusStore.class);

    /** Name of the distributed map that will hold the process statuses */
    public static final String EXECUTION_STATUS_MAP = "wpsExecutionStatusMap";

    private static FilterCapabilities FILTER_CAPABILITIES;

    static {
        FILTER_CAPABILITIES = new FilterCapabilities();
        FILTER_CAPABILITIES.addAll(FilterCapabilities.LOGICAL_OPENGIS);
        FILTER_CAPABILITIES.addAll(FilterCapabilities.SIMPLE_COMPARISONS_OPENGIS);
        FILTER_CAPABILITIES.addType(FilterCapabilities.FID);
        FILTER_CAPABILITIES.addType(FilterCapabilities.BETWEEN);
        FILTER_CAPABILITIES.addType(FilterCapabilities.LIKE);
        FILTER_CAPABILITIES.addType(FilterCapabilities.NULL_CHECK);
        // temporal filters
        FILTER_CAPABILITIES.addType(After.class);
        FILTER_CAPABILITIES.addType(Before.class);
    }

    /** The distributed map holding the various statuses */
    IMap<String, ExecutionStatus> statuses;

    public HazelcastStatusStore(HazelcastLoader loader) {
        statuses = loader.getInstance().getMap(EXECUTION_STATUS_MAP);
    }

    @Override
    public void save(ExecutionStatus status) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Saving status " + status);
        }

        boolean succeded = false;

        // use optimistic locking to update the status, and check the phase transition is a valid
        // one
        while (!succeded) {
            ExecutionStatus oldStatus = statuses.get(status.getExecutionId());
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
                succeded =
                        statuses.replace(
                                status.getExecutionId(), oldStatus, new ExecutionStatus(status));
            } else {
                ExecutionStatus previous = statuses.put(status.getExecutionId(), status);
                succeded = previous == null;
            }
        }
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
    public int remove(Filter filter) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "Removing statuses matching: " + filter);
        }

        FilterPredicate filterPredicate = new FilterPredicate(filter);
        Predicate predicate = filterPredicate.predicate;
        Filter postFilter = filterPredicate.postFilter;

        Map<String, Object> results =
                statuses.executeOnEntries(new RemovingEntryProcessor(postFilter), predicate);
        int removedCount = results.size();
        return removedCount;
    }

    @Override
    public List<ExecutionStatus> list(Query query) {
        int maxFeatures = query.getMaxFeatures();
        int startIndex = query.getStartIndex() == null ? 0 : query.getStartIndex();
        boolean needsSorting = query.getSortBy() != null && query.getSortBy().length > 0;
        FilterPredicate filterPredicate = new FilterPredicate(query.getFilter());
        Predicate predicate = filterPredicate.predicate;
        Filter postFilter = filterPredicate.postFilter;

        // Two cases here: if we have post-filtering we are going to run an entry processor,
        // to filter in the cluster and accumulate, otherwise we are going to run the predicate
        // on the cluster and page if we need/can
        if (postFilter != null && postFilter != Filter.INCLUDE) {
            FilteringEntryProcessor filterProcessor = new FilteringEntryProcessor(postFilter);
            Map<String, Object> entries = statuses.executeOnEntries(filterProcessor);
            List<ExecutionStatus> result = new ArrayList(entries.values());
            return postProcessResults(query, maxFeatures, startIndex, needsSorting, result);
        } else {
            // see if we can handle paging. We do paging if we have the elements to do it and
            // the post filter
            if ((maxFeatures < Integer.MAX_VALUE || startIndex > 0) && postFilter != null) {
                // handle sorting during paging if possible
                Comparator<Map.Entry> pagingComparator = null;
                if (needsSorting) {
                    pagingComparator = getComparator("value.", query.getSortBy());
                }
                if (pagingComparator != null) {
                    predicate = new PagingPredicate(predicate, pagingComparator, maxFeatures);
                } else {
                    predicate = new PagingPredicate(predicate, maxFeatures);
                }
            }

            // are we actually have to page?
            ArrayList<ExecutionStatus> result = new ArrayList<>();
            if (predicate instanceof PagingPredicate) {
                int position = 0;
                PagingPredicate pp = (PagingPredicate) predicate;
                while (position < startIndex - maxFeatures) {
                    pp.nextPage();
                    position += maxFeatures;
                }

                List<ExecutionStatus> page = new ArrayList<>(statuses.values(pp));
                if (startIndex > position) {
                    page = page.subList(startIndex - position, page.size());
                }
                result.addAll(page);
                // we might not have got everything in the first page
                if (result.size() < maxFeatures) {
                    int missing = maxFeatures - result.size();
                    pp.nextPage();
                    List<ExecutionStatus> nextPage = new ArrayList<>(statuses.values(pp));
                    page = nextPage.subList(0, Math.min(missing, nextPage.size()));
                    result.addAll(page);
                }

                // early return, no more post processing needed
                return result;
            }
            // otherwise we load everything locally and post process as necessary
            result = new ArrayList<>(statuses.values(predicate));
            return postProcessResults(query, maxFeatures, startIndex, needsSorting, result);
        }
    }

    private List<ExecutionStatus> postProcessResults(
            Query query,
            int maxFeatures,
            int startIndex,
            boolean needsSorting,
            List<ExecutionStatus> result) {
        if (needsSorting) {
            Comparator<ExecutionStatus> comparator = getComparator("", query.getSortBy());
            Collections.sort(result, comparator);
        }
        if (startIndex > 0) {
            if (startIndex > result.size()) {
                result.clear();
            } else {
                result = new ArrayList<>(result.subList(startIndex, result.size()));
            }
        }
        if (maxFeatures < Integer.MAX_VALUE && maxFeatures < result.size()) {
            result = new ArrayList<>(result.subList(0, maxFeatures));
        }
        return result;
    }

    private Comparator getComparator(String prefix, SortBy[] sorts) {
        if (sorts == null || sorts.length == 0) {
            return null;
        }

        List<Comparator> comparators = new ArrayList<>();
        for (SortBy sort : sorts) {
            if (sort == SortBy.NATURAL_ORDER) {
                comparators.add(new BeanComparator(prefix + "creationTime"));
            } else if (sort == SortBy.REVERSE_ORDER) {
                comparators.add(
                        Collections.reverseOrder(new BeanComparator(prefix + "creationTime")));
            } else {
                String property = sort.getPropertyName().getPropertyName();
                Comparator comparator = new BeanComparator(prefix + property);
                if (sort.getSortOrder() == SortOrder.DESCENDING) {
                    comparator = Collections.reverseOrder(comparator);
                }
                comparators.add(comparator);
            }
        }

        if (comparators.size() > 1) {
            return new CompositeComparator(comparators);
        } else {
            return comparators.get(0);
        }
    }

    /**
     * Splits an OGC filter into a Hazelcast predicate and post-query Filter
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static class FilterPredicate {
        Filter postFilter;

        Predicate predicate;

        public FilterPredicate(Filter filter) {
            try {
                // split the filter
                PostPreProcessFilterSplittingVisitor splitter =
                        new PostPreProcessFilterSplittingVisitor(FILTER_CAPABILITIES, null, null);
                filter.accept(splitter, null);
                postFilter = splitter.getFilterPost();
                Filter preFilter = splitter.getFilterPre();
                // turn into a predicate
                if (preFilter == Filter.INCLUDE) {
                    predicate = TruePredicate.INSTANCE;
                } else {
                    FilterToCriteria transformer = new FilterToCriteria();
                    predicate =
                            (Predicate<String, ExecutionStatus>)
                                    preFilter.accept(transformer, null);
                }
            } catch (Exception e) {
                // the translation might not work since the predicate model is more limited than
                // the OGC filter one
                postFilter = filter;
                predicate = TruePredicate.INSTANCE;
            }
        }
    }

    /**
     * Base class for {@link EntryProcessor} that need to carry around a OGC filter, which is not
     * serializable
     */
    private abstract static class AbstractFilteringEntryProcessor
            implements EntryProcessor<String, ExecutionStatus> {
        private static final long serialVersionUID = -912785821605141531L;

        transient Filter filter;

        String cql;

        public AbstractFilteringEntryProcessor(Filter filter) {
            this.filter = filter;
            this.cql = ECQL.toCQL(filter);
        }

        @Override
        public EntryBackupProcessor<String, ExecutionStatus> getBackupProcessor() {
            return null;
        }

        protected Filter getFilter() {
            if (this.filter == null) {
                try {
                    this.filter = ECQL.toFilter(cql);
                } catch (CQLException e) {
                    throw new IllegalStateException("Invalid cql used to serialize filter: " + cql);
                }
            }
            return this.filter;
        }
    }

    /**
     * Evaluates a filter on map entries, and removes them on match
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static class RemovingEntryProcessor extends AbstractFilteringEntryProcessor
            implements HazelcastInstanceAware {

        private static final long serialVersionUID = 4683730449542722338L;
        transient IMap<String, ExecutionStatus> map;

        public RemovingEntryProcessor(Filter filter) {
            super(filter);
        }

        @Override
        public void setHazelcastInstance(HazelcastInstance instance) {
            map = instance.getMap(EXECUTION_STATUS_MAP);
        }

        @Override
        public Object process(Entry<String, ExecutionStatus> entry) {
            if (getFilter().evaluate(entry.getValue())) {
                map.remove(entry.getKey());
                return 1;
            }

            return null;
        }
    }

    /**
     * Evaluates a filter on map entries, and returns them
     *
     * @author Andrea Aime - GeoSolutions
     */
    private static class FilteringEntryProcessor extends AbstractFilteringEntryProcessor {

        private static final long serialVersionUID = 8952526293041673921L;

        public FilteringEntryProcessor(Filter filter) {
            super(filter);
        }

        @Override
        public Object process(Entry<String, ExecutionStatus> entry) {
            if (getFilter().evaluate(entry.getValue())) {
                return entry.getValue();
            }

            return null;
        }
    }

    @Override
    public boolean supportsPredicate() {
        //
        return true;
    }

    @Override
    public boolean supportsPaging() {

        return true;
    }
}
