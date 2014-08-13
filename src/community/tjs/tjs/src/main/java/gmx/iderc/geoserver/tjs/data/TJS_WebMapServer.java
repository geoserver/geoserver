package gmx.iderc.geoserver.tjs.data;

import gmx.iderc.geoserver.tjs.catalog.DatasetInfo;
import org.apache.log4j.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.ows.util.KvpMap;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wms.WMS;
import org.geoserver.wms.featureinfo.GetFeatureInfoKvpReader;
import org.geotools.data.ResourceInfo;
import org.geotools.data.ServiceInfo;
import org.geotools.data.ows.CRSEnvelope;
import org.geotools.data.ows.HTTPResponse;
import org.geotools.data.ows.Layer;
import org.geotools.data.ows.WMSCapabilities;
import org.geotools.data.wms.*;
import org.geotools.data.wms.request.GetFeatureInfoRequest;
import org.geotools.data.wms.request.GetMapRequest;
import org.geotools.data.wms.response.GetFeatureInfoResponse;
import org.geotools.data.wms.response.GetMapResponse;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.ows.ServiceException;
import org.geotools.referencing.CRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: capote
 * Date: 10/10/12
 * Time: 9:40 AM
 * To change this template use File | Settings | File Templates.
 */
public class TJS_WebMapServer extends WebMapServer {

    WebMapServer cascadedWMS;
    HashMap<String, DatasetInfo> datasetsInfo = new HashMap<String, DatasetInfo>();
    HashMap<DatasetInfo, TJSLayer> datasetLayerMap = new HashMap<DatasetInfo, TJSLayer>();
    boolean updating = false;

    public TJS_WebMapServer(URL serverUrl, WebMapServer cascadedWMS, DatasetInfo datasetInfo) throws IOException, ServiceException {
        super(serverUrl);
        this.cascadedWMS = cascadedWMS;
        String layerName = datasetInfo.getFramework().getAssociatedWMS().getName();
        datasetsInfo.put(datasetInfo.getName(), datasetInfo);
        updateCapabilities();
    }

    class TJSHTTPResponse implements HTTPResponse {
        GetFeatureInfoRequest request;
        TJS_WMSLayer layer;

        public TJSHTTPResponse(GetFeatureInfoRequest request, TJS_WMSLayer layer) {
            this.request = request;
            this.layer = layer;
        }

        public void dispose() {
            //To change body of implemented methods use File | Settings | File Templates.
        }

        public String getContentType() {
            return "application/vnd.ogc.gml";
        }

        public String getResponseHeader(String headerName) {
            return null;
        }

        public InputStream getResponseStream() throws IOException {
            org.geoserver.wms.GetFeatureInfoRequest firequest = new org.geoserver.wms.GetFeatureInfoRequest();

            GetFeatureInfoKvpReader reader = (GetFeatureInfoKvpReader) Dispatcher.findKvpRequestReader(firequest.getClass());
            try {
                Properties props = request.getProperties();
                props.setProperty(GetFeatureInfoRequest.QUERY_LAYERS, layer.datasetInfo.getName());

                Map parsedKvp = KvpUtils.normalize(props);
                Map rawKvp = new KvpMap(parsedKvp);
                KvpUtils.parse(parsedKvp);

                firequest = (org.geoserver.wms.GetFeatureInfoRequest) reader.read(firequest, parsedKvp, rawKvp);
                org.geoserver.wms.GetMapRequest getMapReq = firequest.getGetMapRequest();
                ReferencedEnvelope bbox = new ReferencedEnvelope(getMapReq.getBbox(),
                        getMapReq.getCrs());

                if (CRS.getAxisOrder(bbox.getCoordinateReferenceSystem()) != CRS.getAxisOrder(layer.getReader().getCrs())) {
                    bbox = flipEnvelope(bbox, layer.getCoordinateReferenceSystem());
                }

                return layer.getFeatureInfo(bbox, getMapReq.getWidth(), getMapReq.getHeight(), firequest.getXPixel(), firequest.getYPixel(), firequest.getInfoFormat(), firequest.getFeatureCount());
            } catch (Exception ex) {
                return null;
            }
        }
    }

    private ReferencedEnvelope flipEnvelope(ReferencedEnvelope requestEnvelope, CoordinateReferenceSystem targetCRS) {
        double minx = requestEnvelope.getMinX();
        double miny = requestEnvelope.getMinY();
        double maxx = requestEnvelope.getMaxX();
        double maxy = requestEnvelope.getMaxY();
        requestEnvelope = new ReferencedEnvelope(miny, maxy, minx, maxx, targetCRS);
        return requestEnvelope;
    }

    @Override
    public GetFeatureInfoResponse issueRequest(GetFeatureInfoRequest request) throws IOException, ServiceException {
        String layerName = request.getProperties().get(GetMapRequest.LAYERS).toString();
        if (datasetsInfo.containsKey(layerName)) {
            TJS_WMSLayer tjs_wmsLayer = datasetLayerMap.get(datasetsInfo.get(layerName)).layer;
            TJSHTTPResponse response = new TJSHTTPResponse(request, tjs_wmsLayer);
            return new GetFeatureInfoResponse(response);
        } else {
            return super.issueRequest(request);
        }
    }

    @Override
    public GetMapRequest createGetMapRequest() {
        GetMapRequest getMapRequest = super.createGetMapRequest();
        Request owsRequest = ((ThreadLocal<Request>) Dispatcher.REQUEST).get();
        getMapRequest.setProperty("STYLES", owsRequest.getKvp().get("STYLES").toString());
        return getMapRequest;
    }

    @Override
    public GetMapResponse issueRequest(GetMapRequest request) throws IOException, ServiceException {
        GetMapRequest newRequest = translateRequest(request);

        //recolectar los estilos traducidos para borrarlos después
        String styleNames = newRequest.getProperties().getProperty(GetMapRequest.STYLES);

        GetMapResponse response = super.issueRequest(newRequest);

        //hay que borrar el estilo que se crea con los campos
        //traducidos desde la info en el dataSet
        //primero verificar que se haya pasado un estilo
        for (String styleName : styleNames.split(",")) {
            if (styleName != null && !styleName.isEmpty()) {
                StyleInfo toDelete = getCatalog().getStyleByName(styleName);
                getCatalog().remove(toDelete);
            } //si no es que no se paso ningun estilo
        }
        return response;
    }

    private GetMapRequest translateRequest(GetMapRequest request) {

        final URL finalURL = request.getFinalURL();
        String strFinalUrl = finalURL.getProtocol() + "://" +
                finalURL.getHost() +
                (finalURL.getPort() != -1 ? ":" + finalURL.getPort() : "") +
                finalURL.getPath();
        strFinalUrl = strFinalUrl.substring(0, strFinalUrl.lastIndexOf("/"));
        strFinalUrl = strFinalUrl.substring(0, strFinalUrl.lastIndexOf("/"));
        strFinalUrl = strFinalUrl.concat("/ows");

        URL finalUrl;
        try {
            finalUrl = new URL(strFinalUrl);
        } catch (MalformedURLException ex) {
            finalUrl = request.getFinalURL();
        }

        GetMapRequest newRequest = null;
        if (request instanceof WMS1_3_0.GetMapRequest) {
            newRequest = new WMS1_3_0.GetMapRequest(finalUrl);
        } else if (request instanceof WMS1_1_1.GetMapRequest) {
            newRequest = new WMS1_1_0.GetMapRequest(finalUrl);
        } else if (request instanceof WMS1_0_0.GetMapRequest) {
            newRequest = new WMS1_1_0.GetMapRequest(finalUrl);
        }

        Properties reqProperties = request.getProperties();
        String layers = reqProperties.getProperty(GetMapRequest.LAYERS);
        String[] layersArray = layers.split(",");

        Map orgParams = ((ThreadLocal<org.geoserver.ows.Request>) Dispatcher.REQUEST).get().getKvp();
        String styles = orgParams.get(GetMapRequest.STYLES).toString();

        //String styles = reqProperties.getProperty(GetMapRequest.STYLES);
        String[] stylesArray = styles.split(",");

        for (int index = 0; index < layersArray.length; index++) {
            if (datasetsInfo.containsKey(layersArray[index])) {
                DatasetInfo dataSetInfo = datasetsInfo.get(layersArray[index]);
                TJSLayer tjsLayer = datasetLayerMap.get(dataSetInfo);
                reqProperties.setProperty(GetMapRequest.LAYERS, tjsLayer.getCascadedLayer().getName());
                try {
                    if (stylesArray[index] != null && !stylesArray[index].isEmpty()) {
                        stylesArray[index] = tjsLayer.layer.translateStyle(stylesArray[index]);
                    } else if (dataSetInfo.getDefaultStyle() != null) { //usar el estilo por defecto, Alvaro Javier
                        stylesArray[index] = tjsLayer.layer.translateStyle(dataSetInfo.getDefaultStyle());
                    }
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }

        styles = stylesArray.length > 0 ? stylesArray[0] : "";
        for (int index = 1; index < stylesArray.length; index++) {
            styles = styles.concat("," + stylesArray[index]);
        }

        reqProperties.setProperty(GetMapRequest.STYLES, styles);
        newRequest.setProperties(reqProperties);
        return newRequest;
    }

    WMS getWMS() {
        WMS wms = (WMS) GeoServerExtensions.bean(WMS.class);
        return wms;
    }

    GeoServer getGeoserver() {
        return getWMS().getGeoServer();
    }

    Catalog getCatalog() {
        return getGeoserver().getCatalog();
    }

    private void updateCapabilities() {
        updating = true;
        try {
            WMSCapabilities cascadedCaps = cascadedWMS.getCapabilities();
            capabilities = new WMSCapabilities();
            capabilities.setRequest(cascadedCaps.getRequest());
            capabilities.setService(cascadedCaps.getService());
            capabilities.setUpdateSequence(cascadedCaps.getUpdateSequence());
            capabilities.setVersion(cascadedCaps.getVersion());
            capabilities.setLayer(cascadedCaps.getLayer());
            for (DatasetInfo dataset : datasetsInfo.values()) {
                TJSLayer tjsLayer = datasetLayerMap.get(dataset);
                try {
                    if (tjsLayer == null) {
                        //hay problemas con el prefixedName!, Alvaro Javier Fuentes Suarez
                        //String prefixedName = dataset.getFramework().getAssociatedWMS().getResource().getPrefixedName();
                        String name = dataset.getFramework().getAssociatedWMS().getResource().getName();

                        FeatureTypeInfo ftInfo = dataset.getFramework().getFeatureType();
                        //problemas con prefixedName, Alvaro Javier Fuentes Suarez
                        //Layer cascaded = getCascadedLayerByName(prefixedName);
                        Layer cascaded = getCascadedLayerByName(name);

                        TJS_WMSLayer tjs_wmsLayer = new TJS_WMSLayer(this, cascaded, ftInfo, dataset);
                        tjsLayer = new TJSLayer(tjs_wmsLayer, cascaded);
                        capabilities.getLayer().addChildren(tjsLayer);
                        datasetLayerMap.put(dataset, tjsLayer);
                    } else {
                        capabilities.getLayer().addChildren(tjsLayer);
                    }
                } catch (IOException ex) {
                    Logger.getLogger(TJS_WebMapServer.class).error(ex.getMessage());
                }
            }
        } finally {
            updating = false;
        }
    }

    public void update(WebMapServer cascadedWMS, DatasetInfo datasetInfo) {
        datasetsInfo.put(datasetInfo.getName(), datasetInfo);
        updateCapabilities();
    }

    @Override
    protected ServiceInfo createInfo() {
        return cascadedWMS.getInfo();
    }

    @Override
    protected ResourceInfo createInfo(Layer resource) {
        return cascadedWMS.getInfo(resource);
    }

    @Override
    public WMSCapabilities getCapabilities() {
        if (updating) {
            return cascadedWMS.getCapabilities();
        }
        if (capabilities == null) {
            updateCapabilities();
        }
        return capabilities;
    }

    private Layer getCascadedLayerByName(String name) {
        for (Layer layer : cascadedWMS.getCapabilities().getLayerList()) {
            if (layer.getName() == null) {
                continue;
            }
            if (layer.getName().equalsIgnoreCase(name)) {
                return layer;
            }
        }
        return null;
    }

    public class TJSLayer extends Layer {

        TJS_WMSLayer layer;
        Layer cascadedLayer;

        public TJSLayer(TJS_WMSLayer layer, Layer cascadedLayer) {
            this.layer = layer;
            this.cascadedLayer = cascadedLayer;
        }

        public Layer getCascadedLayer() {
            return cascadedLayer;
        }

        @Override
        public String getName() {
            return layer.datasetInfo.getName();
        }

        @Override
        public String getTitle() {
            return layer.datasetInfo.getName();
        }

        @Override
        public boolean isQueryable() {
            return true;
        }

        @Override
        public synchronized Map<String, CRSEnvelope> getBoundingBoxes() {
            return cascadedLayer.getBoundingBoxes();
        }

        @Override
        public Set<String> getSrs() {
            return cascadedLayer.getSrs();
        }

        @Override
        public List<CRSEnvelope> getLayerBoundingBoxes() {
            return cascadedLayer.getLayerBoundingBoxes();    //To change body of overridden methods use File | Settings | File Templates.
        }


    }


    /*
    public tjsGetCapabilitiesResponse issueRequest(GetCapabilitiesRequest request) throws IOException, ServiceException {
        tjsGetCapabilitiesResponse response = super.issueRequest(request);
        WMSCapabilities capabilities = (WMSCapabilities)response.getCapabilities();
        WMSLayerInfo cascadedLayer = frameworkInfo.getAssociatedWMS();//.getWMSLayer(new NullProgressListener());

        List<DatasetInfo> datasets = frameworkInfo.getCatalog().getDatasetsByFramework(frameworkInfo.getId());
        for(DatasetInfo dataset : datasets){
            TJS_WMSLayer layer = new TJS_WMSLayer(cascadedLayer, dataset);
            capabilities.getLayerList().add(layer);
        }
        return response;
    }
*/

}
