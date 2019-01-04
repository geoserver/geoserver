/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.URLs;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class DefaultGeoServerLoaderTest {
    DefaultGeoServerLoader loader;

    Catalog catalog;
    XStreamPersister xp;

    boolean helloServiceSaved = false;

    static interface HelloServiceInfo extends ServiceInfo {}

    static final class HelloServiceInfoImpl extends ServiceInfoImpl implements HelloServiceInfo {}

    static final class HelloServiceXStreamLoader extends XStreamServiceLoader<HelloServiceInfo> {

        public HelloServiceXStreamLoader(
                GeoServerResourceLoader resourceLoader, String filenameBase) {
            super(resourceLoader, filenameBase);
        }

        @Override
        public Class<HelloServiceInfo> getServiceClass() {
            return HelloServiceInfo.class;
        }

        @Override
        protected HelloServiceInfo createServiceFromScratch(GeoServer gs) {
            return new HelloServiceInfoImpl();
        }
    };

    @Before
    public void setUp() {
        URL url = DefaultGeoServerLoaderTest.class.getResource("/data_dir/nested_layer_groups");
        GeoServerResourceLoader resourceLoader =
                new GeoServerResourceLoader(URLs.urlToFile(url)) {
                    @Override
                    public File createFile(File parentFile, String location) throws IOException {
                        if ("hello.xml".equals(location)) {
                            helloServiceSaved = true;
                        }
                        return super.createFile(parentFile, location);
                    }
                };
        GeoServerExtensionsHelper.singleton(
                "resourceLoader", resourceLoader, GeoServerResourceLoader.class);

        loader = new DefaultGeoServerLoader(resourceLoader);
        catalog = new CatalogImpl();
        catalog.setResourceLoader(resourceLoader);

        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        xp = xpf.createXMLPersister();

        XStreamServiceLoader<HelloServiceInfo> helloLoader =
                new HelloServiceXStreamLoader(resourceLoader, "hello");
        GeoServerExtensionsHelper.singleton("helloLoader", helloLoader, XStreamServiceLoader.class);
    }

    @After
    public void tearDown() {
        GeoServerExtensionsHelper.clear(); // clear singleton
    }

    @Test
    public void testGeneratedStyles() throws Exception {
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        XStreamPersister xp = xpf.createXMLPersister();
        xp.setCatalog(catalog);
        loader.initializeStyles(catalog, xp);

        StyleInfo polygon = catalog.getStyleByName(StyleInfo.DEFAULT_POLYGON);
        assertEquals("default_polygon.sld", polygon.getFilename());
    }

    @Test
    public void testLoadNestedLayerGroups() throws Exception {
        GeoServerResourceLoader resources = GeoServerExtensions.bean(GeoServerResourceLoader.class);
        assertSame(catalog.getResourceLoader(), resources);
        loader.readCatalog(catalog, xp);

        LayerGroupInfo simpleLayerGroup = catalog.getLayerGroupByName("topp", "simplegroup");
        assertNotNull(simpleLayerGroup);
        assertEquals(101, simpleLayerGroup.getAttribution().getLogoWidth());
        assertEquals(102, simpleLayerGroup.getAttribution().getLogoHeight());
        assertEquals(2, simpleLayerGroup.getMetadataLinks().size());
        assertEquals(
                "http://my/metadata/link/1",
                simpleLayerGroup.getMetadataLinks().get(0).getContent());
        assertEquals("text/html", simpleLayerGroup.getMetadataLinks().get(0).getType());

        LayerGroupInfo nestedLayerGroup = catalog.getLayerGroupByName("topp", "nestedgroup");
        assertNotNull(nestedLayerGroup);
        assertNotNull(nestedLayerGroup.getLayers());
        assertEquals(2, nestedLayerGroup.getLayers().size());
        assertTrue(nestedLayerGroup.getLayers().get(0) instanceof LayerGroupInfo);
        assertNotNull(((LayerGroupInfo) nestedLayerGroup.getLayers().get(0)).getLayers());
        assertTrue(nestedLayerGroup.getLayers().get(1) instanceof LayerInfo);
    }

    @Test
    public void testLoadWithoutResaving() throws Exception {
        GeoServerImpl gs = new GeoServerImpl();
        gs.setCatalog(catalog);
        // this one already calls onto loadService
        loader.postProcessBeforeInitialization(gs, "geoServer");

        // for extra measure, also do a reload
        loader.reload();

        assertFalse("hello.xml should not have been saved during load", helloServiceSaved);
    }
}
