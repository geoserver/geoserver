/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.resource;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.config.GeoServer;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.ReflectiveJSONFormat;
import org.opengeo.gsr.core.format.GeoServicesJsonFormat;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class GeoServicesResource extends AbstractCatalogResource {

    public GeoServicesResource(Context context, Request request, Response response, Class<?> clazz,
            GeoServer geoServer) {
        super(context, request, response, clazz, geoServer.getCatalog());
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        return null;
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = new ArrayList<DataFormat>();
        formats.add(createJSONFormat(request, response));
        return formats;
    }

    @Override
    protected ReflectiveJSONFormat createJSONFormat(Request request, Response response) {
        GeoServicesJsonFormat format = new GeoServicesJsonFormat();
        configureXStream(format.getXStream());
        return format;
    }
}
