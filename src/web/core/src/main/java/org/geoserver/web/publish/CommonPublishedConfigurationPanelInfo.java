package org.geoserver.web.publish;

import org.geoserver.catalog.PublishedInfo;

public class CommonPublishedConfigurationPanelInfo extends PublishedConfigurationPanelInfo<PublishedInfo> {

    private static final long serialVersionUID = 8382295309912226673L;

    @Override
    public Class<PublishedInfo> getPublishedInfoClass() {
        return PublishedInfo.class;
    }

}
