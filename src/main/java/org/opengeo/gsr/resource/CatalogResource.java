/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.resource;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.config.GeoServer;
import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.exception.ServiceException;
import org.opengeo.gsr.service.AbstractService;
import org.opengeo.gsr.service.CatalogService;
import org.opengeo.gsr.service.GeometryService;
import org.opengeo.gsr.service.MapService;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * @author Juan Marin, OpenGeo
 */
public class CatalogResource extends GeoServicesResource {

    private String formatValue;

    private final String productName = "OpenGeo Suite";

    private final String specVersion = "1.0";

    private final double currentVersion = 10.1;

    public CatalogResource(Context context, Request request, Response response, Class<?> clazz,
            GeoServer geoServer) {
        super(context, request, response, clazz, geoServer);
        this.formatValue = getRequest().getResourceRef().getQueryAsForm().getFirstValue("f");
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        try {
            if (!formatValue.equals("json")) {
                List<String> details = new ArrayList<String>();
                details.add("Format " + formatValue + " is not supported");
                return new ServiceException(new ServiceError(
                        (Status.CLIENT_ERROR_BAD_REQUEST.getCode()), "Output format not supported",
                        details));
            }
            List<AbstractService> services = new ArrayList<AbstractService>();
            for (WorkspaceInfo ws : catalog.getWorkspaces()) {
                MapService ms = new MapService(ws.getName());
                services.add(ms);
            }
            services.add(new GeometryService("Geometry"));
            return new CatalogService("services", specVersion, productName, currentVersion,
                    Collections.<String>emptyList(), services);
        } catch (Exception e) {
            List<String> details = new ArrayList<String>();
            details.add(e.getMessage());
            return new ServiceException(new ServiceError((Status.SERVER_ERROR_INTERNAL.getCode()),
                    "Internal Server Error", details));
        }
    }

}
