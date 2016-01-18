/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * {@link GeoServerDataProvider} for the list of workspaces available in the {@link Catalog}
 */
@SuppressWarnings("serial")
public class WorkspaceProvider extends GeoServerDataProvider<WorkspaceInfo> {

    public static Property<WorkspaceInfo> NAME = 
        new BeanProperty<WorkspaceInfo>( "name", "name" );
    
    public static Property<WorkspaceInfo> DEFAULT = new AbstractProperty<WorkspaceInfo>("default") {

        @Override
        public Object getPropertyValue(WorkspaceInfo item) {
            Catalog catalog = GeoServerApplication.get().getCatalog();
            WorkspaceInfo defaultWorkspace = catalog.getDefaultWorkspace();
            return Boolean.valueOf(defaultWorkspace != null && defaultWorkspace.equals(item));
        }
    };

    static List PROPERTIES = Arrays.asList(NAME, DEFAULT);
    
    public WorkspaceProvider() {
        setSort(NAME.getName(), SortOrder.ASCENDING);
    }
   
    @Override
    protected List<WorkspaceInfo> getItems() {
        return getCatalog().getWorkspaces();
    }

    @Override
    protected List<Property<WorkspaceInfo>> getProperties() {
        return PROPERTIES;
    }

    public IModel newModel(Object obj) {
        return new WorkspaceDetachableModel( (WorkspaceInfo) obj );
    }

}
