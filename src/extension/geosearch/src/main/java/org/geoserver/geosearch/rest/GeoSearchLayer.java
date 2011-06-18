/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMap;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContext;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geoserver.wms.map.XMLTransformerMap;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.springframework.util.Assert;

/**
 * @author groldan
 * @see LayerKMLDocumentFormat
 */
public class GeoSearchLayer extends AbstractResource {

    private static final Logger LOGGER = Logging.getLogger(GeoSearchLayer.class);

    private final CatalogInfo layer;

    private final GeoServer geoserver;

    public GeoSearchLayer(final Context context, final Request request, final Response response,
            final CatalogInfo layer, GeoServer geoserver) {
        super(context, request, response);
        this.layer = layer;
        this.geoserver = geoserver;
    }

    @Override
    public void handleGet() {
        final WMS wms = new WMS(geoserver) {
            /**
             * Override to KMLMetadataDocumentMapOutputFormat does not need to be in the spring
             * context and hence available to the whole geoserver
             * 
             * @see org.geoserver.wms.WMS#getMapOutputFormat(java.lang.String)
             */
            @Override
            public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
                if (KMLMetadataDocumentMapOutputFormat.MIME_TYPE.equals(mimeType)) {
                    return new KMLMetadataDocumentMapOutputFormat(this);
                }
                return super.getMapOutputFormat(mimeType);
            }
        };

        GetMapKvpRequestReader reader = new GetMapKvpRequestReader(wms);
        reader.setHttpRequest(RESTUtils.getServletRequest(getRequest()));

        GetMapRequest getMapRequest = getRequest(layer);
        final String title;
        final String description;
        String[] keywords = {};
        if (layer instanceof LayerInfo) {
            LayerInfo layerInfo = (LayerInfo) layer;
            title = layerInfo.getResource().getTitle();
            description = layerInfo.getResource().getAbstract();
            List<String> kws = layerInfo.getResource().getKeywords();
            if (kws != null) {
                keywords = kws.toArray(new String[kws.size()]);
            }
        } else {
            LayerGroupInfo lgi = (LayerGroupInfo) layer;
            title = getTitle(lgi);
            description = getDescription(lgi);
            keywords = getKeyWords(lgi);
        }

        WMSMapContext context = new WMSMapContext(getMapRequest);
        context.setTitle(title);
        context.setAbstract(description);
        context.setKeywords(keywords);
        context.setMapWidth(getMapRequest.getWidth());
        context.setMapHeight(getMapRequest.getHeight());
        context.setAreaOfInterest((ReferencedEnvelope) getMapRequest.getBbox());

        WebMap webMap;
        try {
            webMap = new GetMap(wms).run(getMapRequest, context);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RestletException(e.getMessage(), Status.SERVER_ERROR_INTERNAL, e);
        }
        // final WebMap webMap = wmsService.getMap(getMapRequest);
        Assert.isTrue(webMap instanceof XMLTransformerMap);

        DataFormat format = getFormatGet();
        Representation representation = format.toRepresentation(webMap);
        Response response = getResponse();
        response.setEntity(representation);
    }

    /**
     * @see org.geoserver.rest.AbstractResource#createSupportedFormats(org.restlet.data.Request,
     *      org.restlet.data.Response)
     */
    @Override
    protected List<DataFormat> createSupportedFormats(final Request request, final Response response) {
        final List<DataFormat> siteMapFormats = new ArrayList<DataFormat>(1);
        siteMapFormats.add(new LayerKMLDocumentFormat());
        return siteMapFormats;
    }

    @SuppressWarnings("unchecked")
    private GetMapRequest getRequest(CatalogInfo info) {
        GetMapRequest request = new GetMapRequest();
        request.setVersion("1.1.1");
        Catalog catalog = geoserver.getCatalog();
        List<MapLayerInfo> layers = expandLayers(catalog, info);
        request.setLayers(layers);
        request.setFormat(KMLMetadataDocumentMapOutputFormat.MIME_TYPE);
        request.setBbox(getLatLonBbox(info));
        request.setSRS("EPSG:4326");

        // proxy aware base url?
        String baseurl = getRequest().getRootRef().getParentRef().toString();
        request.setBaseUrl(baseurl);
        request.setRawKvp(new KvpMap());
        int maxFeatures = 1000;
        if (info instanceof LayerGroupInfo) {
            maxFeatures = maxFeatures / ((LayerGroupInfo) info).getLayers().size();
        }
        request.setMaxFeatures(maxFeatures);

        try {
            DefaultWebMapService.autoSetMissingProperties(request);
        } catch (Exception e) {
            throw new RestletException(
                    "Could not set figure out automatically a good preview link for " + info,
                    Status.SERVER_ERROR_INTERNAL, e);
        }
        return request;
    }

    private ReferencedEnvelope getLatLonBbox(CatalogInfo info) {
        if (info instanceof LayerInfo) {
            return ((LayerInfo) info).getResource().getLatLonBoundingBox();
        }
        LayerGroupInfo lgi = (LayerGroupInfo) info;
        ReferencedEnvelope latLonBoundingBox;
        latLonBoundingBox = lgi.getBounds();
        try {
            latLonBoundingBox = latLonBoundingBox.transform(DefaultGeographicCRS.WGS84, true);
        } catch (Exception e) {
            LOGGER.log(Level.INFO, "Error transforming " + lgi.getName()
                    + " LayerGroup's bounds to EPSG:4326", e);
            latLonBoundingBox = new ReferencedEnvelope(DefaultGeographicCRS.WGS84);
            try {
                for (LayerInfo li : lgi.getLayers()) {
                    ReferencedEnvelope llbb = li.getResource().getLatLonBoundingBox();
                    latLonBoundingBox.expandToInclude(llbb.transform(DefaultGeographicCRS.WGS84,
                            true));
                }
            } catch (Exception e2) {
                throw new RuntimeException(e2);
            }
        }
        return latLonBoundingBox;
    }

    /**
     * Expands the specified name into a list of layer info names
     */
    private List<MapLayerInfo> expandLayers(Catalog catalog, CatalogInfo info) {
        List<MapLayerInfo> layers = new ArrayList<MapLayerInfo>();

        if (info instanceof LayerInfo) {
            layers.add(new MapLayerInfo((LayerInfo) info));
        } else {
            for (LayerInfo l : ((LayerGroupInfo) info).getLayers()) {
                layers.add(new MapLayerInfo(l));
            }
        }
        return layers;
    }

    private String[] getKeyWords(LayerGroupInfo lgi) {
        Set<String> kws = new TreeSet<String>();
        for (LayerInfo li : lgi.getLayers()) {
            List<String> keywords = li.getResource().getKeywords();
            if (keywords != null) {
                kws.addAll(keywords);
            }
        }
        return kws.toArray(new String[kws.size()]);
    }

    private String getDescription(LayerGroupInfo lgi) {
        StringBuilder desc = new StringBuilder();
        for (LayerInfo li : lgi.getLayers()) {
            desc.append(li.getResource().getPrefixedName());
            desc.append(": ");
            String lidesc = li.getResource().getAbstract();
            if (lidesc != null) {
                desc.append(lidesc);
            }
            desc.append("\n");
        }
        return desc.toString();
    }

    private String getTitle(LayerGroupInfo lgi) {
        StringBuilder title = new StringBuilder("Layer Group composed of: ");
        for (LayerInfo li : lgi.getLayers()) {
            title.append("\"").append(li.getResource().getTitle()).append("\"  ");
        }
        return title.toString();
    }

}
