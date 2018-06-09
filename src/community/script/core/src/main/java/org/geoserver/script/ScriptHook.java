/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptException;

/**
 * Base class for hooks.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ScriptHook {

    protected ScriptPlugin plugin;

    public ScriptHook(ScriptPlugin plugin) {
        this.plugin = plugin;
    }

    /** The script plugin for the hook. */
    public ScriptPlugin getPlugin() {
        return plugin;
    }

    /**
     * Helper method to look up an object in a script engine, verifying its type and optionally
     * throwing an exception if it doesn't exist.
     */
    protected <T> T lookup(ScriptEngine engine, String name, Class<T> type, boolean mandatory)
            throws ScriptException {
        Object obj = engine.get(name);
        if (obj == null) {
            if (mandatory) {
                throw new ScriptException("No such object: " + name);
            } else {
                return null;
            }
        }

        if (!type.isInstance(obj)) {
            throw new IllegalArgumentException(
                    "Object " + obj + " is not of type " + type.getName());
        }

        return type.cast(obj);
    }

    /** Helper method to invoke a function through a script engine. */
    protected Object invoke(ScriptEngine engine, String name, Object... args)
            throws ScriptException {
        return doInvoke(engine, true, name, args);
    }

    /**
     * Helper method to invoke an optional function through the script engine.
     *
     * <p>If the function does not exist <code>null</code> is returned.
     */
    protected Object invokeOptional(ScriptEngine engine, String name, Object... args)
            throws ScriptException {
        return doInvoke(engine, false, name, args);
    }

    Object doInvoke(ScriptEngine engine, boolean mandatory, String name, Object... args)
            throws ScriptException {
        if (engine instanceof Invocable) {
            try {
                return ((Invocable) engine).invokeFunction(name, args);
            } catch (NoSuchMethodException e) {
                if (mandatory) {
                    throw new ScriptException(e);
                }
                return null;
            }
        } else {
            throw new ScriptException(
                    "Engine does not implement Invocable, plugin must implement"
                            + " custom script hook");
        }
    }
}
