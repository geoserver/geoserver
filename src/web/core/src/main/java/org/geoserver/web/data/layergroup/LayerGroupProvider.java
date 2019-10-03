/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;

/** Provides a table model for listing layer groups */
public class LayerGroupProvider extends GeoServerDataProvider<LayerGroupInfo> {

    private static final long serialVersionUID = 4806818198949114395L;

    public static Property<LayerGroupInfo> NAME = new BeanProperty<LayerGroupInfo>("name", "name");

    public static Property<LayerGroupInfo> WORKSPACE =
            new BeanProperty<LayerGroupInfo>("workspace", "workspace.name");

    static List<Property<LayerGroupInfo>> PROPERTIES = Arrays.asList(NAME, WORKSPACE);

    protected LayerGroupProviderFilter groupFilter = null;

    public LayerGroupProvider() {}

    public LayerGroupProvider(LayerGroupProviderFilter groupFilter) {
        this.groupFilter = groupFilter;
    }

    @Override
    protected List<LayerGroupInfo> getItems() {
        List<LayerGroupInfo> groups = getCatalog().getLayerGroups();
        if (groupFilter != null) {
            List<LayerGroupInfo> filtered = new ArrayList<LayerGroupInfo>(groups.size());
            for (LayerGroupInfo group : groups) {
                if (groupFilter.accept(group)) {
                    filtered.add(group);
                }
            }
            groups = filtered;
        }
        return groups;
    }

    @Override
    protected List<Property<LayerGroupInfo>> getProperties() {
        return PROPERTIES;
    }

    public IModel<LayerGroupInfo> newModel(LayerGroupInfo object) {
        return new LayerGroupDetachableModel((LayerGroupInfo) object);
    }
}
