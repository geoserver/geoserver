/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.app;

import java.io.IOException;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.script.ScriptHook;
import org.geoserver.script.ScriptPlugin;

/**
 * Hook for "app" requests.
 *
 * <p>This class is responsible for adapting a raw http request into something that can be handled
 * by an app script. For example, a python extension could transform the request/response into a
 * WSGI request/response to be handled by the underlying app script.
 *
 * <p>Instances of this class must be thread safe.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AppHook extends ScriptHook {

    public AppHook(ScriptPlugin plugin) {
        super(plugin);
    }

    /**
     * Handles a request.
     *
     * @param request The http request.
     * @param response The http response.
     * @param engine The script engine of the appropriate type.
     */
    public void run(HttpServletRequest request, HttpServletResponse response, ScriptEngine engine)
            throws ScriptException, IOException {
        invoke(engine, "run", request, response);
    }
}
