package com.boundlessgeo.gsr.api.map;

import java.util.Arrays;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.geoserver.ows.Dispatcher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.boundlessgeo.gsr.MutableRequestProxy;

/**
 * Simple Generate KML end point. Just redirect to GS KML service
 */
@RestController @RequestMapping(path = "/gsr/services/{workspaceName}/MapServer/generateKml") public class
GenerateKMLController {

    @Autowired
    private Dispatcher dispatcher;

    @GetMapping
    public void generateKml(@RequestParam String layers, @PathVariable String workspaceName, HttpServletRequest request,
        HttpServletResponse response) throws Exception {
        if (StringUtils.isNotEmpty(layers)) {
            String workspacedLayers = Arrays.stream(layers.split(",")).map(l -> workspaceName + ":" + l)
                .collect(Collectors.joining(","));

            MutableRequestProxy modifiedRequest = new MutableRequestProxy(request);

            modifiedRequest.getMutableParams().put("service", new String[] { "wms" });
            modifiedRequest.getMutableParams().put("request", new String[] { "kml" });
            modifiedRequest.getMutableParams().put("workspace", new String[] { workspaceName });
            modifiedRequest.getMutableParams().put("layers", new String[] { workspacedLayers });
            modifiedRequest.getMutableParams()
                .put("format", new String[] { "application/vnd.google-earth.kmz;mode=networklink" });

            dispatcher.handleRequest(modifiedRequest, response);
        }
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public void setDispatcher(Dispatcher dispatcher) {
        this.dispatcher = dispatcher;
    }
}
