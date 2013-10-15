package org.geoserver.wfs.notification;


public class ObjectFactoryEx extends ObjectFactory {
    @Override
    public Link createLink() {
        return new LinkEx();
    }
}
