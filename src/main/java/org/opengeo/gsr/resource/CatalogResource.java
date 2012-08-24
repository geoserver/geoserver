package org.opengeo.gsr.resource;

import java.util.ArrayList;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.rest.AbstractCatalogResource;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.format.DataFormat;
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
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
public class CatalogResource extends GeoServicesResource {

    /**
     * logger
     */

    protected Class clazz;

    private String formatValue;

    private String callback;

    public CatalogResource(Context context, Request request, Response response, Class clazz,
            GeoServer geoServer) {
        super(context, request, response, clazz, geoServer);
        this.formatValue = getAttribute("format");
        this.callback = getRequest().getResourceRef().getQueryAsForm().getFirstValue("callback");
    }

    @Override
    protected Object handleObjectGet() throws Exception {
        try {
            if (!formatValue.equals("json")) {
                List<String> details = new ArrayList<String>();
                details.add("Format " + formatValue + " is not supported");
                return new ServiceException(new ServiceError(
                        (String.valueOf(Status.CLIENT_ERROR_BAD_REQUEST.getCode())),
                        "Output format not supported", details));
            }
            List<AbstractService> services = new ArrayList<AbstractService>();
            GeometryService geometryService = new GeometryService("Geometry");
            List<LayerGroupInfo> layerGroupsInfo = catalog.getFacade().getLayerGroups();
            for (LayerGroupInfo layerGroupInfo : layerGroupsInfo) {
                MapService mapService = new MapService(layerGroupInfo.getName());
                services.add(mapService);
            }
            services.add(geometryService);
            List<String> folders = new ArrayList<String>();
            // TODO: List virtual services
            CatalogService catalogService = new CatalogService("services", "1.0", "OpenGeo Suite",
                    "10.1", folders, services);
            // TODO: handle JSONP callback
            return catalogService;
        } catch (Exception e) {
            List<String> details = new ArrayList<String>();
            details.add(e.getMessage());
            return new ServiceException(new ServiceError(
                    (String.valueOf(Status.SERVER_ERROR_INTERNAL.getCode())),
                    "Internal Server Error", details));
        }
    }
}
