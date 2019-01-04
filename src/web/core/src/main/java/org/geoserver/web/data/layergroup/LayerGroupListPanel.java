/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.BeanProperty;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleAjaxLink;

/** Reusable base class listing layer groups with clickable names */
public abstract class LayerGroupListPanel extends GeoServerTablePanel<LayerGroupInfo> {
    private static final long serialVersionUID = -4906590200057066912L;

    static Property<LayerGroupInfo> NAME = new BeanProperty<LayerGroupInfo>("name", "name");

    static Property<LayerGroupInfo> WORKSPACE =
            new BeanProperty<LayerGroupInfo>("workspace", "workspace.name");

    public LayerGroupListPanel(String id, WorkspaceInfo workspace) {
        super(
                id,
                new GeoServerDataProvider<LayerGroupInfo>() {

                    private static final long serialVersionUID = 6471805356307807737L;

                    @Override
                    protected List<LayerGroupInfo> getItems() {
                        if (workspace == null) {
                            return getCatalog().getLayerGroups();
                        } else {
                            return getCatalog().getLayerGroupsByWorkspace(workspace);
                        }
                    }

                    @Override
                    protected List<Property<LayerGroupInfo>> getProperties() {
                        return Arrays.asList(NAME, WORKSPACE);
                    }

                    public IModel<LayerGroupInfo> newModel(LayerGroupInfo object) {
                        return new LayerGroupDetachableModel((LayerGroupInfo) object);
                    }
                });
        getTopPager().setVisible(false);
    }

    @SuppressWarnings("unchecked")
    @Override
    protected Component getComponentForProperty(
            String id, final IModel<LayerGroupInfo> itemModel, Property<LayerGroupInfo> property) {
        IModel<?> model = property.getModel(itemModel);
        if (NAME == property) {
            return new SimpleAjaxLink<String>(id, (IModel<String>) model) {
                private static final long serialVersionUID = -5189072047640596694L;

                @Override
                protected void onClick(AjaxRequestTarget target) {
                    LayerGroupInfo layerGroup = itemModel.getObject();
                    handleLayerGroup(layerGroup, target);
                }
            };
        } else {
            return new Label(id, model);
        }
    }

    protected void handleLayerGroup(LayerGroupInfo layerGroup, AjaxRequestTarget target) {}
}
