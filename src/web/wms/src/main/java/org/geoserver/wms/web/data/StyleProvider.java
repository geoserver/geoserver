/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/** A {@link GeoServerDataProvider} provider for styles */
@SuppressWarnings("serial")
public class StyleProvider extends GeoServerDataProvider<StyleInfo> {

    public static Property<StyleInfo> NAME = new BeanProperty<StyleInfo>("name", "name");

    public static Property<StyleInfo> WORKSPACE =
            new BeanProperty<StyleInfo>("workspace", "workspace.name");

    static List<Property<StyleInfo>> PROPERTIES = Arrays.asList(NAME, WORKSPACE);

    public StyleProvider() {
        setSort(new SortParam<Object>(NAME.getName(), true));
    }

    @Override
    protected List<StyleInfo> getItems() {
        throw new UnsupportedOperationException(
                "This method should not be being called! " + "We use the catalog streaming API");
    }

    @Override
    protected List<Property<StyleInfo>> getProperties() {
        return PROPERTIES;
    }

    public IModel<StyleInfo> newModel(StyleInfo object) {
        return new StyleDetachableModel(object);
    }

    @Override
    public long size() {
        Filter filter = getFilter();
        int count = getCatalog().count(StyleInfo.class, filter);
        return count;
    }

    @Override
    public int fullSize() {
        Filter filter = Predicates.acceptAll();
        int count = getCatalog().count(StyleInfo.class, filter);
        return count;
    }

    @Override
    public Iterator<StyleInfo> iterator(final long first, final long count) {
        Iterator<StyleInfo> iterator = filteredItems((int) first, (int) count);
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
    private Iterator<StyleInfo> filteredItems(Integer first, Integer count) {
        final Catalog catalog = getCatalog();

        // global sorting
        final SortParam sort = getSort();
        final Property<StyleInfo> property = getProperty(sort);

        SortBy sortOrder = null;
        if (sort != null) {
            if (property instanceof BeanProperty) {
                final String sortProperty = ((BeanProperty<StyleInfo>) property).getPropertyPath();
                sortOrder = sortBy(sortProperty, sort.isAscending());
            }
        }

        final Filter filter = getFilter();
        // our already filtered and closeable iterator
        Iterator<StyleInfo> items = catalog.list(StyleInfo.class, filter, first, count, sortOrder);

        return items;
    }
}
