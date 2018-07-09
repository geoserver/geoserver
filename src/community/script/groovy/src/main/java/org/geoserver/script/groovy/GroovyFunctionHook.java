/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.groovy;

import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.function.FunctionHook;

public class GroovyFunctionHook extends FunctionHook {

    public GroovyFunctionHook(ScriptPlugin plugin) {
        super(plugin);
    }

    @Override
    public Object run(Object value, List<Object> args, ScriptEngine engine) throws ScriptException {

        // Convert GeoTools args to GeoScript args
        List<Object> newArgs = new ArrayList<Object>(args.size());
        for (Object arg : args) {
            if (arg instanceof org.locationtech.jts.geom.Geometry) {
                newArgs.add(geoscript.geom.Geometry.wrap((org.locationtech.jts.geom.Geometry) arg));
            } else {
                newArgs.add(arg);
            }
        }

        // Run the function
        Object result = invoke(engine, "run", value, newArgs);

        // Convert GeoScript result to GeoTools result
        if (result instanceof geoscript.geom.Geometry) {
            return ((geoscript.geom.Geometry) result).getG();
        } else {
            return result;
        }
    }
}
