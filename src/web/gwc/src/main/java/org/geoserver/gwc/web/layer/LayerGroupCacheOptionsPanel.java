/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.web.layer;

import org.apache.wicket.model.IModel;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.config.GWCConfig;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geoserver.gwc.layer.GeoServerTileLayerInfo;
import org.geoserver.gwc.layer.TileLayerInfoUtil;
import org.geoserver.web.publish.LayerGroupConfigurationPanel;
import org.geoserver.web.publish.LayerGroupConfigurationPanelInfo;

/**
 * Contributes the tile caching configuration options for the {@link LayerGroupInfo LayerGroup} edit
 * page.
 * 
 * @see GeoServerTileLayerEditor
 * @see LayerCacheOptionsTabPanel
 * @see LayerGroupConfigurationPanelInfo
 */
public class LayerGroupCacheOptionsPanel extends LayerGroupConfigurationPanel {

    private static final long serialVersionUID = -8651034825347320139L;

    private GeoServerTileLayerEditor editor;

    public LayerGroupCacheOptionsPanel(final String id, IModel<LayerGroupInfo> layerGroupModel) {
        super(id, layerGroupModel);

        final LayerGroupInfo layerGroupInfo = getLayerGroupInfo();
        final boolean isNew = layerGroupInfo.getId() == null;

        GeoServerTileLayerInfo tileLayerInfo;

        final GWC mediator = GWC.get();
        final GWCConfig defaultSettings = mediator.getConfig();

        final GeoServerTileLayer tileLayer = isNew ? null : mediator.getTileLayer(layerGroupInfo);

        if (isNew || tileLayer == null) {
            /*
             * Ensure a sane config for defaults, in case automatic cache of new layers is defined
             * and the defaults is misconfigured
             */
            final GWCConfig saneDefaults = defaultSettings.saneConfig();
            tileLayerInfo = TileLayerInfoUtil.loadOrCreate(layerGroupInfo, saneDefaults);
        } else {
            tileLayerInfo = ((GeoServerTileLayer) tileLayer).getInfo().clone();
        }

        tileLayerInfo.setEnabled(true);
        final boolean initWithTileLayer = (isNew && defaultSettings.isCacheLayersByDefault())
                || tileLayer != null;

        if (!initWithTileLayer) {
            tileLayerInfo.setId(null);// indicate not to create the tile layer
        }

        GeoServerTileLayerInfoModel tileLayerModel = new GeoServerTileLayerInfoModel(tileLayerInfo,
                isNew);

        editor = new GeoServerTileLayerEditor("tileLayerEditor", layerGroupModel, tileLayerModel);
        add(editor);
    }

    @Override
    public void save() {
        editor.save();
    }
}
