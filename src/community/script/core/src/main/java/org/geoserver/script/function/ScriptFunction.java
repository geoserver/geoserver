/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.function;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import javax.script.ScriptEngine;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;
import org.geoserver.script.ScriptFileWatcher;
import org.geoserver.script.ScriptManager;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.VolatileFunction;

/**
 * Implementation of {@link org.opengis.filter.expression.Function} backed by a script.
 *
 * <p>This class does its work by delegating all methods to the {@link FunctionHook} interface. This
 * class maintains a link to the backing script {@link File} and uses a {@link FileWatcher} to
 * detect changes to the underlying script. When changed a new {@link ScriptEngine} is created and
 * the underlying script is reloaded.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ScriptFunction {

    /** the hook for interacting with the script */
    FunctionHook hook;

    /** watcher for changes */
    ScriptFileWatcher watcher;

    public ScriptFunction(Resource file, ScriptManager scriptMgr) {
        watcher = new ScriptFileWatcher(file, scriptMgr);
        hook = scriptMgr.lookupFilterHook(file);
    }

    @Deprecated
    public ScriptFunction(File file, ScriptManager scriptMgr) {
        watcher = new ScriptFileWatcher(file, scriptMgr);
        hook = scriptMgr.lookupFilterHook(file);
    }

    Function instance(Name name, List<Expression> args) {
        return new Function(name, args);
    }

    class Function extends FunctionImpl implements VolatileFunction {

        Function(Name name, List<Expression> params) {
            setName(name.getLocalPart());
            setParameters(params);
            functionName = new FunctionNameImpl(name, params.size());
        }

        @Override
        public Object evaluate(Object object) {
            try {
                // round up the arguments
                List<Object> args = new ArrayList<Object>();
                for (Expression e : getParameters()) {
                    args.add(e.evaluate(object));
                }
                return hook.run(object, args, watcher.readIfModified());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
