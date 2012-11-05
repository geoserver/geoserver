/* Copyright (c) 2012 LMN Solutions, inc. - www.lmnsolutions.com. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.ms.resource;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.config.GeoServer;
import org.opengeo.gsr.core.exception.ServiceError;
import org.opengeo.gsr.core.exception.ServiceException;
import org.opengeo.gsr.core.geometry.Envelope;
import org.opengeo.gsr.ms.service.MapService;
import org.opengeo.gsr.resource.GeoServicesResource;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

/**
 * 
 * @author Brett Antonides, LMN Solutions
 * 
 */

public class MapResource extends GeoServicesResource {

    @SuppressWarnings("unused")
    private final String format;
    
    private String workspace;
    
    @SuppressWarnings("unused")
    private static final String productName = "OpenGeo Suite";

    @SuppressWarnings("unused")
    private static final String specVersion = "1.0";

    private final double currentVersion = 10.1;

    public MapResource(Context context, Request request, Response response,
			Class<?> clazz, GeoServer geoServer, String format) {
		super(context, request, response, clazz, geoServer);
		this.format = format;
		workspace = getAttribute("workspace");
	}
    
    @Override
    protected Object handleObjectGet() throws Exception {
    	try {
    		LayerGroupInfo layerGroupInfo = catalog.getFacade().getLayerGroupByName(workspace);
    		return new MapService(workspace, currentVersion, new Envelope(layerGroupInfo.getBounds()));
        } catch (Exception e) {
            List<String> details = new ArrayList<String>();
            details.add(e.getMessage());
            return new ServiceException(new ServiceError((Status.SERVER_ERROR_INTERNAL.getCode()),
                    "Internal Server Error", details));
        }
    }

}
