/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geotools.styling.Style;
import org.geotools.util.Version;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class StyleResource extends AbstractCatalogResource {

    public StyleResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response, StyleInfo.class, catalog);
    }
    
    @Override
    protected List<DataFormat> createSupportedFormats(Request request,Response response) {
        List<DataFormat> formats =  super.createSupportedFormats(request,response);

        for (StyleHandler sh : Styles.handlers()) {
            for (Version ver : sh.getVersions()) {
                formats.add(new StyleFormat(sh.mimeType(ver), ver, false, sh));
            }
        }

        return formats;
    }
    
    @Override
    protected Object handleObjectGet() {
        String workspace = getAttribute("workspace");
        String style = getAttribute("style");
        
        LOGGER.fine( "GET style " + style );
        StyleInfo sinfo = workspace == null ? catalog.getStyleByName( style ) : 
            catalog.getStyleByName(workspace,style);

        return sinfo;
    }

    @Override
    public boolean allowPost() {
        if (getAttribute("workspace") == null && !isAuthenticatedAsAdmin()) {
            return false;
        }
        return getAttribute("style") == null;
    }
    
    @Override
    protected String handleObjectPost(Object object) throws Exception {
        String workspace = getAttribute("workspace");
        String layer = getAttribute( "layer" );
        
        if ( object instanceof StyleInfo ) {
            StyleInfo style = (StyleInfo) object;
            
            if ( layer != null ) {
                StyleInfo existing = catalog.getStyleByName( style.getName() );
                if ( existing == null ) {
                    //TODO: add a new style to catalog
                    throw new RestletException( "No such style: " + style.getName(), Status.CLIENT_ERROR_NOT_FOUND );
                }
                
                LayerInfo l = catalog.getLayerByName( layer );
                l.getStyles().add( existing );
                
                //check for default
                String def = getRequest().getResourceRef().getQueryAsForm().getFirstValue("default");
                if ( "true".equals( def ) ) {
                    l.setDefaultStyle( existing );
                }
                catalog.save(l);
                LOGGER.info( "POST style " + style.getName() + " to layer " + layer);
            }
            else {

                if (workspace != null) {
                    style.setWorkspace(catalog.getWorkspaceByName(workspace));
                }

                catalog.add( style  );
                LOGGER.info( "POST style " + style.getName() );
            }

            return style.getName();
        }
        else if ( object instanceof Style ) {
            Style style = (Style) object;
            
            //figure out the name of the new style, first check if specified directly
            String name = getRequest().getResourceRef().getQueryAsForm().getFirstValue( "name");
            
            if ( name == null ) {
                //infer name from sld
                name = style.getName();
            }
            
            if ( name == null ) {
                throw new RestletException( "Style must have a name.", Status.CLIENT_ERROR_BAD_REQUEST );
            }
            
            //ensure that the style does not already exist
            if ( catalog.getStyleByName(workspace, name ) != null ) {
                throw new RestletException( "Style " + name + " already exists.", Status.CLIENT_ERROR_FORBIDDEN  );
            }
            
            //serialize the style out into the data directory
            StyleFormat styleFormat = (StyleFormat) getFormatPostOrPut();

            GeoServerResourceLoader loader = catalog.getResourceLoader();
            String path = "styles/" +  name + "." + styleFormat.getHandler().getFileExtension();
            if (workspace != null) {
                path = "workspaces/" + workspace + "/" + path;
            }

            File f;
            try {
                f = loader.find(path);
            } 
            catch (IOException e) {
                throw new RestletException( "Error looking up file", Status.SERVER_ERROR_INTERNAL, e );
            }
            
            if ( f != null ) {
                String msg = "SLD file " + path + ".sld already exists."; 
                throw new RestletException( msg, Status.CLIENT_ERROR_FORBIDDEN);
            }
            
            //TODO: have the writing out of the style delegate to ResourcePool.writeStyle()
            try {
                f = loader.createFile(path) ;
                
                //serialize the file to the styles directory
                BufferedOutputStream out = new BufferedOutputStream( new FileOutputStream ( f ) );
                styleFormat.toRepresentation(style).write(out);
                
                out.flush();
                out.close();
            } 
            catch (IOException e) {
                throw new RestletException( "Error creating file", Status.SERVER_ERROR_INTERNAL, e );
            }
            
            //create a style info object
            StyleInfo sinfo = catalog.getFactory().createStyle();
            sinfo.setName( name );
            sinfo.setFilename( f.getName() );

            if (workspace != null) {
                sinfo.setWorkspace(catalog.getWorkspaceByName(workspace));
            }
            
            catalog.add( sinfo );
            
            LOGGER.info( "POST SLD " + name);
            return name;
        }
        
        return null;
    }

    @Override
    public boolean allowPut() {
        if (getAttribute("workspace") == null && !isAuthenticatedAsAdmin()) {
            return false;
        }
        return getAttribute("style") != null;
    }
    
    @Override
    protected void handleObjectPut(Object object) throws Exception {
        String style = getAttribute("style");
        String workspace = getAttribute("workspace");

        if ( object instanceof StyleInfo ) {
            StyleInfo s = (StyleInfo) object;
            StyleInfo original = catalog.getStyleByName( workspace, style );
     
            //ensure no workspace change
            if (s.getWorkspace() != null) {
                if (!s.getWorkspace().equals(original.getWorkspace())) {
                    throw new RestletException( "Can't change the workspace of a style, instead " +
                        "DELETE from existing workspace and POST to new workspace", Status.CLIENT_ERROR_FORBIDDEN );
                }
            }
            
            new CatalogBuilder( catalog ).updateStyle( original, s );
            catalog.save( original );
        }
        else if ( object instanceof Style ) {
            /*
             * Force the .sld file to be overriden and it's Style object cleared from the
             * ResourcePool cache
             */
            StyleInfo s = catalog.getStyleByName( workspace, style );
            catalog.getResourcePool().writeStyle( s, (Style) object, true );
            /*
             * make sure to save the StyleInfo so that the Catalog issues the notification events
             */
            catalog.save(s);
        }
        
        LOGGER.info( "PUT style " + style);
    }

    @Override
    public boolean allowDelete() {
        return getAttribute( "style" ) != null;
    }
    
    @Override
    protected void handleObjectDelete() throws Exception {
        String workspace = getAttribute("workspace");
        String style = getAttribute("style");
        StyleInfo s = workspace != null ? catalog.getStyleByName(workspace, style) :
            catalog.getStyleByName(style);
        
        //ensure that no layers reference the style
        List<LayerInfo> layers = catalog.getLayers(s);
        if ( !layers.isEmpty() ) {
            throw new RestletException( "Can't delete style referenced by existing layers.", Status.CLIENT_ERROR_FORBIDDEN );
        }
        
        catalog.remove( s );
        
        //check purge parameter to determine if the underlying file 
        // should be deleted
        String p = getRequest().getResourceRef().getQueryAsForm().getFirstValue("purge"); 
        boolean purge = (p != null) ? Boolean.parseBoolean(p) : false;
        catalog.getResourcePool().deleteStyle(s, purge);
        
        LOGGER.info( "DELETE style " + style);
       
    }
}
