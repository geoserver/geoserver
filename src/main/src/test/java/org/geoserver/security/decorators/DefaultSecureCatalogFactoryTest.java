/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.decorators;

import org.easymock.EasyMock;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.security.WrapperPolicy;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class DefaultSecureCatalogFactoryTest {
    DefaultSecureCatalogFactory defaultSecureCatalogFactory = new DefaultSecureCatalogFactory();
    WrapperPolicy wrapperPolicy = EasyMock.createMock(WrapperPolicy.class);

    @Before
    public void setup() {
        defaultSecureCatalogFactory = new DefaultSecureCatalogFactory();
        wrapperPolicy = EasyMock.createMock(WrapperPolicy.class);
    }

    @Test
    public void secureWMSStoreInfo() {
        // Given
        SecuredWMSStoreInfo securedWMSStoreInfo = EasyMock.createMock(SecuredWMSStoreInfo.class);

        // When
        Object object = defaultSecureCatalogFactory.secure(securedWMSStoreInfo, wrapperPolicy);

        // Then
        Assert.assertTrue(object instanceof SecuredWMSStoreInfo);
    }

    @Test
    public void canSecureWMSStoreInfo() {
        // Given
        Class<WMSStoreInfo> clazz = WMSStoreInfo.class;

        // When
        boolean bool = defaultSecureCatalogFactory.canSecure(clazz);

        // Then
        Assert.assertTrue(bool);
    }

    @Test
    public void secureWMTSStoreInfo() {
        // Given
        DefaultSecureCatalogFactory defaultSecureCatalogFactory = new DefaultSecureCatalogFactory();
        WrapperPolicy wrapperPolicy = EasyMock.createMock(WrapperPolicy.class);
        SecuredWMTSStoreInfo securedWMTSStoreInfo = EasyMock.createMock(SecuredWMTSStoreInfo.class);

        // When
        Object object = defaultSecureCatalogFactory.secure(securedWMTSStoreInfo, wrapperPolicy);

        // Then
        Assert.assertTrue(object instanceof SecuredWMTSStoreInfo);
    }

    @Test
    public void canSecureWMTSStoreInfo() {
        // Given
        Class<WMTSStoreInfo> clazz = WMTSStoreInfo.class;

        // When
        boolean bool = defaultSecureCatalogFactory.canSecure(clazz);

        // Then
        Assert.assertTrue(bool);
    }
}
