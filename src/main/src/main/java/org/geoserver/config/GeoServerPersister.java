/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
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
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.data.DataUtilities;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.ExternalGraphic;
import org.geotools.util.logging.Logging;

import static org.geoserver.data.util.IOUtils.rename;
import static org.geoserver.data.util.IOUtils.xStreamPersist;

public class GeoServerPersister implements CatalogListener, ConfigurationListener {

    /**
     * logging instance
     */
    static Logger LOGGER = Logging.getLogger( "org.geoserver.config");
     
    GeoServerResourceLoader rl;
    GeoServerDataDirectory dd;
    XStreamPersister xp;
    
    public GeoServerPersister(GeoServerResourceLoader rl, XStreamPersister xp) {
        this.rl = rl;
        this.dd = new GeoServerDataDirectory(rl);
        this.xp = xp;
    }
    
    public void handleAddEvent(CatalogAddEvent event) {
        Object source = event.getSource();
        try {
            if ( source instanceof WorkspaceInfo ) {
                addWorkspace( (WorkspaceInfo) source );
            }
            else if ( source instanceof NamespaceInfo ) {
                addNamespace( (NamespaceInfo) source );
            }
            else if ( source instanceof DataStoreInfo ) {
                addDataStore( (DataStoreInfo) source );
            }
            else if ( source instanceof WMSStoreInfo ) {
                addWMSStore( (WMSStoreInfo) source );
            }
            else if ( source instanceof FeatureTypeInfo ) {
                addFeatureType( (FeatureTypeInfo) source );
            }
            else if ( source instanceof CoverageStoreInfo ) {
                addCoverageStore( (CoverageStoreInfo) source );
            }
            else if ( source instanceof CoverageInfo ) {
                addCoverage( (CoverageInfo) source );
            }
            else if ( source instanceof WMSLayerInfo ) {
                addWMSLayer( (WMSLayerInfo) source );
            }
            else if ( source instanceof LayerInfo ) {
                addLayer( (LayerInfo) source );
            }
            else if ( source instanceof StyleInfo ) {
                addStyle( (StyleInfo) source );
            }
            else if ( source instanceof LayerGroupInfo ) {
                addLayerGroup( (LayerGroupInfo) source );
            }
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void handleModifyEvent(CatalogModifyEvent event) {
        Object source = event.getSource();
        
        try {
            //here we handle name changes
            int i = event.getPropertyNames().indexOf( "name" );
            if ( i > -1 ) {
                String newName = (String) event.getNewValues().get( i );
                
                if ( source instanceof WorkspaceInfo ) {
                    renameWorkspace( (WorkspaceInfo) source, newName );
                }
                else if ( source instanceof StoreInfo ) {
                    renameStore( (StoreInfo) source, newName );
                }
                else if ( source instanceof ResourceInfo ) {
                    renameResource( (ResourceInfo) source, newName );
                }
                else if ( source instanceof StyleInfo ) {
                    renameStyle( (StyleInfo) source, newName );
                }
                else if ( source instanceof LayerGroupInfo ) {
                    renameLayerGroup( (LayerGroupInfo) source, newName );
                }
            }
            
            //handle the case of a store changing workspace
            if ( source instanceof StoreInfo ) {
                i = event.getPropertyNames().indexOf( "workspace");
                if ( i > -1 ) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get( i );
                    File oldDir = dir( (StoreInfo) source );
                    oldDir.renameTo( new File( dir( newWorkspace ), oldDir.getName() ) );
                }
            }
            
            //handle the case of a feature type changing store
            if ( source instanceof FeatureTypeInfo ) {
                i = event.getPropertyNames().indexOf( "store");
                if ( i > -1 ) {
                    StoreInfo newStore = (StoreInfo) event.getNewValues().get( i );
                    File oldDir = dir( (FeatureTypeInfo) source );
                    oldDir.renameTo( new File( dir( newStore ), oldDir.getName() ) );
                }
            }

            //handle the case of a style changing workspace
            if (source instanceof StyleInfo) {
                i = event.getPropertyNames().indexOf("workspace");
                if (i > -1) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get( i );
                    File newDir = dd.styleDir(true, newWorkspace);

                    //look for any resource files (image, etc...) and copy them over, don't move 
                    // since they could be shared among other styles
                    for (File oldFile : resources((StyleInfo) source)) {
                        FileUtils.copyFile(oldFile, new File(newDir, oldFile.getName()));
                    }

                    //move over the config file and the sld
                    for (File oldFile : files((StyleInfo)source)) {
                        oldFile.renameTo(new File(newDir, oldFile.getName()));
                    }

                }
            }

            //handle the case of a layer group changing workspace
            if (source instanceof LayerGroupInfo) {
                i = event.getPropertyNames().indexOf("workspace");
                if (i > -1) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get( i );
                    File oldFile = file((LayerGroupInfo)source);
                    oldFile.renameTo(new File(dd.layerGroupDir(true, newWorkspace), oldFile.getName()));
                }
            }

            //handle default workspace
            if ( source instanceof Catalog ) {
                i = event.getPropertyNames().indexOf("defaultWorkspace");
                if ( i > -1 ) {
                    WorkspaceInfo defWorkspace = (WorkspaceInfo) event.getNewValues().get( i );
                    // SG don't bother with a default workspace if we do not have one
                    if (defWorkspace != null) {
                        File d = rl.findOrCreateDirectory("workspaces");
                        persist(defWorkspace, new File(d, "default.xml"));
                    }
                }
            }
            
        } 
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }
    
    public void handlePostModifyEvent(CatalogPostModifyEvent event) {
        Object source = event.getSource();
        try {
            if ( source instanceof WorkspaceInfo ) {
                modifyWorkspace( (WorkspaceInfo) source);
            }
            else if ( source instanceof DataStoreInfo ) {
                modifyDataStore( (DataStoreInfo) source );
            }
            else if ( source instanceof WMSStoreInfo ) {
                modifyWMSStore( (WMSStoreInfo) source );
            }
            else if ( source instanceof NamespaceInfo ) {
                modifyNamespace( (NamespaceInfo) source );
            }
            else if ( source instanceof FeatureTypeInfo ) {
                modifyFeatureType( (FeatureTypeInfo) source );
            }
            else if ( source instanceof CoverageStoreInfo ) {
                modifyCoverageStore( (CoverageStoreInfo) source );
            }
            else if ( source instanceof CoverageInfo ) {
                modifyCoverage( (CoverageInfo) source );
            }
            else if ( source instanceof WMSLayerInfo ) {
                modifyWMSLayer( (WMSLayerInfo) source );
            }
            else if ( source instanceof LayerInfo ) {
                modifyLayer( (LayerInfo) source );
            }
            else if ( source instanceof StyleInfo ) {
                modifyStyle( (StyleInfo) source );
            }
            else if ( source instanceof LayerGroupInfo ) {
                modifyLayerGroup( (LayerGroupInfo) source );
            }
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void handleRemoveEvent(CatalogRemoveEvent event) {
        Object source = event.getSource();
        try {
            if ( source instanceof WorkspaceInfo ) {
                removeWorkspace( (WorkspaceInfo) source );
            }
            else if ( source instanceof NamespaceInfo ) {
                removeNamespace( (NamespaceInfo) source );
            }
            else if ( source instanceof DataStoreInfo ) {
                removeDataStore( (DataStoreInfo) source );
            }
            else if ( source instanceof FeatureTypeInfo ) {
                removeFeatureType( (FeatureTypeInfo) source );
            }
            else if ( source instanceof CoverageStoreInfo ) {
                removeCoverageStore( (CoverageStoreInfo) source );
            }
            else if ( source instanceof CoverageInfo ) {
                removeCoverage( (CoverageInfo) source );
            }
            else if ( source instanceof WMSStoreInfo ) {
                removeWMSStore( (WMSStoreInfo) source );
            }
            else if ( source instanceof WMSLayerInfo ) {
                removeWMSLayer( (WMSLayerInfo) source );
            }
            else if ( source instanceof LayerInfo ) {
                removeLayer( (LayerInfo) source );
            }
            else if ( source instanceof StyleInfo ) {
                removeStyle( (StyleInfo) source );
            }
            else if ( source instanceof LayerGroupInfo ) {
                removeLayerGroup( (LayerGroupInfo) source );
            }
        }
        catch( IOException e ) {
            throw new RuntimeException( e );
        }
    }

    public void handleGlobalChange(GeoServerInfo global, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
    }
    
    public void handlePostGlobalChange(GeoServerInfo global) {
        try {
            persist( global, new File( rl.getBaseDirectory(), "global.xml") );
        } 
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }

    public void handleSettingsAdded(SettingsInfo settings) {
        handleSettingsPostModified(settings);
    }

    public void handleSettingsModified(SettingsInfo settings, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
        //handle case of settings changing workspace
        int i = propertyNames.indexOf( "workspace");
        if ( i > -1 ) {
            WorkspaceInfo newWorkspace = (WorkspaceInfo) newValues.get( i );
            LOGGER.fine( "Moving settings '" + settings + " to workspace: " + newWorkspace);

            try {
                File oldFile = file(settings);
                oldFile.renameTo( new File( dir( newWorkspace ), oldFile.getName() ) );
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void handleSettingsPostModified(SettingsInfo settings) {
        LOGGER.fine( "Persisting settings " + settings );
        try {
            persist(settings, file(settings));
        }
        catch(IOException e) {
            throw new RuntimeException( e );
        }
    }

    public void handleSettingsRemoved(SettingsInfo settings) {
        LOGGER.fine( "Removing settings " + settings );
        try {
            file(settings).delete();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void handleLoggingChange(LoggingInfo logging, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
    }
    
    public void handlePostLoggingChange(LoggingInfo logging) {
        try {
            persist( logging, new File( rl.getBaseDirectory(), "logging.xml") );
        } 
        catch (IOException e) {
            throw new RuntimeException( e );
        }
    }
    
    public void handleServiceAdded(ServiceInfo service) {
    }
    
    public void handleServiceChange(ServiceInfo service, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
    }
    
    public void handlePostServiceChange(ServiceInfo service) {
    }

    public void handleServiceRemove(ServiceInfo service) {
    }

    public void reloaded() {
    }
    
    //workspaces
    void addWorkspace( WorkspaceInfo ws ) throws IOException {
        LOGGER.fine( "Persisting workspace " + ws.getName() );
        File dir = dir( ws, true );
        dir.mkdirs();
        
        persist( ws, file( ws ) );
    }
    
    void renameWorkspace( WorkspaceInfo ws, String newName ) throws IOException {
        LOGGER.fine( "Renaming workspace " + ws.getName() + "to " + newName );
        rename( dir( ws ), newName );
    }
    
    void modifyWorkspace( WorkspaceInfo ws ) throws IOException {
        LOGGER.fine( "Persisting workspace " + ws.getName() );
        persist( ws, file( ws ) );
    }
    
    void removeWorkspace( WorkspaceInfo ws ) throws IOException {
        LOGGER.fine( "Removing workspace " + ws.getName() );
        rmdir(dir(ws));
    }
    
    File dir( WorkspaceInfo ws ) throws IOException {
        return dir( ws, false );
    }
    
    File dir( WorkspaceInfo ws, boolean create ) throws IOException {
        File d = rl.find( "workspaces", ws.getName() );
        if ( d == null && create ) {
            d = rl.createDirectory( "workspaces", ws.getName() );
        }
        return d;
    }
    
    File file( WorkspaceInfo ws ) throws IOException {
        return new File( dir( ws ), "workspace.xml" );
    }

    //namespaces
    void addNamespace( NamespaceInfo ns ) throws IOException {
        LOGGER.fine( "Persisting namespace " + ns.getPrefix() );
        File dir = dir( ns, true );
        dir.mkdirs();
        persist( ns, file(ns) );
    }
    
    void modifyNamespace( NamespaceInfo ns) throws IOException {
        LOGGER.fine( "Persisting namespace " + ns.getPrefix() );
        persist( ns, file(ns) );
    }
    
    void removeNamespace( NamespaceInfo ns ) throws IOException {
        LOGGER.fine( "Removing namespace " + ns.getPrefix() );
        file( ns ).delete();
    }
    
    File dir( NamespaceInfo ns ) throws IOException {
        return dir( ns, false );
    }
    
    File dir( NamespaceInfo ns, boolean create ) throws IOException {
        File d = rl.find( "workspaces", ns.getPrefix() );
        if ( d == null && create ) {
            d = rl.createDirectory( "workspaces", ns.getPrefix() );
        }
        return d;
    }
    
    File file( NamespaceInfo ns ) throws IOException {
        return new File( dir( ns ), "namespace.xml");
    }
    
    //datastores
    void addDataStore( DataStoreInfo ds ) throws IOException {
        LOGGER.fine( "Persisting datastore " + ds.getName() );
        File dir = dir( ds );
        dir.mkdir();
        
        persist( ds, file( ds ) );
    }
    
    void renameStore( StoreInfo s, String newName ) throws IOException {
        LOGGER.fine( "Renaming store " + s.getName() + "to " + newName );
        rename( dir( s ), newName );
    }
    
    void modifyDataStore( DataStoreInfo ds ) throws IOException {
        LOGGER.fine( "Persisting datastore " + ds.getName() );
        persist( ds, file( ds ) );
    }
    
    void removeDataStore( DataStoreInfo ds ) throws IOException {
        LOGGER.fine( "Removing datastore " + ds.getName() );
        rmdir(dir( ds ));
    }
    
    File dir( StoreInfo s ) throws IOException {
        return new File( dir( s.getWorkspace() ), s.getName() );
    }
    
    File file( DataStoreInfo ds ) throws IOException {
        return new File( dir( ds ), "datastore.xml" );
    }
    
    //feature types
    void addFeatureType( FeatureTypeInfo ft ) throws IOException {
        LOGGER.fine( "Persisting feature type " + ft.getName() );
        File dir = dir( ft );
        dir.mkdir();
        persist( ft, file( ft ) );
    }
    
    void renameResource( ResourceInfo r, String newName ) throws IOException {
        LOGGER.fine( "Renaming resource " + r.getName() + " to " + newName );
        rename( dir( r ), newName );
    }
    
    void modifyFeatureType( FeatureTypeInfo ft ) throws IOException {
        LOGGER.fine( "Persisting feature type " + ft.getName() );
        persist( ft, file( ft ) );
    }
    
    void removeFeatureType( FeatureTypeInfo ft ) throws IOException {
        LOGGER.fine( "Removing feature type " + ft.getName() );
        rmdir(dir( ft ));
    }
    
    File dir( ResourceInfo r ) throws IOException {
        return new File( dir( r.getStore() ), r.getName() );
    }
    
    File file( FeatureTypeInfo ft ) throws IOException {
        return new File( dir( ft ), "featuretype.xml");
    }
    
    //coverage stores
    void addCoverageStore( CoverageStoreInfo cs ) throws IOException {
        LOGGER.fine( "Persisting coverage store " + cs.getName() );
        File dir = dir( cs );
        dir.mkdir();
        
        persist( cs, file( cs ) );
    }
    
    void modifyCoverageStore( CoverageStoreInfo cs ) throws IOException {
        LOGGER.fine( "Persisting coverage store " + cs.getName() );
        persist( cs, file( cs ) );
    }
    
    void removeCoverageStore( CoverageStoreInfo cs ) throws IOException {
        LOGGER.fine( "Removing coverage store " + cs.getName() );
        rmdir(dir( cs ));
    }
    
    File file( CoverageStoreInfo cs ) throws IOException {
        return new File( dir( cs ), "coveragestore.xml");
    }
    
    //coverages
    void addCoverage( CoverageInfo c ) throws IOException {
        LOGGER.fine( "Persisting coverage " + c.getName() );
        File dir = dir( c );
        dir.mkdir();
        persist( c, dir, "coverage.xml" );
    }
    
    void modifyCoverage( CoverageInfo c ) throws IOException {
        LOGGER.fine( "Persisting coverage " + c.getName() );
        File dir = dir( c );
        persist( c, dir, "coverage.xml");
    }
    
    void removeCoverage( CoverageInfo c ) throws IOException {
        LOGGER.fine( "Removing coverage " + c.getName() );
        rmdir(dir( c ));
    }
    
    //wms stores
    void addWMSStore( WMSStoreInfo wms ) throws IOException {
        LOGGER.fine( "Persisting wms store " + wms.getName() );
        File dir = dir( wms );
        dir.mkdir();
        
        persist( wms, file( wms ) );
    }
    
    void modifyWMSStore( WMSStoreInfo ds ) throws IOException {
        LOGGER.fine( "Persisting wms store " + ds.getName() );
        persist( ds, file( ds ) );
    }
    
    void removeWMSStore( WMSStoreInfo ds ) throws IOException {
        LOGGER.fine( "Removing datastore " + ds.getName() );
        rmdir(dir( ds ));
    }
    
    File file( WMSStoreInfo ds ) throws IOException {
        return new File( dir( ds ), "wmsstore.xml" );
    }
    
    //wms layers
    void addWMSLayer( WMSLayerInfo wms ) throws IOException {
        LOGGER.fine( "Persisting wms layer " + wms.getName() );
        File dir = dir( wms );
        dir.mkdir();
        persist( wms, dir, "wmslayer.xml" );
    }
    
    void modifyWMSLayer( WMSLayerInfo wms ) throws IOException {
        LOGGER.fine( "Persisting wms layer" + wms.getName() );
        File dir = dir( wms );
        persist( wms, dir, "wmslayer.xml");
    }
    
    void removeWMSLayer( WMSLayerInfo c ) throws IOException {
        LOGGER.fine( "Removing wms layer " + c.getName() );
        rmdir(dir( c ));
    }
    
    //layers
    void addLayer( LayerInfo l ) throws IOException {
        LOGGER.fine( "Persisting layer " + l.getName() );
        File dir = dir( l );
        dir.mkdir();
        persist( l, file( l ) );
    }
    
    void modifyLayer( LayerInfo l ) throws IOException {
        LOGGER.fine( "Persisting layer " + l.getName() );
        persist( l, file( l ) );
    }
    
    void removeLayer( LayerInfo l ) throws IOException {
        LOGGER.fine( "Removing layer " + l.getName() );
        rmdir(dir( l ));
    }
    
    File dir( LayerInfo l ) throws IOException {
        if ( l.getResource() instanceof FeatureTypeInfo) {
            return dir( (FeatureTypeInfo) l.getResource() );
        }
        else if ( l.getResource() instanceof CoverageInfo ) {
            return dir( (CoverageInfo) l.getResource() );
        }
        else if ( l.getResource() instanceof WMSLayerInfo ) {
            return dir( (WMSLayerInfo) l.getResource() );
        }
        return null;
    }
    
    File file( LayerInfo l ) throws IOException {
        return new File( dir( l ), "layer.xml" );
    }
    
    //styles
    void addStyle( StyleInfo s ) throws IOException {
        LOGGER.fine( "Persisting style " + s.getName() );
        dir( s, true );
        persist( s, file( s ) );
    }
    
    void renameStyle( StyleInfo s, String newName ) throws IOException {
        LOGGER.fine( "Renameing style " + s.getName() + " to " + newName );
        rename( file( s ), newName+".xml" );
    }
    
    void modifyStyle( StyleInfo s ) throws IOException {
        LOGGER.fine( "Persisting style " + s.getName() );
        persist( s, file( s ) );
        /*
        //save out sld
        File f = file(s);
        BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream( f ) );
        SLDTransformer tx = new SLDTransformer();
        try {
            tx.transform( s.getSLD(),out );
            out.flush();
        } 
        catch (TransformerException e) {
            throw (IOException) new IOException().initCause( e );
        }
        finally {
            out.close();
        }
        */
    }
    
    void removeStyle( StyleInfo s ) throws IOException {
        LOGGER.fine( "Removing style " + s.getName() );
        file( s ).delete();
    }
    
    File dir( StyleInfo s ) throws IOException {
        return dir( s, false );
    }
    
    File dir( StyleInfo s, boolean create ) throws IOException {
        return dd.styleDir(create, s);
    }

    File file( StyleInfo s ) throws IOException {
        //special case for styles, if the file name (minus the suffix) matches the id of the style
        // and the suffix is xml (rather than sld) we need to avoid overwritting the actual 
        // style file
        if (s.getFilename() != null && s.getFilename().endsWith(".xml") 
            && s.getFilename().startsWith(s.getName()+".")) {
            //append a second .xml suffix
            return new File( dir( s ), s.getName() + ".xml.xml");
        }
        else {
            return new File( dir( s ), s.getName() + ".xml");
        }
    }

    /*
     * returns the SLD file as well
     */
    List<File> files(StyleInfo s) throws IOException {
        File f = file(s);
        List<File> list = 
            new ArrayList<File>(Arrays.asList(f, new File(f.getParentFile(), s.getFilename())));
        return list;
    }

    /*
     * returns additional resource files 
     */
    List<File> resources(StyleInfo s) throws IOException {
        final List<File> files = new ArrayList<File>();
        try {
            s.getStyle().accept(new AbstractStyleVisitor() {
                @Override
                public void visit(ExternalGraphic exgr) {
                    if (exgr.getOnlineResource() == null) {
                        return;
                    }
    
                    URI uri = exgr.getOnlineResource().getLinkage();
                    if (uri == null) {
                        return;
                    }
    
                    File f = null;
                    try {
                        f = DataUtilities.urlToFile(uri.toURL());
                    } catch (MalformedURLException e) {
                        LOGGER.log(Level.WARNING, "Error attemping to processing SLD resource", e);
                    }
    
                    if (f != null && f.exists()) {
                        files.add(f);
                    }
                }
            });
        }
        catch(IOException e) {
            LOGGER.log(Level.WARNING, "Error loading style", e);
        }

        return files;
    }

    //layer groups
    void addLayerGroup( LayerGroupInfo lg ) throws IOException {
        LOGGER.fine( "Persisting layer group " + lg.getName() );
        
        dir( lg, true );
        persist( lg, file( lg ) );
    }
    
    void renameLayerGroup( LayerGroupInfo lg, String newName ) throws IOException {
        LOGGER.fine( "Renaming layer group " + lg.getName() + " to " + newName );
        rename( file( lg ), newName+".xml" );
    }

    void modifyLayerGroup( LayerGroupInfo lg ) throws IOException {
        LOGGER.fine( "Persisting layer group " + lg.getName() );
        persist( lg, file( lg ) );
    }
    
    void removeLayerGroup( LayerGroupInfo lg ) throws IOException {
        LOGGER.fine( "Removing layer group " + lg.getName() );
        file( lg ).delete();
    }
    
    File dir( LayerGroupInfo lg ) throws IOException {
        return dir( lg, false );
    }
    
    File dir( LayerGroupInfo lg, boolean create ) throws IOException {
        return dd.layerGroupDir(create, lg);
    }
    
    File file( LayerGroupInfo lg ) throws IOException {
        return new File( dir( lg ), lg.getName() + ".xml" );
    }

    // settings
    File file(SettingsInfo settings) throws IOException {
        File dir = settings.getWorkspace() != null ? dir(settings.getWorkspace()) : 
            rl.getBaseDirectory();
        return new File(dir, "settings.xml");
    }

    //helpers
    void persist( Object o, File dir, String filename ) throws IOException {
        persist( o, new File( dir, filename ) );
    }

    void persist( Object o, File f ) throws IOException {
        try {
            synchronized ( xp ) {
                xStreamPersist(f, o, xp);
            }
            LOGGER.fine("Persisted " + o.getClass().getName() + " to " + f.getAbsolutePath() );
        }
        catch( Exception e ) {
            //catch any exceptions and send them back as CatalogExeptions
            String msg = "Error persisting " + o + " to " + f.getCanonicalPath();
            throw new CatalogException(msg, e);
        }
    }

    void rmdir(File dir) throws IOException {
        if (dir != null) {
            FileUtils.deleteDirectory( dir );
        }
    }
}
