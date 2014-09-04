/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layergroup;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/**
 * Reusable base class listing layer groups with clickable names
 */
public abstract class LayerGroupListPanel extends GeoServerTablePanel<LayerGroupInfo> {
    private static final long serialVersionUID = -4906590200057066912L;

    static Property<LayerGroupInfo> NAME = new BeanProperty<LayerGroupInfo>("name", "name");

    static Property<LayerGroupInfo> WORKSPACE = new BeanProperty<LayerGroupInfo>("workspace",
            "workspace.name");

    public LayerGroupListPanel(String id) {
        super(id, new GeoServerDataProvider<LayerGroupInfo>() {

            @Override
            protected List<LayerGroupInfo> getItems() {
                return getCatalog().getLayerGroups();
            }

            @Override
            protected List<Property<LayerGroupInfo>> getProperties() {
                return Arrays.asList(NAME, WORKSPACE);
            }

            public IModel newModel(Object object) {
                return new LayerGroupDetachableModel((LayerGroupInfo) object);
            }

        });
        getTopPager().setVisible(false);
    }

    @Override
    protected Component getComponentForProperty(String id, final IModel itemModel,
            Property<LayerGroupInfo> property) {
        IModel model = property.getModel(itemModel);
        if (NAME == property) {
            return new SimpleAjaxLink(id, model) {
                @Override
                protected void onClick(AjaxRequestTarget target) {
                    LayerGroupInfo layerGroup = (LayerGroupInfo) itemModel.getObject();
                    handleLayerGroup(layerGroup, target);
                }
            };
        } else {
            return new Label(id, model);
        }
    }

    protected void handleLayerGroup(LayerGroupInfo layerGroup, AjaxRequestTarget target) {
    }
}