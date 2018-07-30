package com.boundlessgeo.gsr.api.map;

import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.boundlessgeo.gsr.MutableRequestProxy;
import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.model.map.LayersAndTables;

/**
 * Handles ArcGIS ExportMap requests
 */
@RestController public class ExportMapController extends AbstractGSRController {

    @Autowired
    public ExportMapController(GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(produces = "application/json", path = "/gsr/services/{workspaceName}/MapServer/export")
    @ResponseBody
    public ExportMap exportMap(@PathVariable String workspaceName, HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        String requestParameters = request.getQueryString();
        String updatedRequestParameters = requestParameters.replaceAll("\\bf=[^&]+", "f=image&format=png");

        return new ExportMap(requestURL + updatedRequestParameters);
    }

    @GetMapping(path = "/gsr/services/{workspaceName}/MapServer/export")
    public void exportMap(@PathVariable String workspaceName, HttpServletRequest request, HttpServletResponse response)
        throws Exception {
        this.exportMapImage(workspaceName, request, response);
    }

    @GetMapping(path = "/gsr/services/{workspaceName}/MapServer/{layerId}/export")
    public void exportMapOfLayer(@PathVariable String workspaceName, @PathVariable String layerId,
        HttpServletRequest request, HttpServletResponse response) throws Exception {
        this.exportMapImageForLayers(workspaceName, request, response, "show:" + layerId);
    }

    /**
     * All of the capitalized fallbacks you see in this are for OpenLayers compat, which feels the need to capitalize
     * EVERY REQUEST PARAMETER. Read RFC 3986 yall query params is case sensitive.
     *
     * @param workspaceName the workspace name
     * @param request       the request
     * @param response      the response
     * @throws Exception when an exception occurs
     */
    private void exportMapImage(String workspaceName, HttpServletRequest request, HttpServletResponse response)
        throws Exception {

        String layers = getAllLayersInEsriFormat(workspaceName);
        exportMapImageForLayers(workspaceName, request, response, layers);
    }

    private void exportMapImageForLayers(String workspaceName, HttpServletRequest request, HttpServletResponse response,
        String layers) throws Exception {

        Map<String, String[]> parameterMap = request.getParameterMap();
        Dispatcher dispatcher = GeoServerExtensions.bean(Dispatcher.class);
        MutableRequestProxy requestProxy = new MutableRequestProxy(request);
        requestProxy.getMutableParams().put("service", new String[] { "WMS" });
        requestProxy.getMutableParams().put("request", new String[] { "GetMap" });

        requestProxy.getMutableParams().put("layers", new String[] { translateLayersParam(layers, workspaceName) });

        String format = parameterMap.getOrDefault("format", parameterMap.get("FORMAT"))[0];
        requestProxy.getMutableParams().put("format", translateImageFormatParam(format));

        String imageSR = parameterMap
            .getOrDefault("imageSR", parameterMap.getOrDefault("IMAGESR", new String[] { "" }))[0];
        requestProxy.getMutableParams().put("crs", translateImageSRParam(imageSR));

        String sizeParam = parameterMap.getOrDefault("size", parameterMap.get("SIZE"))[0];
        String[] sizeParts = sizeParam.split(",");
        requestProxy.getMutableParams().put("width", new String[] { sizeParts[0] });
        requestProxy.getMutableParams().put("height", new String[] { sizeParts[1] });

        //get rid of DPI, since ESRI will use non-integer DPI
        String dpi = translateDPIProperty(request.getParameter("dpi"));
        if (dpi != null) {
            requestProxy.getMutableParams().put("dpi", new String[] { dpi });
        }

        dispatcher.handleRequest(requestProxy, response);
    }

    private String translateDPIProperty(String dpi) {

        if (StringUtils.isNotEmpty(dpi)) {
            String clampedDPI;
            try {
                Double doubleDpi = Double.parseDouble(dpi);
                clampedDPI = Integer.toString(doubleDpi.intValue());
                return clampedDPI;
            } catch (NumberFormatException e) {
                //suppress
                return null;
            }
        } else {
            return null;
        }
    }

    /**
     * Translate the ArcGis image SR parameter into CRS compatible parameter. Only supports EPSG for now.
     *
     * @param imageSR the image SR parameter
     * @return an EPSG code matching that parameter
     */
    private String[] translateImageSRParam(String imageSR) {
        if (StringUtils.isNotEmpty(imageSR)) {
            return new String[] { "EPSG:" + imageSR };
        } else {
            return null;
        }
    }

    private String[] translateImageFormatParam(String format) {
        String formatFixed = format.toLowerCase();
        formatFixed = formatFixed.replaceAll("jpg", "jpeg");
        formatFixed = formatFixed.replaceAll("png32", "png");
        formatFixed = formatFixed.replaceAll("png24", "png");
        return new String[] { "image/" + formatFixed };
    }

    private String translateLayersParam(String layers, String workspaceName) {
        if (StringUtils.isNotEmpty(layers)) {
            String[] layersSpecAndLayers = layers.split(":");
            if (layersSpecAndLayers.length < 2) {
                throw new IllegalArgumentException(
                    "Malformed layers spec. " + "Expected [show | hide | include | exclude]:layerId1,layerId2");
            }

            String layerSpec = layersSpecAndLayers[0];
            if (!"show".equals(layerSpec)) {
                throw new UnsupportedOperationException("Only SHOW layer spec is currently supported");
            }

            //We look up each layer individually to get its name. This has the potential to be slow in non FS based
            // catalogs
            return Arrays.stream(layersSpecAndLayers[1].split(","))
                .map(layerName -> LayersAndTables.integerIdToGeoserverLayerName(catalog, layerName, workspaceName))
                .collect(Collectors.joining(","));
        } else {
            return null;
        }
    }

    private String getAllLayersInEsriFormat(String workspaceName) {
        return "show:" + catalog.getLayers().stream()
            .filter(li -> li.getResource().getStore().getWorkspace().getName().equals(workspaceName))
            .map(LayerInfo::getName)
            .collect(Collectors.joining(","));
    }

}
