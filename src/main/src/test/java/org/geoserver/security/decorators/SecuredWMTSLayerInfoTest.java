/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.impl.WMTSLayerInfoImpl;
import org.geoserver.catalog.impl.WMTSStoreInfoImpl;

public class SecuredWMTSLayerInfoTest
        extends SecuredResourceInfoTest<WMTSLayerInfo, SecuredWMTSLayerInfo> {

    @Override
    WMTSLayerInfo createDelegate() {
        WMTSLayerInfo info = new WMTSLayerInfoImpl(getCatalog());
        info.setStore(new WMTSStoreInfoImpl(getCatalog()));
        return info;
    }

    @Override
    Class getDelegateClass() {
        return WMTSLayerInfo.class;
    }

    @Override
    SecuredWMTSLayerInfo createSecuredDecorator(WMTSLayerInfo delegate) {
        return new SecuredWMTSLayerInfo(delegate, policy);
    }

    @Override
    Class getSecuredDecoratorClass() {
        return SecuredWMTSLayerInfo.class;
    }

    @Override
    Class getSecuredStoreInfoClass() {
        return SecuredWMTSStoreInfo.class;
    }

    @Override
    int getStackOverflowCount() {
        return 50_000;
    }
}
