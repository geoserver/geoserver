/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.web;

import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.catalog.Predicates.or;
import static org.geoserver.catalog.Predicates.sortBy;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.wps.ProcessStatusStore;
import org.geoserver.wps.executor.ExecutionStatus;
import org.geoserver.wps.executor.ProcessStatusTracker;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.sort.SortBy;

/**
 * Provides a filtered, sorted view over the running/recently completed processes
 *
 * @author Andrea Aime - GeoSolutions
 */
@SuppressWarnings("serial")
public class ProcessStatusProvider extends GeoServerDataProvider<ExecutionStatus> {
    private static Logger LOGGER = Logging.getLogger(ProcessStatusProvider.class);
    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    static final Property<ExecutionStatus> TYPE =
            new AbstractProperty<ExecutionStatus>("type") {
                @Override
                public Object getPropertyValue(ExecutionStatus item) {
                    // we might want to have a "C" state for the chained processes
                    return item.isAsynchronous() ? "A" : "S";
                }

                @Override
                public boolean isSearchable() {
                    // when really it isn't sortable or searchable
                    return false;
                }
            };

    static final Property<ExecutionStatus> NODE =
            new BeanProperty<ExecutionStatus>("node", "nodeId");

    static final Property<ExecutionStatus> USER =
            new BeanProperty<ExecutionStatus>("user", "userName");

    static final Property<ExecutionStatus> PROCESS =
            new BeanProperty<ExecutionStatus>("processName", "processName");

    static final Property<ExecutionStatus> CREATED =
            new BeanProperty<ExecutionStatus>("creationTime", "creationTime");

    static final Property<ExecutionStatus> PHASE =
            new BeanProperty<ExecutionStatus>("phase", "phase");

    static final Property<ExecutionStatus> PROGRESS =
            new BeanProperty<ExecutionStatus>("progress", "progress");

    static final Property<ExecutionStatus> EXPIRATION =
            new BeanProperty<ExecutionStatus>("expirationDate", "expirationDate");

    static final Property<ExecutionStatus> COMPLETION =
            new BeanProperty<ExecutionStatus>("estimatedCompletion", "estimatedCompletion");

    static final Property<ExecutionStatus> NEXT_POLL =
            new BeanProperty<ExecutionStatus>("nextPoll", "nextPoll");

    static final Property<ExecutionStatus> TASK = new BeanProperty<ExecutionStatus>("task", "task");

    static final List<Property<ExecutionStatus>> PROPERTIES =
            Arrays.asList(
                    TYPE,
                    NODE,
                    USER,
                    PROCESS,
                    CREATED,
                    PHASE,
                    PROGRESS,
                    EXPIRATION,
                    COMPLETION,
                    NEXT_POLL,
                    TASK);

    private long first;

    private long count;

    @Override
    protected List<ExecutionStatus> getItems() {
        ProcessStatusTracker tracker =
                GeoServerApplication.get().getBeanOfType(ProcessStatusTracker.class);
        return tracker.getStore().list(Query.ALL);
    }

    @Override
    protected List<Property<ExecutionStatus>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected Filter getFilter() {
        final String[] keywords = getKeywords();
        Filter filter = acceptAll();
        if (null != keywords) {
            Filter propContains = getFullSearch(keywords);
            // chain the filters together
            if (Filter.INCLUDE == filter) {
                filter = propContains;
            } else {
                filter = or(filter, propContains);
            }
        }

        return filter;
    }

    private Filter getFullSearch(String[] keywords) {
        ProcessStatusTracker tracker =
                GeoServerApplication.get().getBeanOfType(ProcessStatusTracker.class);
        ProcessStatusStore store = tracker.getStore();
        Filter ret = Filter.INCLUDE;
        if (store.supportsPredicate()) {
            for (String keyword : keywords) {
                Filter propContains = Predicates.fullTextSearch(keyword);
                // chain the filters together
                if (Filter.INCLUDE == ret) {
                    ret = propContains;
                } else {
                    ret = or(ret, propContains);
                }
            }
        } else {
            if (keywords.length > 0) {
                List<Filter> likes = new ArrayList<Filter>();
                for (String word : keywords) {
                    for (Property<?> prop : getProperties()) {
                        if (prop.isSearchable()) {
                            if (prop.equals(NODE)
                                    || prop.equals(PHASE)
                                    || prop.equals(TASK)
                                    || prop.equals(USER)
                                    || prop.equals(PROCESS)) {
                                likes.add(FF.like(FF.property(prop.getName()), "*" + word + "*"));
                            }
                            // TODO: support temporal properties if I can work out what searching
                            // means

                        }
                    }
                }
                ret = FF.or(likes);
            }
        }
        return ret;
    }

    @Override
    protected List<ExecutionStatus> getFilteredItems() {
        ProcessStatusTracker tracker =
                GeoServerApplication.get().getBeanOfType(ProcessStatusTracker.class);
        ProcessStatusStore store = tracker.getStore();
        Query query = new Query("status", getFilter());
        if (count > 0) {
            query.setStartIndex((int) first);
            query.setMaxFeatures((int) count);
        }

        final SortParam sort = getSort();

        SortBy sortOrder = null;
        if (sort != null) {
            final Property<?> property = getProperty(sort);
            if (property.isSearchable()) { // we really need another flag
                final String sortProperty = ((BeanProperty<StoreInfo>) property).getPropertyPath();
                sortOrder = sortBy(sortProperty, sort.isAscending());
                query.setSortBy(new SortBy[] {sortOrder});
            }
        }
        LOGGER.fine("built query " + query + " to filter statuses");
        return store.list(query);
    }

    @Override
    public Iterator<ExecutionStatus> iterator(long first, long count) {
        this.first = first;
        this.count = count;
        Iterator<ExecutionStatus> it = super.iterator(first, count);
        this.first = 0;
        this.count = -1;
        return it;
    }
}
