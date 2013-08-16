/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.kml.KMLEncoder;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.restlet.Context;
import org.restlet.Finder;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * REST finder that sets up a {@link GeoSearchLayer} resource based on the resource name
 * 
 */
public class GeoSearchLayerFinder extends Finder {

    private final GeoServer geoserver;
    private KMLEncoder encoder;

    /**
     * @param geoserver
     *            access to {@link Catalog} and {@link GeoServerInfo}
     */
    private GeoSearchLayerFinder(GeoServer geoserver, KMLEncoder encoder) {
        this.geoserver = geoserver;
        this.encoder = encoder;
    }

    /**
     * Looks up a {@link LayerInfo} or {@link LayerGroupInfo} named after the {@code <layer>} in the
     * requested resource {@code <layer>.kml} name
     * 
     * @see org.restlet.Finder#findTarget(org.restlet.data.Request, org.restlet.data.Response)
     */
    @Override
    public Resource findTarget(final Request request, Response response) {
        if (!Method.GET.equals(request.getMethod())) {
            response.setStatus(Status.CLIENT_ERROR_METHOD_NOT_ALLOWED);
            return null;
        }
        final String name = RESTUtils.getAttribute(request, "layer");
        if (name == null) {
            throw new RestletException("No layer name specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        final Catalog catalog = geoserver.getCatalog();
        CatalogInfo layer = catalog.getLayerByName(name);
        MetadataMap mdmap;
        if (layer == null) {
            layer = catalog.getLayerGroupByName(name);
            if (layer == null) {
                throw new RestletException("Layer " + name + " not found",
                        Status.CLIENT_ERROR_NOT_FOUND);
            }
            mdmap = ((LayerGroupInfo) layer).getMetadata();
        } else {
            mdmap = ((LayerInfo) layer).getMetadata();
        }

        Boolean enabled = mdmap.get(Properties.INDEXING_ENABLED, Boolean.class);
        if (enabled == null || !enabled.booleanValue()) {
            throw new RestletException("Layer " + name + " not found",
                    Status.CLIENT_ERROR_NOT_FOUND);
        }
        final Context context = getContext();
        return new GeoSearchLayer(context, request, response, layer, geoserver, encoder);
    }
}
