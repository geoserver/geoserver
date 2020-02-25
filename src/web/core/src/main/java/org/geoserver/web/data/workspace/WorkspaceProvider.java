/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/** {@link GeoServerDataProvider} for the list of workspaces available in the {@link Catalog} */
public class WorkspaceProvider extends GeoServerDataProvider<WorkspaceInfo> {

    private static final long serialVersionUID = -2464073552094977958L;

    public static Property<WorkspaceInfo> NAME = new BeanProperty<WorkspaceInfo>("name", "name");

    public static Property<WorkspaceInfo> DEFAULT =
            new AbstractProperty<WorkspaceInfo>("default") {

                private static final long serialVersionUID = 7732697329315316826L;

                @Override
                public Object getPropertyValue(WorkspaceInfo item) {
                    Catalog catalog = GeoServerApplication.get().getCatalog();
                    WorkspaceInfo defaultWorkspace = catalog.getDefaultWorkspace();
                    return Boolean.valueOf(
                            defaultWorkspace != null && defaultWorkspace.equals(item));
                }
            };

    public static Property<WorkspaceInfo> ISOLATED = new BeanProperty<>("isolated", "isolated");

    static List<Property<WorkspaceInfo>> PROPERTIES = Arrays.asList(NAME, DEFAULT, ISOLATED);

    public static final Property<WorkspaceInfo> MODIFIED_TIMESTAMP =
            new BeanProperty<>("datemodfied", "dateModified");

    public static final Property<WorkspaceInfo> CREATED_TIMESTAMP =
            new BeanProperty<>("datecreated", "dateCreated");

    public WorkspaceProvider() {
        setSort(NAME.getName(), SortOrder.ASCENDING);
    }

    @Override
    protected List<WorkspaceInfo> getItems() {
        return getCatalog().getWorkspaces();
    }

    @Override
    protected List<Property<WorkspaceInfo>> getProperties() {
        List<Property<WorkspaceInfo>> modifiedPropertiesList =
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

    protected IModel<WorkspaceInfo> newModel(WorkspaceInfo object) {
        return new WorkspaceDetachableModel(object);
    }
}
