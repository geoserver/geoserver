/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import com.google.common.base.Stopwatch;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.util.LegacyCatalogImporter;
import org.geoserver.catalog.util.LegacyCatalogReader;
import org.geoserver.catalog.util.LegacyFeatureTypeInfoReader;
import org.geoserver.config.AsynchResourceIterator.ResourceMapper;
import org.geoserver.config.util.LegacyConfigurationImporter;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.platform.resource.Resources.ExtensionFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.util.Filter;
import org.geoserver.util.IOUtils;
import org.geotools.util.decorate.Wrapper;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Initializes GeoServer configuration and catalog on startup.
 *
 * <p>This class post processes the singleton beans {@link Catalog} and {@link GeoServer}, populating them from stored
 * configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public abstract class GeoServerLoader {

    static Logger LOGGER = Logging.getLogger("org.geoserver.config");

    /** Workspace IO resources */
    static final class WorkspaceContents {
        Resource resource;
        byte[] contents;
        byte[] nsContents;

        public WorkspaceContents(Resource resource, byte[] contents, byte[] nsContents) {
            this.resource = resource;
            this.contents = contents;
            this.nsContents = nsContents;
        }
    }

    /** {@link ResourceMapper} for workspaces */
    static final class WorkspaceMapper implements ResourceMapper<WorkspaceContents> {

        @Override
        public WorkspaceContents apply(Resource rd) throws IOException {
            Resource wr = rd.get("workspace.xml");
            Resource nr = rd.get("namespace.xml");
            if (Resources.exists(wr) && Resources.exists(nr)) {
                byte[] contents = wr.getContents();
                byte[] nrContents = nr.getContents();
                return new WorkspaceContents(rd, contents, nrContents);
            } else {
                LOGGER.warning("Ignoring workspace directory " + rd.path());
                return null;
            }
        }
    }

    /**
     * Holder for both the contents and the resource of a single file to aid in identifying the offending file when
     * loading fails *
     */
    static final class SingleResourceContents {
        final Resource resource;
        final byte[] contents;

        public SingleResourceContents(Resource resource, byte[] contents) {
            this.resource = resource;
            this.contents = contents;
        }
    }

    /** Basic {@link ResourceMapper} for a single {@link Resource} * */
    static final ResourceMapper<SingleResourceContents> RESOURCE_MAPPER =
            r -> new SingleResourceContents(r, r.getContents());

    /** Layer IO resources */
    static final class LayerContents {
        Resource resource;
        byte[] contents;
        byte[] layerContents;

        public LayerContents(Resource resource, byte[] contents, byte[] layerContents) {
            this.resource = resource;
            this.contents = contents;
            this.layerContents = layerContents;
        }
    }

    /** Resource/Layer mapper to IO resources (generic) */
    static final class ResourceLayerMapper implements ResourceMapper<LayerContents> {

        private String resourceFileName;
        private String resourceType;

        public ResourceLayerMapper(String resourceFileName, String resourceType) {
            this.resourceFileName = resourceFileName;
            this.resourceType = resourceType;
        }

        @Override
        public LayerContents apply(Resource rd) throws IOException {
            Resource r = rd.get(resourceFileName);
            Resource lr = rd.get("layer.xml");
            if (Resources.exists(r) && Resources.exists(lr)) {
                byte[] contents = r.getContents();
                byte[] lrContents = lr.getContents();
                return new LayerContents(rd, contents, lrContents);
            } else {
                LOGGER.warning("Ignoring " + resourceType + " directory " + rd.path());
                return null;
            }
        }
    }

    /** Feature Type IO resource mapper */
    static final ResourceLayerMapper FEATURE_LAYER_MAPPER = new ResourceLayerMapper("featuretype.xml", "feature type");
    /** Coverage IO resource mapper */
    static final ResourceLayerMapper COVERAGE_LAYER_MAPPER = new ResourceLayerMapper("coverage.xml", "coverage");
    /** WMS Layer IO resource mapper */
    static final ResourceLayerMapper WMS_LAYER_MAPPER = new ResourceLayerMapper("wmslayer.xml", "wms layer");
    /** WMTS Layer IO resource mapper */
    static final ResourceLayerMapper WMTS_LAYER_MAPPER = new ResourceLayerMapper("wmtslayer.xml", "wmts layer");
    /**
     * Generic layer catalog loader for all types of IO resources
     *
     * @author Andrea Aime - GeoSolutions
     */
    static final class LayerLoader<T extends ResourceInfo> implements Consumer<LayerContents> {

        Class<T> clazz;
        XStreamPersister xp;
        Catalog catalog;

        public LayerLoader(Class<T> clazz, XStreamPersister xp, Catalog catalog) {
            this.clazz = clazz;
            this.xp = xp;
            this.catalog = catalog;
        }

        @Override
        public void accept(LayerContents lc) {
            T ft = null;
            try {
                ft = depersist(xp, lc.contents, clazz);
                catalog.add(ft);
            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load resource", e);
                return;
            }

            if (LOGGER.isLoggable(Level.CONFIG)) {
                String type = ft instanceof CoverageInfo
                        ? "coverage"
                        : ft instanceof FeatureTypeInfo ? "feature type" : "resource";
                LOGGER.config("Loaded "
                        + type
                        + " '"
                        + lc.resource.name()
                        + "', "
                        + (ft.isEnabled() ? "enabled" : "disabled"));
            }

            try {
                LayerInfo l = depersist(xp, lc.layerContents, LayerInfo.class);
                catalog.add(l);

                LOGGER.config("Loaded layer '" + l.getName() + "'");

                for (StyleInfo style : l.getStyles()) {
                    if (null == style) {
                        LOGGER.log(Level.SEVERE, "Layer '" + l.getName() + "' references a missing style");
                    }
                }

            } catch (Exception e) {
                LOGGER.log(Level.WARNING, "Failed to load layer " + lc.resource.name(), e);
            }
        }
    }

    static final ExtensionFilter XML_FILTER = new Resources.ExtensionFilter("XML");

    protected GeoServerResourceLoader resourceLoader;
    GeoServer geoserver;
    XStreamPersisterFactory xpf = new XStreamPersisterFactory();

    // JD: this is a hack for the moment, it is used only to maintain tests since the test setup
    // relies
    // on the old data directory structure, once the tests have been ported to the new structure
    // this ugly hack can die
    static boolean legacy = false;

    public GeoServerLoader(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {}

    public void setXStreamPeristerFactory(XStreamPersisterFactory xpf) {
        this.xpf = xpf;
    }

    public static void setLegacy(boolean legacy) {
        GeoServerLoader.legacy = legacy;
    }

    public final Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        return bean;
    }

    public final Object postProcessBeforeInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof Catalog) {
            // ensure this is not a wrapper but the real deal
            if (bean instanceof Wrapper && ((Wrapper) bean).isWrapperFor(Catalog.class)) {
                return bean;
            }
            postProcessBeforeInitializationCatalog(bean);
        }

        if (bean instanceof GeoServer) {
            postProcessBeforeInitializationGeoServer(bean);
        }

        return bean;
    }

    private void activateAdminRole() {
        Collection<GrantedAuthority> roles = new ArrayList<>();
        roles.add(GeoServerRole.ADMIN_ROLE);
        roles.add(GeoServerRole.AUTHENTICATED_ROLE);
        UsernamePasswordAuthenticationToken auth =
                new UsernamePasswordAuthenticationToken(GeoServerUser.ROOT_USERNAME, null, roles);
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void postProcessBeforeInitializationGeoServer(Object bean) {
        geoserver = (GeoServer) bean;
        try {
            // setup ADMIN_ROLE security context to load secured resources
            activateAdminRole();

            XStreamPersister xp = xpf.createXMLPersister();
            xp.setCatalog(geoserver.getCatalog());
            loadGeoServer(geoserver, xp);

            // load initializers
            loadInitializers(geoserver);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // clear security context
            SecurityContextHolder.clearContext();
        }
        // initialize();
    }

    private void postProcessBeforeInitializationCatalog(Object bean) {
        // load
        try {
            // setup ADMIN_ROLE security context to load secured resources
            activateAdminRole();

            Catalog catalog = (Catalog) bean;
            XStreamPersister xp = xpf.createXMLPersister();
            xp.setCatalog(catalog);
            loadCatalog(catalog, xp);

            // initialize styles
            initializeStyles(catalog, xp);
        } catch (Exception e) {
            throw new RuntimeException(e);
        } finally {
            // clear security context
            SecurityContextHolder.clearContext();
        }
    }

    protected abstract void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception;

    protected abstract void loadGeoServer(final GeoServer geoServer, XStreamPersister xp) throws Exception;

    protected void loadInitializers(GeoServer geoServer) throws Exception {
        // load initializer extensions
        List<GeoServerInitializer> initializers = GeoServerExtensions.extensions(GeoServerInitializer.class);
        for (GeoServerInitializer initer : initializers) {
            try {
                initer.initialize(geoServer);
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to run initializer " + initer, t);
            }
        }
    }

    /**
     * Notify application initializers that reload is about to start.
     *
     * <p>Allows initializers to pause listening for configuration changes until the reload is completed.
     *
     * @param geoServer GeoServer being reloaded
     */
    protected void beforeReloadInitializers(GeoServer geoServer) {
        // unload applicable initializer extensions at start up reload process
        List<GeoServerReinitializer> initializers = GeoServerExtensions.extensions(GeoServerReinitializer.class);
        for (GeoServerReinitializer initer : initializers) {
            try {
                initer.beforeReinitialize(geoServer);
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to run initializer " + initer, t);
            }
        }
    }

    /**
     * Reload application initializers at the end of the reload process.
     *
     * <p>Allows initializers to resume listening for configuration changes.
     *
     * @param geoServer GeoServer being reloaded
     */
    protected void reloadInitializers(GeoServer geoServer) {
        // reload applicable initializer extensions at end of reload process
        List<GeoServerReinitializer> initializers = GeoServerExtensions.extensions(GeoServerReinitializer.class);
        for (GeoServerReinitializer initer : initializers) {
            try {
                initer.reinitialize(geoServer);
            } catch (Throwable t) {
                LOGGER.log(Level.SEVERE, "Failed to run initializer " + initer, t);
            }
        }
    }

    /** Does some post processing on the catalog to ensure that the "well-known" styles are always around. */
    protected void initializeStyles(Catalog catalog, XStreamPersister xp) throws IOException {
        initializeDefaultStyles(catalog);
    }

    protected void initializeDefaultStyles(Catalog catalog) throws IOException {
        if (catalog.getStyleByName(StyleInfo.DEFAULT_POINT) == null) {
            initializeStyle(catalog, StyleInfo.DEFAULT_POINT, "default_point.sld");
        }
        if (catalog.getStyleByName(StyleInfo.DEFAULT_LINE) == null) {
            initializeStyle(catalog, StyleInfo.DEFAULT_LINE, "default_line.sld");
        }
        if (catalog.getStyleByName(StyleInfo.DEFAULT_POLYGON) == null) {
            initializeStyle(catalog, StyleInfo.DEFAULT_POLYGON, "default_polygon.sld");
        }
        if (catalog.getStyleByName(StyleInfo.DEFAULT_RASTER) == null) {
            initializeStyle(catalog, StyleInfo.DEFAULT_RASTER, "default_raster.sld");
        }
        if (catalog.getStyleByName(StyleInfo.DEFAULT_GENERIC) == null) {
            initializeStyle(catalog, StyleInfo.DEFAULT_GENERIC, "default_generic.sld");
        }
    }

    /** Copies a well known style out to the data directory and adds a catalog entry for it. */
    void initializeStyle(Catalog catalog, String styleName, String sld) throws IOException {

        // copy the file out to the data directory if necessary
        Resource styleResource = resourceLoader.get(Paths.path("styles", sld));
        if (!Resources.exists(styleResource)) {
            try (InputStream in = GeoServerLoader.class.getResourceAsStream(sld);
                    OutputStream out = styleResource.out()) {
                IOUtils.copy(in, out);
            }
        }

        // create a style for it
        StyleInfo s = catalog.getFactory().createStyle();
        s.setName(styleName);
        s.setFilename(sld);
        catalog.add(s);
    }

    public void reload() throws Exception {
        destroy();

        beforeReloadInitializers(geoserver);

        // reload catalog, make sure we reload the underlying catalog, not any wrappers
        Catalog catalog = geoserver.getCatalog();
        if (catalog instanceof Wrapper) {
            catalog = ((Wrapper) geoserver.getCatalog()).unwrap(Catalog.class);
        }

        XStreamPersister xp = xpf.createXMLPersister();
        xp.setCatalog(catalog);

        loadCatalog(catalog, xp);
        loadGeoServer(geoserver, xp);

        reloadInitializers(geoserver);
    }

    protected void readCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        // we are going to synch up the catalogs and need to preserve listeners,
        // but these two fellas are attached to the new catalog as well
        catalog.removeListeners(ResourcePool.CacheClearingListener.class);
        catalog.removeListeners(GeoServerConfigPersister.class);
        catalog.removeListeners(GeoServerResourcePersister.class);
        // look for catalog.xml, if it exists assume we are dealing with
        // an old data directory
        Resource f = resourceLoader.get("catalog.xml");
        CatalogImpl catalog2;
        if (!Resources.exists(f)) {
            // assume 2.x style data directory
            Stopwatch sw = logStart();
            catalog2 = (CatalogImpl) readCatalog(xp);
            logStop(sw.stop(), catalog2);
        } else {
            // import old style catalog, register the persister now so that we start
            // with a new version of the catalog
            catalog2 = (CatalogImpl) readLegacyCatalog(f, xp);
        }
        List<CatalogListener> listeners = new ArrayList<>(catalog.getListeners());
        // make to remove the old resource pool catalog listener
        ((CatalogImpl) catalog).sync(catalog2);

        // attach back the old listeners
        for (CatalogListener listener : listeners) {
            catalog.addListener(listener);
        }
    }

    boolean checkStoresOnStartup(XStreamPersister xp) {
        Resource f = resourceLoader.get("global.xml");
        if (Resources.exists(f)) {
            try {
                GeoServerInfo global = depersist(xp, f, GeoServerInfo.class);
                final ResourceErrorHandling resourceErrorHandling = global.getResourceErrorHandling();
                return resourceErrorHandling != null
                        && !ResourceErrorHandling.SKIP_MISCONFIGURED_LAYERS.equals(resourceErrorHandling);
            } catch (IOException e) {
                LOGGER.log(Level.INFO, "Failed to determine the capabilities resource error handling", e);
            }
        }
        return true;
    }

    /** Reads the catalog from disk. */
    Catalog readCatalog(XStreamPersister xp) throws Exception {
        CatalogImpl catalog = new CatalogImpl();
        catalog.setResourceLoader(resourceLoader);
        xp.setCatalog(catalog);
        xp.setUnwrapNulls(false);

        // see if we really need to verify stores on startup
        boolean checkStores = checkStoresOnStartup(xp);
        if (!checkStores) {
            catalog.setExtendedValidation(false);
        }

        // global styles
        loadStyles(resourceLoader.get("styles"), catalog, xp);

        // workspaces, stores, and resources
        Resource workspaces = resourceLoader.get("workspaces");
        if (Resources.exists(workspaces)) {
            // do a first quick scan over all workspaces, setting the default
            Resource dws = workspaces.get("default.xml");
            WorkspaceInfo defaultWorkspace = null;
            if (Resources.exists(dws)) {
                try {
                    defaultWorkspace = depersist(xp, dws, WorkspaceInfo.class);
                    if (LOGGER.isLoggable(Level.CONFIG)) {
                        LOGGER.config("Loaded default workspace '" + defaultWorkspace.getName() + "'");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load default workspace", e);
                }
            } else {
                LOGGER.warning("No default workspace was found.");
            }

            List<Resource> workspaceList = workspaces.list().parallelStream()
                    .filter(r -> Resources.DirectoryFilter.INSTANCE.accept(r))
                    .collect(Collectors.toList());

            try (AsynchResourceIterator<WorkspaceContents> it = new AsynchResourceIterator<>(
                    workspaces, Resources.DirectoryFilter.INSTANCE, new WorkspaceMapper())) {
                while (it.hasNext()) {
                    WorkspaceContents wc = it.next();
                    WorkspaceInfo ws;
                    final Resource workspaceResource = wc.resource;
                    try {
                        ws = depersist(xp, wc.contents, WorkspaceInfo.class);
                        catalog.add(ws);
                        LOGGER.log(Level.CONFIG, () -> String.format("Loaded workspace '%s'", ws.getName()));
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to load workspace '" + workspaceResource.name() + "'", e);
                        continue;
                    }

                    // load the namespace
                    NamespaceInfo ns = null;
                    try {
                        ns = depersist(xp, wc.nsContents, NamespaceInfo.class);
                        catalog.add(ns);
                    } catch (Exception e) {
                        LOGGER.log(Level.WARNING, "Failed to load namespace for '" + workspaceResource.name() + "'", e);
                    }

                    // set the default workspace, this value might be null in the case of coming
                    // from a
                    // 2.0.0 data directory. See https://osgeo-org.atlassian.net/browse/GEOS-3440
                    if (defaultWorkspace != null) {
                        if (ws.getName().equals(defaultWorkspace.getName())) {
                            catalog.setDefaultWorkspace(ws);
                            if (ns != null) {
                                catalog.setDefaultNamespace(ns);
                            }
                        }
                    } else {
                        // create the default.xml file
                        defaultWorkspace = catalog.getDefaultWorkspace();
                        if (defaultWorkspace != null) {
                            try {
                                persist(xp, defaultWorkspace, dws);
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.WARNING,
                                        "Failed to persist default workspace '" + workspaceResource.name() + "'",
                                        e);
                            }
                        }
                    }

                    // load the styles for the workspace
                    Resource styles = workspaceResource.get("styles");
                    if (styles != null) {
                        loadStyles(styles, catalog, xp);
                    }
                }
            }

            // maps each store into a SingleResourceContents
            ResourceMapper<SingleResourceContents> storeMapper = sd -> {
                Resource f = sd.get("datastore.xml");
                if (Resources.exists(f)) {
                    return new SingleResourceContents(f, f.getContents());
                }
                f = sd.get("coveragestore.xml");
                if (Resources.exists(f)) {
                    return new SingleResourceContents(f, f.getContents());
                }
                f = sd.get("wmsstore.xml");
                if (Resources.exists(f)) {
                    return new SingleResourceContents(f, f.getContents());
                }
                f = sd.get("wmtsstore.xml");
                if (Resources.exists(f)) {
                    return new SingleResourceContents(f, f.getContents());
                }
                if (!isConfigDirectory(sd)) {
                    LOGGER.warning("Ignoring store directory '" + sd.name() + "'");
                }
                // nothing found
                return null;
            };

            for (Resource wsd : workspaceList) {
                // load the stores for this workspace
                try (AsynchResourceIterator<SingleResourceContents> it =
                        new AsynchResourceIterator<>(wsd, Resources.DirectoryFilter.INSTANCE, storeMapper)) {
                    while (it.hasNext()) {
                        SingleResourceContents SingleResourceContents = it.next();
                        final String resourceName = SingleResourceContents.resource.name();
                        if ("datastore.xml".equals(resourceName)) {
                            loadDataStore(SingleResourceContents, catalog, xp, checkStores);
                        } else if ("coveragestore.xml".equals(resourceName)) {
                            loadCoverageStore(SingleResourceContents, catalog, xp);
                        } else if ("wmsstore.xml".equals(resourceName)) {
                            loadWmsStore(SingleResourceContents, catalog, xp);
                        } else if ("wmtsstore.xml".equals(resourceName)) {
                            loadWmtsStore(SingleResourceContents, catalog, xp);
                        } else if (!isConfigDirectory(SingleResourceContents.resource)) {
                            LOGGER.warning("Ignoring store directory '" + SingleResourceContents.resource.name() + "'");
                            continue;
                        }
                    }
                }

                // load the layer groups for this workspace
                Resource layergroups = wsd.get("layergroups");
                if (layergroups != null) {
                    loadLayerGroups(layergroups, catalog, xp);
                }
            }
        } else {
            LOGGER.warning("No 'workspaces' directory found, unable to load any stores.");
        }

        // layergroups
        Resource layergroups = resourceLoader.get("layergroups");
        if (layergroups != null) {
            loadLayerGroups(layergroups, catalog, xp);
        }
        xp.setUnwrapNulls(true);
        catalog.resolve();
        // re-enable extended validation
        if (!checkStores) {
            catalog.setExtendedValidation(true);
        }
        return catalog;
    }

    private void loadWmsStore(SingleResourceContents SingleResourceContents, CatalogImpl catalog, XStreamPersister xp) {
        final Resource storeResource = SingleResourceContents.resource;
        WMSStoreInfo wms = null;
        try {
            wms = depersist(xp, SingleResourceContents.contents, WMSStoreInfo.class);
            catalog.add(wms);

            LOGGER.info("Loaded wmsstore '" + wms.getName() + "', " + (wms.isEnabled() ? "enabled" : "disabled"));
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load wms store '" + storeResource.name() + "'", e);
            return;
        }

        // load wms layers
        LayerLoader<WMSLayerInfo> coverageLoader = new LayerLoader<>(WMSLayerInfo.class, xp, catalog);
        try (AsynchResourceIterator<LayerContents> it = new AsynchResourceIterator<>(
                storeResource.parent(), Resources.DirectoryFilter.INSTANCE, WMS_LAYER_MAPPER)) {
            while (it.hasNext()) {
                LayerContents lc = it.next();
                coverageLoader.accept(lc);
            }
        }
    }

    private void loadWmtsStore(
            SingleResourceContents SingleResourceContents, CatalogImpl catalog, XStreamPersister xp) {
        final Resource storeResource = SingleResourceContents.resource;
        WMTSStoreInfo wmts = null;
        try {
            wmts = depersist(xp, SingleResourceContents.contents, WMTSStoreInfo.class);
            catalog.add(wmts);

            LOGGER.info("Loaded wmtsstore '" + wmts.getName() + "'");
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load wmts store '" + storeResource.name() + "'", e);
            return;
        }

        // load wmts layers
        LayerLoader<WMTSLayerInfo> coverageLoader = new LayerLoader<>(WMTSLayerInfo.class, xp, catalog);
        try (AsynchResourceIterator<LayerContents> it = new AsynchResourceIterator<>(
                storeResource.parent(), Resources.DirectoryFilter.INSTANCE, WMTS_LAYER_MAPPER)) {
            while (it.hasNext()) {
                LayerContents lc = it.next();
                coverageLoader.accept(lc);
            }
        }
    }

    private void loadCoverageStore(
            SingleResourceContents SingleResourceContents, CatalogImpl catalog, XStreamPersister xp) {
        CoverageStoreInfo cs = null;
        final Resource storeResource = SingleResourceContents.resource;
        try {
            cs = depersist(xp, SingleResourceContents.contents, CoverageStoreInfo.class);
            catalog.add(cs);

            if (LOGGER.isLoggable(Level.INFO)) {
                LOGGER.config(
                        "Loaded coverage store '" + cs.getName() + "', " + (cs.isEnabled() ? "enabled" : "disabled"));
            }
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Failed to load coverage store '" + storeResource.name() + "'", e);
            return;
        }

        // load coverages
        LayerLoader<CoverageInfo> coverageLoader = new LayerLoader<>(CoverageInfo.class, xp, catalog);
        try (AsynchResourceIterator<LayerContents> it = new AsynchResourceIterator<>(
                storeResource.parent(), Resources.DirectoryFilter.INSTANCE, COVERAGE_LAYER_MAPPER)) {
            while (it.hasNext()) {
                LayerContents lc = it.next();
                coverageLoader.accept(lc);
            }
        }
    }

    private void loadDataStore(
            SingleResourceContents SingleResourceContents,
            CatalogImpl catalog,
            XStreamPersister xp,
            boolean checkStores) {
        final Resource storeResource = SingleResourceContents.resource;
        DataStoreInfo ds;
        try {
            ds = depersist(xp, SingleResourceContents.contents, DataStoreInfo.class);
            catalog.add(ds);

            if (LOGGER.isLoggable(Level.CONFIG)) {
                LOGGER.config("Loaded data store '" + ds.getName() + "', " + (ds.isEnabled() ? "enabled" : "disabled"));
            }

            if (checkStores && ds.isEnabled()) {
                // connect to the datastore to determine if we should disable it
                try {
                    ds.getDataStore(null);
                } catch (Throwable t) {
                    LOGGER.warning("Error connecting to '" + ds.getName() + "'. Disabling.");
                    LOGGER.log(Level.INFO, "", t);

                    ds.setError(t);
                    ds.setEnabled(false);
                }
            }
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING,
                    "Failed to load data store '" + storeResource.parent().name() + "'",
                    e);
            return;
        }

        // load feature types
        LayerLoader<FeatureTypeInfo> featureLoader = new LayerLoader<>(FeatureTypeInfo.class, xp, catalog);
        try (AsynchResourceIterator<LayerContents> it = new AsynchResourceIterator<>(
                storeResource.parent(), Resources.DirectoryFilter.INSTANCE, FEATURE_LAYER_MAPPER)) {
            while (it.hasNext()) {
                LayerContents lc = it.next();
                featureLoader.accept(lc);
            }
        }
    }

    /**
     * Some config directories in GeoServer are used to store workspace specific configurations, identify them so that
     * we don't log complaints about their existence
     */
    private boolean isConfigDirectory(Resource dir) {
        String name = dir.name();
        boolean result = "styles".equals(name) || "layergroups".equals(name);
        return result;
    }

    /** Reads the legacy (1.x) catalog from disk. */
    Catalog readLegacyCatalog(Resource f, XStreamPersister xp) throws Exception {
        Catalog catalog2 = new CatalogImpl();
        catalog2.setResourceLoader(resourceLoader);

        // add listeners now as a converter which will convert from the old style
        // data directory to the new
        GeoServerConfigPersister cp = new GeoServerConfigPersister(resourceLoader, xp);
        GeoServerResourcePersister rp = new GeoServerResourcePersister(catalog2);
        if (!legacy) {
            catalog2.addListener(cp);
            catalog2.addListener(rp);
        }

        LegacyCatalogImporter importer = new LegacyCatalogImporter(catalog2);
        importer.setResourceLoader(resourceLoader);
        importer.imprt(resourceLoader.getBaseDirectory());

        if (!legacy) {
            catalog2.removeListener(cp);
            catalog2.removeListener(rp);
        }

        if (!legacy) {
            // copy files from old feature type directories to new
            Resource featureTypesDir = resourceLoader.get("featureTypes");
            if (featureTypesDir != null) {
                LegacyCatalogReader creader = new LegacyCatalogReader();
                creader.read(f);
                Map<String, Map<String, Object>> dataStores = creader.dataStores();

                for (Resource featureTypeDir : featureTypesDir.list()) {
                    if (featureTypeDir.getType() != Type.DIRECTORY) {
                        continue;
                    }

                    Resource featureTypeInfo = featureTypeDir.get("info.xml");
                    if (!Resources.exists(featureTypeInfo)) {
                        continue;
                    }

                    LegacyFeatureTypeInfoReader reader = new LegacyFeatureTypeInfoReader();
                    reader.read(featureTypeInfo);

                    Map<String, Object> dataStore = dataStores.get(reader.dataStore());
                    if (dataStore == null) {
                        continue;
                    }

                    String namespace = (String) dataStore.get("namespace");
                    Resource destFeatureTypeDir =
                            resourceLoader.get(Paths.path("workspaces", namespace, reader.dataStore(), reader.name()));
                    if (destFeatureTypeDir != null) {
                        // copy all the files over
                        for (Resource file : featureTypeDir.list()) {
                            if (file.getType() == Type.RESOURCE && !featureTypeInfo.equals(file)) {
                                IOUtils.copy(
                                        file.in(),
                                        destFeatureTypeDir.get(file.name()).out());
                            }
                        }
                    }
                }
            }

            // rename catalog.xml
            f.renameTo(f.parent().get("catalog.xml.old"));
        }

        return catalog2;
    }

    protected void readConfiguration(GeoServer geoServer, XStreamPersister xp) throws Exception {
        // look for services.xml, if it exists assume we are dealing with
        // an old data directory
        Resource f = resourceLoader.get("services.xml");
        if (!Resources.exists(f)) {
            // assume 2.x style
            f = resourceLoader.get("global.xml");
            if (Resources.exists(f)) {
                try {
                    GeoServerInfo global = depersist(xp, f, GeoServerInfo.class);
                    geoServer.setGlobal(global);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load global configuration file '" + f.name() + "'", e);
                }
            }

            // load logging
            f = resourceLoader.get("logging.xml");
            if (Resources.exists(f)) {
                try {
                    LoggingInfo logging = depersist(xp, f, LoggingInfo.class);
                    geoServer.setLogging(logging);
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load logging configuration file '" + f.name() + "'", e);
                }
            }

            // load workspace specific settings
            Resource workspaces = resourceLoader.get("workspaces");
            if (Resources.exists(workspaces)) {
                for (Resource dir : workspaces.list()) {
                    if (dir.getType() != Type.DIRECTORY) continue;

                    f = dir.get("settings.xml");
                    if (Resources.exists(f)) {
                        try {
                            SettingsInfo settings = depersist(xp, f, SettingsInfo.class);
                            geoServer.add(settings);
                        } catch (Exception e) {
                            LOGGER.log(
                                    Level.WARNING,
                                    "Failed to load configuration file '" + f.name() + "' for workspace " + dir.name(),
                                    e);
                        }
                    }
                }
            }

            // load services
            @SuppressWarnings("unchecked")
            final List<XStreamServiceLoader<ServiceInfo>> loaders =
                    (List) GeoServerExtensions.extensions(XStreamServiceLoader.class);
            loadServices(resourceLoader.get(""), true, loaders, geoServer);

            // load services specific to workspace
            if (workspaces != null) {
                for (Resource dir : workspaces.list()) {
                    if (dir.getType() != Type.DIRECTORY) continue;

                    loadServices(dir, false, loaders, geoServer);
                }
            }

        } else {
            // add listener now as a converter which will convert from the old style
            // data directory to the new
            GeoServerConfigPersister p = new GeoServerConfigPersister(resourceLoader, xp);
            geoServer.addListener(p);

            // import old style services.xml
            new LegacyConfigurationImporter(geoServer).imprt(resourceLoader.getBaseDirectory());

            geoServer.removeListener(p);

            // rename the services.xml file
            f.renameTo(f.parent().get("services.xml.old"));
        }
    }

    void loadStyles(Resource styles, Catalog catalog, XStreamPersister xp) throws IOException {
        Filter<Resource> styleFilter = r -> XML_FILTER.accept(r) && !Resources.exists(styles.get(r.name() + ".xml"));
        try (AsynchResourceIterator<SingleResourceContents> it =
                new AsynchResourceIterator<>(styles, styleFilter, RESOURCE_MAPPER)) {
            while (it.hasNext()) {
                SingleResourceContents r = it.next();
                try {
                    StyleInfo s = depersist(xp, r.contents, StyleInfo.class);
                    catalog.add(s);

                    if (LOGGER.isLoggable(Level.INFO)) {
                        LOGGER.config("Loaded style '" + s.getName() + "'");
                    }
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load style" + r.resource.name(), e);
                }
            }
        }
    }

    void loadLayerGroups(Resource layerGroups, Catalog catalog, XStreamPersister xp) {
        try (AsynchResourceIterator<SingleResourceContents> it =
                new AsynchResourceIterator<>(layerGroups, XML_FILTER, RESOURCE_MAPPER)) {
            while (it.hasNext()) {
                SingleResourceContents r = it.next();
                try {
                    LayerGroupInfo lg = depersist(xp, r.contents, LayerGroupInfo.class);
                    if (lg.getLayers() == null || lg.getLayers().isEmpty()) {
                        LOGGER.warning("Skipping empty layer group '" + lg.getName() + "', it is invalid");
                        continue;
                    }
                    catalog.add(lg);

                    LOGGER.config("Loaded layer group '" + lg.getName() + "'");
                } catch (Exception e) {
                    LOGGER.log(Level.WARNING, "Failed to load layer group " + r.resource.name(), e);
                }
            }
        }
    }

    void loadServices(
            Resource directory, boolean global, List<XStreamServiceLoader<ServiceInfo>> loaders, GeoServer geoServer) {
        for (XStreamServiceLoader<ServiceInfo> l : loaders) {
            try {
                ServiceInfo s = l.load(geoServer, directory);
                if (!global && s.getWorkspace() == null) continue;

                geoServer.add(s);

                LOGGER.config("Loaded service '" + s.getId() + "', " + (s.isEnabled() ? "enabled" : "disabled"));
            } catch (Throwable t) {
                if (Resources.exists(directory)) {
                    LOGGER.log(
                            Level.SEVERE,
                            "Failed to load the service configuration in directory: "
                                    + directory
                                    + " with loader for "
                                    + l.getServiceClass(),
                            t);
                } else {
                    LOGGER.log(
                            Level.SEVERE,
                            "Failed to load the root service configuration with loader for " + l.getServiceClass(),
                            t);
                }
            }
        }
    }

    /** Helper method which uses xstream to persist an object as xml on disk. */
    void persist(XStreamPersister xp, Object obj, Resource f) throws Exception {
        try (BufferedOutputStream out = new BufferedOutputStream(f.out())) {
            xp.save(obj, out);
            out.flush();
        }
    }

    /** Helper method which uses xstream to depersist an object as xml from disk. */
    <T> T depersist(XStreamPersister xp, Resource f, Class<T> clazz) throws IOException {
        try (InputStream in = new ByteArrayInputStream(f.getContents())) {
            return xp.load(in, clazz);
        }
    }

    /** Helper method which uses xstream to depersist an object as xml from disk. */
    static <T> T depersist(XStreamPersister xp, byte[] contents, Class<T> clazz) throws IOException {
        try (InputStream in = new ByteArrayInputStream(contents)) {
            return xp.load(in, clazz);
        }
    }

    public void destroy() {
        // dispose
        if (geoserver != null) {
            geoserver.dispose();
        }
    }

    protected Stopwatch logStart() {
        LOGGER.log(Level.CONFIG, "Loading catalog {0}", resourceLoader.getBaseDirectory());
        return Stopwatch.createStarted();
    }

    protected void logStop(Stopwatch stoppedSw, final Catalog catalog) {
        Supplier<String> msg = () -> String.format(
                "Read Catalog in %s: workspaces: %,d, namespaces: %,d, styles: %,d, stores: %,d, resources: %,d, layers: %,d, layer groups: %,d.",
                stoppedSw,
                catalog.count(WorkspaceInfo.class, Predicates.acceptAll()),
                catalog.count(NamespaceInfo.class, Predicates.acceptAll()),
                catalog.count(StyleInfo.class, Predicates.acceptAll()),
                catalog.count(StoreInfo.class, Predicates.acceptAll()),
                catalog.count(ResourceInfo.class, Predicates.acceptAll()),
                catalog.count(LayerInfo.class, Predicates.acceptAll()),
                catalog.count(LayerGroupInfo.class, Predicates.acceptAll()));
        LOGGER.info(msg);
    }
}
