/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.Styles;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.MediaTypes;
import org.geotools.util.Version;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

/**
 * {@link AbstractCatalogFinder} implementation for Styles.
 * 
 * Implements {@link ApplicationListener} in order to register style file extensions as shortcuts for the corresponding media type in the
 * {@link MediaTypes} registry, but only if the file extension is not already mapped to another media type.
 *
 */
public class StyleFinder extends AbstractCatalogFinder implements ApplicationListener {

    public StyleFinder(Catalog catalog) {
        super(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        String workspace = getAttribute(request, "workspace");
        String style = getAttribute(request, "style");
        String layer = getAttribute(request, "layer");

        //check if workspace exists
        if (workspace != null && catalog.getWorkspaceByName(workspace) == null) {
            throw new RestletException( "No such workspace: " + workspace, Status.CLIENT_ERROR_NOT_FOUND );
        }
        //check style exists if specified
        if ( style != null) {
            // Check if the quietOnNotFound parameter is set
            boolean quietOnNotFound=quietOnNotFoundEnabled(request);            
            //ensure it exists
            if (workspace != null && catalog.getStyleByName( workspace, style ) == null) {
                // If true, no exception is returned
                if(quietOnNotFound){
                    return null;
                }
                throw new RestletException(String.format("No such style %s in workspace %s", 
                    style, workspace), Status.CLIENT_ERROR_NOT_FOUND );
            }
            if (workspace == null && catalog.getStyleByName((String)null, style) == null) {
                // If true, no exception is returned
                if(quietOnNotFound){
                    return null;
                }
                throw new RestletException( "No such style: " + style, Status.CLIENT_ERROR_NOT_FOUND );
            }
        }

        //check layer exists if specified
        if ( layer != null && catalog.getLayerByName( layer ) == null ) {
            throw new RestletException( "No such layer: " + layer, Status.CLIENT_ERROR_NOT_FOUND);
            /*
            String ns = null;
            String resource = null;
            
            if ( layer.contains( ":" ) ) {
                String[] split = layer.split(":");
                ns = split[0];
                resource = split[1];
            }
            else {
                ns = catalog.getDefaultNamespace().getPrefix();
                resource = layer;
            }
            
            if ( catalog.getResourceByName( ns, resource, ResourceInfo.class) == null ) {
                throw new RestletException( "No such layer: " + ns + "," + resource, Status.CLIENT_ERROR_NOT_FOUND);
            }
            
            //set the parsed result as request attributes
            request.getAttributes().put( "namespace", ns );
            request.getAttributes().put( "resource", resource );
            */
        }
        
        if ( style == null && request.getMethod() == Method.GET ) {
            return new StyleListResource(getContext(),request,response,catalog);
        }
        
        return new StyleResource(getContext(),request,response,catalog);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextLoadedEvent) {
            // register style format mime types
            for (StyleHandler sh : Styles.handlers()) {
                // Only map the StyleHandler's extension to its mime type 
                // if the extension does not conflict with existing mappings.
                if (MediaTypes.getMediaTypeForExtension(sh.getFileExtension()) == null) {
                    Version ver = sh.getVersions().iterator().next();
                    MediaTypes.registerExtension(sh.getFileExtension(), new MediaType(sh.mimeType(ver)));
                }
            }
        }
    }
}
