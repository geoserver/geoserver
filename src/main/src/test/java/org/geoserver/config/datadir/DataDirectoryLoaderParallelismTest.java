/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static org.geoserver.config.datadir.DataDirectoryGeoServerLoader.GEOSERVER_DATA_DIR_LOADER_THREADS;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.geoserver.GeoServerConfigurationLock;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.WMSStoreInfoImpl;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

/**
 * Loads a lopsided data directory, where one workspace holds most of the content, with the loader pinned to a single
 * thread and to the full pool.
 *
 * <p>The single thread run makes the load order reproducible, so a regression that drops objects fails every time
 * instead of once in a while. The full pool run is the opposite, it is where a starving or deadlocking phase shows up,
 * which is what the timeout rule is for.
 */
@RunWith(Parameterized.class)
public class DataDirectoryLoaderParallelismTest extends GeoServerSystemTestSupport {

    private static final String FAT_WORKSPACE = "fat";
    private static final int FAT_LAYERS = 40;
    private static final int THIN_WORKSPACES = 4;

    /** A deadlock in the loader hangs forever, this turns it into a failure that names the stuck thread. */
    @Rule
    public Timeout timeout = Timeout.builder()
            .withTimeout(2, TimeUnit.MINUTES)
            .withLookingForStuckThread(true)
            .build();

    @Parameter
    public int threads;

    @Parameters(name = "threads={0}")
    public static List<Object[]> data() {
        return List.of(new Object[] {1}, new Object[] {Runtime.getRuntime().availableProcessors()});
    }

    private DataDirectoryLoaderTestSupport support;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        support = new DataDirectoryLoaderTestSupport(getCatalog(), getGeoServer());
        support.setUpServiceLoaders();
        if (getCatalog().getWorkspaceByName(FAT_WORKSPACE) == null) {
            buildLopsidedDataDirectory();
        }
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        support.tearDown();
    }

    @Test
    public void loadsEverythingRegardlessOfParallelism() {
        Catalog expected = getCatalog();
        CatalogImpl loaded = loadWithPinnedThreads();

        assertEquals(expected.getWorkspaces().size(), loaded.getWorkspaces().size());
        assertEquals(expected.getNamespaces().size(), loaded.getNamespaces().size());
        assertEquals(expected.getStyles().size(), loaded.getStyles().size());
        assertEquals(
                expected.getStores(StoreInfo.class).size(),
                loaded.getStores(StoreInfo.class).size());
        assertEquals(
                expected.getResources(ResourceInfo.class).size(),
                loaded.getResources(ResourceInfo.class).size());
        assertEquals(expected.getLayers().size(), loaded.getLayers().size());
        assertEquals(expected.getLayerGroups().size(), loaded.getLayerGroups().size());
    }

    /** Loads the data directory into a throwaway catalog with the parallelism of this run, restoring the property. */
    private CatalogImpl loadWithPinnedThreads() {
        String previous = System.setProperty(GEOSERVER_DATA_DIR_LOADER_THREADS, String.valueOf(threads));
        CatalogImpl loaded = new CatalogImpl();
        try {
            newLoader().postProcessBeforeInitialization(loaded, "catalog");
        } finally {
            if (previous == null) {
                System.clearProperty(GEOSERVER_DATA_DIR_LOADER_THREADS);
            } else {
                System.setProperty(GEOSERVER_DATA_DIR_LOADER_THREADS, previous);
            }
        }
        return loaded;
    }

    private DataDirectoryGeoServerLoader newLoader() {
        GeoServerResourceLoader resourceLoader = getResourceLoader();
        GeoServerSecurityManager secManager = getSecurityManager();
        GeoServerConfigurationLock configLock = GeoServerExtensions.bean(GeoServerConfigurationLock.class);
        return new DataDirectoryGeoServerLoader(new GeoServerDataDirectory(resourceLoader), secManager, configLock);
    }

    /**
     * One workspace holding most of the layers plus a global group over them, and a handful of one layer workspaces,
     * the shape where a phase that cannot split its own work becomes the tail of the load.
     */
    private void buildLopsidedDataDirectory() {
        WorkspaceInfo fat = support.addWorkspace(FAT_WORKSPACE);
        List<PublishedInfo> fatLayers = new ArrayList<>(FAT_LAYERS);
        for (int i = 0; i < FAT_LAYERS; i++) {
            fatLayers.add(addCascadedLayer(fat, FAT_WORKSPACE + i));
        }
        getCatalog().add(support.createLayerGroup("fatGroup", fatLayers));

        for (int i = 0; i < THIN_WORKSPACES; i++) {
            WorkspaceInfo thin = support.addWorkspace("thin" + i);
            addCascadedLayer(thin, "thin" + i);
        }
    }

    private LayerInfo addCascadedLayer(WorkspaceInfo ws, String name) {
        Catalog catalog = getCatalog();
        WMSStoreInfoImpl store = support.createWmsStore(ws);
        store.setName(name + "Store");
        catalog.add(store);

        WMSLayerInfo resource = catalog.getFactory().createWMSLayer();
        resource.setName(name);
        resource.setNativeName(name);
        resource.setNamespace(catalog.getNamespaceByPrefix(ws.getName()));
        resource.setStore(catalog.getStore(store.getId(), WMSStoreInfo.class));
        catalog.add(resource);

        LayerInfo layer = catalog.getFactory().createLayer();
        layer.setResource(catalog.getResource(resource.getId(), WMSLayerInfo.class));
        layer.setDefaultStyle(catalog.getStyleByName(StyleInfo.DEFAULT_RASTER));
        catalog.add(layer);
        return catalog.getLayer(layer.getId());
    }
}
