/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import static org.geoserver.catalog.Predicates.sortBy;

import com.google.common.collect.Streams;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.util.lang.Objects;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.SettingsInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;

/**
 * {@link GeoServerDataProvider} for the list of workspaces available in the {@link Catalog}
 *
 * @implNote This class overrides the following methods in order to leverage the Catalog filtering
 *     and paging support:
 *     <ul>
 *       <li>{@link #size()}: in order to call {@link Catalog#count(Class, Filter)} with any filter
 *           criteria set on the page
 *       <li>{@link #fullSize()}: in order to call {@link Catalog#count(Class, Filter)} with {@link
 *           Predicates#acceptAll()}
 *       <li>{@link #iterator}: in order to ask the catalog for paged and sorted contents directly
 *           through {@link Catalog#list(Class, Filter, Integer, Integer, SortBy)}
 *       <li>{@link #getItems()} throws an unsupported operation exception, as given the above it
 *           should not be called
 *     </ul>
 */
public class WorkspaceProvider extends GeoServerDataProvider<WorkspaceInfo> {

    private static final long serialVersionUID = -2464073552094977958L;

    public static Property<WorkspaceInfo> NAME = new BeanProperty<>("name", "name");

    /**
     * "Default" is not a {@link WorkspaceInfo} attribute, this property relies on {@link
     * #iterator()} decorating the default workspace, so {@link Catalog#getDefaultWorkspace()}
     * doesn't need to be called for each item.
     *
     * @see #decorateDefault(WorkspaceInfo, WorkspaceInfo)
     * @see #isDefaultWorkspace(WorkspaceInfo)
     */
    public static Property<WorkspaceInfo> DEFAULT =
            new AbstractProperty<WorkspaceInfo>("default") {

                private static final long serialVersionUID = 7732697329315316826L;

                @Override
                public Object getPropertyValue(WorkspaceInfo item) {
                    return isDefaultWorkspace(item);
                }
            };

    public static Property<WorkspaceInfo> ISOLATED = new BeanProperty<>("isolated", "isolated");

    static List<Property<WorkspaceInfo>> PROPERTIES = List.of(NAME, DEFAULT, ISOLATED);

    public static final Property<WorkspaceInfo> MODIFIED_TIMESTAMP =
            new BeanProperty<>("datemodfied", "dateModified");

    public static final Property<WorkspaceInfo> CREATED_TIMESTAMP =
            new BeanProperty<>("datecreated", "dateCreated");

    public WorkspaceProvider() {
        setSort(NAME.getName(), SortOrder.ASCENDING);
    }

    @Override
    protected List<WorkspaceInfo> getItems() {
        // forced to implement this method as its abstract in the super class
        throw new UnsupportedOperationException(
                "This method should not be being called! We use the catalog streaming API");
    }

    @Override
    public long size() {
        Filter filter = getFilter();
        int count = getCatalog().count(WorkspaceInfo.class, filter);
        return count;
    }

    @Override
    public int fullSize() {
        Filter filter = Predicates.acceptAll();
        int count = getCatalog().count(WorkspaceInfo.class, filter);
        return count;
    }

    @Override
    public Iterator<WorkspaceInfo> iterator(final long first, final long count) {
        validate(first, count);
        final Catalog catalog = getCatalog();

        // global sorting
        final SortParam<Object> sort = getSort();
        final Property<WorkspaceInfo> property = getProperty(sort);
        Filter filter = getFilter();
        WorkspaceInfo defaultWorkspace = catalog.getDefaultWorkspace();
        SortBy sortOrder = null;
        if (property instanceof BeanProperty) {
            String sortProperty = ((BeanProperty<WorkspaceInfo>) property).getPropertyPath();
            sortOrder = sortBy(sortProperty, sort.isAscending());
        } else if (property == DEFAULT) {
            // "default" is not a WorkspaceInfo property
            if (null != defaultWorkspace && filter.evaluate(defaultWorkspace)) {
                // filter out default workspace
                Filter excludeDefault = Predicates.notEqual("id", defaultWorkspace.getId());
                filter = Predicates.and(excludeDefault, filter);
                // and add it to the head or tail as appropriate below
            } else {
                defaultWorkspace = null;
            }
        } else if (null != property) {
            throw new IllegalStateException("Unknown sort property " + property);
        }

        LinkedList<WorkspaceInfo> list;
        try (CloseableIterator<WorkspaceInfo> items =
                catalog.list(WorkspaceInfo.class, filter, (int) first, (int) count, sortOrder)) {
            Stream<WorkspaceInfo> stream = Streams.stream(items);
            if (null != defaultWorkspace) {
                WorkspaceInfo def = defaultWorkspace;
                stream = stream.map(item -> decorateDefault(def, item));
            }
            list = stream.collect(Collectors.toCollection(LinkedList::new));
        }
        if (property == DEFAULT && defaultWorkspace != null) {
            // and add it to the head or tail as appropriate
            if (sort.isAscending()) {
                list.addFirst(defaultWorkspace);
            } else {
                list.addLast(defaultWorkspace);
            }
        }
        return list.iterator();
    }

    @Override
    protected List<Property<WorkspaceInfo>> getProperties() {
        List<Property<WorkspaceInfo>> modifiedPropertiesList = new ArrayList<>(PROPERTIES);
        // check geoserver properties
        SettingsInfo settings = getSettings();
        if (settings.isShowCreatedTimeColumnsInAdminList())
            modifiedPropertiesList.add(CREATED_TIMESTAMP);
        if (settings.isShowModifiedTimeColumnsInAdminList())
            modifiedPropertiesList.add(MODIFIED_TIMESTAMP);
        return modifiedPropertiesList;
    }

    protected SettingsInfo getSettings() {
        return GeoServerApplication.get().getGeoServer().getSettings();
    }

    @Override
    protected IModel<WorkspaceInfo> newModel(WorkspaceInfo object) {
        return new WorkspaceDetachableModel(object);
    }

    private void validate(Long first, Long count) {
        if (first > Integer.MAX_VALUE
                || first < Integer.MIN_VALUE
                || count > Integer.MAX_VALUE
                || count < Integer.MIN_VALUE) {
            throw new IllegalArgumentException(); // TODO Possibly change catalog API to use long
        }
    }

    /**
     * Uses the items metadata map to mark it as the default workspace if {@code item} is the same
     * as {@code defaultWorkspace}
     *
     * @param defaultWorkspace the catalog's default workspace
     * @param item the item to mark as the default workspace or not, for the {@link #DEFAULT}
     *     property to get the property value
     */
    static WorkspaceInfo decorateDefault(WorkspaceInfo defaultWorkspace, WorkspaceInfo item) {
        if (Objects.equal(defaultWorkspace.getId(), item.getId())) {
            item = ModificationProxy.create(ModificationProxy.unwrap(item), WorkspaceInfo.class);
            item.getMetadata().put("defaultWorkspace", Boolean.TRUE);
        }
        return item;
    }

    static boolean isDefaultWorkspace(WorkspaceInfo item) {
        return Boolean.TRUE.equals(item.getMetadata().get("defaultWorkspace"));
    }
}
