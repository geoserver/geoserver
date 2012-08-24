/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.service;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

import org.geoserver.catalog.rest.AbstractCatalogFinder;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.wms.WMS;
import org.opengeo.gsr.resource.CatalogResource;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.Resource;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class ServiceFinder extends AbstractCatalogFinder {

    private GeoServer geoServer;

    private WMS wms;

    private Dispatcher dispatcher;

    protected ServiceFinder(GeoServer geoServer, WMS wms, Dispatcher dispatcher) {
        super(geoServer.getCatalog());
        this.geoServer = geoServer;
        this.wms = wms;
        this.dispatcher = dispatcher;
    }

    public Resource findTarget(Request request, Response response) {
        Resource resource = null;

        try {
            Map<String, Object> attributes = request.getAttributes();
            String serviceType = "CatalogServer";
            String format = getAttribute(request, "format");
            // if (attributes.get("serviceType") != null) {
            // serviceType = attributes.get("serviceType").toString();
            // }
            // String operation = "";
            // String params = attributes.get("params").toString();
            // Map<String, String> paramsMap = getParamsMap(params);
            // String format = paramsMap.get("f");
            // if (attributes.get("operation") != null) {
            // operation = attributes.get("operation").toString();
            // }
            switch (ServiceType.valueOf(serviceType)) {
            case CatalogServer:
                resource = new CatalogResource(null, request, response, CatalogService.class,
                        geoServer);
                break;
            case MapServer:
                break;
            case FeatureServer:
                break;
            case GeocodeServer:
                break;
            case GeometryServer:
                break;
            case GPServer:
                break;
            case ImageServer:
                break;
            }

        } catch (Exception e) {
            response.setEntity("NOT IMPLEMENTED", MediaType.TEXT_HTML);
            resource = new Resource(getContext(), request, response);
        }
        return resource;
    }

    private Map<String, String> getParamsMap(String params) {
        Map<String, String> paramsMap = new HashMap<String, String>();
        StringTokenizer tokenizer = new StringTokenizer(params, "&");
        while (tokenizer.hasMoreTokens()) {
            String str = tokenizer.nextToken();
            StringTokenizer tokenizer2 = new StringTokenizer(str, "=");
            while (tokenizer2.hasMoreTokens()) {
                if (tokenizer2.countTokens() == 2) {
                    paramsMap.put(tokenizer2.nextToken(), tokenizer2.nextToken());
                } else {
                    tokenizer2.nextToken();
                }
            }
        }
        return paramsMap;
    }

}
