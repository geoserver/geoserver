/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.model.LoadableDetachableModel;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.data.layergroup.LayerGroupProviderFilter;


/**
 * Simple detachable model listing layer groups.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
@SuppressWarnings("serial")
public class LayerGroupsModel extends LoadableDetachableModel<List<LayerGroupInfo>> {
    
    private LayerGroupProviderFilter filter;


    public LayerGroupsModel() {
        this.filter = new LayerGroupProviderFilter() {
            @Override
            public boolean accept(LayerGroupInfo group) {
                return true;
            }
        };
    }

    public LayerGroupsModel(LayerGroupProviderFilter filter) {
        this.filter = filter;
    }
    
    
    @Override
    protected List<LayerGroupInfo> load() {
        Catalog catalog = GeoServerApplication.get().getCatalog();
        
        List<LayerGroupInfo> allGroups = catalog.getLayerGroups();
        List<LayerGroupInfo> filteredGroups = new ArrayList<LayerGroupInfo>(allGroups.size());
        for (LayerGroupInfo group : allGroups) {
            if (filter.accept(group)) {
                filteredGroups.add(group);
            }
        }
        
        Collections.sort(filteredGroups, new Comparator<LayerGroupInfo>() {
            public int compare(LayerGroupInfo g1, LayerGroupInfo g2) {
                return g1.prefixedName().compareToIgnoreCase(g2.prefixedName());
            }    
        });
        
        return filteredGroups;
    }
}