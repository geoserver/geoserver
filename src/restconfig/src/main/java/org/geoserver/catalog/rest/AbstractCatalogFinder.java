/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;


import org.geoserver.catalog.Catalog;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.Finder;
import org.restlet.data.Request;

/**
 * Abstract base class for finders of catalog resources.
 * 
 * @author Justin Deoliveira, OpenGEO
 *
 */
public abstract class AbstractCatalogFinder extends Finder {

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
}
