/**
 * (c) 2014 Open Source Geospatial Foundation - all rights reserved (c) 2001 - 2013 OpenPlans This
 * code is licensed under the GPL 2.0 license, available at the root application directory.
 *
 * @author David Vick, Boundless 2017
 */
package org.geoserver.script.rest.service;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestException;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.app.AppHook;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
public class AppService {
    static Logger LOGGER = Logging.getLogger(AppService.class);

    @Autowired ScriptManager scriptManager;

    public ResponseEntity<?> getAppList() {
        Resource appRoot = scriptManager.app();
        List<Resource> apps = Resources.list(appRoot, Resources.DirectoryFilter.INSTANCE);

        JSONArray array = new JSONArray();
        for (Resource f : apps) {
            array.add(f.name());
        }

        JSONObject obj = new JSONObject();
        obj.put("scripts", array);

        return new ResponseEntity<String>(obj.toString(2), HttpStatus.OK);
    }

    public void getApp(HttpServletRequest request, HttpServletResponse response, String app) {
        Resource script = null;
        try {
            Resource appDir;
            try {
                appDir = scriptManager.app(app);
            } catch (IllegalStateException e) {
                throw new RestException(
                        format("Error looking up app directory %s", app),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            if (appDir == null) {
                throw new RestException(format("No such app %s", app), HttpStatus.NOT_FOUND);
            }

            // look for main script
            script = scriptManager.findAppMainScript(appDir);
            if (script == null) {
                throw new RestException(
                        format("No main file for app %s", app), HttpStatus.NOT_FOUND);
            }

            ScriptEngine eng = scriptManager.createNewEngine(script);
            if (eng == null) {
                throw new RestException(
                        format("Script engine for %s not found", script.name()),
                        HttpStatus.BAD_REQUEST);
            }

            // look up the app hook
            AppHook hook = scriptManager.lookupAppHook(script);
            if (hook == null) {
                // TODO: fall back on default
                throw new RestException(
                        format("No hook found for %s", script.path()),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }

            Reader in = new BufferedReader(new InputStreamReader(script.in()));
            try {
                eng.eval(in);
                hook.run(request, response, eng);
            } finally {
                in.close();
            }

        } catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            throw new RestException(
                    "Error executing script " + script.name(), HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }
}
