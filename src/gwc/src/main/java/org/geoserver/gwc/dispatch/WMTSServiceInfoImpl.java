package org.geoserver.gwc.dispatch;

import org.geoserver.config.impl.ServiceInfoImpl;

public class WMTSServiceInfoImpl extends ServiceInfoImpl {
    @Override
    public String getType() {
        return "WMTS";
    }
}
