/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoserver.script.wps.WpsHook;
import org.geotools.data.Parameter;
import org.geotools.text.Text;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.python.core.Py;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyTuple;
import org.python.core.PyType;

/**
 * Python wps hook.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class PyWpsHook extends WpsHook {

    static Logger LOGGER = Logging.getLogger(PyWpsHook.class);

    public PyWpsHook(PythonPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getTitle(ScriptEngine engine) throws ScriptException {
        return str(process(engine).__getattr__("title"));
    }

    @Override
    public String getDescription(ScriptEngine engine) throws ScriptException {
        return str(process(engine).__getattr__("description"));
    }

    @Override
    public Map<String, Parameter<?>> getInputs(ScriptEngine engine) throws ScriptException {
        engine.eval("import inspect");
        PyList args =
                (PyList) engine.eval("inspect.getargspec(run.func_closure[0].cell_contents)[0]");
        PyTuple defaults =
                (PyTuple) engine.eval("inspect.getargspec(run.func_closure[0].cell_contents)[3]");
        PyDictionary inputs = (PyDictionary) process(engine).__getattr__("inputs");

        if (args.size() != inputs.size()) {
            throw new RuntimeException(
                    String.format(
                            "process function specified %d arguments but" + " describes %d inputs",
                            args.size(), inputs.size()));
        }

        Map<String, Parameter<?>> map = new LinkedHashMap<String, Parameter<?>>();
        for (int i = 0; i < args.size(); i++) {
            String arg = args.get(i).toString();
            PyTuple input = (PyTuple) inputs.get(arg);
            if (input == null) {
                throw new RuntimeException(
                        String.format(
                                "process function specified argument %s"
                                        + " but does not specify it as an input",
                                arg));
            }
            int min = 1;
            int max = 1;
            Object defaultValue = null;
            Map metadata = null;
            if (input.size() == 3) {
                PyDictionary meta = (PyDictionary) input.get(2);
                min = getParameter(meta, "min", Integer.class, 1);
                max = getParameter(meta, "max", Integer.class, 1);
                List<String> options = getParameter(meta, "domain", List.class, null);
                if (options != null) {
                    metadata = new HashMap();
                    metadata.put(Parameter.OPTIONS, options);
                }
                // map every other key as parameter metadata entry
                HashSet<String> otherKeys = new HashSet<String>(meta.keySet());
                otherKeys.remove("min");
                otherKeys.remove("max");
                otherKeys.remove("domain");
                if (!otherKeys.isEmpty()) {
                    if (metadata == null) {
                        metadata = new HashMap();
                    }
                    for (String key : otherKeys) {
                        metadata.put(key, meta.get(key));
                    }
                }
            }
            if (defaults != null) {
                // from the python guide:
                // defaults is a tuple of default argument values or None if there are no
                // default arguments; if this tuple has n elements,
                // they correspond to the last n elements listed in args.
                int defaultIdx = defaults.size() - (args.size() - i);
                if (defaultIdx >= 0) {
                    defaultValue = defaults.get(defaultIdx);
                }
            }
            Parameter parameter =
                    parameter(
                            arg,
                            input.__getitem__(0),
                            min,
                            max,
                            input.__getitem__(1),
                            defaultValue,
                            metadata);
            map.put(arg, parameter);
        }
        return map;
    }

    private <T> T getParameter(PyDictionary meta, String key, Class<T> targetType, T defaultValue) {
        if (!meta.containsKey(key)) {
            return defaultValue;
        }

        Object value = meta.get(key);
        return (T) Converters.convert(value, targetType);
    }

    @Override
    public Map<String, Parameter<?>> getOutputs(ScriptEngine engine) throws ScriptException {

        PyDictionary outputs = (PyDictionary) process(engine).__getattr__("outputs");
        Map<String, Parameter<?>> map = new TreeMap<String, Parameter<?>>();

        for (String name : (List<String>) outputs.keys()) {
            PyTuple output = (PyTuple) outputs.get(name);

            Object type = output.__getitem__(0);
            Object desc = output.__getitem__(1);

            // map every key in the optional dictionary as parameter metadata entry
            Map<String, Object> metadata = null;
            if (output.size() == 3) {
                PyDictionary meta = (PyDictionary) output.get(2);
                if (meta != null && !meta.isEmpty()) {
                    metadata = new HashMap<String, Object>();
                    for (Object key : meta.keySet()) {
                        metadata.put((String) key, meta.get(key));
                    }
                }
            }

            map.put(name, parameter(name, type, 1, 1, desc, null, metadata));
        }

        return map;
    }

    @Override
    public Map<String, Object> run(Map<String, Object> inputs, ScriptEngine engine)
            throws ScriptException {
        PyObject run = process(engine);

        List<PyObject> args = new ArrayList();
        List<String> kw = new ArrayList();

        for (Map.Entry<String, Object> input : inputs.entrySet()) {
            args.add(Py.java2py(input.getValue()));
            kw.add(input.getKey());
        }

        PyObject r =
                run.__call__(
                        args.toArray(new PyObject[args.size()]), kw.toArray(new String[kw.size()]));

        Map<String, Parameter<?>> outputs = getOutputs(engine);

        Map<String, ?> result = null;
        if (r instanceof Map) {
            result = (Map<String, ?>) r;
        } else {
            result = Collections.singletonMap(outputs.keySet().iterator().next(), r);
        }

        if (result.size() != outputs.size()) {
            throw new IllegalStateException(
                    String.format(
                            "Process returned %d values, should have " + "returned %d",
                            result.size(), outputs.size()));
        }

        Map<String, Object> results = new LinkedHashMap<String, Object>();

        for (Map.Entry<String, Parameter<?>> e : outputs.entrySet()) {
            String key = e.getKey();
            Parameter output = e.getValue();

            Object obj = result.get(key);

            if (obj instanceof PyObject) {
                obj = ((PyObject) obj).__tojava__(output.type);
            }
            if (obj != null && !output.type.isInstance(obj)) {
                LOGGER.warning(
                        String.format(
                                "Output %s declared type %s but returned %s",
                                output.getName(),
                                output.getType().getSimpleName(),
                                obj.getClass().getSimpleName()));
            }

            results.put(output.key, obj);
        }

        return results;
    }

    PyObject process(ScriptEngine engine) {
        return (PyObject) engine.get("run");
    }

    String str(Object obj) {
        return obj != null ? obj.toString() : null;
    }

    Parameter parameter(
            String name,
            Object type,
            int min,
            int max,
            Object desc,
            Object defaultValue,
            Map metadata) {
        Class clazz = null;
        if (type != null && type instanceof PyType) {
            clazz = PythonPlugin.toJavaClass((PyType) type);
        }
        if (clazz == null) {
            clazz = Object.class;
        }

        desc = desc != null ? desc : name;
        return new Parameter(
                name,
                clazz,
                Text.text(name),
                Text.text(desc.toString()),
                min > 0,
                min,
                max,
                defaultValue,
                metadata);
    }
}
