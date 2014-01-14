/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geosearch.rest;

import java.io.IOException;
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
import org.geoserver.config.GeoServerInfo;
import org.geoserver.kml.KMLEncoder;
import org.geoserver.kml.KMZMapOutputFormat;
import org.geoserver.kml.KmlEncodingContext;
import org.geoserver.kml.builder.SimpleNetworkLinkBuilder;
import org.geoserver.kml.sequence.PlainFolderSequenceFactory;
import org.geoserver.kml.sequence.SequenceFactory;
import org.geoserver.kml.sequence.SequenceList;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.platform.ServiceException;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.wms.DefaultWebMapService;
import org.geoserver.wms.GetMap;
import org.geoserver.wms.GetMapOutputFormat;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.MapLayerInfo;
import org.geoserver.wms.MapProducerCapabilities;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSMapContent;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.map.Layer;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.logging.Logging;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;

import de.micromata.opengis.kml.v_2_2_0.Document;
import de.micromata.opengis.kml.v_2_2_0.Feature;
import de.micromata.opengis.kml.v_2_2_0.Kml;
import de.micromata.opengis.kml.v_2_2_0.atom.Author;

/**
 * @author groldan
 * @see LayerKMLDocumentFormat
 */
public class GeoSearchLayer extends AbstractResource {

    private static final Logger LOGGER = Logging.getLogger(GeoSearchLayer.class);

    private final CatalogInfo layer;

    private final GeoServer geoserver;

    private KMLEncoder encoder;

    public GeoSearchLayer(final Context context, final Request request, final Response response,
            final CatalogInfo layer, GeoServer geoserver, KMLEncoder encoder) {
        super(context, request, response);
        this.layer = layer;
        this.geoserver = geoserver;
        this.encoder = encoder;
    }

    @Override
    public void handleGet() {
        final WMS wms = new WMS(geoserver) {
            @Override
            public GetMapOutputFormat getMapOutputFormat(final String mimeType) {
                return new GetMapSnatcher();
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
            List<String> kws = layerInfo.getResource().keywordValues();
            if (kws != null) {
                keywords = kws.toArray(new String[kws.size()]);
            }
        } else {
            LayerGroupInfo lgi = (LayerGroupInfo) layer;
            title = getTitle(lgi);
            description = getDescription(lgi);
            keywords = getKeyWords(lgi);
        }

        WMSMapContent context = new WMSMapContent(getMapRequest);
        context.setTitle(title);
        context.setAbstract(description);
        context.setKeywords(keywords);
        context.setMapWidth(getMapRequest.getWidth());
        context.setMapHeight(getMapRequest.getHeight());
        context.getViewport().setBounds((ReferencedEnvelope) getMapRequest.getBbox());

        MapContentWebMap webMap;
        try {
            webMap = (MapContentWebMap) new GetMap(wms).run(getMapRequest, context);
        } catch (Exception e) {
            throw new RestletException(e.getMessage(), Status.SERVER_ERROR_INTERNAL, e);
        }
        
        KmlEncodingContext encodingContext = new KmlEncodingContext(webMap.getMapContent(), wms, true);
        Kml kml = buildKml(encodingContext);

        DataFormat format = getFormatGet();
        Representation representation = format.toRepresentation(kml);
        Response response = getResponse();
        response.setEntity(representation);
    }

    private Kml buildKml(KmlEncodingContext encodingContext) {
        SimpleNetworkLinkBuilder nlBuilder = new SimpleNetworkLinkBuilder(encodingContext);
        Kml kml = nlBuilder.buildKMLDocument();

        Document doc = (Document) kml.getFeature();
        doc.createAndSetAtomAuthor();
        doc.setOpen(true);
        GeoServerInfo gsInfo = encodingContext.getWms().getGeoServer().getGlobal();
        String authorName = gsInfo.getSettings().getContact().getContactPerson();
        Author author = doc.createAndSetAtomAuthor();
        author.addToNameOrUriOrEmail(authorName);
        doc.createAndSetAtomLink(gsInfo.getSettings().getOnlineResource());
        
        WMSMapContent mapContent = encodingContext.getMapContent();
        doc.setDescription(buildDescription(mapContent));

        // see if we have to include sample data
        List<Layer> layers = mapContent.layers();
        boolean includeSampleData = false;
        for (int i = 0; i < layers.size(); i++) {
            // layer and info
            MapLayerInfo layerInfo = mapContent.getRequest().getLayers().get(i);
            final int type = layerInfo.getType();
            if (MapLayerInfo.TYPE_VECTOR == type || MapLayerInfo.TYPE_REMOTE_VECTOR == type) {
                includeSampleData = true;
            }
        }
        if(includeSampleData) {
            SequenceFactory<Feature> generatorFactory = new PlainFolderSequenceFactory(encodingContext);
            SequenceList<Feature> folders = new SequenceList<Feature>(generatorFactory);
            encodingContext.addFeatures(doc, folders);
        }
        
        return kml;
    }
    
    private String buildDescription(WMSMapContent mapContent) {
        StringBuilder sb = new StringBuilder();
        if (null != mapContent.getAbstract()) {
            sb.append(mapContent.getAbstract());
        }
        if (null != mapContent.getKeywords()) {
            sb.append("\n");
            for (String kw : mapContent.getKeywords()) {
                if (null != kw) {
                    sb.append(kw).append(" ");
                }
            }
        }
        return sb.toString();
    }

    /**
     * @see org.geoserver.rest.AbstractResource#createSupportedFormats(org.restlet.data.Request,
     *      org.restlet.data.Response)
     */
    @Override
    protected List<DataFormat> createSupportedFormats(final Request request, final Response response) {
        final List<DataFormat> siteMapFormats = new ArrayList<DataFormat>(1);
        siteMapFormats.add(new LayerKMLDocumentFormat(encoder));
        return siteMapFormats;
    }

    @SuppressWarnings("unchecked")
    private GetMapRequest getRequest(CatalogInfo info) {
        GetMapRequest request = new GetMapRequest();
        request.setVersion("1.1.1");
        Catalog catalog = geoserver.getCatalog();
        List<MapLayerInfo> layers = expandLayers(catalog, info);
        request.setLayers(layers);
        request.setFormat(KMZMapOutputFormat.MIME_TYPE);
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
                for (LayerInfo li : lgi.layers()) {
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
            for (LayerInfo l : ((LayerGroupInfo) info).layers()) {
                layers.add(new MapLayerInfo(l));
            }
        }
        return layers;
    }

    private String[] getKeyWords(LayerGroupInfo lgi) {
        Set<String> kws = new TreeSet<String>();
        for (LayerInfo li : lgi.layers()) {
            List<String> keywords = li.getResource().keywordValues();
            if (keywords != null) {
                kws.addAll(keywords);
            }
        }
        return kws.toArray(new String[kws.size()]);
    }

    private String getDescription(LayerGroupInfo lgi) {
        StringBuilder desc = new StringBuilder();
        for (LayerInfo li : lgi.layers()) {
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
        for (LayerInfo li : lgi.layers()) {
            title.append("\"").append(li.getResource().getTitle()).append("\"  ");
        }
        return title.toString();
    }
    
    private static class GetMapSnatcher implements GetMapOutputFormat {

        @Override
        public WebMap produceMap(WMSMapContent mapContent) throws ServiceException, IOException {
            return new MapContentWebMap(mapContent);
        }

        @Override
        public Set<String> getOutputFormatNames() {
            return null;
        }

        @Override
        public String getMimeType() {
            return null;
        }

        @Override
        public MapProducerCapabilities getCapabilities(String format) {
            return new MapProducerCapabilities(false, false, false, false, "fake/mime");
        }
        
    }
    
    private static class MapContentWebMap extends WebMap {

        private WMSMapContent content;

        public MapContentWebMap(WMSMapContent context) {
            super(context);
            this.content = context;
        }
        
        public WMSMapContent getMapContent() {
            return content;
        }
        
    }

}
