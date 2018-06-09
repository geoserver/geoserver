/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.catalog.impl.FeatureTypeInfoImpl;

public class SecuredFeatureTypeInfoTest
        extends SecuredResourceInfoTest<FeatureTypeInfo, SecuredFeatureTypeInfo> {

    @Override
    FeatureTypeInfo createDelegate() {
        FeatureTypeInfo info = new FeatureTypeInfoImpl(getCatalog());
        info.setStore(new DataStoreInfoImpl(getCatalog()));
        return info;
    }

    @Override
    SecuredFeatureTypeInfo createSecuredDecorator(FeatureTypeInfo delegate) {
        return new SecuredFeatureTypeInfo(delegate, policy);
    }

    @Override
    Class getDelegateClass() {
        return FeatureTypeInfo.class;
    }

    @Override
    Class getSecuredDecoratorClass() {
        return SecuredFeatureTypeInfo.class;
    }

    @Override
    Class getSecuredStoreInfoClass() {
        return SecuredDataStoreInfo.class;
    }

    @Override
    int getStackOverflowCount() {
        return 500;
    }
}
