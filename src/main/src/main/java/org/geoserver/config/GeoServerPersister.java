/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.geoserver.config.util.XStreamUtils.xStreamPersist;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.io.FilenameUtils;
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
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
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
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;



public class GeoServerPersister implements CatalogListener, ConfigurationListener {

    private static final int MAX_RENAME_ATTEMPTS = 100;

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
                    Resource oldDir = dd.get((StoreInfo) source );
                    moveResToDir(oldDir, dd.get(newWorkspace));
                }
            }
            
            //handle the case of a feature type changing store
            if ( source instanceof FeatureTypeInfo ) {
                i = event.getPropertyNames().indexOf( "store");
                if ( i > -1 ) {
                    StoreInfo newStore = (StoreInfo) event.getNewValues().get( i );
                    Resource oldDir = dd.get((FeatureTypeInfo) source);
                    Resource newDir = dd.get(newStore);
                    moveResToDir(oldDir, newDir);
                }
            }

            //handle the case of a style changing workspace
            if (source instanceof StyleInfo) {
                i = event.getPropertyNames().indexOf("workspace");
                if (i > -1) {
                    WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get( i );
                    Resource newDir = dd.getStyles(newWorkspace);

                    // look for any resource files (image, etc...) and copy them over, don't move 
                    // since they could be shared among other styles
                    for (Resource old : dd.additionalStyleResources((StyleInfo) source)) {
                        if (old.getType() != Type.UNDEFINED){
                            copyResToDir(old, newDir);
                        }
                    }

                    //move over the config file and the sld
                    for (Resource old : baseResources((StyleInfo)source)) {
                        if (old.getType() != Type.UNDEFINED){
                            moveResToDir(old, newDir);
                        }
                    }

                }
            }

            //handle the case of a layer group changing workspace
            if (source instanceof LayerGroupInfo) {
                i = event.getPropertyNames().indexOf("workspace");
                if (i > -1) {
                    final WorkspaceInfo newWorkspace = (WorkspaceInfo) event.getNewValues().get( i );
                    final Resource oldRes = dd.config((LayerGroupInfo)source);
                    final Resource newDir = dd.getLayerGroups(newWorkspace);
                    moveResToDir(oldRes, newDir);
                }
            }

            //handle default workspace
            if ( source instanceof Catalog ) {
                i = event.getPropertyNames().indexOf("defaultWorkspace");
                if ( i > -1 ) {
                    WorkspaceInfo defWorkspace = (WorkspaceInfo) event.getNewValues().get( i );
                    // SG don't bother with a default workspace if we do not have one
                    if (defWorkspace != null) {
                        persist(defWorkspace, dd.getWorkspaces("default.xml"));
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
            persist( global, dd.config(global) );
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
            
            moveResToDir(dd.config(settings), dd.get(newWorkspace));
        }
    }

    public void handleSettingsPostModified(SettingsInfo settings) {
        LOGGER.fine( "Persisting settings " + settings );
        try {
            persist(settings, dd.config(settings));
        }
        catch(IOException e) {
            throw new RuntimeException( e );
        }
    }

    public void handleSettingsRemoved(SettingsInfo settings) {
        LOGGER.fine( "Removing settings " + settings );
        rmRes(dd.config(settings));
    }

    public void handleLoggingChange(LoggingInfo logging, List<String> propertyNames,
            List<Object> oldValues, List<Object> newValues) {
    }
    
    public void handlePostLoggingChange(LoggingInfo logging) {
        try {
            persist( logging, dd.config(logging) );
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
    private void addWorkspace( WorkspaceInfo ws ) throws IOException {
        LOGGER.fine( "Persisting workspace " + ws.getName() );
        Resource xml = dd.config(ws);
        persist( ws, xml );
    }
    
    private void renameWorkspace( WorkspaceInfo ws, String newName ) throws IOException {
        LOGGER.fine( "Renaming workspace " + ws.getName() + "to " + newName );
        Resource directory = dd.get(ws);
        renameRes(directory, newName);
    }
    
    private void modifyWorkspace( WorkspaceInfo ws ) throws IOException {
        LOGGER.fine( "Persisting workspace " + ws.getName() );
        Resource r = dd.config(ws);
        persist( ws, r );
    }
    
    private void removeWorkspace( WorkspaceInfo ws ) throws IOException {
        LOGGER.fine( "Removing workspace " + ws.getName() );
        Resource directory = dd.get(ws);
        rmRes(directory);
    }
    
    //namespaces
    private void addNamespace( NamespaceInfo ns ) throws IOException {
        LOGGER.fine( "Persisting namespace " + ns.getPrefix() );
        Resource xml = dd.config(ns);
        persist( ns, xml );
    }
    
    private void modifyNamespace( NamespaceInfo ns) throws IOException {
        LOGGER.fine( "Persisting namespace " + ns.getPrefix() );
        Resource xml = dd.config(ns);
        persist( ns, xml );
    }
    
    private void removeNamespace( NamespaceInfo ns ) throws IOException {
        LOGGER.fine( "Removing namespace " + ns.getPrefix() );
        Resource directory = dd.get(ns);
        rmRes(directory);
    }
    
    //datastores
    private void addDataStore( DataStoreInfo ds ) throws IOException {
        LOGGER.fine( "Persisting datastore " + ds.getName() );
        Resource xml = dd.config(ds);
        persist( ds, xml );
    }
    
    private void renameStore( StoreInfo s, String newName ) throws IOException {
        LOGGER.fine( "Renaming store " + s.getName() + "to " + newName );
        Resource directory = dd.get(s);
        renameRes(directory, newName);
    }
    
    private void modifyDataStore( DataStoreInfo ds ) throws IOException {
        LOGGER.fine( "Persisting datastore " + ds.getName() );
        Resource xml = dd.config(ds);
        persist( ds, xml );
    }
    
    private void removeDataStore( DataStoreInfo ds ) throws IOException {
        LOGGER.fine( "Removing datastore " + ds.getName() );
        Resource directory = dd.get(ds);
        rmRes(directory);
    }
    
    //feature types
    private void addFeatureType( FeatureTypeInfo ft ) throws IOException {
        LOGGER.fine( "Persisting feature type " + ft.getName() );
        Resource xml = dd.config(ft);
        persist( ft, xml );
    }
    
    private void renameResource( ResourceInfo r, String newName ) throws IOException {
        LOGGER.fine( "Renaming resource " + r.getName() + " to " + newName );
        Resource directory = dd.get(r);
        renameRes(directory, newName);
    }
    
    private void modifyFeatureType( FeatureTypeInfo ft ) throws IOException {
        LOGGER.fine( "Persisting feature type " + ft.getName() );
        Resource xml = dd.config(ft);
        persist( ft, xml );
    }
    
    private void removeFeatureType( FeatureTypeInfo ft ) throws IOException {
        LOGGER.fine( "Removing feature type " + ft.getName() );
        Resource directory = dd.get(ft);
        rmRes(directory);
    }
    
    //coverage stores
    private void addCoverageStore( CoverageStoreInfo cs ) throws IOException {
        LOGGER.fine( "Persisting coverage store " + cs.getName() );
        Resource xml = dd.config(cs);
        persist( cs, xml );
    }
    
    private void modifyCoverageStore( CoverageStoreInfo cs ) throws IOException {
        LOGGER.fine( "Persisting coverage store " + cs.getName() );
        Resource r = dd.config(cs);
        persist( cs, r );
    }
    
    private void removeCoverageStore( CoverageStoreInfo cs ) throws IOException {
        LOGGER.fine( "Removing coverage store " + cs.getName() );
        Resource r = dd.get(cs);
        rmRes(r);
    }
    
    //coverages
    private void addCoverage( CoverageInfo c ) throws IOException {
        LOGGER.fine( "Persisting coverage " + c.getName() );
        Resource xml = dd.config(c);
        persist( c, xml );
    }
    
    private void modifyCoverage( CoverageInfo c ) throws IOException {
        LOGGER.fine( "Persisting coverage " + c.getName() );
        Resource xml = dd.config(c);
        persist( c, xml );
    }
    
    private void removeCoverage( CoverageInfo c ) throws IOException {
        LOGGER.fine( "Removing coverage " + c.getName() );
        Resource directory = dd.get(c);
        rmRes(directory);
    }
    
    //wms stores
    private void addWMSStore( WMSStoreInfo wmss ) throws IOException {
        LOGGER.fine( "Persisting wms store " + wmss.getName() );
        Resource xml = dd.config(wmss);
        persist(wmss, xml);
    }
    
    private void modifyWMSStore( WMSStoreInfo wmss ) throws IOException {
        LOGGER.fine( "Persisting wms store " + wmss.getName() );
        Resource xml = dd.config(wmss);
        persist(wmss, xml);
    }
    
    private void removeWMSStore( WMSStoreInfo wmss ) throws IOException {
        LOGGER.fine( "Removing datastore " + wmss.getName() );
        Resource directory = dd.get(wmss);
        rmRes(directory);
    }
    
    //wms layers
    private void addWMSLayer( WMSLayerInfo wms ) throws IOException {
        LOGGER.fine( "Persisting wms layer " + wms.getName() );
        Resource xml = dd.config(wms);
        persist( wms, xml );
    }
    
    private void modifyWMSLayer( WMSLayerInfo wms ) throws IOException {
        LOGGER.fine( "Persisting wms layer" + wms.getName() );
        Resource xml = dd.config(wms);
        persist( wms, xml );
    }
    
    private void removeWMSLayer( WMSLayerInfo wms ) throws IOException {
        LOGGER.fine( "Removing wms layer " + wms.getName() );
        Resource directory = dd.get(wms);
        rmRes(directory);
    }
    
    //layers
    private void addLayer( LayerInfo l ) throws IOException {
        LOGGER.fine( "Persisting layer " + l.getName() );
        Resource xml = dd.config(l);
        persist( l, xml );
    }
    
    private void modifyLayer( LayerInfo l ) throws IOException {
        LOGGER.fine( "Persisting layer " + l.getName() );
        Resource xml = dd.config(l);
        persist( l, xml );
    }
    
    private void removeLayer( LayerInfo l ) throws IOException {
        LOGGER.fine( "Removing layer " + l.getName() );
        Resource directory = dd.get(l);
        rmRes(directory);
    }
    
    //styles
    private void addStyle( StyleInfo s ) throws IOException {
        LOGGER.fine( "Persisting style " + s.getName() );
        Resource xml = dd.config(s);
        persist( s, xml );
    }
    
    private void renameStyle( StyleInfo s, String newName ) throws IOException {
        LOGGER.fine( "Renaming style " + s.getName() + " to " + newName );
        
        // rename xml configuration file
        Resource xml = dd.config(s);
        renameRes( xml, newName+".xml" );
        
        // rename style definition file
        Resource style = dd.style(s);
        StyleHandler format = Styles.handler( s.getFormat() );
        
        Resource target = uniqueResource( style, newName, format.getFileExtension() );
        renameRes(style, target.name());
        s.setFilename(target.name());
        
        // rename generated sld if appropriate
        if( !SLDHandler.FORMAT.equals(format.getFormat())){
            Resource sld = style.parent().get(FilenameUtils.getBaseName(style.name()) + ".sld");
            if( sld.getType() == Type.RESOURCE ){
                Resource generated = uniqueResource( sld, newName, "sld" );
                renameRes(sld, generated.name());    
            }
        }
    }
    
    /**
     * Determine unique name of the form <code>newName.extension</code>. newName will
     * have a number appended as required to produce a unique resource name.
     * 
     * @param resource Resource being renamed
     * @param newName proposed name to use as a template
     * @param extension extension
     * @return New UNDEFINED resource suitable for use with rename
     * @throws IOException If unique resource cannot be produced
     */
    private Resource uniqueResource(Resource resource, String newName, String extension)
            throws IOException {
        Resource target = resource.parent().get(newName + "." + extension);

        int i = 0;
        while (target.getType() != Type.UNDEFINED && ++i <= MAX_RENAME_ATTEMPTS) {
            target = resource.parent().get(newName + i + "." + extension);
        }
        if (i > MAX_RENAME_ATTEMPTS) {
            throw new IOException("All target files between " + newName + "1." + extension
                    + " and " + newName + MAX_RENAME_ATTEMPTS + "." + extension
                    + " are in use already, giving up");
        }
        return target;
    }
    
    private void modifyStyle( StyleInfo s ) throws IOException {
        LOGGER.fine( "Persisting style " + s.getName() );
        Resource xml = dd.config(s);
        persist( s, xml );
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
    
    private void removeStyle( StyleInfo s ) throws IOException {
        LOGGER.fine( "Removing style " + s.getName() );
        Resource xml = dd.config(s);
        rmRes(xml);
    }

    /*
     * returns the SLD file as well
     */
    private List<Resource> baseResources(StyleInfo s) throws IOException {
        List<Resource> list = 
            Arrays.asList(dd.config(s), dd.style(s));
        return list;
    }
    
    /*
     * returns additional resource files 
     */
    private List<Resource> additionalResources(StyleInfo s) throws IOException {
        final List<Resource> files = new ArrayList<Resource>();
        final Resource baseDir = dd.get(s);
        try {
            Style parsedStyle = dd.parsedStyle(s);
            parsedStyle.accept(new AbstractStyleVisitor() {
                @Override
                public void visit(ExternalGraphic exgr) {
                    if (exgr.getOnlineResource() == null) {
                        return;
                    }
    
                    URI uri = exgr.getOnlineResource().getLinkage();
                    if (uri == null) {
                        return;
                    }
    
                    Resource r = null;
                    try {
                        r = uriToResource(baseDir, uri);
                        if (r!=null && r.getType()!=Type.UNDEFINED) files.add(r);
                    } catch (IllegalArgumentException|MalformedURLException e) {
                        LOGGER.log(Level.WARNING, "Error attemping to process SLD resource", e);
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
    private void addLayerGroup( LayerGroupInfo lg ) throws IOException {
        LOGGER.fine( "Persisting layer group " + lg.getName() );
        Resource xml = dd.config(lg);
        persist(lg, xml);
    }
    
    private void renameLayerGroup( LayerGroupInfo lg, String newName ) throws IOException {
        LOGGER.fine( "Renaming layer group " + lg.getName() + " to " + newName );
        Resource xml = dd.config(lg);
        renameRes( xml, String.format("%s.xml", newName));
    }

    private void modifyLayerGroup( LayerGroupInfo lg ) throws IOException {
        LOGGER.fine( "Persisting layer group " + lg.getName() );
        Resource xml = dd.config(lg);
        persist(lg, xml);
    }
    
    private void removeLayerGroup( LayerGroupInfo lg ) throws IOException {
        LOGGER.fine( "Removing layer group " + lg.getName() );
        Resource xml = dd.config(lg);
        rmRes(xml);
    }
    
    private void persist( Object o, Resource r ) throws IOException {
        try {
            synchronized ( xp ) {
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                xp.save(o, bos);
                r.setContents(bos.toByteArray());
            }
            LOGGER.fine("Persisted " + o.getClass().getName() + " to " + r.path() );
        }
        catch( Exception e ) {
            //catch any exceptions and send them back as CatalogExeptions
            String msg = "Error persisting " + o + " to " + r.path();
            throw new CatalogException(msg, e);
        }
    }
    
    private void rmRes(Resource r) {
        rl.remove(r.path());
    }
    private void renameRes(Resource r, String newName) {
        rl.move(r.path(), r.parent().get(newName).path());
    }
    private void moveResToDir(Resource r, Resource newDir) {
        rl.move(r.path(), newDir.get(r.name()).path());
    }
    
    private void copyResToDir(Resource r, Resource newDir) throws IOException {
        Resource newR = newDir.get(r.name());
        try(InputStream in = r.in();
            OutputStream out = newR.out()){
            IOUtils.copy(in, out);
        }
    }
    
    private Resource uriToResource(Resource base, URI uri) throws MalformedURLException {
        if(uri.getScheme()!=null && !uri.getScheme().equals("file")) {
            return null;
        }
        if(uri.isAbsolute() && ! uri.isOpaque()) {
            assert uri.getScheme().equals("file");
            return Files.asResource(new File(uri.toURL().getFile()));
        }  else {
            return base.get(uri.getSchemeSpecificPart());
        }
    }

}
