/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.io.File;

import junit.framework.TestCase;

import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geowebcache.config.XMLConfiguration;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;

public class DefaultTileLayerCatalogTest extends TestCase {

    private File baseDirectory;

    private DefaultTileLayerCatalog catalog;

    protected void setUp() throws Exception {
        baseDirectory = new File("target", "mockTileLayerCatalog");
        FileUtils.deleteDirectory(baseDirectory);
        baseDirectory.mkdirs();
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(baseDirectory);

        XStream xStream = XMLConfiguration.getConfiguredXStream(new XStream(), null);
        xStream = new GWCGeoServerConfigurationProvider().getConfiguredXStream(xStream);

        catalog = new DefaultTileLayerCatalog(resourceLoader, xStream);
    }

    protected void tearDown() throws Exception {
        FileUtils.deleteDirectory(baseDirectory);
    }

    public void testGetLayerById() {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId("id1");
        info.setName("name1");
        catalog.save(info);
        GeoServerTileLayerInfo actual = catalog.getLayerById("id1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);
    }

    public void testGetLayerByName() {
        GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
        info.setId("id1");
        info.setName("name1");
        catalog.save(info);
        GeoServerTileLayerInfo actual = catalog.getLayerByName("name1");
        actual = ModificationProxy.unwrap(actual);
        assertEquals(info, actual);
    }

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
