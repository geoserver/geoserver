/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;

/** Provides a table model for listing layer groups */
public class LayerGroupProvider extends GeoServerDataProvider<LayerGroupInfo> {

    private static final long serialVersionUID = 4806818198949114395L;

    public static Property<LayerGroupInfo> NAME = new BeanProperty<LayerGroupInfo>("name", "name");

    public static Property<LayerGroupInfo> WORKSPACE =
            new BeanProperty<LayerGroupInfo>("workspace", "workspace.name");

    static final Property<LayerGroupInfo> MODIFIED_TIMESTAMP =
            new BeanProperty<>("datemodfied", "dateModified");

    static final Property<LayerGroupInfo> CREATED_TIMESTAMP =
            new BeanProperty<>("datecreated", "dateCreated");

    public static Property<LayerGroupInfo> ENABLED =
            new AbstractProperty<LayerGroupInfo>("Enabled") {

                public Boolean getPropertyValue(LayerGroupInfo item) {
                    return Boolean.valueOf(item.isEnabled());
                }
            };

    static List<Property<LayerGroupInfo>> PROPERTIES = Arrays.asList(NAME, WORKSPACE, ENABLED);

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
        List<Property<LayerGroupInfo>> modifiedPropertiesList =
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

    public IModel<LayerGroupInfo> newModel(LayerGroupInfo object) {
        return new LayerGroupDetachableModel((LayerGroupInfo) object);
    }
}
