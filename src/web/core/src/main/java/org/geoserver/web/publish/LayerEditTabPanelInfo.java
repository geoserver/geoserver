package org.geoserver.web.publish;

import org.geoserver.catalog.LayerInfo;

public class LayerEditTabPanelInfo extends PublishedEditTabPanelInfo<LayerInfo> {

    private static final long serialVersionUID = -388475157541960108L;

    @Override
    public Class<LayerInfo> getPublishedInfoClass() {
        return LayerInfo.class;
    }

}
