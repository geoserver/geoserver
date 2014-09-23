package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.restlet.Finder;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 *
 * @author Jose García (josegar74@gmail.com)
 *
 */
public class StylePackageFinder extends AbstractCatalogFinder {

    public StylePackageFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        String style = (String) request.getAttributes().get("style");
        String workspace = (String) request.getAttributes().get("workspace");

        if (style == null) {
            throw new RestletException( "No such style: " + style, Status.CLIENT_ERROR_NOT_FOUND );
        }

        if (workspace == null) {
            throw new RestletException( "No such workspace: " + workspace, Status.CLIENT_ERROR_NOT_FOUND );
        }

        return new StylePackageResource(request,response,catalog);
    }
}