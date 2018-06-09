/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.impl.WMSLayerInfoImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;

public class SecuredWMSLayerInfoTest
        extends SecuredResourceInfoTest<WMSLayerInfo, SecuredWMSLayerInfo> {

    @Override
    WMSLayerInfo createDelegate() {
        WMSLayerInfo info = new WMSLayerInfoImpl(getCatalog());
        info.setStore(new WMSStoreInfoImpl(getCatalog()));
        return info;
    }

    @Override
    Class getDelegateClass() {
        return WMSLayerInfo.class;
    }

    @Override
    SecuredWMSLayerInfo createSecuredDecorator(WMSLayerInfo delegate) {
        return new SecuredWMSLayerInfo(delegate, policy);
    }

    @Override
    Class getSecuredDecoratorClass() {
        return SecuredWMSLayerInfo.class;
    }

    @Override
    Class getSecuredStoreInfoClass() {
        return SecuredWMSStoreInfo.class;
    }

    @Override
    int getStackOverflowCount() {
        return 50_000;
    }
}
