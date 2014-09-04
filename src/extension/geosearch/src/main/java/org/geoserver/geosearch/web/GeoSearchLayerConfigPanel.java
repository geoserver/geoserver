/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geosearch.web;

import static org.geoserver.geosearch.rest.Properties.INDEXING_ENABLED;
import static org.geoserver.geosearch.rest.Properties.LAST_MODIFIED;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.web.publish.LayerConfigurationPanel;
import org.geoserver.web.util.MapModel;

/**
 * Configures a {@link LayerInfo} geo-search related metadata
 */
public class GeoSearchLayerConfigPanel extends LayerConfigurationPanel {

    private static final long serialVersionUID = 5739568775378997529L;

    @SuppressWarnings("unchecked")
    public GeoSearchLayerConfigPanel(String id, IModel<LayerInfo> layerModel) {
        super(id, layerModel);

        IModel<LayerInfo> layerInfoModel = (IModel<LayerInfo>) getDefaultModel();
        PropertyModel<MetadataMap> layerMetadataModel;
        layerMetadataModel = new PropertyModel<MetadataMap>(layerInfoModel, "metadata");

        MapModel lastModModel = new MapModel(layerMetadataModel, LAST_MODIFIED);
        lastModModel.setObject(Long.valueOf(System.currentTimeMillis()));

        MapModel enableLayerModel = new MapModel(layerMetadataModel, INDEXING_ENABLED);
        CheckBox enable = new CheckBox("geosearch.enable", enableLayerModel);
        add(enable);
    }
}
