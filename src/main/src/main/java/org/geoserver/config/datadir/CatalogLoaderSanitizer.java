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
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.ehcache.config.ResourcePool;
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
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.catalog.impl.ResolvingProxyResolver;
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
                copySld(sld);
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

    private void copySld(String sld) throws IOException {
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

    /** Aids {@link CatalogLoader#doAddToCatalog()} in resolving the {@link ResolvingProxy} links in {@code info}. */
    public void resolveProxies(CatalogInfo info) {
        CatalogImpl catalog = loader.catalog;
        catalog.resolve(info); // only ensures collection properties are initialized if null
        ResolvingProxyResolver.resolve(info, catalog); // resolves proxies

        if (info instanceof LayerInfo) {
            sanitize(catalog, (LayerInfo) info);
        }
    }

    /**
     * If the style is missing associate a default one, to avoid breaking WMS
     *
     * <p>Note we really need to avoid {@link CatalogImpl#add(LayerInfo)} calling {@link CatalogImpl#validate(LayerInfo,
     * boolean)}, for it'll end up calling {@link FeatureTypeInfo#getFeatureType()} ->
     * {@link ResourcePool#getFeatureType()} -> {@link ResourcePool#getDataStore()}, which will dead-lock when hit by
     * the first time under concurrency.
     *
     * <p>For the time being then we'll assign the {@link StyleInfo#DEFAULT_GENERIC} style instead, pending fixing the
     * above mentiond race condition.
     */
    private void sanitize(CatalogImpl catalog, LayerInfo layer) {
        if (layer.getDefaultStyle() == null) {
            LOGGER.warning(() ->
                    format("Layer %s is missing the default style, assigning one automatically", layer.prefixedName()));
            ResourceInfo resource = layer.getResource();
            String styleName;
            if (resource instanceof FeatureTypeInfo) {
                styleName = StyleInfo.DEFAULT_GENERIC;
            } else { // coverage, wms, wmts
                styleName = StyleInfo.DEFAULT_RASTER;
            }
            layer.setDefaultStyle(catalog.getStyleByName(styleName));
        }
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
