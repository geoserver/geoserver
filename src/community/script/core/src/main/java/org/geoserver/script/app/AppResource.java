/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.app;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.script.ScriptEngine;

import org.geoserver.platform.resource.Files;
import org.geoserver.rest.RestletException;
import org.geoserver.script.ScriptManager;
import org.geotools.util.logging.Logging;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Resource;

/**
 * App resource that handles an app request.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class AppResource extends Resource {

    static Logger LOGGER = Logging.getLogger(AppResource.class);

    ScriptManager scriptMgr;
    org.geoserver.platform.resource.Resource script;

    public AppResource(org.geoserver.platform.resource.Resource script, ScriptManager scriptMgr, Request request, Response response) {
        super(null, request, response);
        this.scriptMgr = scriptMgr;
        this.script = script;
    }

    @Deprecated
    public AppResource(File script, ScriptManager scriptMgr, Request request, Response response) {
        this(Files.asResource(script), scriptMgr, request, response);
    }
    
    @Override
    public void handleGet() {
        try {
            ScriptEngine eng = scriptMgr.createNewEngine(script);
            if (eng == null) {
                throw new RestletException(format("Script engine for %s not found", script.name()), 
                    Status.CLIENT_ERROR_BAD_REQUEST);
            }

            //look up the app hook
            AppHook hook = scriptMgr.lookupAppHook(script);
            if (hook == null) {
                //TODO: fall back on default
                throw new RestletException(format("No hook found for %s", script.path()), 
                    Status.SERVER_ERROR_INTERNAL);
            }

            Reader in = new BufferedReader(new InputStreamReader(script.in()));
            try {
                eng.eval(in);
                hook.run(getRequest(), getResponse(), eng);
            }
            finally {
                in.close();
            }
        } 
        catch (Exception e) {
            LOGGER.log(Level.WARNING, e.getLocalizedMessage(), e);
            throw new RestletException("Error executing script " + script.name(), 
                Status.SERVER_ERROR_INTERNAL, e);
        }
    }
}
;
