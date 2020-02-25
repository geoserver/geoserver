/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.gwc.layer.CatalogConfiguration;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.web.publish.PublishedEditTabPanel;

/**
 * A contribution to the layer edit page to set up the layer caching options on a separate tab.
 *
 * @author groldan
 * @see GeoServerTileLayerEditor
 * @see PublishedEditTabPanelInfo
 * @see LayerGroupCacheOptionsPanel
 */
public class LayerCacheOptionsTabPanel extends PublishedEditTabPanel<PublishedInfo> {

    private static final long serialVersionUID = -2995387155768727100L;

    private GeoServerTileLayerEditor editor;

    public LayerCacheOptionsTabPanel(
            String id,
            IModel<? extends PublishedInfo> layerModel,
            IModel<GeoServerTileLayerInfo> tileLayerModel) {
        super(id, layerModel);

        if (!(layerModel.getObject() instanceof LayerInfo)
                || CatalogConfiguration.isLayerExposable((LayerInfo) layerModel.getObject())) {
            editor = new GeoServerTileLayerEditor("tileLayerEditor", layerModel, tileLayerModel);
            add(editor);
        } else {
            add(new Label("tileLayerEditor", new ResourceModel("geometryLessLabel")));
        }
    }

    @Override
    public void save() {
        if (editor != null) {
            editor.save();
        }
    }
}
