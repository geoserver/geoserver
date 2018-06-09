/**
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 *
 * @author David Vick, Boundless 2017
 */
package org.geoserver.script.rest.controller;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.rest.RestBaseController;
import org.geoserver.script.rest.service.AppService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = {RestBaseController.ROOT_PATH, "/script"})
public class AppController extends RestBaseController {

    @Autowired AppService appService;

    @GetMapping(
        path = "/apps",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public ResponseEntity<?> getAppList(HttpServletRequest request) {
        return appService.getAppList();
    }

    @GetMapping(path = "/apps/{appName}/{fileName:.+}")
    public void getAppMain(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String appName,
            @PathVariable String fileName) {
        appService.getApp(request, response, appName);
    }
}
