/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.password;

import static org.geoserver.security.password.ConfigurationPasswordEncryptionHelper.CACHE;
import static org.geoserver.security.password.ConfigurationPasswordEncryptionHelper.STORE_INFO_TYPE_CACHE;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.DataStoreInfoImpl;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.junit.Before;
import org.junit.Test;

public class ConfigurationPasswordEncryptionHelperTest {

    private Catalog catalog;
    private GeoServerSecurityManager mockSecurityManager;
    private ConfigurationPasswordEncryptionHelper helper;

    @Before
    public void setUp() {
        catalog = new CatalogImpl();
        GeoServerExtensionsHelper.singleton("rawCatalog", catalog, Catalog.class);
        mockSecurityManager = mock(GeoServerSecurityManager.class);
        helper = new ConfigurationPasswordEncryptionHelper(mockSecurityManager);
        CACHE.clear();
        STORE_INFO_TYPE_CACHE.clear();
    }

    @Test
    public void getEncryptedFieldsNullFactory() throws IOException {
        DataStoreInfo info = new DataStoreInfoImpl(catalog);
        // preflight
        assertNull(helper.getCatalog().getResourcePool().getDataStoreFactory(info));

        helper.getEncryptedFields(info);
        assertTrue(CACHE.isEmpty());
        assertTrue(STORE_INFO_TYPE_CACHE.isEmpty());
    }

    @Test
    public void getEncryptedFieldsStoreInfoTypeCacheOnFactoryFound() throws IOException {
        DataStoreInfo info = new DataStoreInfoImpl(catalog);
        Map<String, Serializable> params = info.getConnectionParameters();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, (String) PostgisNGDataStoreFactory.DBTYPE.getDefaultValue());
        info.setType(new PostgisNGDataStoreFactory().getDisplayName());

        // preflight
        assertThat(
                helper.getCatalog().getResourcePool().getDataStoreFactory(info),
                instanceOf(PostgisNGDataStoreFactory.class));

        helper.getEncryptedFields(info);
        Set<String> expected = CACHE.get(PostgisNGDataStoreFactory.class);
        assertThat(expected, equalTo(Set.of(JDBCDataStoreFactory.PASSWD.key)));

        assertEquals(expected, STORE_INFO_TYPE_CACHE.get(info.getType()));
    }

    /**
     * Some old data directories have DataStoreInfo configs with no type property set, which causes
     * {@link ConfigurationPasswordEncryptionHelper#getEncryptedFields()} to return immediately once it found a hit on
     * the CACHE by factory class, but then it'll keep calling
     * {@code getCatalog().getResourcePool().getDataStoreFactory(info)} for all the stores, which drains performance due
     * to the cost of {@link ResourcePool} cloning the info using serialization and deserialization, beside the cost of
     * the factory lookup itself
     */
    @Test
    public void getEncryptedFieldsStoreInfoTypeCacheOnFactoryGet() throws IOException {
        DataStoreInfo infoWithNoType = new DataStoreInfoImpl(catalog);
        Map<String, Serializable> params = infoWithNoType.getConnectionParameters();
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, (String) PostgisNGDataStoreFactory.DBTYPE.getDefaultValue());
        params.put(JDBCDataStoreFactory.HOST.key, "localhost");
        params.put(JDBCDataStoreFactory.DATABASE.key, "test");
        params.put(PostgisNGDataStoreFactory.PORT.key, 54329);
        params.put(JDBCDataStoreFactory.USER.key, "geo");
        params.put(JDBCDataStoreFactory.PASSWD.key, "123");

        // preflight
        assertThat(
                helper.getCatalog().getResourcePool().getDataStoreFactory(infoWithNoType),
                instanceOf(PostgisNGDataStoreFactory.class));

        DataStoreInfo infoWithType = new DataStoreInfoImpl(catalog);
        infoWithType.getConnectionParameters().putAll(infoWithNoType.getConnectionParameters());
        infoWithType.setType(new PostgisNGDataStoreFactory().getDisplayName());

        helper.getEncryptedFields(infoWithNoType);
        assertNotNull(CACHE.get(PostgisNGDataStoreFactory.class));

        assertTrue("infoWithNoType can't get a hit on the info type cache", STORE_INFO_TYPE_CACHE.isEmpty());

        // now call it with a DataStoreInfo of the same factory but with type set
        helper.getEncryptedFields(infoWithType);

        Set<String> expected = Set.of(JDBCDataStoreFactory.PASSWD.key);
        assertEquals(expected, STORE_INFO_TYPE_CACHE.get(infoWithType.getType()));
    }

    @Test
    public void testDecodeWontCallSecurityManagerLoadPasswordEncodersIfNotNeeded() {
        DataStoreInfo info = new DataStoreInfoImpl(catalog);
        Map<String, Serializable> params = info.getConnectionParameters();
        params.put(ShapefileDataStoreFactory.FILE_TYPE.key, (String)
                ShapefileDataStoreFactory.FILE_TYPE.getDefaultValue());
        info.setType(new ShapefileDataStoreFactory().getDisplayName());

        // preflight
        assertTrue(helper.getEncryptedFields(info).isEmpty());

        helper.decode(info);
        verify(mockSecurityManager, never()).loadPasswordEncoders(any(), any(), any());

        // now check it does call it when there's something to decode
        params.put(PostgisNGDataStoreFactory.DBTYPE.key, (String) PostgisNGDataStoreFactory.DBTYPE.getDefaultValue());
        params.put(JDBCDataStoreFactory.HOST.key, "localhost");
        params.put(JDBCDataStoreFactory.DATABASE.key, "test");
        params.put(PostgisNGDataStoreFactory.PORT.key, 54329);
        params.put(JDBCDataStoreFactory.USER.key, "geo");
        params.put(JDBCDataStoreFactory.PASSWD.key, "123");
        info.setType(new PostgisNGDataStoreFactory().getDisplayName());

        when(mockSecurityManager.loadPasswordEncoders(any(), any(), any())).thenReturn(List.of());
        helper.decode(info);
        verify(mockSecurityManager, times(1)).loadPasswordEncoders(any(), any(), any());
    }
}
