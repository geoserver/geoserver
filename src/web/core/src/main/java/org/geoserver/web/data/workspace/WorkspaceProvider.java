/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.workspace;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;

/**
 * {@link GeoServerDataProvider} for the list of workspaces available in the {@link Catalog}
 */
@SuppressWarnings("serial")
public class WorkspaceProvider extends GeoServerDataProvider<WorkspaceInfo> {

    public static Property<WorkspaceInfo> NAME = 
        new BeanProperty<WorkspaceInfo>( "name", "name" );
    
    public static Property<WorkspaceInfo> DEFAULT = 
        new BeanProperty<WorkspaceInfo>( "default", "default" );

    static List PROPERTIES = Arrays.asList(NAME, DEFAULT);
    
    public WorkspaceProvider() {
        setSort(NAME.getName(), true);
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
