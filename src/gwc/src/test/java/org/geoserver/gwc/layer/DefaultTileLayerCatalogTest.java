/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.common.collect.ImmutableSet;
import com.thoughtworks.xstream.XStream;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.FileSystemWatcher;
import org.geowebcache.config.ContextualConfigurationProvider.Context;
import org.geowebcache.config.XMLConfiguration;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.web.context.WebApplicationContext;

public class DefaultTileLayerCatalogTest {

    public @Rule TemporaryFolder tmpFolder = new TemporaryFolder();

    private File baseDirectory;

    private GeoServerResourceLoader resourceLoader;

    private DefaultTileLayerCatalog catalog;

    @Before
    public void setUp() throws Exception {
        baseDirectory = tmpFolder.getRoot();
        resourceLoader = new GeoServerResourceLoader(baseDirectory);

        new File(baseDirectory, "gwc-layers").mkdir();

        XStream xStream =
                XMLConfiguration.getConfiguredXStreamWithContext(
                        new SecureXStream(), (WebApplicationContext) null, Context.PERSIST);

        catalog = new DefaultTileLayerCatalog(resourceLoader, xStream);
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

    @Test
    public void testSaveWithEmptyStyleParamFilter() {
        final GeoServerTileLayerInfo original;
        {
            final GeoServerTileLayerInfo info = new GeoServerTileLayerInfoImpl();
            info.setId("id1");
            info.setName("name1");
            info.getMimeFormats().add("image/png");
            info.getMimeFormats().add("image/jpeg");

            StyleParameterFilter parameterFilter = new StyleParameterFilter();
            parameterFilter.setStyles(Collections.emptySet());
            info.addParameterFilter(parameterFilter);

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

    @Test
    public void testEvents() throws IOException, InterruptedException {

        ((FileSystemWatcher) resourceLoader.getResourceNotificationDispatcher())
                .schedule(50, TimeUnit.MILLISECONDS);

        AtomicBoolean hasBeenCreated = new AtomicBoolean(false);

        AtomicBoolean hasBeenModified = new AtomicBoolean(false);

        AtomicBoolean hasBeenDeleted = new AtomicBoolean(false);

        catalog.addListener(
                new TileLayerCatalogListener() {

                    @Override
                    public void onEvent(String layerId, Type type) {
                        switch (type) {
                            case CREATE:
                                hasBeenCreated.set(true);
                                break;
                            case DELETE:
                                hasBeenDeleted.set(true);
                                break;
                            case MODIFY:
                                hasBeenModified.set(true);
                                break;
                            default:
                                break;
                        }
                    }
                });

        File file = new File(baseDirectory, "gwc-layers/id1.xml");

        FileUtils.writeStringToFile(
                file,
                "<org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl><id>id1</id><name>originalname</name></org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl>",
                "UTF-8");

        waitForFlag(hasBeenCreated, 200);
        GeoServerTileLayerInfo info = catalog.getLayerById("id1");
        assertEquals("originalname", info.getName());
        assertNotNull(catalog.getLayerByName("originalname"));

        // it is necessary to wait a second, otherwise
        // the change is not detected because it is too soon after creation
        Thread.sleep(1000);

        FileUtils.writeStringToFile(
                file,
                "<org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl><id>id1</id><name>newname</name></org.geoserver.gwc.layer.GeoServerTileLayerInfoImpl>",
                "UTF-8");

        waitForFlag(hasBeenModified, 200);

        info = catalog.getLayerById("id1");
        assertEquals("newname", info.getName());
        assertNull(catalog.getLayerByName("originalname"));
        assertNotNull(catalog.getLayerByName("newname"));

        file.delete();

        waitForFlag(hasBeenDeleted, 200);

        assertNull(catalog.getLayerById("id1"));
        assertNull(catalog.getLayerByName("newname"));
    }

    public void waitForFlag(AtomicBoolean flag, int maxMillis) throws InterruptedException {
        int counter = 0;
        while (!flag.get() && counter * 100 < maxMillis) {
            Thread.sleep(100);
            counter++;
        }
        assertTrue(flag.get());
    }
}
