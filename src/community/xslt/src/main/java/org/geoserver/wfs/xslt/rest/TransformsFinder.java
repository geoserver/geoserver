/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.xslt.rest;

import java.io.IOException;

import org.geoserver.catalog.Catalog;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.wfs.xslt.config.TransformRepository;
import org.restlet.Finder;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * Locates XSLT transformations
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class TransformsFinder extends Finder {

    private TransformRepository repository;
    private Catalog catalog;

    public TransformsFinder(TransformRepository repository, Catalog catalog) {
        this.repository = repository;
        this.catalog = catalog;
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        String transform = RESTUtils.getAttribute(request, "transform");

        // if it's not referencing a particular transform, return the list
        if (transform == null && request.getMethod() == Method.GET) {
            return new TransformListResource(getContext(), request, response, repository);
        }

        // ensure referenced transform exist
        try {
            if (transform != null && repository.getTransformInfo(transform) == null) {
                throw new RestletException("No such transform: " + transform,
                        Status.CLIENT_ERROR_NOT_FOUND);
            }
        } catch (IOException e) {
            throw new RestletException("An error occurred while trying to locate the transform: "
                    + transform, Status.SERVER_ERROR_INTERNAL, e);
        }

        // return the specific resource
        return new TransformResource(null, request, response, repository, catalog);
    }

}
