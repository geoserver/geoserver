package org.geoserver.wfs.notification;

import javax.xml.namespace.QName;

public class LinkEx extends Link {
    @Override
    public QName getForeign() {
        return foreign == null ? key : foreign;
    }
}
