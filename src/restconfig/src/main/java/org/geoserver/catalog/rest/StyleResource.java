/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.UUID;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geotools.styling.Style;
import org.geotools.util.Converters;
import org.geotools.util.Version;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Form;
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
        boolean prettyPrint = isPrettyPrint(request);
        for (StyleHandler sh : Styles.handlers()) {
            for (Version ver : sh.getVersions()) {
                formats.add(new StyleFormat(sh.mimeType(ver), ver, prettyPrint, sh, request));
            }
        }

        return formats;
    }
    
    boolean isPrettyPrint(Request request) {
        Form q = request.getResourceRef().getQueryAsForm();
        String pretty = q.getFirstValue("pretty");
        return pretty != null && Boolean.TRUE.equals(Converters.convert(pretty, Boolean.class));
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
        else if (object instanceof Style || object instanceof InputStream) {

            //figure out the name of the new style, first check if specified directly
            String name = getRequest().getResourceRef().getQueryAsForm().getFirstValue("name");
            ;

            if (name == null) {
                name = findNameFromObject(object);
            }

            //ensure that the style does not already exist
            if (catalog.getStyleByName(workspace, name) != null) {
                throw new RestletException("Style " + name + " already exists.", Status.CLIENT_ERROR_FORBIDDEN);
            }

            // style format/handler
            StyleHandler styleFormat = ((StyleFormat) getFormatPostOrPut()).getHandler();

            StyleInfo sinfo = catalog.getFactory().createStyle();
            sinfo.setName(name);
            sinfo.setFilename(name + "." + styleFormat.getFileExtension());
            sinfo.setFormat(styleFormat.getFormat());
            sinfo.setFormatVersion(styleFormat.versionForMimeType(getRequest().getEntity().getMediaType().getName()));

            if (workspace != null) {
                sinfo.setWorkspace(catalog.getWorkspaceByName(workspace));
            }

            // ensure that a existing resource does not already exist, because we may not want to overwrite it
            GeoServerDataDirectory dataDir = new GeoServerDataDirectory(catalog.getResourceLoader());
            if (dataDir.style(sinfo).getType() != Resource.Type.UNDEFINED) {
                String msg = "Style resource " + sinfo.getFilename() + " already exists.";
                throw new RestletException(msg, Status.CLIENT_ERROR_FORBIDDEN);
            }

            ResourcePool resourcePool = catalog.getResourcePool();
            try {
                if (object instanceof Style) {
                    resourcePool.writeStyle(sinfo, (Style) object);
                } else {
                    resourcePool.writeStyle(sinfo, (InputStream) object);
                }
            } catch (IOException e) {
                throw new RestletException("Error writing style", Status.SERVER_ERROR_INTERNAL, e);
            }

            catalog.add(sinfo);
            LOGGER.info("POST Style " + name);
            return name;
        }

        return null;
    }

    String findNameFromObject(Object object) {
        String name = null;
        if (object instanceof Style) {
            name = ((Style)object).getName();
        }

        if (name == null) {
            // generate a random one
            for (int i = 0; name == null && i < 100; i++) {
                String candidate = "style-"+UUID.randomUUID().toString().substring(0, 7);
                if (catalog.getStyleByName(candidate) == null) {
                    name = candidate;
                }
            }
        }

        if (name == null) {
            throw new RestletException("Unable to generate style name, specify one with 'name' parameter",
                Status.SERVER_ERROR_INTERNAL);
        }

        return name;
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
        else if (object instanceof Style || object instanceof InputStream) {
            /*
             * Force the .sld file to be overriden and it's Style object cleared from the
             * ResourcePool cache
             */
            StyleInfo s = catalog.getStyleByName( workspace, style );

            ResourcePool resourcePool = catalog.getResourcePool();
            if (object instanceof Style) {
                resourcePool.writeStyle(s, (Style) object, true);
            }
            else {
                resourcePool.writeStyle(s, (InputStream)object);
            }

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
