/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoserver.script.ScriptHook;
import org.geoserver.script.ScriptPlugin;
import org.geotools.data.Parameter;
import org.geotools.util.SimpleInternationalString;
import org.opengis.util.InternationalString;

/**
 * Handles wps / process requests.
 *
 * <p>This class is a bridge between the GeoTools/GeoServer process api and the api for process
 * scripts.
 *
 * <p>All the methods on this interface take a {@link ScriptEngine} instance that is already
 * "loaded" with the current version of the process script.
 *
 * <p>Instances of this class must be thread safe.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class WpsHook extends ScriptHook {

    public WpsHook(ScriptPlugin plugin) {
        super(plugin);
    }

    /**
     * The title of the process.
     *
     * <p>Subclasses may override this method, the default behavior is to look for a defined object
     * named "title" in the script engine and return its string representation.
     */
    public String getTitle(ScriptEngine engine) throws ScriptException {
        return lookup(engine, "title", Object.class, true).toString();
    }

    /**
     * The description of the process.
     *
     * <p>Subclasses may override this method, the default behavior is to look for a defined object
     * named "description" in the script engine and return its string representation. If no such
     * object exists {@link #getTitle(ScriptEngine)} is returned.
     */
    public String getDescription(ScriptEngine engine) throws ScriptException {
        Object d = engine.get("description");
        if (d != null) {
            return d.toString();
        }
        return getTitle(engine);
    }

    /**
     * The version of the process.
     *
     * <p>Subclasses should override this method, the default behavior is simply to return "1.0.0".
     */
    public String getVersion(ScriptEngine engine) throws ScriptException {
        return "1.0.0";
    }

    /**
     * The process inputs.
     *
     * <p>Subclasses may override this method, the default behavior is to look for a defined object
     * named "inputs" in the script engine and assume it is a map of the following structure:
     *
     * <pre>
     * {
     *  'arg1': {
     *    'title', ...
     *    'type': ...,
     *  },
     *  'arg2: {
     *    'title', ...
     *    'type': ...,
     *  }
     * }
     * </pre>
     */
    public Map<String, Parameter<?>> getInputs(ScriptEngine engine) throws ScriptException {
        return params(lookup(engine, "inputs", Map.class, true));
    }

    /**
     * The process outputs.
     *
     * <p>Subclasses may override this method, the default behavior is to look for a defined object
     * named "outputs" in the script engine and assume it is a map of the following structure:
     *
     * <pre>
     * {
     *  'result1': {
     *    'title', ...
     *    'type': ...,
     *  },
     *  'result2: {
     *    'title', ...
     *    'type': ...,
     *  }
     * }
     * </pre>
     */
    public Map<String, Parameter<?>> getOutputs(ScriptEngine engine) throws ScriptException {
        return params(lookup(engine, "outputs", Map.class, true));
    }

    /** Helper method to create parameter map. */
    protected Map<String, Parameter<?>> params(Map map) {
        Map params = new HashMap();
        for (Map.Entry e : (Set<Map.Entry>) map.entrySet()) {
            params.put(e.getKey(), param((String) e.getKey(), e.getValue()));
        }
        return params;
    }

    /**
     * Helper method to morph input into a Parameter instance, will throw exception if can't
     * convert.
     */
    protected Parameter<?> param(String name, Object o) {
        if (o instanceof Parameter) {
            return (Parameter) o;
        }
        if (o instanceof Map) {
            Map m = (Map) o;

            InternationalString title = null, desc = null;
            boolean required = true;
            int min = 1, max = 1;
            Object sample = null;

            if (m.containsKey("title")) {
                title = new SimpleInternationalString((String) m.get("title"));
            }
            if (m.containsKey("description")) {
                desc = new SimpleInternationalString((String) m.get("description"));
            } else {
                desc = title;
            }
            if (m.containsKey("required")) {
                required = (Boolean) m.get("required");
            }
            if (m.containsKey("min")) {
                min = (Integer) m.get("min");
            }
            if (m.containsKey("max")) {
                max = (Integer) m.get("max");
            }
            sample = m.get("default");

            return new Parameter(
                    name, (Class) m.get("type"), title, desc, required, min, max, sample, null);
        }
        throw new IllegalArgumentException(
                "Unable to turn " + o + " into " + Parameter.class.getName());
    }

    /** Executes the process. */
    public Map<String, Object> run(Map<String, Object> input, ScriptEngine engine)
            throws ScriptException {

        return (Map<String, Object>) invoke(engine, "run", input);
    }
}
