/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.geoserver.catalog.Predicates.sortBy;
import static org.geoserver.config.CatalogModificationUserUpdater.TRACK_USER;

import com.google.common.collect.Lists;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.SettingsInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.style.StyleDetachableModel;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.sort.SortBy;

/** A {@link GeoServerDataProvider} provider for styles */
@SuppressWarnings("serial")
public class StyleProvider extends GeoServerDataProvider<StyleInfo> {

    public static Property<StyleInfo> NAME = new BeanProperty<>("name", "name");

    public static Property<StyleInfo> WORKSPACE = new BeanProperty<>("workspace", "workspace.name");

    static final Property<StyleInfo> MODIFIED_TIMESTAMP = new BeanProperty<>("datemodfied", "dateModified");

    static final Property<StyleInfo> CREATED_TIMESTAMP = new BeanProperty<>("datecreated", "dateCreated");

    static final Property<StyleInfo> MODIFIED_BY = new BeanProperty<>("modifiedby", "modifiedBy");

    static final Property<StyleInfo> FORMAT = new BeanProperty<>("format", "format");

    static final Property<StyleInfo> FORMAT_VERSION = new BeanProperty<>("formatversion", "formatVersion");

    static List<Property<StyleInfo>> PROPERTIES = Arrays.asList(NAME, FORMAT, WORKSPACE);

    public StyleProvider() {
        setSort(new SortParam<>(NAME.getName(), true));
    }

    @Override
    protected List<StyleInfo> getItems() {
        throw new UnsupportedOperationException(
                "This method should not be being called! " + "We use the catalog streaming API");
    }

    @Override
    protected List<Property<StyleInfo>> getProperties() {
        List<Property<StyleInfo>> modifiedPropertiesList =
                PROPERTIES.stream().map(c -> c).collect(Collectors.toList());
        // check geoserver properties
        SettingsInfo settings = GeoServerApplication.get().getGeoServer().getSettings();
        if (settings.isShowCreatedTimeColumnsInAdminList()) modifiedPropertiesList.add(CREATED_TIMESTAMP);
        if (settings.isShowModifiedTimeColumnsInAdminList()) modifiedPropertiesList.add(MODIFIED_TIMESTAMP);
        String trackUser = GeoServerExtensions.getProperty(TRACK_USER);
        if (trackUser == null
                        && GeoServerApplication.get()
                                .getGeoServer()
                                .getSettings()
                                .isShowModifiedUserInAdminList()
                || Boolean.parseBoolean(trackUser)) modifiedPropertiesList.add(MODIFIED_BY);
        return modifiedPropertiesList;
    }

    @Override
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
        try (CloseableIterator<StyleInfo> iterator = filteredItems((int) first, (int) count)) {
            // don't know how to force wicket to close the iterator, lets return
            // a copy. Shouldn't be much overhead as we're paging
            return Lists.newArrayList(iterator).iterator();
        }
    }

    /** Returns the requested page of layer objects after applying any keyword filtering set on the page */
    private CloseableIterator<StyleInfo> filteredItems(Integer first, Integer count) {
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
        return catalog.list(StyleInfo.class, filter, first, count, sortOrder);
    }
}
