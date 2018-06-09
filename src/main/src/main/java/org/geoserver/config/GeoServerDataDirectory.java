/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.annotation.Nonnull;
import org.apache.commons.io.FileUtils;
import org.geoserver.catalog.*;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.EntityResolverProvider;
import org.geotools.data.DataUtilities;
import org.geotools.styling.*;
import org.geotools.util.URLs;
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
     * Returns a directory under the {@link #dataRoot()} directory.
     *
     * @param create Create directory if needed
     * @param location directory location
     * @return Directory (which may be newly created) or null if not found
     * @deprecated Unused
     */
    private File dataDir(boolean create, String... location) throws IOException {
        Resource directory = get(Paths.path("data", Paths.path(location)));
        if (create) {
            return directory.dir();
        } else {
            return Resources.directory(directory);
        }
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
     * Returns a file under the {@link #dataRoot()} directory, if the file does not exist it a file
     * object will still be returned.
     *
     * @deprecated Unused
     */
    public File findOrResolveDataFile(String... location) throws IOException {
        Resource resource = get(Paths.path("data", Paths.path(location)));
        return resource.file();
    }

    /**
     * Returns a file under the {@link #dataRoot()} directory.
     *
     * @param create Create file (if required)
     * @param location file location
     * @return File (created if needed) or null if not found
     * @deprecated Unused
     */
    private File dataFile(boolean create, String... location) throws IOException {
        Resource resource = get(Paths.path("data", Paths.path(location)));
        if (create) {
            return resource.file();
        } else {
            return Resources.file(resource);
        }
    }

    /**
     * Returns the root of the directory which contains security configuration files, if the
     * directory does exist, null is returned.
     *
     * <p>This directory is called 'security', and is located directly under {@link #root()}
     *
     * @deprecated As of GeoServer 2.6, replaced by @link {@link #getSecurity()}
     */
    @Deprecated
    public File findSecurityRoot() throws IOException {
        return Resources.directory(getSecurity());
    }

    /**
     * Returns the root of the directory which contains security configuration files, if the
     * directory does exist it is created.
     *
     * <p>This directory is called 'security', and is located directly under {@link #root()}
     *
     * @deprecated As of GeoServer 2.6, replaced by @link {@link #getSecurity()}
     */
    @Deprecated
    public File findOrCreateSecurityRoot() throws IOException {
        return getSecurity().dir(); // will create directory as needed
    }

    /**
     * Access to security directory.
     *
     * @deprecated As of GeoServer 2.6, replaced by @link {@link #getSecurity()}
     */
    @Deprecated
    private File securityRoot(boolean create) throws IOException {
        final Resource directory = getSecurity();
        final File f;
        if (create) {
            f = directory.dir();
        } else {
            f = Resources.directory(directory);
        }
        return f;
    }

    /**
     * Returns a directory under the {@link #securityRoot()} directory, if the directory does not
     * exist null will be returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by @link {@link #getSecurity()}
     */
    @Deprecated
    public File findSecurityDir(String... location) throws IOException {
        return Resources.directory(getSecurity(location));
    }

    /**
     * Returns a directory under the {@link #securityRoot()} directory, if the directory does not
     * exist it will be created.
     *
     * @deprecated As of GeoServer 2.6, replaced by @link {@link #getSecurity()}
     */
    @Deprecated
    public File findOrCreateSecurityDir(String... location) throws IOException {
        return getSecurity(location).dir();
    }

    /**
     * Copies a file into a security configuration directory.
     *
     * <p>If the security configuration directory does exist it will be created.
     *
     * @deprecated As of GeoServer 2.6, replaced by @link {@link #getSecurity()}
     */
    @Deprecated
    public void copyToSecurityDir(File f) throws IOException {
        Resource resource = getSecurity();
        Resources.copy(f, resource);
    }

    /**
     * Copies data into a security configuration directory.
     *
     * <p>If the security configuration directory does exist it will be created
     *
     * @deprecated As of GeoServer 2.6, replaced by @link {@link #getSecurity()}
     */
    @Deprecated
    public void copyToSecurityDir(InputStream data, String filename) throws IOException {
        Resource resource = getSecurity();
        Resources.copy(data, resource, filename);
    }

    /**
     * Returns the directory for the specified workspace, if the directory does not exist null is
     * returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by @link {@link #get(WorkspaceInfo)}
     */
    @Deprecated
    public File findWorkspaceDir(WorkspaceInfo ws) throws IOException {
        Resource directory = get(ws);
        return Resources.directory(directory);
    }

    /**
     * Returns the directory for the specified workspace, if the directory does not exist it will be
     * created.
     *
     * @param create If set to true the directory will be created when it does not exist.
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(WorkspaceInfo, String...)}
     */
    @Deprecated
    public File findOrCreateWorkspaceDir(WorkspaceInfo ws) throws IOException {
        Resource directory = get(ws);
        return directory.dir();
    }

    /**
     * Returns the configuration file for the specified workspace, if the file does not exist null
     * is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #config(WorkspaceInfo)}
     */
    @Deprecated
    public File findWorkspaceFile(WorkspaceInfo ws) throws IOException {
        Resource workspaceFile = config(ws);
        return Resources.file(workspaceFile);
    }

    /**
     * Returns the configuration file for the specified workspace, if the file does not exist a file
     * object will still be returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #config(WorkspaceInfo)}
     */
    @Deprecated
    public File findOrResolveWorkspaceFile(WorkspaceInfo ws) throws IOException {
        Resource workspaceFile = config(ws);
        return workspaceFile.file();
    }

    /**
     * Returns a supplementary configuration file for a workspace, if the file does not exist null
     * is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(WorkspaceInfo, String...)}
     */
    @Deprecated
    public File findSuppWorkspaceFile(WorkspaceInfo ws, String filename) throws IOException {
        Resource resource = get(ws, filename);
        return Resources.file(resource);
    }

    /**
     * Returns a supplementary configuration file in the workspaces directory, if the file does not
     * exist null is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #getWorkspaces(String...)}
     */
    @Deprecated
    public File findSuppWorkspacesFile(WorkspaceInfo ws, String filename) throws IOException {
        Resource resource = getWorkspaces(filename);
        return Resources.file(resource);
    }

    /**
     * Copies a file into a workspace configuration directory.
     *
     * <p>If the workspace configuration directory does exist it will be created.
     *
     * @param ws Target workspace for copied file
     * @param file File to copy
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(WorkspaceInfo, String...)}
     */
    @Deprecated
    public void copyToWorkspaceDir(WorkspaceInfo ws, File file) throws IOException {
        Resource directory = get(ws);
        Resources.copy(file, directory);
    }

    /**
     * Copies data into a workspace configuration directory.
     *
     * <p>If the workspace configuration directory does exist it will be created
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(WorkspaceInfo, String...)}
     */
    @Deprecated
    public void copyToWorkspaceDir(WorkspaceInfo ws, InputStream data, String filename)
            throws IOException {
        Resource directory = get(ws);
        Resources.copy(data, directory, filename);
    }

    /**
     * Copies data into the root workspaces configuration directory.
     *
     * <p>If the workspace configuration directory does exist it will be created
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(WorkspaceInfo, String...)}
     */
    @Deprecated
    public void copyToWorkspacesDir(InputStream data, String filename) throws IOException {
        Resources.copy(data, getWorkspaces(), filename);
    }

    /**
     * Returns the directory in which a stores configuration is persisted, if the directory does not
     * exists null is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StoreInfo, String...)}
     */
    @Deprecated
    public File findStoreDir(StoreInfo store) throws IOException {
        Resource directory = get(store);
        return Resources.directory(directory);
    }

    @Deprecated
    private String path(StoreInfo store) {
        WorkspaceInfo workspace = store.getWorkspace();
        return Paths.path("workspaces", workspace.getName(), store.getName());
    }

    /**
     * Returns the directory in which a stores configuration is persisted, if the directory does not
     * exist it is created.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StoreInfo, String...)}
     */
    @Deprecated
    public File findOrCreateStoreDir(StoreInfo store) throws IOException {
        Resource resource = get(store);
        return resource.dir();
    }

    /**
     * Returns the configuration file for the specified store, if the file does not exist null is
     * returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #config(DataStoreInfo)}, {@link
     *     #config(CoverageStoreInfo)}, and {@link #config(WMSStoreInfo)}
     */
    @Deprecated
    public File findStoreFile(StoreInfo store) throws IOException {
        Resource resource = config(store);
        return Resources.file(resource);
    }

    /**
     * Returns the configuration file for the specified store, if the file does not exist a file
     * object is still returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StoreInfo, String...)}
     */
    @Deprecated
    public File findOrResolveStoreFile(StoreInfo store) throws IOException {
        Resource resource = get(store);
        return resource.file();
    }

    /**
     * Returns a supplementary configuration file for a store, if the file does not exist null is
     * returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StoreInfo, String...)}
     */
    @Deprecated
    public File findSuppStoreFile(StoreInfo store, String filename) throws IOException {
        Resource resource = get(store, filename);
        return Resources.file(resource);
    }

    /**
     * Copies a file into a store configuration directory.
     *
     * <p>If the store configuration directory does exist it will be created
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StoreInfo, String...)}
     */
    @Deprecated
    public void copyToStoreDir(StoreInfo store, File file) throws IOException {
        Resource directory = get(path(store));
        Resources.copy(file, directory);
    }

    /**
     * Copies data into a store configuration directory.
     *
     * <p>If the store configuration directory does exist it will be created
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StoreInfo, String...)}
     */
    public void copyToStoreDir(StoreInfo store, InputStream data, String filename)
            throws IOException {
        Resource directory = get(store);
        Resources.copy(data, directory, filename);
    }

    /**
     * Returns the directory in which a resources configuration is persisted, if the directory does
     * not exist null is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(ResourceInfo, String...)}
     */
    @Deprecated
    public File findResourceDir(ResourceInfo resource) throws IOException {
        Resource directory = get(resource);
        return Resources.directory(directory);
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

    /**
     * Returns the directory in which a resources configuration is persisted, if the directory does
     * not exist it will be created.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(ResourceInfo, String...)}
     */
    public File findOrCreateResourceDir(ResourceInfo r) throws IOException {
        Resource directory = get(r);
        return directory.dir();
    }

    /**
     * Returns the configuration file for the specified resource, if the file does not exist null is
     * returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #config(FeatureTypeInfo, String...)},
     *     {@link #config(CoverageInfo, String...)}, {@link #config(WMSLayerInfo, String...)}
     */
    @Deprecated
    public File findResourceFile(ResourceInfo r) throws IOException {
        Resource resource = config(r);
        return Resources.file(resource);
    }

    /**
     * Returns the configuration file for the specified resource, if the file does not exist a file
     * object is still returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #config(FeatureTypeInfo, String...)},
     *     {@link #config(CoverageInfo, String...)}, {@link #config(WMSLayerInfo, String...)}
     */
    @Deprecated
    public File findOrResolveResourceFile(ResourceInfo r) throws IOException {
        Resource resource = config(r);
        return resource.file();
    }

    /**
     * Returns a supplementary configuration file for a resource, if the file does not exist null is
     * returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(ResourceInfo, String...)}
     */
    @Deprecated
    public File findSuppResourceFile(ResourceInfo r, String filename) throws IOException {
        Resource resource = get(r, filename);
        return Resources.file(resource);
    }

    /**
     * Returns a supplementary configuration file for a resource in a 1.x data directory format. If
     * the file does not exist null is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(ResourceInfo, String...)}
     */
    public File findSuppLegacyResourceFile(ResourceInfo r, String filename) throws IOException {
        File rdir = findLegacyResourceDir(r);
        if (rdir != null) {
            File file = new File(rdir, filename);
            return file.exists() ? file : null;
        } else {
            return null;
        }
    }

    /**
     * Copies a file into a feature type configuration directory.
     *
     * <p>If the resource directory does exist it will be created
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(ResourceInfo, String...)}
     */
    @Deprecated
    public void copyToResourceDir(ResourceInfo resource, File file) throws IOException {
        Resource directory = get(resource);
        Resources.copy(file, directory);
    }

    /**
     * Copies data into a feature type configuration directory.
     *
     * <p>If the resource directory does exist it will be created
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(ResourceInfo, String...)}
     */
    @Deprecated
    public void copyToResourceDir(ResourceInfo resource, InputStream data, String filename)
            throws IOException {
        Resource directory = get(resource);
        Resources.copy(data, directory, filename);
    }

    /**
     * Returns the configuration file for the specified namespace, if the file does not exist null
     * is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(WorkspaceInfo, String...)}
     */
    @Deprecated
    public File findNamespaceFile(WorkspaceInfo ws) throws IOException {
        Resource directory = get(ws);
        return Resources.directory(directory);
    }

    /**
     * Returns the configuration file for the specified namespace, if the file does not exist a file
     * object is still returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(WorkspaceInfo, String...)}
     */
    @Deprecated
    public File findOrResolveNamespaceFile(WorkspaceInfo ws) throws IOException {
        Resource directory = get(ws);
        return directory.dir();
    }

    /**
     * Returns the configuration file for the specified layer, if the file does not exist null is
     * returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(LayerInfo, String...)}
     */
    @Deprecated
    public File findLayerFile(LayerInfo layer) throws IOException {
        Resource resource = get(layer);
        return Resources.file(resource);
    }

    /**
     * Returns the configuration file for the specified layer, if the file does not exist a file
     * object is still returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(LayerInfo, String...)}
     */
    public File findOrResolveLayerFile(LayerInfo layer) throws IOException {
        Resource resource = get(layer);
        return resource.file();
    }

    /**
     * Returns the directory in which global styles are persisted, if the directory does not exist
     * null is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StyleInfo, String...)}
     */
    public File findStyleDir() throws IOException {
        Resource styles = get(STYLE_DIR);
        return Resources.directory(styles);
    }

    /**
     * Returns the directory in which global styles are persisted, if the directory does not exist
     * it will be created.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StyleInfo, String...)}
     */
    public File findOrCreateStyleDir() throws IOException {
        Resource styles = get(STYLE_DIR);
        return styles.dir();
    }

    /**
     * Styles directory (using StyleInfo).
     *
     * <p>Package visibility {@link GeoServerPersister#dir(StyleInfo)}.
     *
     * @param create Create if needed
     * @param styleInfo
     * @throws IOException
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StyleInfo, String...)}
     */
    File styleDir(boolean create, StyleInfo styleInfo) throws IOException {
        Resource styles = get(styleInfo);
        return Resources.directory(styles, create);
    }

    /**
     * Access to styles directory for provided workspace (or global styles directory if workspace
     * not provided).
     *
     * <p>Package visibility for {@link GeoServerPersister}.
     *
     * @param create Create directory if required
     * @param workspaceInfo Workspace used to access styles directory
     * @return styles directory
     * @throws IOException
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StyleInfo, String...)}
     */
    File styleDir(boolean create, WorkspaceInfo workspaceInfo) throws IOException {
        Resource styles = get(workspaceInfo, STYLE_DIR);
        return Resources.directory(styles, create);
    }

    /**
     * Returns the configuration file for the specified style, if the file does not exist null is
     * returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #config(StyleInfo, String...)}
     */
    public File findStyleFile(StyleInfo s) throws IOException {
        Resource resource = config(s);
        return Resources.file(resource);
    }

    /**
     * Returns the SLD file for the specified style, if the file does not exist null is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #style(StyleInfo, String...)}
     */
    public File findStyleSldFile(StyleInfo s) throws IOException {
        Resource resource = style(s);
        return Resources.file(resource);
    }

    /**
     * Returns the configuration file for the specified style, if the file does not exist a file
     * object is still returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #config(StyleInfo, String...)}
     */
    public File findOrCreateStyleFile(StyleInfo s) throws IOException {
        Resource resource = config(s);
        return resource.file();
    }

    /**
     * Returns the SLD file for the specified style, if the file does not exist a file object is
     * still returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #style(StyleInfo, String...)}
     */
    public File findOrCreateStyleSldFile(StyleInfo s) throws IOException {
        Resource resource = style(s);
        return resource.file();
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
     * @param wmss The coverage store
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
     * @param wmss The resource
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
     * @param li The store
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
     * @param c The style
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
     * @param c The style
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
        final DefaultResourceLocator locator = new DefaultResourceLocator();
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
            throw new IOException("No such resource: " + s.getFilename());
        }
        File input = styleResource.file();

        DefaultResourceLocator locator =
                new DefaultResourceLocator() {

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
                                    GeoServerPersister.LOGGER.log(
                                            Level.WARNING,
                                            "Error processing query string for resource with uri: "
                                                    + uri,
                                            ex);
                                    return null;
                                }
                            }

                            if (url.getRef() != null) {
                                try {
                                    u = new URL(u.toString() + "#" + url.getRef());
                                } catch (MalformedURLException ex) {
                                    GeoServerPersister.LOGGER.log(
                                            Level.WARNING,
                                            "Error processing # fragment for resource with uri: "
                                                    + uri,
                                            ex);
                                    return null;
                                }
                            }

                            return u;
                        } else {
                            return url;
                        }
                    }

                    @Override
                    protected URL validateRelativeURL(URL relativeUrl) {
                        // the resource:/ thing does not make for a valid url, so don't validate it
                        if (relativeUrl.getProtocol().equalsIgnoreCase("resource")) {
                            return relativeUrl;
                        } else {
                            return super.validateRelativeURL(relativeUrl);
                        }
                    }
                };
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

    /**
     * Copy file to styles directory (determined using {@link StyleInfo#getWorkspace()}).
     *
     * @param file
     * @param style
     * @throws IOException
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(StyleInfo, String...)}
     */
    @Deprecated
    public void copyToStyleDir(File file, StyleInfo style) throws IOException {
        Resource styles = get(style);
        FileUtils.copyFileToDirectory(file, styles.dir());
    }

    /**
     * Returns the directory in which global layer groups are persisted, if the directory does not
     * exist null is returned.
     *
     * @deprecated As of GeoServer 2.6, replaced by {@link #get(LayerGroupInfo, String...)}
     */
    public File findLayerGroupDir() throws IOException {
        Resource resource = getLayerGroups();
        return Resources.directory(resource);
    }

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
                                Resource r = resourceLoader.fromURL(exgr.getLocation());

                                if (r != null && r.getType() != Type.UNDEFINED) {
                                    resources.add(r);
                                }
                            } catch (IllegalArgumentException | MalformedURLException e) {
                                GeoServerPersister.LOGGER.log(
                                        Level.WARNING,
                                        "Error attemping to process SLD resource",
                                        e);
                            }
                        }

                        // TODO: Workaround for GEOT-4803, Remove when it is fixed, KS
                        @Override
                        public void visit(ChannelSelection cs) {
                            if (cs.getGrayChannel() != null) {
                                cs.getGrayChannel().accept(this);
                            }
                            final SelectedChannelType[] rgbChannels = cs.getRGBChannels();
                            for (SelectedChannelType ch : rgbChannels) {
                                if (ch != null) ch.accept(this);
                            }
                        }
                    });
        } catch (FileNotFoundException e) {
            GeoServerPersister.LOGGER.log(Level.WARNING, "Error loading style:" + e);
        } catch (IOException e) {
            GeoServerPersister.LOGGER.log(Level.WARNING, "Error loading style", e);
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
}
