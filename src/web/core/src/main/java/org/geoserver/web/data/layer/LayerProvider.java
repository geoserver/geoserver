/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * Provides a filtered, sorted view over the catalog layers.
 *
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
    static final Property<LayerInfo> TYPE = new BeanProperty<>("type", "type");

    static final Property<LayerInfo> STORE = new BeanProperty<>("store", "resource.store.name");

    static final Property<LayerInfo> NAME = new BeanProperty<>("name", "name");

    static final Property<LayerInfo> TITLE = new BeanProperty<>("title", "title");

    static final Property<LayerInfo> MODIFIED_TIMESTAMP =
            new BeanProperty<>("datemodfied", "dateModified");

    static final Property<LayerInfo> CREATED_TIMESTAMP =
            new BeanProperty<>("datecreated", "dateCreated");

    /**
     * A custom property that uses the derived enabled() property instead of isEnabled() to account
     * for disabled resource/store
     */
    static final Property<LayerInfo> ENABLED =
            new AbstractProperty<LayerInfo>("enabled") {

                public Boolean getPropertyValue(LayerInfo item) {
                    return Boolean.valueOf(item.enabled());
                }
            };

    static final Property<LayerInfo> SRS =
            new BeanProperty<LayerInfo>("SRS", "resource.SRS") {

                /**
                 * We roll a custom comparator that treats the numeric part of the code as a number
                 */
                public java.util.Comparator<LayerInfo> getComparator() {
                    return new Comparator<LayerInfo>() {

                        public int compare(LayerInfo o1, LayerInfo o2) {
                            // split out authority and code
                            String[] srs1 = o1.getResource().getSRS().split(":");
                            String[] srs2 = o2.getResource().getSRS().split(":");

                            // use sign to control sort order
                            if (srs1[0].equalsIgnoreCase(srs2[0])
                                    && srs1.length > 1
                                    && srs2.length > 1) {
                                try {
                                    // in case of same authority, compare numbers
                                    return Integer.valueOf(srs1[1])
                                            .compareTo(Integer.valueOf(srs2[1]));
                                } catch (NumberFormatException e) {
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

    static final List<Property<LayerInfo>> PROPERTIES =
            Arrays.asList(TYPE, TITLE, NAME, STORE, ENABLED, SRS); //

    @Override
    protected List<LayerInfo> getItems() {
        // forced to implement this method as its abstract in the super class
        throw new UnsupportedOperationException(
                "This method should not be being called! " + "We use the catalog streaming API");
    }

    @Override
    protected List<Property<LayerInfo>> getProperties() {
        List<Property<LayerInfo>> modifiedPropertiesList =
                PROPERTIES.stream().map(c -> c).collect(Collectors.toList());
        // check geoserver properties
        if (GeoServerApplication.get()
                .getGeoServer()
                .getSettings()
                .isShowCreatedTimeColumnsInAdminList())
            modifiedPropertiesList.add(CREATED_TIMESTAMP);
        if (GeoServerApplication.get()
                .getGeoServer()
                .getSettings()
                .isShowModifiedTimeColumnsInAdminList())
            modifiedPropertiesList.add(MODIFIED_TIMESTAMP);
        return modifiedPropertiesList;
    }

    @Override
    public IModel<LayerInfo> newModel(LayerInfo object) {
        return new LayerDetachableModel(object);
    }

    @Override
    protected Comparator<LayerInfo> getComparator(SortParam<?> sort) {
        return super.getComparator(sort);
    }

    @Override
    public long size() {
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
    public Iterator<LayerInfo> iterator(final long first, final long count) {
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
     * Returns the requested page of layer objects after applying any keyword filtering set on the
     * page
     */
    private Iterator<LayerInfo> filteredItems(Long first, Long count) {
        final Catalog catalog = getCatalog();

        // global sorting
        final SortParam<?> sort = getSort();
        final Property<LayerInfo> property = getProperty(sort);

        SortBy sortOrder = null;
        if (sort != null) {
            if (property instanceof BeanProperty) {
                final String sortProperty = ((BeanProperty<LayerInfo>) property).getPropertyPath();
                sortOrder = sortBy(sortProperty, sort.isAscending());
            } else if (property == ENABLED) {
                sortOrder = sortBy("enabled", sort.isAscending());
            }
        }
        if (first > Integer.MAX_VALUE
                || first < Integer.MIN_VALUE
                || count > Integer.MAX_VALUE
                || count < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(); // TODO Possibly change catalog API to use long
        }

        final Filter filter = getFilter();
        // our already filtered and closeable iterator
        Iterator<LayerInfo> items =
                catalog.list(
                        LayerInfo.class, filter, first.intValue(), count.intValue(), sortOrder);

        return items;
    }
}
