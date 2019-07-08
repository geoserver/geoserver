/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.apache.commons.lang3.SystemUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.data.DataUtilities;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.DefaultResourceLocator;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Mark;
import org.geotools.styling.ResourceLocator;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.URLs;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.xml.sax.EntityResolver;

/**
 * File or Resource access to GeoServer data directory. In addition to paths Catalog obhjects such
 * as workspace or FeatureTypeInfo can be used to locate resources.
 *
 * <p>Example usage:
 *
 * <pre>
 * <code>
 *   GeoServerDataDirectory dd = new GeoServerDataDirectory(resourceLoader);
 *
 *   //find some data
 *   File shp = dd.findDataFile( "shapefiles/somedata.shp" );
 *
 *   //create a directory for some data
 *   File shapefiles = dd.findOrCreateDataDirectory("shapefiles");
 *
 *   //find a template file for a feature type
 *   FeatureTypeInfo ftinfo = ...;
 *   File template = dd.findSuppResourceFile(ftinfo,"title.ftl");
 * </code>
 * </pre>
 *
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("unused")
public class GeoServerDataDirectory {

    /** resource loader */
    GeoServerResourceLoader resourceLoader;

    EntityResolverProvider entityResolverProvider;

    GeoServerResourceLocator resourceLocator;

    /** Creates the data directory specifying the resource loader. */
    public GeoServerDataDirectory(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /** Creates the data directory specifying the base directory. */
    public GeoServerDataDirectory(File baseDirectory) {
        this(new GeoServerResourceLoader(baseDirectory));
    }

    /** Returns the underlying resource loader. */
    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    public Resource get(String path) {
        return resourceLoader.get(path);
    }

    /** The root of the data directory. */
    public File root() {
        return resourceLoader.getBaseDirectory();
    }

    /**
     * Returns a directory under the {@link #root()} directory, if the directory does not exist it
     * will be created.
     *
     * @return directory (created if needed)
     */
    public File findOrCreateDir(String... location) throws IOException {
        return get(Paths.path(location)).dir();
    }

    /**
     * Returns a file under the {@link #root()} directory, if the file does not exist null is
     * returned.
     */
    public File findFile(String... location) throws IOException {
        Resource resource = get(Paths.path(location));
        return Resources.find(resource);
    }

    /**
     * Returns the root of the directory which contains spatial data files, if the directory does
     * exist, null is returned.
     *
     * <p>This directory is called 'data', and is located directly under {@link #root()}
     */
    public File findDataRoot() throws IOException {
        Resource directory = get("data");
        return Resources.directory(directory);
    }

    /**
     * Returns the root of the directory which contains spatial data files, if the directory does
     * not exist it will be created.
     *
     * <p>This directory is called 'data', and is located directly under {@link #root()}
     */
    public File findOrCreateDataRoot() throws IOException {
        Resource directory = get("data");
        return directory.dir(); // will create directory as needed
    }

    /**
     * Returns a directory under the {@link #dataRoot()} directory, if the directory does not exist
     * null will be returned.
     */
    public File findDataDir(String... location) throws IOException {
        Resource resource = get(Paths.path("data", Paths.path(location)));
        return Resources.directory(resource);
    }

    /**
     * Returns a directory under the {@link #dataRoot()} directory, if the directory does not exist
     * it will be created.
     */
    public File findOrCreateDataDir(String... location) throws IOException {
        Resource resource = get(Paths.path("data", Paths.path(location)));
        return resource.dir();
    }

    /**
     * Returns a file under the {@link #dataRoot()} directory, if the file does not exist null is
     * returned.
     */
    public File findDataFile(String... location) throws IOException {
        Resource resource = get(Paths.path("data", Paths.path(location)));
        return Resources.file(resource);
    }

    /**
     * Finds the directory for the resource assuming a 1.x style data directory.
     *
     * <p>Something like:
     *
     * <pre>
     * featureTypes/states_shapefile_states
     * coverages/sfdem_dem
     * </pre>
     *
     * @param resource The resource.
     * @return The directory for the resource, or null if it could not be found.
     */
    public File findLegacyResourceDir(ResourceInfo resource) throws IOException {
        StoreInfo store = resource.getStore();
        String dirname = store.getName() + "_" + resource.getName();
        File dir = null;
        if (resource instanceof FeatureTypeInfo) {
            dir = resourceLoader.find("featureTypes", dirname);
        } else if (resource instanceof CoverageInfo) {
            dir = resourceLoader.find("coverages", dirname);
        }

        return dir != null ? dir : null;
    }

    // Resource lookup methods
    static final String WORKSPACE_XML = "workspace.xml";
    static final String NAMESPACE_XML = "namespace.xml";
    static final String DATASTORE_XML = "datastore.xml";
    static final String COVERAGESTORE_XML = "coveragestore.xml";
    static final String WMSSTORE_XML = "wmsstore.xml";
    static final String WMTSSTORE_XML = "wmtsstore.xml";
    static final String FEATURETYPE_XML = "featuretype.xml";
    static final String COVERAGE_XML = "coverage.xml";
    static final String WMSLAYER_XML = "wmslayer.xml";
    static final String WMTSLAYER_XML = "wmtslayer.xml";
    static final String LAYER_XML = "layer.xml";
    static final String WORKSPACE_DIR = "workspaces";
    static final String LAYERGROUP_DIR = "layergroups";
    static final String STYLE_DIR = "styles";
    static final String SECURITY_DIR = "security";

    /**
     * Retrieve a resource relative to the root of the data directory. An empty path will retrieve
     * the directory itself.
     *
     * @return A {@link Resource}
     */
    public @Nonnull Resource getRoot(String... path) {
        Resource r = get(Paths.path(path));
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the security directory. An empty path will retrieve the directory
     * itself.
     *
     * @return A {@link Resource}
     */
    public @Nonnull Resource getSecurity(String... path) {
        Resource r = get(Paths.path(SECURITY_DIR, Paths.path(path)));
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the workspaces directory. An empty path will retrieve the
     * directory itself.
     *
     * @return A {@link Resource}
     */
    public @Nonnull Resource getWorkspaces(String... path) {
        Resource r = get(Paths.path(WORKSPACE_DIR, Paths.path(path)));
        assert r != null;
        return r;
    }

    /**
     * Retrieve the configuration xml for the default workspace.
     *
     * @return A {@link Resource}
     */
    public @Nonnull Resource defaultWorkspaceConfig() {
        Resource r = getRoot("default.xml");
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the workspace configuration directory. An empty path will retrieve
     * the directory itself. A null workspace will retrieve the resouce in the global configuration
     * directory.
     *
     * @param ws The workspace
     * @return A {@link Resource}
     */
    public @Nonnull Resource get(WorkspaceInfo ws, String... path) {
        Resource r;
        if (ws == null) {
            r = get(Paths.path(path));
        } else {
            r = getWorkspaces(ws.getName(), Paths.path(path));
        }
        assert r != null;
        return r;
    }

    /**
     * Retrieve the workspace configuration XML as a Resource
     *
     * @param ws The workspace
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(WorkspaceInfo ws) {
        Resource r = get(ws, WORKSPACE_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the configuration directory of the workspace associated with a
     * namespace. An empty path will retrieve the directory itself. A null namespace will retrieve
     * the resouce in the global configuration directory.
     *
     * @param ns The namespace
     * @return A {@link Resource}
     */
    public @Nonnull Resource get(NamespaceInfo ns, String... path) {
        Resource r;
        if (ns == null) {
            r = get(Paths.path(path));
        } else {
            r = getWorkspaces(ns.getPrefix(), Paths.path(path));
        }
        assert r != null;
        return r;
    }

    /**
     * Retrieve the namespace configuration XML as a Resource
     *
     * @param ns The namespace
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(NamespaceInfo ns) {
        Resource r = get(ns, NAMESPACE_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the configuration directory of a Store. An empty path will
     * retrieve the directory itself.
     *
     * @param store The store
     * @return A {@link Resource}
     */
    public @Nonnull Resource get(StoreInfo store, String... path) {
        Resource r = get(store.getWorkspace(), store.getName(), Paths.path(path));
        assert r != null;
        return r;
    }

    /**
     * Retrieve the datastore configuration XML as a Resource
     *
     * @param ds The datastore
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(DataStoreInfo ds) {
        Resource r = get(ds, DATASTORE_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the coverage store configuration XML as a Resource
     *
     * @param cs The coverage store
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(CoverageStoreInfo cs) {
        Resource r = get(cs, COVERAGESTORE_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the WMS store configuration XML as a Resource
     *
     * @param wmss The coverage store
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(WMSStoreInfo wmss) {
        Resource r = get(wmss, WMSSTORE_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the WMTS store configuration XML as a Resource
     *
     * @param wmss The coverage store
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(WMTSStoreInfo wmss) {
        Resource r = get(wmss, WMTSSTORE_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the WMS store configuration XML as a Resource
     *
     * @param si The store
     * @return A {@link Resource}
     */
    private @Nonnull Resource config(StoreInfo si) {
        final Resource r;
        if (si instanceof DataStoreInfo) {
            r = config((DataStoreInfo) si);
        } else if (si instanceof CoverageStoreInfo) {
            r = config((CoverageStoreInfo) si);
        } else if (si instanceof WMTSStoreInfo) {
            r = config((WMTSStoreInfo) si);
        } else if (si instanceof WMSStoreInfo) {
            r = config((WMSStoreInfo) si);
        } else {
            // It'd be nice if we could be generic and cover potential future StoreInfo types.
            throw new IllegalArgumentException(
                    "Only DataStoreInfo, CoverageStoreInfo, and WMS/WMTSStoreInfo are supported.");
        }
        assert r != null;
        return r;
    }

    /**
     * Retrieve the resource configuration XML as a Resource
     *
     * @param si The resource
     * @return A {@link Resource}
     */
    private @Nonnull Resource config(ResourceInfo si) {
        final Resource r;
        if (si instanceof FeatureTypeInfo) {
            r = config((FeatureTypeInfo) si);
        } else if (si instanceof CoverageInfo) {
            r = config((CoverageInfo) si);
        } else if (si instanceof WMTSLayerInfo) {
            r = config((WMTSLayerInfo) si);
        } else if (si instanceof WMSLayerInfo) {
            r = config((WMSLayerInfo) si);
        } else {
            // It'd be nice if we could be generic and cover potential future ResourceInfo types.
            throw new IllegalArgumentException(
                    "Only FeatureTypeInfo, CoverageInfo, and WMS/WMTSLayerInfo are supported.");
        }
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the configuration directory of a Resource. An empty path will
     * retrieve the directory itself.
     *
     * @param ri The store
     * @return A {@link Resource}
     */
    public @Nonnull Resource get(ResourceInfo ri, String... path) {
        Resource r = get(ri.getStore(), ri.getName(), Paths.path(path));
        assert r != null;
        return r;
    }

    /**
     * Retrieve the feature type configuration XML as a Resource
     *
     * @param fti The feature type
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(FeatureTypeInfo fti) {
        Resource r = get(fti, FEATURETYPE_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the coverage configuration XML as a Resource
     *
     * @param c The feature type
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(CoverageInfo c) {
        Resource r = get(c, COVERAGE_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the WMS layer configuration XML as a Resource
     *
     * @param wmsl The feature type
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(WMSLayerInfo wmsl) {
        Resource r = get(wmsl, WMSLAYER_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the WMS layer configuration XML as a Resource
     *
     * @param wmsl The feature type
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(WMTSLayerInfo wmsl) {
        Resource r = get(wmsl, WMTSLAYER_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the configuration directory of a Layer. An empty path will
     * retrieve the directory itself.
     *
     * @param l The layer
     * @return A {@link Resource}
     */
    public @Nonnull Resource get(LayerInfo l, String... path) {
        final Resource r;
        if (l.getResource() instanceof FeatureTypeInfo) {
            r = get(l.getResource(), path);
        } else if (l.getResource() instanceof CoverageInfo) {
            r = get(l.getResource(), path);
        } else if (l.getResource() instanceof WMTSLayerInfo) {
            r = get(l.getResource(), path);
        } else if (l.getResource() instanceof WMSLayerInfo) {
            r = get(l.getResource(), path);
        } else {
            // It'd be nice if we could be generic and cover potential future ResourceInfo types.
            throw new IllegalArgumentException(
                    "Only FeatureTypeInfo, CoverageInfo, and WMS/WMTSLayerInfo are supported.");
        }
        assert r != null;
        return r;
    }

    /**
     * Retrieve the layer configuration XML as a Resource
     *
     * @param li The feature type
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(LayerInfo li) {
        Resource r = get(li, LAYER_XML);
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the layer groups directory. An empty path will retrieve the
     * directory itself.
     *
     * @return A {@link Resource}
     */
    public @Nonnull Resource getLayerGroups(String... path) {
        Resource r = getLayerGroups(null, path);
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the layer groups directory of a workspace. An empty path will
     * retrieve the directory itself. A null workspace will return the resource in the global layer
     * groups directory
     *
     * @return A {@link Resource}
     */
    public @Nonnull Resource getLayerGroups(WorkspaceInfo wsi, String... path) {
        Resource r = get(wsi, Paths.path(LAYERGROUP_DIR, Paths.path(path)));
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the configuration directory of a LayerGroup. An empty path will
     * retrieve the directory itself. This directory is shared by all Layer Groups in a Workspace.
     *
     * @param lgi The store
     * @return A {@link Resource}
     */
    public @Nonnull Resource get(LayerGroupInfo lgi, String... path) {
        WorkspaceInfo wsi = lgi.getWorkspace();
        Resource r = getLayerGroups(wsi, path);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the layer group configuration XML as a Resource
     *
     * @param lgi The layer group
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(LayerGroupInfo lgi) {
        Resource r = get(lgi, String.format("%s.xml", lgi.getName()));
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the styles directory. An empty path will retrieve the directory
     * itself.
     *
     * @return A {@link Resource}
     */
    public @Nonnull Resource getStyles(String... path) {
        Resource r = getStyles(null, path);
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the styles directory of a workspace. An empty path will retrieve
     * the directory itself. A null workspace will return the resource in the global styles
     * directory
     *
     * @return A {@link Resource}
     */
    public @Nonnull Resource getStyles(WorkspaceInfo wsi, String... path) {
        Resource r = get(wsi, Paths.path(STYLE_DIR, Paths.path(path)));
        assert r != null;
        return r;
    }

    /**
     * Retrieve a resource in the the configuration directory of a Resource. An empty path will
     * retrieve the directory itself.
     *
     * @param si The store
     * @return A {@link Resource}
     */
    public @Nonnull Resource get(StyleInfo si, String... path) {
        WorkspaceInfo workspace = si != null ? si.getWorkspace() : null;
        final Resource r = getStyles(workspace, path);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the style configuration XML as a Resource
     *
     * @param s The style
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(StyleInfo s) {
        // special case for styles, if the file name (minus the suffix) matches the id of the style
        // and the suffix is xml (rather than sld) we need to avoid overwritting the actual
        // style file
        final String filename;
        if (s.getFilename() != null
                && s.getFilename().endsWith(".xml")
                && s.getFilename().startsWith(s.getName() + ".")) {
            // append a second .xml suffix
            filename = s.getName() + ".xml.xml";
        } else {
            filename = s.getName() + ".xml";
        }

        Resource r = get(s, filename);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the style definition (SLD) as a Resource
     *
     * @param s The style
     * @return A {@link Resource}
     */
    public @Nonnull Resource style(StyleInfo s) {
        // Must be a simple filename
        final String filename = s.getFilename();
        Resource r = get(s, filename);
        assert r != null;
        return r;
    }

    /**
     * Retrieve the StyleInfo as a GeoTools Style object. Note this is just the data structure as
     * written, the matching external graphics are unmodified and may not be (yet) available on the
     * local system.
     *
     * @param s The style
     * @return A {@link Resource}
     */
    protected @Nonnull Style parsedStyleResources(StyleInfo s) throws IOException {
        final Resource styleResource = style(s);
        if (styleResource.getType() == Type.UNDEFINED) {
            throw new FileNotFoundException("No such resource: " + s.getFilename());
        }
        final DefaultResourceLocator locator = new ResourceAwareResourceLocator();
        locator.setSourceUrl(Resources.toURL(styleResource));
        StyledLayerDescriptor sld =
                Styles.handler(s.getFormat())
                        .parse(styleResource, s.getFormatVersion(), locator, null);
        final Style style = Styles.style(sld);
        assert style != null;
        return style;
    }

    /**
     * Retrieve the styled layer descriptor prepared for direct GeoTools use. All file references
     * have been made absolute.
     *
     * @param s The style
     * @return A {@link StyledLayerDescriptor}
     */
    public @Nonnull StyledLayerDescriptor parsedSld(final StyleInfo s) throws IOException {
        final Resource styleResource = style(s);
        if (styleResource.getType() == Type.UNDEFINED) {
            throw new IOException(
                    "No such resource: "
                            + s.getFilename()
                            + (s.getWorkspace() != null
                                    ? " in workspace " + s.getWorkspace()
                                    : ""));
        }
        File input = styleResource.file();

        DefaultResourceLocator locator = new GeoServerResourceLocator();
        locator.setSourceUrl(Resources.toURL(styleResource));
        EntityResolver entityResolver = getEntityResolver();
        final StyledLayerDescriptor sld =
                Styles.handler(s.getFormat())
                        .parse(input, s.getFormatVersion(), locator, getEntityResolver());

        return sld;
    }

    /**
     * Retrieve the style prepared for direct GeoTools use. All file references have been made
     * absolute.
     *
     * @param s The style
     * @return A {@link Style}
     */
    public @Nonnull Style parsedStyle(final StyleInfo s) throws IOException {
        final StyledLayerDescriptor sld = parsedSld(s);
        final Style style = Styles.style(sld);
        assert style != null;
        return style;
    }

    private EntityResolver getEntityResolver() {
        // would be best injected, but apparently most of the code is
        // actually creating a GeoServerDataDirectory object programmatically and/or on the fly, so
        // we have to resort to a dynamic spring context lookup instead
        EntityResolver resolver = null;
        EntityResolverProvider provider = GeoServerExtensions.bean(EntityResolverProvider.class);
        if (provider != null) {
            resolver = provider.getEntityResolver();
        }
        return resolver;
    }

    /**
     * Retrieve the settings configuration XML as a Resource
     *
     * @param s The settings
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(SettingsInfo s) {
        Resource r = get(s.getWorkspace(), "settings.xml");
        assert r != null;
        return r;
    }

    /**
     * Retrieve the logging configuration XML as a Resource
     *
     * @param l The settings
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(LoggingInfo l) {
        Resource r = getRoot("logging.xml");
        assert r != null;
        return r;
    }

    /**
     * Retrieve the global configuration xml
     *
     * @param g The global info
     * @return A {@link Resource}
     */
    public @Nonnull Resource config(GeoServerInfo g) {
        Resource r = getRoot("global.xml");
        assert r != null;
        return r;
    }

    // SLD Manipulation

    List<Resource> additionalStyleResources(StyleInfo s) throws IOException {
        final List<Resource> resources = new ArrayList<Resource>();
        final Resource baseDir = get(s);
        try {
            Style parsedStyle = parsedStyleResources(s);
            parsedStyle.accept(
                    new AbstractStyleVisitor() {
                        @Override
                        public void visit(ExternalGraphic exgr) {
                            if (exgr.getOnlineResource() == null) {
                                return;
                            }
                            try {
                                final String location = exgr.getURI();
                                Resource r = resourceLoader.fromURL(location);

                                if (r != null && r.getType() != Type.UNDEFINED) {
                                    resources.add(r);
                                }
                            } catch (IllegalArgumentException e) {
                                GeoServerConfigPersister.LOGGER.log(
                                        Level.WARNING,
                                        "Error attemping to process SLD resource",
                                        e);
                            }
                        }

                        @Override
                        public void visit(Mark mark) {
                            final Expression wellKnownName = mark.getWellKnownName();
                            if (wellKnownName instanceof Literal) {
                                final String name = wellKnownName.evaluate(null, String.class);
                                if (name.startsWith("resource:/")) {
                                    try {
                                        Resource r = resourceLoader.fromURL(name);

                                        if (r != null && r.getType() != Type.UNDEFINED) {
                                            resources.add(r);
                                        }
                                    } catch (IllegalArgumentException e) {
                                        GeoServerConfigPersister.LOGGER.log(
                                                Level.WARNING,
                                                "Error attemping to process SLD resource",
                                                e);
                                    }
                                }
                            }
                        }

                        // TODO: Workaround for GEOT-4803, Remove when it is fixed, KS
                        @Override
                        public void visit(ChannelSelection cs) {
                            if (cs.getGrayChannel() != null) {
                                cs.getGrayChannel().accept(this);
                            }
                            final SelectedChannelType[] rgbChannels = cs.getRGBChannels();
                            if (rgbChannels != null) {
                                for (SelectedChannelType ch : rgbChannels) {
                                    if (ch != null) ch.accept(this);
                                }
                            }
                        }
                    });
        } catch (FileNotFoundException e) {
            GeoServerConfigPersister.LOGGER.log(Level.WARNING, "Error loading style:" + e);
        } catch (IOException e) {
            GeoServerConfigPersister.LOGGER.log(Level.WARNING, "Error loading style", e);
        }
        return resources;
    }

    /**
     * Wrapper for {@link DataUtilities#fileToURL} that unescapes braces used to delimit CQL
     * templates.
     */
    public static URL fileToUrlPreservingCqlTemplates(File file) {
        URL url = URLs.fileToUrl(file);
        if (!file.getPath().contains("${")) {
            // guard against situations in which braces are used but not for CQL templates
            return url;
        } else {
            try {
                return new URL(url.toExternalForm().replace("%7B", "{").replace("%7D", "}"));
            } catch (MalformedURLException e) {
                return null;
            }
        }
    }

    public ResourceStore getResourceStore() {
        return resourceLoader.getResourceStore();
    }

    public ResourceLocator getResourceLocator() {
        GeoServerResourceLocator locator = new GeoServerResourceLocator();
        locator.setSourceUrl(URLs.fileToUrl(getStyles().dir()));
        return locator;
    }

    private class ResourceAwareResourceLocator extends DefaultResourceLocator {
        @Override
        protected URL validateRelativeURL(URL relativeUrl) {
            if (relativeUrl.getProtocol().equalsIgnoreCase("resource")) {
                String path = relativeUrl.getPath();
                if (resourceLoader.get(path).getType() != Type.UNDEFINED) {
                    return relativeUrl;
                } else {
                    return null;
                }
            } else {
                return super.validateRelativeURL(relativeUrl);
            }
        }

        @Override
        protected URL makeRelativeURL(String uri, String query) {
            if (SystemUtils.IS_OS_WINDOWS && uri.contains("\\")) {
                uri = uri.replace('\\', '/');
            }
            return super.makeRelativeURL(uri, query);
        }
    }

    private class GeoServerResourceLocator extends ResourceAwareResourceLocator {

        @Override
        public URL locateResource(String uri) {
            URL url = super.locateResource(uri);
            if (url != null && url.getProtocol().equalsIgnoreCase("resource")) {
                Resource resource = resourceLoader.fromURL(url);
                File file;
                if (Resources.exists(resource)) {
                    // GEOS-7741: cache resource as file, otherwise it can't be found
                    file = resource.file();
                } else {
                    // GEOS-7025: Just get the path; don't try to create the file
                    file = Paths.toFile(root(), resource.path());
                }

                URL u = fileToUrlPreservingCqlTemplates(file);

                if (url.getQuery() != null) {
                    try {
                        u = new URL(u.toString() + "?" + url.getQuery());
                    } catch (MalformedURLException ex) {
                        GeoServerConfigPersister.LOGGER.log(
                                Level.WARNING,
                                "Error processing query string for resource with uri: " + uri,
                                ex);
                        return null;
                    }
                }

                if (url.getRef() != null) {
                    try {
                        u = new URL(u.toString() + "#" + url.getRef());
                    } catch (MalformedURLException ex) {
                        GeoServerConfigPersister.LOGGER.log(
                                Level.WARNING,
                                "Error processing # fragment for resource with uri: " + uri,
                                ex);
                        return null;
                    }
                }

                return u;
            } else {
                return url;
            }
        }
    }
}
