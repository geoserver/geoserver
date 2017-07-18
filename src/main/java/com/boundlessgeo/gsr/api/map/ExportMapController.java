package com.boundlessgeo.gsr.api.map;

import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.Dispatcher;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;

import com.boundlessgeo.gsr.MutableRequestProxy;
import com.boundlessgeo.gsr.api.AbstractGSRController;
import com.boundlessgeo.gsr.core.map.LayersAndTables;

/**
 * Handles ArcGIS ExportMap requests
 */
@RestController
@RequestMapping(path = "/gsr/services/{workspaceName}/MapServer/export")
public class ExportMapController extends AbstractGSRController {

    @Autowired
    public ExportMapController(GeoServer geoServer) {
        super(geoServer);
    }

    @GetMapping(produces = "application/json")
    @ResponseBody
    public ExportMap exportMap(@PathVariable String workspaceName, HttpServletRequest request) {
        String requestURL = request.getRequestURL().toString();
        String requestParameters = request.getQueryString();
        String updatedRequestParameters = requestParameters.replaceAll("\\bf=[^&]+", "f=image&format=png");

        return new ExportMap(requestURL + updatedRequestParameters);
    }

    @GetMapping
    public void exportMap(@PathVariable String workspaceName, HttpServletRequest request, HttpServletResponse response) throws Exception {
        this.exportMapImage(workspaceName, request, response);
    }

    private void exportMapImage(String workspaceName, HttpServletRequest request, HttpServletResponse response) throws Exception {

        Dispatcher dispatcher = GeoServerExtensions.bean(Dispatcher.class);
        MutableRequestProxy requestProxy = new MutableRequestProxy(request);
        requestProxy.getMutableParams().put("service", new String[]{"WMS"});
        requestProxy.getMutableParams().put("request", new String[]{"GetMap"});

        requestProxy.getMutableParams().put("layers", translateLayersParam(request.getParameter("layers"), workspaceName));
        requestProxy.getMutableParams().put("format", translateImageFormatParam(request.getParameter("format")));
        requestProxy.getMutableParams().put("crs", translateImageSRParam(request.getParameter("imageSR")));

        String sizeParam = request.getParameter("size");
        String[] sizeParts = sizeParam.split(",");
        requestProxy.getMutableParams().put("width", new String[]{sizeParts[0]});
        requestProxy.getMutableParams().put("height", new String[]{sizeParts[1]});
        dispatcher.handleRequest(requestProxy, response);
    }

    /**
     * Translate the ArcGis image SR parameter into CRS compatible parameter. Only supports EPSG for now.
     * @param imageSR the image SR parameter
     * @return an EPSG code matching that parameter
     */
    private String[] translateImageSRParam(String imageSR) {
        if (StringUtils.isNotEmpty(imageSR)) {
            return new String[]{"EPSG:" + imageSR};
        } else {
            return null;
        }
    }

    private String[] translateImageFormatParam(String format) {
        String jpgFixedFormat = format.replaceAll("jpg", "jpeg");
        return new String[]{"image/" + jpgFixedFormat};
    }

    private String[] translateLayersParam(String layers, String workspaceName) {
        String[] layersSpecAndLayers = layers.split(":");
        if (layersSpecAndLayers.length < 2) {
            throw new IllegalArgumentException(
                "Malformed layers spec. " + "Expected [show | hide | include | exclude]:layerId1,layerId2");
        }

        String layerSpec = layersSpecAndLayers[0];
        if (!"show".equals(layerSpec)) {
            throw new UnsupportedOperationException("Only SHOW layer spec is currently supported");
        }

        //We look up each layer individually to get its name. This has the potential to be slow in non FS based catalogs
        return Arrays.stream(layersSpecAndLayers[1].split(","))
            .map(layerName -> LayersAndTables.integerIdToGeoserverLayerName(catalog, layerName, workspaceName))
            .toArray(String[]::new);
    }

}
