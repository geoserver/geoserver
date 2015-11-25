package org.geoserver.web.publish;

import org.geoserver.catalog.LayerGroupInfo;

public class LayerGroupEditTabPanelInfo extends PublishedEditTabPanelInfo<LayerGroupInfo> {

    private static final long serialVersionUID = -388475157541960108L;

    @Override
    public Class<LayerGroupInfo> getPublishedInfoClass() {
        return LayerGroupInfo.class;
    }

}
