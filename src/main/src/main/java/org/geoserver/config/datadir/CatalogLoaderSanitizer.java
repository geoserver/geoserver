/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import static java.lang.String.format;
import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.AbstractCatalogFacade;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.LayerGroupStyle;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.util.logging.Logging;
import org.springframework.lang.Nullable;

/**
 * Helper class for {@link CatalogLoader} that sanitizes and validates various catalog operations before objects can be
 * safely added to the {@link Catalog}.
 *
 * <p>This class handles tasks such as setting the default workspace, initializing default styles, validating style
 * references, and resolving proxy references between catalog objects.
 */
class CatalogLoaderSanitizer {

    private static final Logger LOGGER =
            Logging.getLogger(CatalogLoaderSanitizer.class.getPackage().getName());

    private CatalogLoader loader;

    /** @param loader the CatalogLoader to work with */
    public CatalogLoaderSanitizer(CatalogLoader loader) {
        this.loader = loader;
    }

    /**
     * Sets the default workspace in the catalog.
     *
     * <p>This method handles the case where a default workspace file exists and loads the referenced workspace, or
     * creates a new default workspace if none exists. This is particularly important when migrating from older
     * GeoServer versions (2.0.0) where the default workspace might be null.
     *
     * @see <a href="https://osgeo-org.atlassian.net/browse/GEOS-3440">GEOS-3440</a>
     */
    public void setDefaultWorkspace() {
        // set the default workspace, this value might be null in the case of coming
        // from a 2.0.0 data directory. See https://osgeo-org.atlassian.net/browse/GEOS-3440
        final Catalog catalog = loader.catalog;
        final DataDirectoryWalker fileWalk = loader.fileWalk;
        Optional<Path> defaultWorkspaceFile = fileWalk.defaultWorkspace();

        if (defaultWorkspaceFile.isPresent()) {
            Optional<WorkspaceInfo> defaultWorkspace = loader.depersist(defaultWorkspaceFile.orElseThrow());
            defaultWorkspace = defaultWorkspace.map(WorkspaceInfo::getId).map(catalog::getWorkspace);
            if (defaultWorkspace.isPresent()) {
                WorkspaceInfo ws = defaultWorkspace.orElseThrow();
                NamespaceInfo ns = catalog.getNamespaceByPrefix(ws.getName());
                catalog.setDefaultWorkspace(ws);
                catalog.setDefaultNamespace(ns);
                return;
            }
        }

        // There's no default workspace, assign one.
        fileWalk.lock();
        try {
            if (fileWalk.defaultWorkspace().isEmpty()) {
                WorkspaceInfo newDefault = findFirstWorkspace();
                if (newDefault != null) {
                    Path path = fileWalk.getDefaultWorkspaceFile();
                    try {
                        loader.persist(newDefault, path);
                        // set the ns too, there's no NamespaceWorkspaceConsistencyListener at this stage
                        // if the ws exists, we've already made sure the ns exists, see #loadWorkspace
                        NamespaceInfo ns = requireNonNull(catalog.getNamespaceByPrefix(newDefault.getName()));
                        catalog.setDefaultWorkspace(newDefault);
                        catalog.setDefaultNamespace(ns);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to persist " + newDefault + " at '" + path + "'", e);
                    }
                }
            }
        } finally {
            fileWalk.unlock();
        }
    }

    /**
     * Determine which workspace to assign as the default. Do not trust the default workspace being the first one added,
     * they are appended to the catalog concurrently. Nor trust the order the files were traversed in.
     */
    private WorkspaceInfo findFirstWorkspace() {
        Catalog catalog = loader.catalog;
        SortBy sortByName = Predicates.sortBy("name", true);
        try (CloseableIterator<WorkspaceInfo> list =
                catalog.list(WorkspaceInfo.class, Predicates.acceptAll(), 0, 1, sortByName)) {
            if (list.hasNext()) {
                return list.next();
            }
        }
        return null;
    }

    /**
     * Initializes the default styles in the catalog if they don't already exist.
     *
     * <p>This method ensures that the standard default styles (point, line, polygon, raster, generic) are available in
     * the catalog. These styles are used as fallbacks when specific styles are not available.
     *
     * @throws IOException if there's an error writing the style files to the data directory
     */
    public void initializeDefaultStyles() throws IOException {
        initializeStyle(StyleInfo.DEFAULT_POINT, "default_point.sld");
        initializeStyle(StyleInfo.DEFAULT_LINE, "default_line.sld");
        initializeStyle(StyleInfo.DEFAULT_POLYGON, "default_polygon.sld");
        initializeStyle(StyleInfo.DEFAULT_RASTER, "default_raster.sld");
        initializeStyle(StyleInfo.DEFAULT_GENERIC, "default_generic.sld");
    }

    /**
     * Copies a well known style out to the data directory and adds a catalog entry for it.
     *
     * @throws IOException if the style cannot be written to the data directory
     */
    void initializeStyle(String styleName, String sld) throws IOException {
        final Catalog catalog = loader.catalog;
        if (catalog.getStyleByName(styleName) != null) {
            return;
        }
        final DataDirectoryWalker fileWalk = loader.fileWalk;
        fileWalk.lock();
        try {
            if (catalog.getStyleByName(styleName) == null) {
                copySld(styleName, sld);
                // create a style for it
                StyleInfo s = catalog.getFactory().createStyle();
                s.setName(styleName);
                s.setFilename(sld);
                catalog.add(s);

                Resource resource = fileWalk.getDataDirectory().config(s);
                Path path = resource.file().toPath();
                loader.persist(s, path);
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error initializing default style %s" + styleName, e);
        } finally {
            fileWalk.unlock();
        }
    }

    private void copySld(String styleName, String sld) throws IOException {
        // copy the file out to the data directory if necessary
        GeoServerDataDirectory dataDirectory = loader.fileWalk.getDataDirectory();
        Resource styleResource = dataDirectory.getStyles(sld);
        if (!Resources.exists(styleResource)) {
            try (InputStream in = GeoServerLoader.class.getResourceAsStream(sld);
                    OutputStream out = styleResource.out()) {
                IOUtils.copy(in, out);
            }
        }
    }

    /**
     * Validates that a style belongs to the expected workspace.
     *
     * <p>This method checks if a style's workspace matches the expected workspace:
     *
     * <ul>
     *   <li>If expectedWorkspace is null, the style should have no workspace
     *   <li>If expectedWorkspace is not null, the style should belong to that workspace
     * </ul>
     *
     * @param expectedWorkspace the workspace the style should belong to, or null if the style should be global
     * @param style the style to validate
     * @return true if the style's workspace is valid, false otherwise
     */
    public boolean validate(@Nullable WorkspaceInfo expectedWorkspace, StyleInfo style) {
        if (null == expectedWorkspace) {
            if (style.getWorkspace() != null) {
                String ws = style.getWorkspace().getId();
                String msg = "Style %s (%s) is expected to have no workspace but has workspace %s. Style ignored.";
                LOGGER.severe(format(msg, style.getName(), style.getId(), ws));
                return false;
            }
        } else {
            if (style.getWorkspace() == null) {
                String msg = "Style %s[%s] should have workspace %s but has no workspace. Style ignored.";
                LOGGER.severe(format(msg, style.getName(), style.getId(), expectedWorkspace.getName()));
                return false;
            } else if (!expectedWorkspace.getId().equals(style.getWorkspace().getId())) {
                String ws = style.getWorkspace().getId();
                String msg = "Style %s[%s] should have workspace %s but has workspace %s. Style ignored.";
                LOGGER.severe(format(msg, style.getName(), style.getId(), expectedWorkspace.getName(), ws));
                return false;
            }
        }
        return true;
    }

    /**
     * Aids {@link CatalogLoader#doAddToCatalog()} in resolving the {@link ResolvingProxy} links in {@code info}.
     *
     * <p>If a reference can't be resolved, an {@link IllegalStateException} is thrown, causing the object not being
     * added to the catalog.
     *
     * @throws IllegalStateException if a referred object is a {@link ResolvingProxy} and can't be dereferenced in the
     *     catalog
     */
    @SuppressWarnings("PMD.EmptyControlStatement")
    public void resolveProxies(CatalogInfo info) {
        if (info instanceof WorkspaceInfo) {
            // no-op
        } else if (info instanceof NamespaceInfo) {
            // no-op
        } else if (info instanceof StoreInfo) {
            StoreInfo store = (StoreInfo) info;
            store.setWorkspace(resolveProxy(store.getWorkspace(), info));
        } else if (info instanceof ResourceInfo) {
            // note, gotta use ResourceInfoImpl.rawStore() cause the subclasses will override getStore() with a cast to
            // their concrete store types that will fail when the store is a ResolvingProxy
            ResourceInfoImpl res = (ResourceInfoImpl) info;
            res.setStore(resolveProxy(res.rawStore(), res));
            res.setNamespace(resolveProxy(res.getNamespace(), res));
        } else if (info instanceof LayerInfo) {
            resolveProxies((LayerInfo) info);
        } else if (info instanceof LayerGroupInfo) {
            resolveProxies((LayerGroupInfo) info);
        } else if (info instanceof StyleInfo) {
            StyleInfo style = (StyleInfo) info;
            style.setWorkspace(resolveProxy(style.getWorkspace(), info));
        } else {
            LOGGER.warning("Unknown CatalogInfo: " + info);
        }
    }

    /** Resolves the resource, missing styles are removed from the list of styles */
    private void resolveProxies(LayerInfo l) {
        l.setResource(resolveProxy(l.getResource(), l));
        resolveDefaultStyle(l);

        Set<StyleInfo> styles = l.getStyles();
        boolean resolveStyles = styles != null
                && !styles.isEmpty()
                && !(l.getResource() instanceof WMSLayerInfo); // avoid loading remote styles
        if (resolveStyles) {
            List<StyleInfo> resolved = styles.stream()
                    .map(s -> resolveProxy(s, l, false))
                    .filter(Objects::nonNull)
                    .collect(Collectors.toList());
            styles.clear();
            styles.addAll(resolved);
        }
    }

    private void resolveDefaultStyle(LayerInfo l) {
        StyleInfo defaultStyle = l.getDefaultStyle();
        if (defaultStyle == null) {
            LOGGER.severe(format("Layer %s has no default style", l.prefixedName()));
        } else {
            // note: would it be better to just assign a default style here for a dangling proxy? but there's a test
            // (org.geoserver.catalog.impl.CatalogProxiesTest) that actually checks that it resolved to null
            if (null == resolveProxy(defaultStyle, l, false)) {
                l.setDefaultStyle(null);
                LOGGER.severe(
                        format("Layer %s has a dangling default style %s", l.prefixedName(), defaultStyle.getId()));
            }
        }
        l.setDefaultStyle(defaultStyle);
    }

    /**
     * Resolves all proxy references in a LayerGroupInfo, except layers and styles, that are resolved by
     * {@link AbstractCatalogFacade#add}.
     *
     * <p>This includes the workspace, root layer, root layer style, and all contained layers and styles.
     *
     * @param layerGroup the layer group to resolve references for
     */
    protected void resolveProxies(LayerGroupInfo layerGroup) {
        LayerGroupInfoImpl lg = (LayerGroupInfoImpl) layerGroup;

        lg.setWorkspace(resolveProxy(lg.getWorkspace(), lg, false));
        lg.setRootLayer(resolveProxy(lg.getRootLayer(), lg, false));
        lg.setRootLayerStyle(resolveProxy(lg.getRootLayerStyle(), lg, false));

        if (lg.getLayers() != null && !lg.getLayers().isEmpty()) {
            resolveLayerGroupLayers(lg.getLayers(), lg);
        }
        if (lg.getStyles() != null && !lg.getStyles().isEmpty()) {
            resolveLayerGroupStyles(lg.getLayers(), lg.getStyles(), lg);
        }

        // now resolves layers and styles defined in layer group styles
        for (LayerGroupStyle groupStyle : lg.getLayerGroupStyles()) {
            resolveLayerGroupLayers(groupStyle.getLayers(), lg);
            resolveLayerGroupStyles(groupStyle.getLayers(), groupStyle.getStyles(), lg);
        }
    }

    /**
     * Resolves proxy references for styles in a layer group
     *
     * <p>This method handles the special case where a style in a layer group might represent a LayerGroupStyle name
     * rather than a regular catalog style.
     *
     * @param assignedLayers the layers that the styles are assigned to
     * @param styles the styles to resolve
     */
    private void resolveLayerGroupStyles(
            List<PublishedInfo> assignedLayers, List<StyleInfo> styles, LayerGroupInfo referrer) {
        CatalogImpl catalog = loader.catalog;
        for (int i = 0; i < styles.size(); i++) {
            StyleInfo style = styles.get(i);
            if (style == null) {
                continue;
            }
            PublishedInfo assignedLayer = assignedLayers.get(i);
            StyleInfo resolved = null;
            if (assignedLayer instanceof LayerGroupInfo) {
                // Special case we might have a StyleInfo representing only the name of a LayerGroupStyle thus not
                // present in Catalog.
                // We take the ref and create a new object without searching in catalog.
                String ref = ResolvingProxy.getRef(style);
                if (ref != null) {
                    StyleInfo styleInfo = new StyleInfoImpl(catalog);
                    styleInfo.setName(ref);
                    resolved = styleInfo;
                }
            }
            if (resolved == null) resolved = resolveProxy(style, referrer, false);

            styles.set(i, resolved);
        }
    }

    /**
     * Resolves proxy references for layers in a layer group.
     *
     * <p>This method handles the special case during catalog loading where nested published items might not be loaded
     * yet, keeping the original proxy reference in such cases.
     *
     * @param layers the list of layers to resolve references for
     * @param referrer object that links to layers, for logging purposes
     */
    private void resolveLayerGroupLayers(List<PublishedInfo> layers, LayerGroupInfo referrer) {
        for (int i = 0; i < layers.size(); i++) {
            PublishedInfo layer = layers.get(i);
            if (layer == null) {
                continue;
            }
            PublishedInfo resolved = resolveProxy(layer, referrer, false);
            if (resolved == null && (layer instanceof LayerInfo || layer instanceof LayerGroupInfo)) {
                // special case to handle catalog loading, when nested publishibles might not be
                // loaded.
                resolved = layer;
            }
            layers.set(i, resolved);
        }
    }

    /** @throws IllegalStateException if the {@code proxy} does not resolve to an object in the catalog */
    private <I extends CatalogInfo> I resolveProxy(I proxy, CatalogInfo referrer) {
        return resolveProxy(proxy, referrer, true);
    }

    /**
     * @param proxy the potential {@link ResolvingProxy proxy} reference to resolve
     * @param referrer the object holding a reference to {@code proxy}, only used for logging
     * @param fail whether to fail with an {@code IllegalStateException} if the {@code proxy} can't be resolved to an
     *     object in the catalog
     * @return The resolved object, {@code null} if {@code proxy} itself was {@code null}, or {@code fail == false} and
     *     the object didn't resolve
     * @throws IllegalStateException if the {@code proxy} does not resolve to an object in the catalog and {@code fail
     *     == true}
     */
    private <I extends CatalogInfo> I resolveProxy(I proxy, CatalogInfo referrer, boolean fail) {
        if (proxy == null) return null;
        CatalogImpl catalog = loader.catalog;
        I resolved = ResolvingProxy.resolve(catalog, proxy);
        if (resolved == null && fail) {
            String msg = format(
                    "%s[%s] has a missing link to %s[%s]. Object ignored.",
                    referrer, referrer.getId(), typeOf(proxy), proxy.getId());
            LOGGER.severe(msg);
            throw new IllegalStateException(msg);
        }
        return resolved;
    }

    /**
     * Returns a human-readable string representation of the catalog object type for logging purposes.
     *
     * @param info the catalog object (usually a {@link ResolvingProxy}) to get the type for
     */
    public <I extends CatalogInfo> String typeOf(I info) {
        if (info instanceof WorkspaceInfo) return "workspace";
        if (info instanceof NamespaceInfo) return "namespace";
        if (info instanceof DataStoreInfo) return "data store";
        if (info instanceof CoverageStoreInfo) return "coverage store";
        if (info instanceof WMSStoreInfo) return "WMS store";
        if (info instanceof WMTSStoreInfo) return "WMTS store";
        if (info instanceof FeatureTypeInfo) return "feature type";
        if (info instanceof CoverageInfo) return "coverage";
        if (info instanceof WMSLayerInfo) return "WMS layer";
        if (info instanceof WMTSLayerInfo) return "WMTS layer";
        if (info instanceof LayerInfo) return "layer";
        if (info instanceof LayerGroupInfo) return "layer group";
        if (info instanceof StyleInfo) return "style";
        return "unknown type";
    }
}
