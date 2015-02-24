/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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

/**
 * Abstracts access to the geoserver data directory.
 * <p>
 * Example usage:
 * <pre>
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
 *   
 * </pre>
 * </p>
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class GeoServerDataDirectory {

    /**
     * resource loader
     */
    GeoServerResourceLoader resourceLoader;
    
    /**
     * Creates the data directory specifying the resource loader.
     */
    public GeoServerDataDirectory( GeoServerResourceLoader resourceLoader ) {
        this.resourceLoader = resourceLoader;
    }
    
    /**
     * Creates the data directory specifying the base directory.
     */
    public GeoServerDataDirectory( File baseDirectory ) {
        this( new GeoServerResourceLoader( baseDirectory ) );
    }

    /**
     * Returns the underlying resource loader.
     */
    public GeoServerResourceLoader getResourceLoader() {
        return resourceLoader;
    }

    /**
     * The root of the data directory.
     */
    public File root() {
        return resourceLoader.getBaseDirectory();
    }

    /**
     * Returns a directory under the {@link #root()} directory, if the directory does not exist
     * null will be returned.
     */
    public File findOrCreateDir(String... location) throws IOException {
        return resourceLoader.findOrCreateDirectory(location);
    }

    /**
     * Returns a file under the {@link #root()} directory, if the file does not exist null is 
     * returned.
     */
    public File findFile(String... location) throws IOException {
        return resourceLoader.find(location);
    }

    /**
     * Returns the root of the directory which contains spatial data files, if the directory does
     * exist, null is returned.
     * <p>
     * This directory is called 'data', and is located directly under {@link #root()}
     * </p>
     */
    public File findDataRoot() throws IOException {
        return dataRoot(false);
    }
    
    /**
     * Returns the root of the directory which contains spatial data
     * files, if the directory does not exist it will be created.
     * <p>
     * This directory is called 'data', and is located directly under {@link #root()}
     * </p>
     */
    public File findOrCreateDataRoot() throws IOException {
        return dataRoot(true);
    }
    
    File dataRoot(boolean create) throws IOException {
        return create ? resourceLoader.findOrCreateDirectory( "data" ) 
            : resourceLoader.find( "data");
    }
    
    /**
     * Returns a directory under the {@link #dataRoot()} directory, if the directory does not exist
     * null will be returned.
     */
    public File findDataDir( String... location ) throws IOException {
        return dataDir( false, location );
    }
    
    /**
     * Returns a directory under the {@link #dataRoot()} directory, if the directory does not exist 
     * it will be created.
     */
    public File findOrCreateDataDir( String... location ) throws IOException {
        return dataDir(true, location);
    }
    
    protected File dataDir( boolean create, String... location ) throws IOException {
        return create ? resourceLoader.findOrCreateDirectory(dataRoot(create), location) 
            : resourceLoader.find( dataRoot(create), location );
    }
    
    /**
     * Returns a file under the {@link #dataRoot()} directory, if the file does not exist null is 
     * returned.
     */
    public File findDataFile( String... location ) throws IOException {
        return dataFile(false,location);
    }
    
    /**
     * Returns a file under the {@link #dataRoot()} directory, if the file does not exist it a file
     * object will still be returned.
     */
    public File findOrResolveDataFile( String... location ) throws IOException {
        return dataFile(true,location);
    }
    
    File dataFile( boolean create, String... location ) throws IOException {
        return create ? resourceLoader.createFile(dataRoot(create), location) 
            : resourceLoader.find( dataRoot(create), location );
    }
    
    /**
     * Returns the root of the directory which contains security configuration files, if the 
     * directory does exist, null is returned.
     * <p>
     * This directory is called 'security', and is located directly under {@link #root()}
     * </p>
     */
    public File findSecurityRoot() throws IOException {
        return securityRoot(false);
    }
    
    /**
     * Returns the root of the directory which contains security configuration files, if the 
     * directory does exist it is created.
     * <p>
     * This directory is called 'security', and is located directly under {@link #root()}
     * </p>
     */
    public File findOrCreateSecurityRoot() throws IOException {
        return securityRoot(true);
    }

    File securityRoot(boolean create) throws IOException {
        return create ? resourceLoader.findOrCreateDirectory( "security" ) 
                : resourceLoader.find( "security");
    }

    /**
     * Returns a directory under the {@link #securityRoot()} directory, if the directory does not 
     * exist null will be returned.
     */
    public File findSecurityDir(String... location) throws IOException {
        return securityDir( false, location );
    }

    /**
     * Returns a directory under the {@link #securityRoot()} directory, if the directory does not 
     * exist it will be created.
     */
    public File findOrCreateSecurityDir(String... location) throws IOException {
        return securityDir(true, location);
    }

    File securityDir(boolean create, String... location) throws IOException {
// TODO, mcr ???        
//        return create ? resourceLoader.findOrCreateDirectory(securityRoot(create), location) 
//            : resourceLoader.find( securityRoot(create), location );
      return create ? resourceLoader.findOrCreateDirectory(new File("security"), location) 
              : resourceLoader.find( securityRoot(create), location );
        
    }

    /**
     * Copies a file into a security configuration directory.
     * <p>
     * If the security configuration directory does exist it will be created.
     * </p>
     */
    public void copyToSecurityDir( File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, securityRoot( true ) );
    }

    /**
     * Copies data into a security configuration directory.
     * <p>
     * If the security configuration directory does exist it will be created
     * </p>
     */
    public void copyToSecurityDir( InputStream data, String filename ) 
        throws IOException {
        copy( data, securityRoot( true ), filename );
    }
    
    /**
     * Returns the directory for the specified workspace, if the directory does not exist null is
     * returned.
     */
    public File findWorkspaceDir( WorkspaceInfo ws ) throws IOException {
        return workspaceDir(false,ws); 
    }
    
    /**
     * Returns the directory for the specified workspace, if the directory does not exist it will be
     * created.
     * 
     * @param create If set to true the directory will be created when it does not exist.
     */
    public File findOrCreateWorkspaceDir( WorkspaceInfo ws ) throws IOException {
        return workspaceDir(true,ws); 
    }
    
    File workspaceDir( boolean create, WorkspaceInfo ws ) throws IOException {
        File workspaces = create ? resourceLoader.findOrCreateDirectory( "workspaces" ) 
           : resourceLoader.find( "workspaces" );
        if ( workspaces != null ) {
            return dir(new File( workspaces, ws.getName() ), create);
        }
        return null;
    }
    
    File workspacesDir( boolean create ) throws IOException {
        return create ? resourceLoader.findOrCreateDirectory( "workspaces" ) 
                : resourceLoader.find( "workspaces" );
    }
    
    /**
     * Returns the configuration file for the specified workspace, if the file does not exist null 
     * is returned.
     */
    public File findWorkspaceFile( WorkspaceInfo ws ) throws IOException {
        return workspaceFile(false,ws);
    }
    
    /**
     * Returns the configuration file for the specified workspace, if the file does not exist a 
     * file object will still be returned.
     * 
     */
    public File findOrResolveWorkspaceFile( WorkspaceInfo ws ) throws IOException {
        return workspaceFile(true,ws);
    }
    
    File workspaceFile( boolean create, WorkspaceInfo ws ) throws IOException {
        File wsdir = workspaceDir(create, ws);
        return wsdir != null ? file(new File( wsdir, "workspace.xml" ), create) : null;
    }
    
    /**
     * Returns a supplementary configuration file for a workspace, if the file does not exist null
     * is returned.
     */
    public File findSuppWorkspaceFile( WorkspaceInfo ws, String filename ) throws IOException {
        File wsdir = findWorkspaceDir( ws );
        return wsdir != null ? file(new File( wsdir, filename ), false) : null;
    }
    
    /**
     * Returns a supplementary configuration file in the workspaces directory, if the file 
     * does not exist null is returned.
     */
    public File findSuppWorkspacesFile( WorkspaceInfo ws, String filename ) throws IOException {
        File workspaces = resourceLoader.find( "workspaces" );
        if(workspaces == null) {
            return null;
        } else {
            return file(new File(workspaces, filename), false);
        }
    }
    
    /**
     * Copies a file into a workspace configuration directory.
     * <p>
     * If the workspace configuration directory does exist it will be created.
     * </p>
     */
    public void copyToWorkspaceDir( WorkspaceInfo ws, File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, workspaceDir( true, ws ) );
    }

    /**
     * Copies data into a workspace configuration directory.
     * <p>
     * If the workspace configuration directory does exist it will be created
     * </p>
     */
    public void copyToWorkspaceDir( WorkspaceInfo ws, InputStream data, String filename ) 
        throws IOException {
        copy( data, workspaceDir( true, ws ), filename );
    }
    
    /**
     * Copies data into the root workspaces configuration directory.
     * <p>
     * If the workspace configuration directory does exist it will be created
     * </p>
     */
    public void copyToWorkspacesDir( InputStream data, String filename ) 
        throws IOException {
        copy( data, workspacesDir( true ), filename );
    }
    
    /**
     * Returns the directory in which a stores configuration is persisted, if the directory does 
     * not exists null is returned.
     */
    public File findStoreDir( StoreInfo s ) throws IOException {
        return storeDir( false, s );
    }
    
    /**
     * Returns the directory in which a stores configuration is persisted, if the directory does 
     * not exist it is created.
     */
    public File findOrCreateStoreDir( StoreInfo s ) throws IOException {
        return storeDir( true, s );
    }
    
    File storeDir( boolean create, StoreInfo s ) throws IOException {
        File wsdir = workspaceDir(create,s.getWorkspace());
        return wsdir != null ? dir(new File( wsdir, s.getName() ),create) : null ;   
    }
    
    /**
     * Returns the configuration file for the specified store, if the file does not exist null is 
     * returned.
     */
    public File findStoreFile( StoreInfo s ) throws IOException {
        return storeFile(false,s);
    }
    
    /**
     * Returns the configuration file for the specified store, if the file does not exist a file 
     * object is still returned.
     */
    public File findOrResolveStoreFile( StoreInfo s ) throws IOException {
        return storeFile(true,s);
    }
    
    File storeFile( boolean create, StoreInfo s ) throws IOException {
        File sdir = storeDir(create, s);
        if ( sdir == null ) {
            return null;
        }
        
        if ( s instanceof DataStoreInfo ) {
            return file(new File( sdir, "datastore.xml"), create);
        }
        else if ( s instanceof CoverageStoreInfo ) {
            return file(new File( sdir, "coveragestore.xml"), create);
        }
        return null;
    }
    
    /**
     * Returns a supplementary configuration file for a store, if the file does not exist null is 
     * returned.
     */
    public File findSuppStoreFile( StoreInfo ws, String filename ) throws IOException {
        File sdir = findStoreDir( ws );
        return sdir != null ? file(new File( sdir, filename ), false) : null;
    }
    
    /**
     * Copies a file into a store configuration directory.
     * <p>
     * If the store configuration directory does exist it will be created
     * </p>
     */
    public void copyToStoreDir( StoreInfo s, File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, storeDir( true, s ) );
    }

    /**
     * Copies data into a store configuration directory.
     * <p>
     * If the store configuration directory does exist it will be created
     * </p>
     */
    public void copyToStoreDir( StoreInfo s, InputStream data, String filename ) 
        throws IOException {
        copy( data, storeDir( true, s ), filename );
    }
    

    /**
     * Returns the directory in which a resources configuration is persisted, if the directory does
     * not exist null is returned.
     */
    public File findResourceDir( ResourceInfo r ) throws IOException {
        return resourceDir(false,r);
    }
    
    /**
     * Finds the directory for the resource assuming a 1.x style data directory.
     * <p>
     * Something like:
     * <pre>
     * featureTypes/states_shapefile_states
     * coverages/sfdem_dem
     * </pre>
     * </p>
     * 
     * @param r The resource.
     * 
     * @return The directory for the resource, or null if it could not be found.
     */
    public File findLegacyResourceDir( ResourceInfo r ) throws IOException {
        String dirname = r.getStore().getName() + "_" + r.getName();
        File dir = null;
        if ( r instanceof FeatureTypeInfo ) {
            dir = resourceLoader.find("featureTypes", dirname);
        }
        else if ( r instanceof CoverageInfo ) {
            dir = resourceLoader.find("coverages", dirname);
        }
        
        return dir != null ? dir : null;
    }
    
    /**
     * Returns the directory in which a resources configuration is persisted, if the directory does
     * not exist it will be created.
     */
    public File findOrCreateResourceDir( ResourceInfo r ) throws IOException {
        return resourceDir(true,r);
    }
    
    File resourceDir( boolean create, ResourceInfo r ) throws IOException {
        File sdir = storeDir(create, r.getStore());
        return sdir != null ? dir(new File( sdir, r.getName() ), create) : null;
    }
    
    
    /**
     * Returns the configuration file for the specified resource, if the file does not exist null is
     * returned.
     */
    public File findResourceFile( ResourceInfo r ) throws IOException {
        return resourceFile(false,r);
    }
    
    /**
     * Returns the configuration file for the specified resource, if the file does not exist a file
     * object is still returned.
     * 
     */
    public File findOrResolveResourceFile( ResourceInfo r ) throws IOException {
        return resourceFile(true,r);
    }
    
    File resourceFile( boolean create, ResourceInfo r ) throws IOException {
        File rdir = resourceDir( create, r );
        if ( rdir == null ) {
            return null;
        }
        
        if ( r instanceof FeatureTypeInfo ) {
            return file(new File( rdir, "featuretype.xml"), create);
        }
        else if ( r instanceof CoverageInfo ) {
            return file(new File( rdir, "coverage"), create);
        }
        
        return null;
    }
   
    /**
     * Returns a supplementary configuration file for a resource, if the file does not exist null
     * is returned.
     */
    public File findSuppResourceFile( ResourceInfo r, String filename ) throws IOException {
        File rdir = findResourceDir( r );
        return rdir != null ? file(new File( rdir, filename ), false) : null;
    }
    
    /**
     * Returns a supplementary configuration file for a resource in a 1.x data directory format. If 
     * the file does not exist null is returned.
     */
    public File findSuppLegacyResourceFile( ResourceInfo r, String filename ) throws IOException {
        File rdir = findLegacyResourceDir( r );
        return rdir != null ? file(new File( rdir, filename ), false ) : null;
    }
    
    /**
     * Copies a file into a feature type configuration directory.
     * <p>
     * If the resource directory does exist it will be created
     * </p>
     */
    public void copyToResourceDir( ResourceInfo r, File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, resourceDir( true, r ) );
    }

    /**
     * Copies data into a feature type configuration directory.
     * <p>
     * If the resource directory does exist it will be created
     * </p>
     */
    public void copyToResourceDir( ResourceInfo r, InputStream data, String filename ) 
        throws IOException {
        copy( data, resourceDir( true, r ), filename );
    }
    
    /**
     * Returns the configuration file for the specified namespace, if the file does not exist null
     * is returned.
     */
    public File findNamespaceFile( WorkspaceInfo ws ) throws IOException {
        return workspaceDir(false,ws);
    }
    
    /**
     * Returns the configuration file for the specified namespace, if the file does not exist a file
     * object is still returned.
     */
    public File findOrResolveNamespaceFile( WorkspaceInfo ws ) throws IOException {
        return workspaceDir(true,ws);
    }
    
    File namespaceFile( boolean create, WorkspaceInfo ws ) throws IOException {
        File wsdir = workspaceDir(create, ws);
        return wsdir != null ? file(new File( wsdir, "namespace.xml"), create) : null;
    }
    
    /**
     * Returns the configuration file for the specified layer, if the file does not exist null is 
     * returned.
     */
    public File findLayerFile( LayerInfo l ) throws IOException {
        return layerFile(false,l);
    }
    
    /**
     * Returns the configuration file for the specified layer, if the file does not exist a file
     * object is still returned.
     * 
     */
    public File findOrResolveLayerFile( LayerInfo l ) throws IOException {
        return layerFile(true,l);
    }
    
    File layerFile( boolean create, LayerInfo l ) throws IOException {
        File rdir = resourceDir(create, l.getResource());
        return rdir != null ? file(new File( rdir, "layer.xml"), create) : null;
    }
    
    /**
     * Returns the directory in which global styles are persisted, if the directory does not exist 
     * null is returned.
     */
    public File findStyleDir() throws IOException {
        return styleDir(false, (WorkspaceInfo)null);
    }

    /**
     * Returns the directory in which global styles are persisted, if the directory does not exist 
     * it will be created.
     */
    public File findOrCreateStyleDir() throws IOException {
        return styleDir(true, (WorkspaceInfo)null);
    }

    File styleDir(boolean create, StyleInfo s) throws IOException {
        return styleDir(create, s.getWorkspace());
    }

    File styleDir(boolean create, WorkspaceInfo ws) throws IOException {
        File base = ws != null ? workspaceDir(true, ws) : null;
        File d = resourceLoader.find( base, "styles" );
        if ( d == null && create ) {
            d = resourceLoader.createDirectory( base, "styles" );
        }
        return d;
    }

    /**
     * Returns the configuration file for the specified style, if the file does not exist null is
     * returned.
     */
    public File findStyleFile( StyleInfo s ) throws IOException {
        return styleFile(false,s);
    }

    /**
     * Returns the SLD file for the specified style, if the file does not exist null is returned.
     */
    public File findStyleSldFile(StyleInfo s) throws IOException {
        return styleSldFile(false, s);
    }

    /**
     * Returns the configuration file for the specified style, if the file does not exist a file 
     * object is still returned.
     */
    public File findOrCreateStyleFile( StyleInfo s ) throws IOException {
        return styleFile(true,s);
    }

    /**
     * Returns the SLD file for the specified style, if the file does not exist a file object is 
     * still returned.
     */
    public File findOrCreateStyleSldFile(StyleInfo s) throws IOException {
        return styleSldFile(true, s);
    }

    File styleFile( boolean create, StyleInfo s ) throws IOException {
        File sdir = styleDir(create, s);
        if (sdir == null) {
            return null;
        }

        String configFileName = s.getName()+".xml";
        if (configFileName.equals(s.getFilename())) {
            configFileName = configFileName + ".xml";
        }
        return file(new File(sdir, configFileName), create);
    }

    File styleSldFile(boolean create, StyleInfo s) throws IOException {
        File sdir = styleDir(create, s);
        return sdir != null ? file(new File( sdir, s.getFilename()),create) : null;
    }

    /**
     * Copies a file into the global style configuration directory.
     * <p>
     * If the resource directory does exist it will be created
     * </p>
     * @deprecated use {@link #copyToStyleDir(File, StyleInfo)}
     */
    public void copyToStyleDir( File f ) throws IOException {
        FileUtils.copyFileToDirectory( f, styleDir(true, (WorkspaceInfo)null) );
    }

    public void copyToStyleDir(File f, StyleInfo s) throws IOException {
        FileUtils.copyFileToDirectory( f, styleDir( true, s ) );
    }

    /**
     * Copies data into the global style directory.
     * <p>
     * If the style directory does exist it will be created
     * </p>
     */
    public void copyToStyleDir( InputStream data, String filename ) throws IOException {
        copy( data, styleDir(true, (WorkspaceInfo)null), filename );
    }

    
    /**
     * Returns the directory in which global layer groups are persisted, if the directory does not
     * exist null is returned.
     */
    public File finLayerGroupDir() throws IOException {
        return layerGroupDir(false, (WorkspaceInfo)null);
    }

    /**
     * Returns the directory in which global layer groups are persisted, if the directory does not 
     * exist it will be created.
     */
    public File findOrCreateLayerGroupDir() throws IOException {
        return layerGroupDir(true, (WorkspaceInfo)null);
    }

    File layerGroupDir(boolean create, LayerGroupInfo lg) throws IOException {
        return layerGroupDir(create, lg.getWorkspace());
    }

    File layerGroupDir(boolean create, WorkspaceInfo ws) throws IOException {
        File base = ws != null ? workspaceDir(true, ws) : null;
        File d = resourceLoader.find( base, "layergroups" );
        if ( d == null && create ) {
            d = resourceLoader.createDirectory( base, "layergroups" );
        }
        return d;
    }

    //
    // Helper methods
    //
    void copy( InputStream data, File targetDir, String filename ) throws IOException {
        OutputStream out = null;
        try {
            out = new FileOutputStream(new File( targetDir, filename ));
            IOUtils.copy( data, out );
            out.flush();
        } finally {
            IOUtils.closeQuietly(out);
        }
    }
    
    File file( File f ) {
        return file(f,true);
    }
    
    File file( File f, boolean create) {
        if ( create ) {
            return f;
        }
        
        return f.exists() ? f : null;
    }
    
    File dir( File d, boolean create ) {
        if ( create ) {
            d.mkdirs();
            return d;
        }
        
        return d.exists() ? d : null;
    }

}
