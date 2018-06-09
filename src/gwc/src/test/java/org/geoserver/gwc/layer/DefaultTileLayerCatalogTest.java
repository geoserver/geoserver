/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geowebcache.config.ContextualConfigurationProvider.Context;
import org.geowebcache.config.XMLConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.WebApplicationContext;

public class DefaultTileLayerCatalogTest {

    private File baseDirectory;

    private DefaultTileLayerCatalog catalog;

    @Before
    public void setUp() throws Exception {
        baseDirectory = new File("target", "mockTileLayerCatalog");
        FileUtils.deleteDirectory(baseDirectory);
        baseDirectory.mkdirs();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(baseDirectory);

        XStream xStream =
                XMLConfiguration.getConfiguredXStreamWithContext(
                        new SecureXStream(), (WebApplicationContext) null, Context.PERSIST);

        catalog = new DefaultTileLayerCatalog(resourceLoader, xStream);
    }

    @After
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(baseDirectory);
    }

    @Test
    public void testGetLayerById() {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId("id1");
        info.setName("name1");
        catalog.save(info);
        GeoServerTileLayerInfo actual = catalog.getLayerById("id1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);
    }

    @Test
    public void testGetLayerByName() {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId("id1");
        info.setName("name1");
        catalog.save(info);
        GeoServerTileLayerInfo actual = catalog.getLayerByName("name1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);
    }

    @Test
    public void testDelete() {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId("id1");
        info.setName("name1");
        catalog.save(info);

        GeoServerTileLayerInfo actual = catalog.getLayerByName("name1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);

        GeoServerTileLayerInfo deleted = catalog.delete("id1");
        assertEquals(info, ModificationProxy.unwrap(deleted));

        assertNull(catalog.getLayerById("id1"));
    }

    @Test
    public void testSave() {
        final GeoServerTileLayerInfo original;
        {
            final GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
            info.setId("id1");
            info.setName("name1");
            info.getMimeFormats().add("image/png");
            info.getMimeFormats().add("image/jpeg");
            assertNull(catalog.save(info));

            original = catalog.getLayerById("id1");
            assertEquals(info.getMimeFormats(), original.getMimeFormats());
        }

        original.getMimeFormats().clear();
        original.getMimeFormats().add("image/gif");
        original.setName("name2");

        final GeoServerTileLayerInfo oldValue = catalog.save(original);

        assertNotNull(oldValue);
        assertEquals(ImmutableSet.of("image/png", "image/jpeg"), oldValue.getMimeFormats());
        assertEquals("name1", oldValue.getName());

        assertNull(catalog.getLayerByName("name1"));
        assertNotNull(catalog.getLayerByName("name2"));

        GeoServerTileLayerInfo modified = catalog.getLayerById("id1");
        assertEquals(ImmutableSet.of("image/gif"), modified.getMimeFormats());
    }
}
