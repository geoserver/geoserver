/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.io.IOException;
import java.io.OutputStream;
import java.util.logging.Logger;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.app.AppHook;
import org.geoserver.script.js.engine.CommonJSEngine;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class JavaScriptAppHook extends AppHook {

    static Logger LOGGER = Logging.getLogger("org.geoserver.script.js");

    OutputStream out;

    public JavaScriptAppHook(ScriptPlugin plugin) {
        super(plugin);
    }

    @Override
    public void run(HttpServletRequest request, HttpServletResponse response, ScriptEngine engine)
            throws ScriptException, IOException {

        Invocable invocable = (Invocable) engine;
        Object exportsObj = engine.get("exports");
        Scriptable exports = null;
        if (exportsObj instanceof Scriptable) {
            exports = (Scriptable) exportsObj;
        } else {
            throw new ScriptException("Couldn't get exports for app.");
        }
        Scriptable scope = exports.getParentScope();
        Context cx = CommonJSEngine.enterContext();
        Scriptable jsgiRequest = null;
        try {
            jsgiRequest = new JsgiRequest(request, response, cx, scope);
        } catch (Exception e) {
            throw new ScriptException(e);
        } finally {
            Context.exit();
        }
        Object appReturn = null;
        try {
            appReturn = invocable.invokeMethod(exports, "app", jsgiRequest);
        } catch (NoSuchMethodException e) {
            throw new ScriptException(e);
        }
        if (!(appReturn instanceof Scriptable)) {
            throw new ScriptException("bad return from JSGI app");
        }
        JsgiResponse jsgiResponse = new JsgiResponse((Scriptable) appReturn, scope);
        try {
            jsgiResponse.commit(response, scope);
        } catch (Exception e) {
            throw new ScriptException("Failed to write response: " + e.getMessage());
        }
    }
}
