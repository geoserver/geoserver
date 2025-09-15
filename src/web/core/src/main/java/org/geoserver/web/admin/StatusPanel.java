/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.admin;

import java.awt.GraphicsEnvironment;
import java.io.Serial;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.StringResourceModel;
import org.eclipse.imagen.JAI;
import org.eclipse.imagen.TileCache;
import org.eclipse.imagen.media.util.CacheDiagnostics;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.CoverageAccessInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.LockingManager;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.data.InProcessLockingManager;
import org.geotools.util.logging.Logging;

public class StatusPanel extends Panel {

    @Serial
    private static final long serialVersionUID = 7732030199323990637L;

    /** The map used as the model source so the label contents are updated */
    private Map<String, Object> values;

    private static final String KEY_DATA_DIR = "dataDir";

    private static final String KEY_LOCKS = "locks";

    private static final String KEY_CONNECTIONS = "connections";

    private static final String KEY_MEMORY = "memory";

    private static final String KEY_JVM_VERSION = "jvm_version";

    private static final String KEY_JAI_MAX_MEM = "jai_max_mem";

    private static final String KEY_JAI_MEM_USAGE = "jai_mem_usage";

    private static final String KEY_JAI_MEM_THRESHOLD = "jai_mem_threshold";

    private static final String KEY_JAI_TILE_THREADS = "jai_tile_threads";

    private static final String KEY_JAI_TILE_THREAD_PRIORITY = "jai_tile_thread_priority";

    private static final String KEY_COVERAGEACCESS_CORE_POOL_SIZE = "coverage_thread_corepoolsize";

    private static final String KEY_COVERAGEACCESS_MAX_POOL_SIZE = "coverage_thread_maxpoolsize";

    private static final String KEY_COVERAGEACCESS_KEEP_ALIVE_TIME = "coverage_thread_keepalivetime";

    private static final String KEY_UPDATE_SEQUENCE = "update_sequence";

    private static final String RESOURCE_CACHE = "resource_cache";

    private static final String KEY_JAVA_RENDERER = "renderer";

    private static final Logger LOGGER = Logging.getLogger(StatusPanel.class);

    private AbstractStatusPage parent;

    public StatusPanel(String id, AbstractStatusPage parent) {
        super(id);
        this.parent = parent;
        initUI();
    }

    public void initUI() {
        values = new HashMap<>();
        updateModel();

        // TODO: if we just provide the values directly as the models they won't
        // be refreshed on a page reload (ugh).
        add(new Label("dataDir", new MapModel<>(values, KEY_DATA_DIR)));
        add(new Label("locks", new MapModel<>(values, KEY_LOCKS)));
        add(new Label("connections", new MapModel<>(values, KEY_CONNECTIONS)));
        add(new Label("memory", new MapModel<>(values, KEY_MEMORY)));
        add(new Label("jvm.version", new MapModel<>(values, KEY_JVM_VERSION)));
        add(new Label("jai.memory.available", new MapModel<>(values, KEY_JAI_MAX_MEM)));
        add(new Label("jai.memory.used", new MapModel<>(values, KEY_JAI_MEM_USAGE)));
        add(new Label("jai.memory.threshold", new MapModel<>(values, KEY_JAI_MEM_THRESHOLD)));
        add(new Label(
                "jai.tile.threads",
                new StringResourceModel("values.threads", this)
                        .setParameters(new MapModel<>(values, KEY_JAI_TILE_THREADS))));
        add(new Label("jai.tile.priority", new MapModel<>(values, KEY_JAI_TILE_THREAD_PRIORITY)));
        add(new Label(
                "coverage.corepoolsize",
                new StringResourceModel("values.threads", this)
                        .setParameters(new MapModel<>(values, KEY_COVERAGEACCESS_CORE_POOL_SIZE))));
        add(new Label(
                "coverage.maxpoolsize",
                new StringResourceModel("values.threads", this)
                        .setParameters(new MapModel<>(values, KEY_COVERAGEACCESS_MAX_POOL_SIZE))));
        add(new Label(
                "coverage.keepalivetime",
                new StringResourceModel("values.milliseconds", this)
                        .setParameters(new MapModel<>(values, KEY_COVERAGEACCESS_KEEP_ALIVE_TIME))));
        add(new Label("updateSequence", new MapModel<>(values, KEY_UPDATE_SEQUENCE)));
        add(new Label("resourceCache", new MapModel<>(values, RESOURCE_CACHE)));
        add(new Label("renderer", new MapModel<>(values, KEY_JAVA_RENDERER)));
        // serialization error here
        add(new Link<>("free.locks") {
            @Serial
            private static final long serialVersionUID = -2889353495319211391L;

            @Override
            public void onClick() {
                // TODO: see GEOS-2130
                updateModel();
            }
        });
        add(new Link<>("free.memory") {
            @Serial
            private static final long serialVersionUID = 3695369177295089346L;

            @Override
            public void onClick() {
                System.gc();
                System.runFinalization();
                updateModel();
            }
        });

        add(new Link<>("free.memory.jai") {
            @Serial
            private static final long serialVersionUID = -3556725607958589003L;

            @Override
            public void onClick() {
                TileCache jaiCache = parent.getGeoServer().getGlobal().getJAI().getTileCache();
                final long capacityBefore = jaiCache.getMemoryCapacity();
                jaiCache.flush();
                jaiCache.setMemoryCapacity(0); // to be sure we realease all tiles
                System.gc();
                System.runFinalization();
                jaiCache.setMemoryCapacity(capacityBefore);
                updateModel();
            }
        });

        int fontCount = GraphicsEnvironment.getLocalGraphicsEnvironment().getAllFonts().length;
        add(new Label("fontCount", new ParamResourceModel("StatusPage.fontCount", this, fontCount)));
        add(new BookmarkablePageLink<>("show.fonts", JVMFontsPage.class));

        add(new AjaxLink<>("clear.resourceCache") {
            @Serial
            private static final long serialVersionUID = 2663650174059497376L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    parent.getGeoServer().reset();
                    info(getLocalizer().getString("resourceCacheClearedSuccessfully", this));
                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE, "Error resetting resource caches", t);
                    error(t);
                }
                parent.addFeedbackPanels(target);
                updateModel();
            }
        });

        add(new AjaxLink<>("reload.catalogConfig") {
            @Serial
            private static final long serialVersionUID = -7476556423889306321L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                try {
                    parent.getGeoServer().reload();
                    info(getLocalizer().getString("catalogConfigReloadedSuccessfully", StatusPanel.this));
                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE, "An error occurred while reloading the catalog", t);
                    error(t);
                }
                parent.addFeedbackPanels(target);
                updateModel();
            }
        });
    }

    /** Refresh values displayed by page. */
    private void updateModel() {
        values.put(KEY_DATA_DIR, getDataDirectory());
        values.put(KEY_LOCKS, getLockCount());
        values.put(KEY_CONNECTIONS, getConnectionCount());
        values.put(KEY_MEMORY, formatUsedMemory());
        values.put(
                KEY_JVM_VERSION,
                System.getProperty("java.vendor")
                        + ": "
                        + System.getProperty("java.version")
                        + " ("
                        + System.getProperty("java.vm.name")
                        + ")");

        GeoServerInfo geoServerInfo = parent.getGeoServer().getGlobal();
        JAIInfo jaiInfo = geoServerInfo.getJAI();
        @SuppressWarnings("PMD.CloseResource")
        JAI jai = jaiInfo.getJAI();
        CoverageAccessInfo coverageAccess = geoServerInfo.getCoverageAccess();
        TileCache jaiCache = jaiInfo.getTileCache();

        values.put(KEY_JAI_MAX_MEM, formatMemory(jaiCache.getMemoryCapacity()));
        if (jaiCache instanceof CacheDiagnostics diagnostics) {
            values.put(KEY_JAI_MEM_USAGE, formatMemory(diagnostics.getCacheMemoryUsed()));
        } else {
            values.put(KEY_JAI_MEM_USAGE, "-");
        }
        values.put(KEY_JAI_MEM_THRESHOLD, Integer.toString((int) (100.0f * jaiCache.getMemoryThreshold())) + "%");
        values.put(KEY_JAI_TILE_THREADS, jai.getTileScheduler().getParallelism());
        values.put(
                KEY_JAI_TILE_THREAD_PRIORITY,
                Integer.toString(jai.getTileScheduler().getPriority()));

        values.put(KEY_COVERAGEACCESS_CORE_POOL_SIZE, coverageAccess.getCorePoolSize());
        values.put(KEY_COVERAGEACCESS_MAX_POOL_SIZE, coverageAccess.getMaxPoolSize());
        values.put(KEY_COVERAGEACCESS_KEEP_ALIVE_TIME, coverageAccess.getKeepAliveTime());

        values.put(KEY_UPDATE_SEQUENCE, geoServerInfo.getUpdateSequence());
        values.put(RESOURCE_CACHE, getResourceCache());

        values.put(KEY_JAVA_RENDERER, checkRenderer());
    }

    /** Retrieves the GeoServer data directory */
    private String getDataDirectory() {
        GeoServerDataDirectory dd = parent.getGeoServerApplication().getBeanOfType(GeoServerDataDirectory.class);
        return dd.root().getAbsolutePath();
    }

    private String checkRenderer() {
        try {
            // static access to sun.java2d.pipe.RenderingEngine gives a warning that cannot be
            // suppressed
            String renderer = Class.forName("sun.java2d.pipe.RenderingEngine")
                    .getMethod("getInstance")
                    .invoke(null)
                    .getClass()
                    .getName();
            return renderer;
        } catch (Throwable e) {
            return "Unknown";
        }
    }

    /** @return a human friendly string for the VM used memory */
    private String formatUsedMemory() {
        final Runtime runtime = Runtime.getRuntime();
        final long usedBytes = runtime.totalMemory() - runtime.freeMemory();
        String formattedUsedMemory = formatMemory(usedBytes);

        String formattedMaxMemory = formatMemory(runtime.maxMemory());

        return formattedUsedMemory + " / " + formattedMaxMemory;
    }

    private String formatMemory(final long bytes) {
        final long KB = 1024;
        final long MB = KB * KB;
        final long GB = KB * MB;
        final NumberFormat formatter = NumberFormat.getInstance();
        formatter.setMaximumFractionDigits(2);

        String formattedUsedMemory;
        if (bytes > GB) {
            formattedUsedMemory = formatter.format((float) bytes / GB) + " GB";
        } else if (bytes > MB) {
            formattedUsedMemory = formatter.format(bytes / MB) + " MB";
        } else {
            formattedUsedMemory = formatter.format(bytes / KB) + " KB";
        }
        return formattedUsedMemory;
    }

    private synchronized int getLockCount() {
        int count = 0;

        try (CloseableIterator<StoreInfo> i = getStores()) {
            while (i.hasNext()) {
                StoreInfo meta = i.next();

                if (!meta.isEnabled()) {
                    // Don't count locks from disabled datastores.
                    continue;
                }

                if (meta instanceof DataStoreInfo dataStoreInfo) {
                    try {
                        DataAccess store = dataStoreInfo.getDataStore(null);
                        if (store instanceof DataStore dataStore) {
                            LockingManager lockingManager = dataStore.getLockingManager();
                            if (lockingManager instanceof InProcessLockingManager inprocess) {
                                count += inprocess.allLocks().size();
                            }
                        }
                    } catch (Throwable notAvailable) {
                        continue;
                    }
                }
            }
        }
        return count;
    }

    /**
     * Count the resources held in cache (active or not).
     *
     * @return resources held in cache
     */
    private int getResourceCache() {
        ResourcePool pool = parent.getGeoServer().getCatalog().getResourcePool();

        int count = 0;
        count += pool.getCrsCache().size();
        count += pool.getDataStoreCache().size();
        count += pool.getFeatureTypeCache().size();
        count += pool.getFeatureTypeAttributeCache().size();
        count += pool.getHintCoverageReaderCache().size();
        count += pool.getWmsCache().size();
        count += pool.getWmtsCache().size();
        count += pool.getStyleCache().size();
        count += pool.getSldCache().size();

        return count;
    }

    /**
     * Count number of stores that are enabled (and available).
     *
     * @return number of stores enabled and available
     */
    private synchronized int getConnectionCount() {
        int count = 0;

        try (CloseableIterator<StoreInfo> i = getStores()) {
            while (i.hasNext()) {
                StoreInfo meta = i.next();

                if (!meta.isEnabled()) {
                    // Don't count connections from disabled datastores.
                    continue;
                }
                if (meta instanceof DataStoreInfo dataMeta) {
                    try {
                        DataAccess<? extends FeatureType, ? extends Feature> store = dataMeta.getDataStore(null);
                        if (store == null) {
                            continue; // do not count connection
                        }
                    } catch (Throwable notAvailable) {
                        if (LOGGER.isLoggable(Level.FINE)) {
                            LOGGER.log(
                                    Level.FINE,
                                    "Store '" + meta.getName() + "' unavailable: " + notAvailable,
                                    notAvailable);
                        }
                        continue; // do not count connection
                    }
                }
                count += 1;
            }
        }

        return count;
    }

    /**
     * Query all StoreInfo entries in catalog.
     *
     * @return iterator of StoreInfo entries
     */
    private CloseableIterator<StoreInfo> getStores() {
        Catalog catalog = parent.getGeoServer().getCatalog();
        Filter filter = Predicates.acceptAll();
        CloseableIterator<StoreInfo> stores = catalog.list(StoreInfo.class, filter);
        return stores;
    }
}
