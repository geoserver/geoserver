/**
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 *
 * @author David Vick, Boundless 2017
 */
package org.geoserver.script.rest.controller;

import com.thoughtworks.xstream.XStream;
import java.util.Collection;
import java.util.List;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.script.rest.model.Script;
import org.geoserver.script.rest.service.ScriptService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = {RestBaseController.ROOT_PATH + "/scripts", "/script/scripts"})
public class FunctionController extends RestBaseController {

    @Autowired ScriptService scriptService;

    @GetMapping(
        path = "/function",
        produces = {
            MediaType.TEXT_HTML_VALUE,
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE
        }
    )
    public RestWrapper<Script> getAppList(HttpServletRequest request) {
        List<Script> scripts = scriptService.getScriptList(request);
        return wrapList(scripts, Script.class);
    }

    @GetMapping(path = "/function/{fileName:.+}")
    public void getAppMain(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String fileName) {
        scriptService.getScript(request, response);
    }

    @PutMapping(path = "/function/{fileName:.+}")
    public void doPut(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String fileName) {
        scriptService.doPut(request, response);
    }

    @DeleteMapping(path = "/function/{fileName:.+}")
    public void doDelete(
            HttpServletRequest request,
            HttpServletResponse response,
            @PathVariable String fileName) {
        scriptService.doDelete(request, response);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("script", Script.class);
        xstream.alias("scripts", Collection.class);
    }
}
