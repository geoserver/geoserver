/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.net.URL;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.DataUtilities;
import org.junit.Before;
import org.junit.Test;
import org.vfny.geoserver.global.GeoserverDataDirectory;

public class DefaultGeoServerLoaderTest {
    DefaultGeoServerLoader loader;
    Catalog catalog;
    XStreamPersister xp;
    
    @Before
    public void setUp() {
        URL url = DefaultGeoServerLoaderTest.class.getResource("/data_dir/nested_layer_groups");
        GeoServerResourceLoader resourceLoader = new GeoServerResourceLoader(DataUtilities.urlToFile(url));
        GeoserverDataDirectory.setResourceLoader(resourceLoader);
        loader = new DefaultGeoServerLoader(resourceLoader);
        catalog = new CatalogImpl();
        XStreamPersisterFactory xpf = new XStreamPersisterFactory();
        xp = xpf.createXMLPersister();
    }
    
    @Test
    public void testLoadNestedLayerGroups() throws Exception {
        loader.readCatalog(catalog, xp);
        assertNotNull(catalog.getLayerGroupByName("topp", "simplegroup"));
        LayerGroupInfo nestedLayerGroup = catalog.getLayerGroupByName("topp", "nestedgroup");
        assertNotNull(nestedLayerGroup);
        assertNotNull(nestedLayerGroup.getLayers());
        assertEquals(2, nestedLayerGroup.getLayers().size());
        assertTrue(nestedLayerGroup.getLayers().get(0) instanceof LayerGroupInfo);
        assertNotNull(((LayerGroupInfo)nestedLayerGroup.getLayers().get(0)).getLayers());
        assertTrue(nestedLayerGroup.getLayers().get(1) instanceof LayerInfo);
    }
}
