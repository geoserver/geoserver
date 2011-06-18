package org.geoserver.geosearch;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.NoSuchElementException;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.Operation;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.util.RESTUtils;
import org.geoserver.wms.GetMapRequest;
import org.geoserver.wms.WMS;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WebMap;
import org.geoserver.wms.WebMapService;
import org.geoserver.wms.map.GetMapKvpRequestReader;
import org.geoserver.wms.map.XMLTransformerMap;
import org.geoserver.wms.map.XMLTransformerMapResponse;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.util.logging.Logging;
import org.restlet.Restlet;
import org.restlet.data.ClientInfo;
import org.restlet.data.Form;
import org.restlet.data.MediaType;
import org.restlet.data.Method;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.OutputRepresentation;
import org.springframework.util.Assert;

public class FeatureRestlet extends Restlet {
    private static Logger LOGGER = Logging.getLogger("org.geoserver.geosearch");

    private WMSInfo myWMSInfo;
    private Catalog catalog;
    private GeoServer geoserver;
    private WMS wms;
    private WebMapService webMapService;

    public void setWmsInfo(WMSInfo wms){
        myWMSInfo = wms;
    }

    public WMSInfo getWmsInfo(){
        return myWMSInfo;
    }

    public GeoServer getGeoServer() {
        return geoserver;
    }

    public WMS getWMS() {
        return wms;
    }

    public void setWMS(WMS wms) {
        this.wms = wms;
    }
    
    public void setGeoServer(GeoServer geoserver) {
        this.geoserver = geoserver;
    }

    public void setCatalog(Catalog catalog) {
        this.catalog = catalog;
    }

    public void setWebMapService(WebMapService webMapService) {
        this.webMapService = webMapService;
    }

    public FeatureRestlet() {
    }

    public void handle(Request request, Response response){

        if (request.getMethod().equals(Method.GET)){
            try {
                doGet(request, response);
            } 
            catch (Exception e) {
                throw new RuntimeException( e );
            }
        } else {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST);
        }

    }

    public void doGet(Request request, Response response) throws Exception{
        String layer = (String) request.getAttributes().get("layer");
        String namespace = (String) request.getAttributes().get("namespace");
        String feature = (String) request.getAttributes().get("feature");

        Form form = request.getResourceRef().getQueryAsForm();
        String reqRaw = form.getFirstValue("raw");
        
        ClientInfo cliInfo = request.getClientInfo();
        String agent = null;
        if(cliInfo != null) {
            agent = cliInfo.getAgent();
        }
        
        boolean wantsRaw = (
                agent == null
                || cliInfo.getAgent().contains("Googlebot")
                || (reqRaw != null && Boolean.parseBoolean(reqRaw)) );
        
        // We only show the actual KML placemark to googlebot or users who append ?raw=true 
        if ( ! wantsRaw ) {
            response.redirectSeeOther(feature+".html");
        } else {
            int startIndex = 0;
            int maxFeatures = 100;
            String regionateBy = null;
            String regionateAttr = null;

            try {
                startIndex = Integer.valueOf(form.getFirstValue("startindex",
                        true));
            } catch (Exception e) {
            }

            try {
                maxFeatures = Integer.valueOf(form.getFirstValue("maxfeatures",
                        true));
            } catch (Exception e) {
            }

            regionateBy = form.getFirstValue("regionateBy", true);
            // if (regionateBy == null) regionateBy = "sld";
            regionateBy = "random";

            regionateAttr = form.getFirstValue("regionateAttr", true);

            NamespaceInfo ns = catalog.getNamespaceByPrefix(namespace);
            if (ns == null) {
                throw new RestletException("No such namespace:" + namespace,
                        Status.CLIENT_ERROR_NOT_FOUND);
            }

            FeatureTypeInfo featureType = null;
            try {
                featureType = catalog.getFeatureTypeByName(ns, layer);
            } catch (NoSuchElementException e) {
                // ignore, handled later
            }

            if (featureType == null) {
                throw new RestletException("No such layer:" + layer,
                        Status.CLIENT_ERROR_NOT_FOUND);
            }

            if (!(Boolean)featureType.getMetadata().get("indexingEnabled")) {
                throw new RestletException("Layer not indexable: " + layer,
                        Status.CLIENT_ERROR_FORBIDDEN);
            }

            // create some kvp and pass through to GetMapKvpreader
            KvpMap raw = new KvpMap();
            raw.put("layers", namespace + ":" + layer);
            raw.put("format", "kml");
            raw.put("format_options", "selfLinks:true;relLinks:true;");
            // regionateby:" + regionateBy + (regionateAttr != null ? ";regionateAttr:" + regionateAttr : ""));

            if (feature != null) {
                raw.put("featureid", feature);
            } else {
                raw.put("startIndex", Integer.toString(startIndex));
                raw.put("maxfeatures", Integer.toString(maxFeatures));
            }

            GetMapKvpRequestReader reader = new GetMapKvpRequestReader(getWMS());
            reader.setHttpRequest(RESTUtils.getServletRequest(request));

            // parse into request object
            raw = KvpUtils.normalize(raw);
            KvpMap kvp = new KvpMap(raw);
            KvpUtils.parse(kvp);
            final GetMapRequest getMapRequest = (GetMapRequest) reader.read(
                    (GetMapRequest) reader.createRequest(), kvp, raw);
            getMapRequest.setBaseUrl(RESTUtils.getBaseURL(request));

            // delegate to wms reflector
            final WebMap webMap = webMapService.reflect(getMapRequest);
            //as per KMLMapOutputFormat.produceMap
            Assert.isInstanceOf(XMLTransformerMap.class, webMap);
            final XMLTransformerMapResponse respEncoder = new XMLTransformerMapResponse();
            
            // wrap response in a reslet output rep
            OutputRepresentation output = new OutputRepresentation(
                    new MediaType("application/vnd.google-earth.kml+xml")) {
                public void write(OutputStream outputStream) throws IOException {
                    try {
                        respEncoder.write(webMap, outputStream);
                    } catch (IOException ioe) {
                        throw ioe;
                    } catch (Exception e) {
                        PrintStream printStream = new PrintStream(outputStream);
                        printStream.println("Unable to index feature due to: "
                                + e.getMessage());
                        LOGGER.log(Level.WARNING, "Failure to index features.",
                                e);
                    }
                }
            };
            response.setEntity(output);

        }
    }
}

