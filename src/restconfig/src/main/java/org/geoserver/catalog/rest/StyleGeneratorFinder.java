/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleGenerator;
import org.geoserver.catalog.StyleHandler;
import org.geoserver.catalog.StyleType;
import org.geoserver.catalog.Styles;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.MediaTypes;
import org.geotools.util.Version;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;

public class StyleGeneratorFinder extends AbstractCatalogFinder implements ApplicationListener {

    StyleGenerator styleGen;

    public StyleGeneratorFinder(Catalog catalog) {
        super(catalog);
        styleGen = new StyleGenerator(catalog);
    }
    
    @Override
    public Resource findTarget(Request request, Response response) {
        
        String template = getAttribute(request, "template");
        String format = getAttribute(request, "format");
        
        // default to sld
        if (format == null) {
            request.getAttributes().put("format", "sld");
        }
        
        StyleType styleType = null;
        
        // check for style type
        if (template != null) {
            for (StyleType st : StyleType.values()) {
                if (template.equalsIgnoreCase(st.toString())) {
                    styleType = st;
                    break;
                }
            }
            if (styleType == null) {
                throw new RestletException( "No such template: " + template, Status.CLIENT_ERROR_NOT_FOUND );
            }
            
        // default to generic
        } else {
            styleType = StyleType.GENERIC;
        }
        
        return new StyleGeneratorResource(getContext(),request,response,styleGen,styleType);
    }

    @Override
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof ContextLoadedEvent) {
            // register style format mime types
            for (StyleHandler sh : Styles.handlers()) {
                Version ver = sh.getVersions().iterator().next();
                MediaTypes.registerExtension(sh.getFileExtension(), new MediaType(sh.mimeType(ver)));
            }
        }
    }
}
