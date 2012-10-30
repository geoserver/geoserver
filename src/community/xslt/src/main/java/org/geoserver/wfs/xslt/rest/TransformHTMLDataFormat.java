package org.geoserver.wfs.xslt.rest;

import org.geoserver.catalog.rest.CatalogFreemarkerHTMLFormat;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

public class TransformHTMLDataFormat extends CatalogFreemarkerHTMLFormat {

    public TransformHTMLDataFormat(Class clazz, Request request, Response response,
            Resource resource) {
        super(clazz, request, response, resource);
    }

}
