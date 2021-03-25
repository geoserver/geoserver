/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.impl.CoverageInfoImpl;
import org.geoserver.catalog.impl.CoverageStoreInfoImpl;

public class SecuredCoverageInfoTest
        extends SecuredResourceInfoTest<CoverageInfo, SecuredCoverageInfo> {

    @Override
    CoverageInfo createDelegate() {
        final CoverageInfo info = new CoverageInfoImpl(getCatalog());
        final CoverageStoreInfo storeInfo = new CoverageStoreInfoImpl(getCatalog());
        info.setStore(storeInfo);
        return info;
    }

    @Override
    SecuredCoverageInfo createSecuredDecorator(CoverageInfo delegate) {
        return new SecuredCoverageInfo(delegate, policy);
    }

    @Override
    Class<CoverageInfo> getDelegateClass() {
        return CoverageInfo.class;
    }

    @Override
    Class<SecuredCoverageInfo> getSecuredDecoratorClass() {
        return SecuredCoverageInfo.class;
    }

    @Override
    Class<SecuredCoverageStoreInfo> getSecuredStoreInfoClass() {
        return SecuredCoverageStoreInfo.class;
    }

    @Override
    int getStackOverflowCount() {
        return 500;
    }
}
