/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;


import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.Finder;
import org.restlet.data.Form;
import org.restlet.data.Request;

/**
 * Abstract base class for finders of catalog resources.
 * 
 * @author Justin Deoliveira, OpenGEO
 *
 */
public abstract class AbstractCatalogFinder extends Finder {
    
    public static final boolean DEFAULT_QUIET_ON_NOT_FOUND = false;
    
    /**
     * reference to the catalog
     */
    protected Catalog catalog;
    
    protected AbstractCatalogFinder( Catalog catalog ) {
        this.catalog = catalog;
    }
    
    /**
     * Convenience method for subclasses to look up the (URL-decoded)value of
     * an attribute from the request, ie {@link Request#getAttributes()}.
     * 
     * @param attribute The name of the attribute to lookup.
     * 
     * @return The value as a string, or null if the attribute does not exist
     *     or cannot be url-decoded.
     */
    protected String getAttribute(Request request, String attribute) {
        return RESTUtils.getAttribute(request, attribute);
    }
    //@Override
    //public abstract AbstractCatalogResource findTarget(Request request, Response response);
    
    /**
     * Utility method for checking if the "quietOnNotFound" parameter is present.
     * 
     * @param request Input REST request
     * @return a boolean indicating the parameter value. It not present false is set.
     */
    protected boolean quietOnNotFoundEnabled(Request request) {
        
        Boolean quietOnNotFound = null;
        Form form = request.getResourceRef().getQueryAsForm();
        String quietOnNotFoundS = form
                .getFirstValue(RESTUtils.QUIET_ON_NOT_FOUND_KEY);
        if(quietOnNotFoundS != null && !quietOnNotFoundS.isEmpty()){
            quietOnNotFound = Boolean.parseBoolean(quietOnNotFoundS);
        }else{
            GeoServerInfo gsInfo = GeoServerExtensions.bean(GeoServer.class).getGlobal();
            // Global info should be always not null
            if(gsInfo != null){
                SettingsInfo info = gsInfo.getSettings();
                MetadataMap map = info != null ? info.getMetadata() : null;
                if(map != null && map.containsKey(RESTUtils.QUIET_ON_NOT_FOUND_KEY)){
                    quietOnNotFound = map.get(RESTUtils.QUIET_ON_NOT_FOUND_KEY, Boolean.class);
                }
            }
        }
        if(quietOnNotFound == null){
            quietOnNotFound = DEFAULT_QUIET_ON_NOT_FOUND;
        }
        return quietOnNotFound;
    }
}
