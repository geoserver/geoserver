/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;

/**
 * File or Resource access to GeoServer data directory. In addition to paths Catalog obhjects such as workspace or FeatureTypeInfo can be used to
 * locate resources.
 * <p>
 * Example usage:
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
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 */
@SuppressWarnings("unused")
public class GeoServerDataDirectory implements ResourceStore {

    /**
     * resource loader
     */
    GeoServerResourceLoader resourceLoader;

    /**
     * Creates the data directory specifying the resource loader.
     */
    public GeoServerDataDirectory(GeoServerResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    /**
     * Creates the data directory specifying the base directory.
     */
    public GeoServerDataDirectory(File baseDirectory) {
        this(new GeoServerResourceLoader(baseDirectory));
    }

    /**
     * Returns the underlying resource loader.
     */
    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    @Override
    public Resource get(String path) {
        return resourceLoader.get(path);
    }

    @Override
    public boolean move(String path, String target) {
        return resourceLoader.move(path, target);
    }

    @Override
    public boolean remove(String path) {
        return resourceLoader.remove(path);
    }

    /**
     * The root of the data directory.
     */
    public File root() {
        return resourceLoader.getBaseDirectory();
    }

    /**
     * Returns a directory under the {@link #root()} directory, if the directory does not exist it will be created.
     * 
     * @return directory (created if needed)
     */
    public File findOrCreateDir(String... location) throws IOException {
        String path = Paths.path(location);
        return get(path).dir();
    }

    /**
     * Returns a file under the {@link #root()} directory, if the file does not exist null is returned.
     */
    public File findFile(String... location) throws IOException {
        String path = Paths.path(location);
        return Resources.find(get(path));
    }

    /**
     * Returns the root of the directory which contains spatial data files, if the directory does exist, null is returned.
     * <p>
     * This directory is called 'data', and is located directly under {@link #root()}
     * </p>
     */
    public File findDataRoot() throws IOException {
        return Resources.directory(get("data"));
    }

    /**
     * Returns the root of the directory which contains spatial data files, if the directory does not exist it will be created.
     * <p>
     * This directory is called 'data', and is located directly under {@link #root()}
     * </p>
     */
    public File findOrCreateDataRoot() throws IOException {
        return get("data").dir(); // will create directory as needed
    }

    /**
     * Returns a directory under the {@link #dataRoot()} directory, if the directory does not exist null will be returned.
     */
    public File findDataDir(String... location) throws IOException {
        String path = Paths.path("data", Paths.path(location));
        return Resources.directory(get(path));
    }

    /**
     * Returns a directory under the {@link #dataRoot()} directory, if the directory does not exist it will be created.
     */
    public File findOrCreateDataDir(String... location) throws IOException {
        String path = Paths.path("data", Paths.path(location));
        return get(path).dir();
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
        String path = Paths.path("data", Paths.path(location));
        Resource dataDir = get(path);
        if (create) {
            return dataDir.dir();
        } else {
            return Resources.directory(dataDir);
        }
    }

    /**
     * Returns a file under the {@link #dataRoot()} directory, if the file does not exist null is returned.
     */
    public File findDataFile(String... location) throws IOException {
        String path = Paths.path("data", Paths.path(location));
        return Resources.file(get(path));
    }

    /**
     * Returns a file under the {@link #dataRoot()} directory, if the file does not exist it a file object will still be returned.
     * 
     * @deprecated Unused
     */
    public File findOrResolveDataFile(String... location) throws IOException {
        String path = Paths.path("data", Paths.path(location));
        return get(path).file();
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
        String path = Paths.path("data", Paths.path(location));
        Resource dataFile = get(path);
        if (create) {
            return dataFile.file();
        } else {
            return Resources.file(dataFile);
        }
    }

    /**
     * Returns the root of the directory which contains security configuration files, if the directory does exist, null is returned.
     * <p>
     * This directory is called 'security', and is located directly under {@link #root()}
     * </p>
     */
    public File findSecurityRoot() throws IOException {
        return Resources.directory(get("security"));
    }

    /**
     * Returns the root of the directory which contains security configuration files, if the directory does exist it is created.
     * <p>
     * This directory is called 'security', and is located directly under {@link #root()}
     * </p>
     */
    public File findOrCreateSecurityRoot() throws IOException {
        return get("security").dir(); // will create directory as needed
    }

    /**
     * Access to security directory.
     * 
     * @Unused
     */
    private File securityRoot(boolean create) throws IOException {
        if (create) {
            return get("security").dir();
        } else {
            return Resources.directory(get("security"));
        }
    }

    /**
     * Returns a directory under the {@link #securityRoot()} directory, if the directory does not exist null will be returned.
     */
    public File findSecurityDir(String... location) throws IOException {
        String path = Paths.path("security", Paths.path(location));
        return Resources.directory(get(path));
    }

    /**
     * Returns a directory under the {@link #securityRoot()} directory, if the directory does not exist it will be created.
     */
    public File findOrCreateSecurityDir(String... location) throws IOException {
        String path = Paths.path("security", Paths.path(location));
        return get(path).dir();
    }

    /**
     * Access to "security" folder.
     */
    @Deprecated
    private File securityDir(boolean create, String... location) throws IOException {
        String path = Paths.path("security", Paths.path(location));
        Resource directory = get(path);
        if (create) {
            return directory.dir(); // will create directory as needed
        } else {
            return Resources.directory(directory);
        }
    }

    /**
     * Copies a file into a security configuration directory.
     * <p>
     * If the security configuration directory does exist it will be created.
     * </p>
     */
    public void copyToSecurityDir(File f) throws IOException {
        Resource resource = get("security");
        File securityRoot = resource.dir();

        FileUtils.copyFileToDirectory(f, securityRoot);
    }

    /**
     * Copies data into a security configuration directory.
     * <p>
     * If the security configuration directory does exist it will be created
     * </p>
     */
    public void copyToSecurityDir(InputStream data, String filename) throws IOException {
        Resource resource = get("security");
        File securityRoot = resource.dir();

        copy(data, securityRoot, filename);
    }

    /**
     * Returns the directory for the specified workspace, if the directory does not exist null is returned.
     */
    public File findWorkspaceDir(WorkspaceInfo ws) throws IOException {
        Resource directory = get(path(ws));
        return Resources.directory(directory);
    }

    /**
     * Returns the directory for the specified workspace, if the directory does not exist it will be created.
     * 
     * @param create If set to true the directory will be created when it does not exist.
     */
    public File findOrCreateWorkspaceDir(WorkspaceInfo ws) throws IOException {
        Resource directory = get(path(ws));
        return directory.dir();
    }

    @Deprecated
    private File workspaceDir(boolean create, WorkspaceInfo ws) throws IOException {
        if (create) {
            Resource directory = get(path(ws));
            return directory.dir();
        } else {
            Resource directory = get(path(ws));
            return Resources.directory(directory);
        }
    }

    @Deprecated
    private File workspacesDir(boolean create) throws IOException {
        if (create) {
            Resource directory = get("workspaces");
            return directory.dir();
        } else {
            Resource directory = get("workspaces");
            return Resources.directory(directory);
        }
    }

    /**
     * Returns the configuration file for the specified workspace, if the file does not exist null is returned.
     */
    public File findWorkspaceFile(WorkspaceInfo ws) throws IOException {
        Resource workspaceFile = get(path(ws, "workspace.xml"));
        return Resources.file(workspaceFile);
    }

    /**
     * Returns the configuration file for the specified workspace, if the file does not exist a file object will still be returned.
     * 
     */
    public File findOrResolveWorkspaceFile(WorkspaceInfo ws) throws IOException {
        Resource workspaceFile = get(path(ws, "workspace.xml"));
        return workspaceFile.file();
    }

    @Deprecated
    private File workspaceFile(boolean create, WorkspaceInfo ws) throws IOException {
        Resource workspaceFile = get(path(ws, "workspace.xml"));
        if (create) {
            return workspaceFile.file();
        } else {
            return Resources.file(workspaceFile);
        }
    }

    /**
     * Returns a supplementary configuration file for a workspace, if the file does not exist null is returned.
     */
    public File findSuppWorkspaceFile(WorkspaceInfo ws, String filename) throws IOException {
        Resource resource = get(path(ws, filename));
        return Resources.file(resource);
    }

    private String path(WorkspaceInfo workspace, String filename) {
        return Paths.path("workspaces", workspace.getName(), filename);
    }

    /**
     * Returns a supplementary configuration file in the workspaces directory, if the file does not exist null is returned.
     */
    public File findSuppWorkspacesFile(WorkspaceInfo ws, String filename) throws IOException {
        Resource resource = get(Paths.path("workspaces", filename));
        return Resources.file(resource);
    }

    /**
     * Copies a file into a workspace configuration directory.
     * <p>
     * If the workspace configuration directory does exist it will be created.
     * </p>
     * 
     * @param ws Target workspace for copied file
     * @param file File to copy
     */
    public void copyToWorkspaceDir(WorkspaceInfo ws, File file) throws IOException {
        Resource directory = get(path(ws));
        FileUtils.copyFileToDirectory(file, directory.dir());
    }

    /**
     * Copies data into a workspace configuration directory.
     * <p>
     * If the workspace configuration directory does exist it will be created
     * </p>
     */
    public void copyToWorkspaceDir(WorkspaceInfo ws, InputStream data, String filename)
            throws IOException {
        Resource directory = get(path(ws));
        copy(data, directory.dir(), filename);
    }

    private String path(WorkspaceInfo workspace) {
        return Paths.path("workspaces", workspace.getName());
    }

    /**
     * Copies data into the root workspaces configuration directory.
     * <p>
     * If the workspace configuration directory does exist it will be created
     * </p>
     */
    public void copyToWorkspacesDir(InputStream data, String filename) throws IOException {
        copy(data, get("workspaces").dir(), filename);
    }

    /**
     * Returns the directory in which a stores configuration is persisted, if the directory does not exists null is returned.
     */
    public File findStoreDir(StoreInfo store) throws IOException {
        Resource resource = get(path(store));
        return Resources.directory(resource);
    }

    private String path(StoreInfo store) {
        WorkspaceInfo workspace = store.getWorkspace();
        return Paths.path("workspaces", workspace.getName(), store.getName());
    }

    /**
     * Returns the directory in which a stores configuration is persisted, if the directory does not exist it is created.
     */
    public File findOrCreateStoreDir(StoreInfo store) throws IOException {
        Resource resource = get(path(store));
        return resource.dir();
    }

    @Deprecated
    private File storeDir(boolean create, StoreInfo store) throws IOException {
        if (create) {
            Resource directory = get(path(store));
            return directory.dir();
        } else {
            Resource directory = get(path(store));
            return Resources.directory(directory);
        }
    }

    /**
     * Returns the configuration file for the specified store, if the file does not exist null is returned.
     */
    public File findStoreFile(StoreInfo store) throws IOException {
        Resource resource = get(pathStoreFile(store));
        return Resources.file(resource);
    }

    /**
     * Returns the configuration file for the specified store, if the file does not exist a file object is still returned.
     */
    public File findOrResolveStoreFile(StoreInfo store) throws IOException {
        Resource resource = get(pathStoreFile(store));
        return resource.file();
    }

    /**
     * Determines the appropriate XML resource file for DataStoreInfo or CoverageStoreInfo.
     * 
     * @param store
     * @return datastore.xml or coveragestore.xml as appropriate
     */
    private String pathStoreFile(StoreInfo store) {
        WorkspaceInfo workspace = store.getWorkspace();
        if (store instanceof DataStoreInfo) {
            return Paths.path("workspaces", workspace.getName(), store.getName(), "datastore.xml");
        } else if (store instanceof CoverageStoreInfo) {
            return Paths.path("workspaces", workspace.getName(), store.getName(),
                    "coveragestore.xml");
        } else {
            throw new IllegalStateException("Unsupported Store " + store.getType() + " "
                    + store.getClass().getSimpleName());
        }
    }

    @Deprecated
    private File storeFile(boolean create, StoreInfo store) throws IOException {
        String path = pathStoreFile(store);
        Resource resource = get(path);
        if (create) {
            return resource.file();
        } else {
            return Resources.file(resource);
        }
    }

    /**
     * Returns a supplementary configuration file for a store, if the file does not exist null is returned.
     */
    public File findSuppStoreFile(StoreInfo store, String filename) throws IOException {
        Resource resource = get(path(store, filename));
        return Resources.file(resource);
    }

    private String path(StoreInfo store, String filename) {
        WorkspaceInfo workspace = store.getWorkspace();
        return Paths.path("workspaces", workspace.getName(), store.getName(), filename);
    }

    /**
     * Copies a file into a store configuration directory.
     * <p>
     * If the store configuration directory does exist it will be created
     * </p>
     */
    public void copyToStoreDir(StoreInfo store, File file) throws IOException {
        String path = path(store);
        Resource directory = get(path);
        FileUtils.copyFileToDirectory(file, directory.dir());
    }

    /**
     * Copies data into a store configuration directory.
     * <p>
     * If the store configuration directory does exist it will be created
     * </p>
     */
    public void copyToStoreDir(StoreInfo store, InputStream data, String filename)
            throws IOException {
        String path = path(store);
        Resource directory = get(path);
        copy(data, directory.dir(), filename);
    }

    /**
     * Returns the directory in which a resources configuration is persisted, if the directory does not exist null is returned.
     */
    public File findResourceDir(ResourceInfo resource) throws IOException {
        Resource directory = get(path(resource));
        return Resources.directory(directory);
    }

    /**
     * Finds the directory for the resource assuming a 1.x style data directory.
     * <p>
     * Something like:
     * 
     * <pre>
     * featureTypes/states_shapefile_states
     * coverages/sfdem_dem
     * </pre>
     * 
     * </p>
     * 
     * @param resource The resource.
     * 
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
     * Returns the directory in which a resources configuration is persisted, if the directory does not exist it will be created.
     */
    public File findOrCreateResourceDir(ResourceInfo r) throws IOException {
        Resource directory = get(path(r));
        return directory.dir();
    }

    @Deprecated
    private File resourceDir(boolean create, ResourceInfo resource) throws IOException {
        if (create) {
            Resource directory = get(path(resource));
            return directory.dir();
        } else {
            Resource directory = get(path(resource));
            return Resources.directory(directory);
        }
    }

    private String path(ResourceInfo resource) {
        StoreInfo store = resource.getStore();
        WorkspaceInfo workspace = store.getWorkspace();
        String path = Paths.path("workspaces", workspace.getName(), store.getName(),
                resource.getName());
        return path;
    }

    private String path(ResourceInfo resource, String filename) {
        StoreInfo store = resource.getStore();
        WorkspaceInfo workspace = store.getWorkspace();
        String path = Paths.path("workspaces", workspace.getName(), store.getName(),
                resource.getName(), filename);
        return path;
    }

    /**
     * Returns the configuration file for the specified resource, if the file does not exist null is returned.
     */
    public File findResourceFile(ResourceInfo r) throws IOException {
        String path = pathResourceFile(r);
        Resource resource = get(path);
        return Resources.file(resource);
    }

    /**
     * Returns the configuration file for the specified resource, if the file does not exist a file object is still returned.
     * 
     */
    public File findOrResolveResourceFile(ResourceInfo r) throws IOException {
        String path = pathResourceFile(r);
        Resource resource = get(path);
        return resource.file();
    }

    private String pathResourceFile(ResourceInfo resource) {
        StoreInfo store = resource.getStore();
        WorkspaceInfo workspace = store.getWorkspace();
        if (resource instanceof FeatureTypeInfo) {
            return Paths.path("workspaces", workspace.getName(), store.getName(),
                    resource.getName(), "featuretype.xml");
        } else if (resource instanceof CoverageInfo) {
            return Paths.path("workspaces", workspace.getName(), store.getName(),
                    resource.getName(), "coverage");
        } else {
            throw new IllegalArgumentException("Unsupported resource " + resource.getName() + " "
                    + resource.getClass().getName());
        }
    }

    @Deprecated
    private File resourceFile(boolean create, ResourceInfo r) throws IOException {
        String path = pathResourceFile(r);
        Resource resource = get(path);
        if (create) {
            return resource.file();
        } else {
            return Resources.file(resource);
        }
    }

    /**
     * Returns a supplementary configuration file for a resource, if the file does not exist null is returned.
     */
    public File findSuppResourceFile(ResourceInfo r, String filename) throws IOException {
        Resource resource = get(path(r, filename));
        return Resources.file(resource);
    }

    /**
     * Returns a supplementary configuration file for a resource in a 1.x data directory format. If the file does not exist null is returned.
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
     * <p>
     * If the resource directory does exist it will be created
     * </p>
     */
    public void copyToResourceDir(ResourceInfo resource, File file) throws IOException {
        Resource directory = get(path(resource));
        FileUtils.copyFileToDirectory(file, directory.dir());
    }

    /**
     * Copies data into a feature type configuration directory.
     * <p>
     * If the resource directory does exist it will be created
     * </p>
     */
    public void copyToResourceDir(ResourceInfo resource, InputStream data, String filename)
            throws IOException {
        Resource directory = get(path(resource));
        copy(data, directory.dir(), filename);
    }

    /**
     * Returns the configuration file for the specified namespace, if the file does not exist null is returned.
     */
    public File findNamespaceFile(WorkspaceInfo ws) throws IOException {
        Resource directory = get(pathNamespaceFile(ws));
        return Resources.directory(directory);
    }

    /**
     * Returns the configuration file for the specified namespace, if the file does not exist a file object is still returned.
     */
    public File findOrResolveNamespaceFile(WorkspaceInfo ws) throws IOException {
        Resource directory = get(pathNamespaceFile(ws));
        return directory.dir();
    }

    @Deprecated
    private File namespaceFile(boolean create, WorkspaceInfo ws) throws IOException {
        if (create) {
            Resource resource = get(pathNamespaceFile(ws));
            return resource.file();
        } else {
            Resource resource = get(pathNamespaceFile(ws));
            return Resources.file(resource);
        }
    }

    private String pathNamespaceFile(WorkspaceInfo ws) {
        return Paths.path("workspaces", ws.getName(), "namespace.xml");
    }

    /**
     * Returns the configuration file for the specified layer, if the file does not exist null is returned.
     */
    public File findLayerFile(LayerInfo layer) throws IOException {
        Resource resource = get(pathLayerFile(layer));
        return Resources.file(resource);
    }

    /**
     * Returns the configuration file for the specified layer, if the file does not exist a file object is still returned.
     * 
     */
    public File findOrResolveLayerFile(LayerInfo layer) throws IOException {
        Resource resource = get(pathLayerFile(layer));
        return resource.file();
    }

    @Deprecated
    private File layerFile(boolean create, LayerInfo layer) throws IOException {
        if (create) {
            Resource resource = get(pathLayerFile(layer));
            return resource.file();
        } else {
            Resource resource = get(pathLayerFile(layer));
            return Resources.file(resource);
        }
    }

    private String pathLayerFile(LayerInfo layer) {
        ResourceInfo resource = layer.getResource();
        StoreInfo store = resource.getStore();
        WorkspaceInfo workspace = store.getWorkspace();
        return Paths.path("workspaces", workspace.getName(), store.getName(), resource.getName(),
                "layer.xml");
    }

    /**
     * Returns the directory in which global styles are persisted, if the directory does not exist null is returned.
     */
    public File findStyleDir() throws IOException {
        Resource styles = get("styles");
        return Resources.directory(styles);
    }

    /**
     * Returns the directory in which global styles are persisted, if the directory does not exist it will be created.
     */
    public File findOrCreateStyleDir() throws IOException {
        Resource styles = get("styles");
        return styles.dir();
    }

/**
     * Styles directory (using StyleInfo).
     * 
     * Package visibility {@link GeoServerPersister#dir(StyleInfo).
     * 
     * @param create Create if needed
     * @param styleInfo
     * @return
     * @throws IOException
     */
    File styleDir(boolean create, StyleInfo styleInfo) throws IOException {
        Resource styles = get(pathStyles(styleInfo));
        if (create) {
            return styles.dir();
        } else {
            return Resources.directory(styles);
        }
    }

    /**
     * Access to styles directory for provided workspace (or global styles directory if workspace not provided).
     * 
     * Package visibility for {@link GeoServerPersister}.
     * 
     * @param create Create directory if required
     * @param workspaceInfo Workspace used to access styles directory
     * @return styles directory
     * @throws IOException
     */
    File styleDir(boolean create, WorkspaceInfo workspaceInfo) throws IOException {
        Resource styles = get(pathStyles(workspaceInfo));
        if (create) {
            return styles.dir();
        } else {
            return Resources.directory(styles);
        }
    }

    /**
     * Style directory for the provided workspace (or global styles directory if workspace not provided).
     * 
     * @param workspace
     * @return Path to styles directory
     */
    private String pathStyles(StyleInfo style) {
        WorkspaceInfo workspace = style != null ? style.getWorkspace() : null;
        if (workspace == null) {
            return "styles";
        } else {
            return Paths.path("workspaces", workspace.getName(), "styles");
        }
    }

    /**
     * Style directory for the provided workspace (or global styles directory if workspace not provided).
     * 
     * @param workspace
     * @return Path to styles directory
     */
    private String pathStyles(WorkspaceInfo workspace) {
        if (workspace == null) {
            return "styles";
        } else {
            return Paths.path("workspaces", workspace.getName(), "styles");
        }
    }

    /**
     * Returns the configuration file for the specified style, if the file does not exist null is returned.
     */
    public File findStyleFile(StyleInfo s) throws IOException {
        Resource resource = get(pathStyleFile(s));
        return Resources.file(resource);
    }

    /**
     * Returns the SLD file for the specified style, if the file does not exist null is returned.
     */
    public File findStyleSldFile(StyleInfo s) throws IOException {
        Resource resource = get(pathSldFile(s));
        return Resources.file(resource);
    }

    /**
     * Returns the configuration file for the specified style, if the file does not exist a file object is still returned.
     */
    public File findOrCreateStyleFile(StyleInfo s) throws IOException {
        Resource resource = get(pathStyleFile(s));
        return resource.file();
    }

    /**
     * Returns the SLD file for the specified style, if the file does not exist a file object is still returned.
     */
    public File findOrCreateStyleSldFile(StyleInfo s) throws IOException {
        Resource resource = get(pathSldFile(s));
        return resource.file();
    }

    @Deprecated
    private File styleFile(boolean create, StyleInfo s) throws IOException {
        Resource resource = get(pathStyleFile(s));
        if (create) {
            return resource.file();
        } else {
            return Resources.file(resource);
        }
    }

    /**
     * Path to generated style file, style directory used based on {@link StyleInfo#getWorkspace()}.
     * 
     * @param style
     * @return
     */
    String pathStyleFile(StyleInfo style) {
        String configFileName = style.getName() + ".xml";
        if (configFileName.equals(style.getFilename())) {
            configFileName = configFileName + ".xml";
        }
        WorkspaceInfo workspace = style != null ? style.getWorkspace() : null;
        if (workspace == null) {
            return Paths.convert("styles", configFileName);
        } else {
            return Paths.convert(Paths.path("workspaces", workspace.getName(), "styles"),
                    configFileName);
        }
    }

    /**
     * Path to generated style file, style directory used based on {@link StyleInfo#getWorkspace()}.
     * 
     * @param style
     * @return
     */
    String pathSldFile(StyleInfo style) {
        WorkspaceInfo workspace = style != null ? style.getWorkspace() : null;
        if (workspace == null) {
            return Paths.convert("styles", style.getFilename());
        } else {
            return Paths.convert(Paths.path("workspaces", workspace.getName(), "styles"),
                    style.getFilename());
        }
    }

    @Deprecated
    private File styleSldFile(boolean create, StyleInfo s) throws IOException {
        if (create) {
            Resource resource = get(pathSldFile(s));
            return resource.file();
        } else {
            Resource resource = get(pathSldFile(s));
            return Resources.file(resource);
        }
    }

    /**
     * Copies a file into the global style configuration directory.
     * <p>
     * If the resource directory does exist it will be created
     * </p>
     * 
     * @deprecated use {@link #copyToStyleDir(File, StyleInfo)}
     */
    public void copyToStyleDir(File f) throws IOException {
        Resource styles = get("styles");
        FileUtils.copyFileToDirectory(f, styles.dir());
    }

    /**
     * Copy file to styles directory (determined using {@link StyleInfo#getWorkspace()}).
     * 
     * @param file
     * @param style
     * @throws IOException
     */
    @Deprecated
    public void copyToStyleDir(File file, StyleInfo style) throws IOException {
        Resource styles = get(pathStyles(style));
        FileUtils.copyFileToDirectory(file, styles.dir());
    }

    /**
     * Copies data into the global style directory.
     * <p>
     * If the style directory does exist it will be created
     * </p>
     */
    public void copyToStyleDir(InputStream data, String filename) throws IOException {
        Resource styles = get("styles");
        copy(data, styles.dir(), filename);
    }

    /**
     * Style directory for the provided workspace (or global styles directory if workspace not provided).
     * 
     * @param workspace
     * @return Path to styles directory
     */
    private String pathLayerGroup(LayerGroupInfo layerGroup) {
        WorkspaceInfo workspace = layerGroup.getWorkspace();
        if (workspace == null) {
            return "layergroups";
        } else {
            return Paths.path("workspaces", workspace.getName(), "layergroups");
        }
    }

    /**
     * Style directory for the provided workspace (or global styles directory if workspace not provided).
     * 
     * @param workspace
     * @return Path to styles directory
     */
    private String pathLayerGroup(WorkspaceInfo workspace) {
        if (workspace == null) {
            return "layergroups";
        } else {
            return Paths.path("workspaces", workspace.getName(), "layergroups");
        }
    }

    /**
     * Returns the directory in which global layer groups are persisted, if the directory does not exist null is returned.
     */
    public File findLayerGroupDir() throws IOException {
        Resource resource = get("layergroups");
        return Resources.directory(resource);
    }

    /**
     * Returns the directory in which global layer groups are persisted, if the directory does not exist it will be created.
     */
    public File findOrCreateLayerGroupDir() throws IOException {
        Resource resource = get("layergroups");
        return resource.dir();
    }

    /** Package visibility for {@link GeoServerPersister#dir(LayerGroupInfo)  */
    File layerGroupDir(boolean create, LayerGroupInfo lg) throws IOException {
        if (create) {
            Resource resource = get(pathLayerGroup(lg));
            return resource.dir();
        } else {
            Resource resource = get(pathLayerGroup(lg));
            return Resources.directory(resource);
        }
    }

    /** Package visibility for {@link GeoServerPersister} */
    File layerGroupDir(boolean create, WorkspaceInfo ws) throws IOException {
        if (create) {
            Resource resource = get(pathLayerGroup(ws));
            return resource.dir();
        } else {
            Resource resource = get(pathLayerGroup(ws));
            return Resources.directory(resource);
        }
    }

    //
    // Helper methods
    //
    /**
     * Utility method to copy an InputStream to a new file.
     * 
     * @param data Input stream to copy to a new file
     * @param targetDir Directory for target file
     * @param filename Name of target file
     */
    private void copy(InputStream data, File targetDir, String filename) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(new File(targetDir, filename));
            IOUtils.copy(data, out);
            out.flush();
        } finally {
            IOUtils.closeQuietly(out);
        }
    }

}
