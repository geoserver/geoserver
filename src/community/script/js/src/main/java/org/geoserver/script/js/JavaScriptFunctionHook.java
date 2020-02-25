/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.util.List;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoscript.js.GeoObject;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.function.FunctionHook;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

public class JavaScriptFunctionHook extends FunctionHook {

    public JavaScriptFunctionHook(ScriptPlugin plugin) {
        super(plugin);
    }

    @Override
    public Object run(Object object, List<Object> args, ScriptEngine engine)
            throws ScriptException {
        Invocable invocable = (Invocable) engine;
        Object results;
        Object exportsObj = engine.get("exports");
        Scriptable exports = null;
        if (exportsObj instanceof Scriptable) {
            exports = (Scriptable) exportsObj;
        } else {
            throw new ScriptException("Couldn't get exports for function.");
        }
        Scriptable scope = exports.getParentScope();
        Context.enter();
        Object geoObject;
        try {
            geoObject = GeoObject.javaToJS(object, scope);
        } catch (Exception e) {
            // We can only convert to GeoScript objects if a script has already
            // called require('geoscript').
            geoObject = object;
        }
        Object geoArgs;
        try {
            geoArgs = GeoObject.javaToJS(args, scope);
        } catch (Exception e) {
            // As above, scripts must require('geoscript') first for this to
            // work with GeoScript objects.
            geoArgs = args;
        }
        try {
            results = invocable.invokeMethod(exports, "run", geoObject, geoArgs);
            results = GeoObject.jsToJava(results);
        } catch (NoSuchMethodException e) {
            throw new ScriptException(e);
        } finally {
            Context.exit();
        }
        return results;
    }
}
