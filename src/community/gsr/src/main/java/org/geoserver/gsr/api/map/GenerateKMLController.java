package org.geoserver.gsr.api.map;
/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

import java.util.Arrays;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.apache.commons.lang.StringUtils;
import org.geoserver.gsr.MutableRequestProxy;
import org.geoserver.ogcapi.APIService;
import org.geoserver.ows.Dispatcher;
import org.geoserver.wms.WMSInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Simple Generate KML end point. Just redirect to GS KML service */
@APIService(
    service = "MapServer",
    version = "1.0",
    landingPage = "/gsr/services",
    serviceClass = WMSInfo.class
)
@RestController
@RequestMapping(path = "/gsr/services/{workspaceName}/MapServer/generateKml")
public class GenerateKMLController {

    @Autowired private Dispatcher dispatcher;

    @GetMapping
    public void generateKml(
            @RequestParam(name = "layers") String layers,
            @PathVariable(name = "workspaceName") String workspaceName,
            HttpServletRequest request,
            HttpServletResponse response)
            throws Exception {
        if (StringUtils.isNotEmpty(layers)) {
            String workspacedLayers =
                    Arrays.stream(layers.split(","))
                            .map(l -> workspaceName + ":" + l)
                            .collect(Collectors.joining(","));

            MutableRequestProxy modifiedRequest = new MutableRequestProxy(request);

            modifiedRequest.getMutableParams().put("service", new String[] {"wms"});
            modifiedRequest.getMutableParams().put("request", new String[] {"kml"});
            modifiedRequest.getMutableParams().put("workspace", new String[] {workspaceName});
            modifiedRequest.getMutableParams().put("layers", new String[] {workspacedLayers});
            modifiedRequest
                    .getMutableParams()
                    .put(
                            "format",
                            new String[] {"application/vnd.google-earth.kmz;mode=networklink"});

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
