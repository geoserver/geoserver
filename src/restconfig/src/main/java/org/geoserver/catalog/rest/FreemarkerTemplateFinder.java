/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import org.geoserver.catalog.Catalog;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;


public class FreemarkerTemplateFinder extends AbstractCatalogFinder {

    public FreemarkerTemplateFinder(Catalog catalog) {
        super(catalog);
    }

    @Override
    public Resource findTarget(Request request, Response response) {
        if (request.getResourceRef().getLastSegment().endsWith(".ftl")) {
            return new FreemarkerTemplateResource(request, response, catalog);
        } else {
            return new FreemarkerTemplateListResource(getContext(), request, response, catalog);
        }
    }
}
