/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.collect.Streams;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;
import org.springframework.lang.Nullable;

/**
 * {@link GeoServerDataProvider} providing a table model for listing {@link LayerGroupInfo layer groups} available in
 * the {@link Catalog}.
 *
 * @implNote This class overrides the following methods in order to leverage the Catalog filtering and paging support:
 *     <ul>
 *       <li>{@link #size()}: in order to call {@link Catalog#count(Class, Filter)} with any filter criteria set on the
 *           page
 *       <li>{@link #fullSize()}: in order to call {@link Catalog#count(Class, Filter)} with
 *           {@link Predicates#acceptAll()}
 *       <li>{@link #iterator}: in order to ask the catalog for paged and sorted contents directly through
 *           {@link Catalog#list(Class, Filter, Integer, Integer, SortBy)}
 *       <li>{@link #getItems()} throws an unsupported operation exception, as given the above it should not be called
 *     </ul>
 */
public class LayerGroupProvider extends GeoServerDataProvider<LayerGroupInfo> {

    private static final long serialVersionUID = 4806818198949114395L;

    public static Property<LayerGroupInfo> NAME = new BeanProperty<>("name", "name");

    public static Property<LayerGroupInfo> WORKSPACE = new BeanProperty<>("workspace", "workspace.name");

    static final Property<LayerGroupInfo> MODIFIED_TIMESTAMP = new BeanProperty<>("datemodfied", "dateModified");

    static final Property<LayerGroupInfo> CREATED_TIMESTAMP = new BeanProperty<>("datecreated", "dateCreated");

    public static Property<LayerGroupInfo> ENABLED = new BeanProperty<>("enabled", "enabled");

    static List<Property<LayerGroupInfo>> PROPERTIES = Arrays.asList(NAME, WORKSPACE, ENABLED);

    @Override
    public long size() {
        return count(getFilter());
    }

    @Override
    public int fullSize() {
        return count(Predicates.acceptAll());
    }

    private int count(Filter filter) {
        return getCatalog().count(LayerGroupInfo.class, filter);
    }

    @Override
    public Iterator<LayerGroupInfo> iterator(final long first, final long count) {
        SortBy sortOrder = getSortOrder();
        Filter filter = getFilter();
        try (Stream<LayerGroupInfo> items = query(filter, (int) first, (int) count, sortOrder)) {
            return items.collect(Collectors.toList()).iterator();
        }
    }

    /**
     * This method shouldn't be called at all due to the overloading of {@link #size()}, {@link #fullSize()}, and
     * {@link #iterator(long, long)}
     */
    @Override
    protected List<LayerGroupInfo> getItems() {
        // forced to implement this method as its abstract in the super class
        throw new UnsupportedOperationException(
                "This method should not be being called! We use the catalog streaming API");
    }

    @Override
    protected List<Property<LayerGroupInfo>> getProperties() {
        List<Property<LayerGroupInfo>> modifiedPropertiesList =
                PROPERTIES.stream().map(c -> c).collect(Collectors.toList());
        // check geoserver properties
        if (GeoServerApplication.get().getGeoServer().getSettings().isShowCreatedTimeColumnsInAdminList())
            modifiedPropertiesList.add(CREATED_TIMESTAMP);
        if (GeoServerApplication.get().getGeoServer().getSettings().isShowModifiedTimeColumnsInAdminList())
            modifiedPropertiesList.add(MODIFIED_TIMESTAMP);
        return modifiedPropertiesList;
    }

    @Override
    public IModel<LayerGroupInfo> newModel(LayerGroupInfo object) {
        return new LayerGroupDetachableModel(object);
    }

    /**
     * Query the {@link Catalog#list(Class, Filter, Integer, Integer, SortBy)} streaming API and adapt it to a
     * {@link Stream}; note {@link Stream} is {@link AutoCloseable} and hence the returned stream shall be used in a
     * try-with-resources block.
     */
    @SuppressWarnings("PMD.CloseResource")
    private Stream<LayerGroupInfo> query(
            Filter filter, @Nullable Integer first, @Nullable Integer count, @Nullable SortBy sortOrder) {
        Catalog catalog = getCatalog();

        CloseableIterator<LayerGroupInfo> items = catalog.list(LayerGroupInfo.class, filter, first, count, sortOrder);

        Stream<LayerGroupInfo> stream = Streams.stream(items);
        return stream.onClose(items::close);
    }

    private SortBy getSortOrder() {
        SortParam<Object> sort = getSort();
        if (null == sort) {
            sort = new SortParam<>(NAME.getName(), true);
        }

        Property<LayerGroupInfo> property = getProperty(sort);
        SortBy sortOrder = null;
        if (property instanceof BeanProperty) {
            String sortProperty = ((BeanProperty<LayerGroupInfo>) property).getPropertyPath();
            sortOrder = sortBy(sortProperty, sort.isAscending());
        } else if (null != property) {
            throw new IllegalStateException("Unknown sort property " + property);
        }
        return sortOrder;
    }
}
