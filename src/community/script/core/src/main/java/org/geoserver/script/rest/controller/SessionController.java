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
import org.geoserver.script.rest.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {RestBaseController.ROOT_PATH, "/script"})
public class SessionController extends RestBaseController {

    @Autowired SessionService sessionService;

    /** Get a list of Scripting Sessions */
    @GetMapping(
        path = {"/sessions", "/sessions/{language}"},
        produces = {MediaType.APPLICATION_JSON_VALUE, MediaType.TEXT_PLAIN_VALUE}
    )
    public void getList(
            HttpServletResponse response, @PathVariable(required = false) String language) {
        sessionService.getSessions(response, language);
    }

    /**
     * Get a scripting session
     *
     * @param language = ext in old restlet code
     * @param id = session in old restlet code
     */
    @GetMapping(path = "/sessions/{language}/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    public void getScriptSession(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String language,
            @PathVariable int id) {
        sessionService.getScriptSession(request, response, language, id);
    }

    /**
     * Create a scripting session
     *
     * @param language = ext in old restlet code
     */
    @PostMapping(path = "/sessions/{language}", produces = MediaType.TEXT_PLAIN_VALUE)
    public void postScriptSession(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String language) {
        sessionService.createScriptingSession(request, response, language);
    }

    /**
     * Run a scripting session
     *
     * @param language = ext in old restlet code
     * @param id = session in old restlet code
     */
    @PutMapping(path = "/sessions/{language}/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public void putScriptSession(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String language,
            @PathVariable int id) {
        sessionService.executeScript(request, response, language, id);
    }
}
