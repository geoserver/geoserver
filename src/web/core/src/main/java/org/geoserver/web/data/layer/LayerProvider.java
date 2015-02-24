/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.geoserver.catalog.Predicates.acceptAll;
import static org.geoserver.catalog.Predicates.or;
import static org.geoserver.catalog.Predicates.sortBy;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

import com.google.common.collect.Lists;

/**
 * Provides a filtered, sorted view over the catalog layers.
 * <p>
 * <!-- Implementation detail: This class overrides the following methods in
 * order to leverage the Catalog filtering and paging support:
 * <ul>
 * <li> {@link #size()}: in order to call {@link Catalog#count(Class, Filter)}
 * with any filter criteria set on the page
 * <li> {@link #fullSize()}: in order to call
 * {@link Catalog#count(Class, Filter)} with {@link Predicates#acceptAll()}
 * <li>{@link #iterator}: in order to ask the catalog for paged and sorted
 * contents directly through
 * {@link Catalog#list(Class, Filter, Integer, Integer, SortBy)}
 * <li> {@link #getItems()} throws an unsupported operation exception, as given
 * the above it should not be called
 * </ul>
 * -->
 * 
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class LayerProvider extends GeoServerDataProvider<LayerInfo> {
    static final Property<LayerInfo> TYPE = new BeanProperty<LayerInfo>("type",
            "type");

    static final Property<LayerInfo> WORKSPACE = new BeanProperty<LayerInfo>(
            "workspace", "resource.store.workspace.name");

    static final Property<LayerInfo> STORE = new BeanProperty<LayerInfo>(
            "store", "resource.store.name");

    static final Property<LayerInfo> NAME = new BeanProperty<LayerInfo>("name",
            "name");

    /**
     * A custom property that uses the derived enabled() property instead of isEnabled() to account
     * for disabled resource/store
     */
    static final Property<LayerInfo> ENABLED = new AbstractProperty<LayerInfo>("enabled") {

        public Boolean getPropertyValue(LayerInfo item) {
            return Boolean.valueOf(item.enabled());
        }

    };

    static final Property<LayerInfo> SRS = new BeanProperty<LayerInfo>("SRS",
            "resource.SRS") {

        /**
         * We roll a custom comparator that treats the numeric part of the
         * code as a number
         */
        public java.util.Comparator<LayerInfo> getComparator() {
            return new Comparator<LayerInfo>() {

                public int compare(LayerInfo o1, LayerInfo o2) {
                    // split out authority and code
                    String[] srs1 = o1.getResource().getSRS().split(":");
                    String[] srs2 = o2.getResource().getSRS().split(":");

                    // use sign to control sort order
                    if (srs1[0].equalsIgnoreCase(srs2[0]) && srs1.length > 1
                            && srs2.length > 1) {
                        try {
                            // in case of same authority, compare numbers
                            return new Integer(srs1[1]).compareTo(new Integer(
                                    srs2[1]));
                        } catch(NumberFormatException e) {
                            // a handful of codes are not numeric,
                            // handle the general case as well
                            return srs1[1].compareTo(srs2[1]);
                        }
                    } else {
                        // compare authorities
                        return srs1[0].compareToIgnoreCase(srs2[0]);
                    }
                }

            };

        }
    };
    
    static final List<Property<LayerInfo>> PROPERTIES = Arrays.asList(TYPE,
            WORKSPACE, STORE, NAME, ENABLED, SRS);

    @Override
    protected List<LayerInfo> getItems() {
        // forced to implement this method as its abstract in the super class
        throw new UnsupportedOperationException(
                "This method should not be being called! "
                        + "We use the catalog streaming API");
    }

    @Override
    protected List<Property<LayerInfo>> getProperties() {
        return PROPERTIES;
    }

    public IModel newModel(Object object) {
        return new LayerDetachableModel((LayerInfo) object);
    }

    @Override
    protected Comparator<LayerInfo> getComparator(SortParam sort) {
        return super.getComparator(sort);
    }

    @Override
    public int size() {
        Filter filter = getFilter();
        int count = getCatalog().count(LayerInfo.class, filter);
        return count;
    }

    @Override
    public int fullSize() {
        Filter filter = Predicates.acceptAll();
        int count = getCatalog().count(LayerInfo.class, filter);
        return count;
    }
    
    @Override
    public Iterator<LayerInfo> iterator(final int first, final int count) {
        Iterator<LayerInfo> iterator = filteredItems(first, count);
        if (iterator instanceof CloseableIterator) {
            // don't know how to force wicket to close the iterator, lets return
            // a copy. Shouldn't be much overhead as we're paging
            try {
                return Lists.newArrayList(iterator).iterator();
            } finally {
                CloseableIteratorAdapter.close(iterator);
            }
        } else {
            return iterator;
        }
    }

    /**
     * Returns the requested page of layer objects after applying any keyword
     * filtering set on the page
     */
    private Iterator<LayerInfo> filteredItems(Integer first, Integer count) {
        final Catalog catalog = getCatalog();

        // global sorting
        final SortParam sort = getSort();
        final Property<LayerInfo> property = getProperty(sort);

        SortBy sortOrder = null;
        if (sort != null) {
            if(property instanceof BeanProperty){
                final String sortProperty = ((BeanProperty<LayerInfo>)property).getPropertyPath();
                sortOrder = sortBy(sortProperty, sort.isAscending());
            }else if(property == ENABLED){
                sortOrder = sortBy("enabled", sort.isAscending());
            }
        }

        final Filter filter = getFilter();
        //our already filtered and closeable iterator
        Iterator<LayerInfo> items = catalog.list(LayerInfo.class, filter, first, count, sortOrder);

        return items;
    }

    private Filter getFilter() {
        final String[] keywords = getKeywords();
        Filter filter = acceptAll();
        if (null != keywords) {
            for (String keyword : keywords) {
                Filter propContains = Predicates.fullTextSearch(keyword);
                // chain the filters together
                if (Filter.INCLUDE == filter) {
                    filter = propContains;
                } else {
                    filter = or(filter, propContains);
                }
            }
        }
        return filter;
    }
}
