package org.geoserver.web.publish;

import org.geoserver.catalog.PublishedInfo;

public class CommonPublishedEditTabPanelInfo extends PublishedEditTabPanelInfo<PublishedInfo> {

    private static final long serialVersionUID = -388475157541960108L;

    @Override
    public Class<PublishedInfo> getPublishedInfoClass() {
        return PublishedInfo.class;
    }

}
