/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.scriptlet;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Script;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.Wrapper;
import org.mozilla.javascript.tools.shell.Global;

import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.text.Text;

import org.opengis.util.ProgressListener;

public class JavaScriptProcess implements Process{
    private File myScript;
    private Global scope;

    /**
     * Constructs a new process that wraps a script input file
     * @param algorithm the javascript input file
     */
    public JavaScriptProcess(File algorithm) {
        myScript = algorithm;
        Context cx = Context.enter();
        try {
            scope = new Global(); // cx.initStandardObjects();
            scope.initStandardObjects(cx, true);
            scope.installRequire(cx, new java.util.ArrayList(), true);
            FileReader reader = new FileReader(myScript);
            /// Script script = cx.compileReader(reader, myScript.getName(), 1, null);
            /// script.exec(cx, scope);
            cx.evaluateReader(scope, reader, myScript.getName(), 1, null);
        } catch (IOException e) {
            throw new RuntimeException("I/O error while loading process script...");
        } finally {
            Context.exit();
        }
    }

    public Map<String, Object> execute(Map<String, Object> input,
            ProgressListener monitor) {

        Context cx = Context.enter();
        Map<String,Object> results = null;
        Object process = scope.get("process", scope);
        try {
            if (process instanceof Function) {
                Function processFn = (Function)process;
                Object[] args = {mapToJsObject(input, scope)};
                Object result = processFn.call(cx, scope, scope, args);
                results = jsObjectToMap((Scriptable)result);
            } else {
                throw new RuntimeException(
                    "Script for process: " + myScript.getName() + 
                    " is not a valid process script."
                );
            }
        } finally { 
            Context.exit();
        }

        return results;
    }

    Map<String, Object> getMetadata() { 
        Object metadataObj = scope.get("metadata", scope);
        if (metadataObj instanceof Scriptable) {
            return jsObjectToMap((Scriptable)metadataObj);
        } else {
            return null;
        }
    }

    Map<String, Parameter<?>> getParameterInfo() {
        Object metadataObj = scope.get("metadata", scope);
        Map<String, Object> jsParams = null;

        if (metadataObj instanceof Scriptable) {
            Scriptable metadata = (Scriptable) metadataObj;
            Object nativeParamsObj = metadata.get("inputs", metadata);
            if (nativeParamsObj instanceof Scriptable) 
                jsParams = jsObjectToMap((Scriptable)nativeParamsObj);
        } 
        
        if (jsParams == null) {
            return new HashMap<String, Parameter<?>>();
        }

        Map<String, Parameter<?>> gtParams = new HashMap<String, Parameter<?>>();
        for (Map.Entry<String,Object> entry : jsParams.entrySet()) {
            gtParams.put(
                entry.getKey(), 
                new Parameter(
                    entry.getKey(), 
                    (Class)entry.getValue(),
                    Text.text(entry.getKey()),
                    Text.text(entry.getKey())
                )
            );
        }

        return gtParams;
    }

    Map<String, Parameter<?>> getResultInfo() {
        Object metadataObj = scope.get("metadata", scope);
        Map<String, Object> jsParams = null;

        if (metadataObj instanceof Scriptable) {
            Scriptable metadata = (Scriptable) metadataObj;
            Object nativeParamsObj = metadata.get("outputs", metadata);
            if (nativeParamsObj instanceof Scriptable) 
                jsParams = jsObjectToMap((Scriptable)nativeParamsObj);
        } 
        
        if (jsParams == null) {
            return new HashMap<String, Parameter<?>>();
        }

        Map<String, Parameter<?>> gtParams = new HashMap<String, Parameter<?>>();
        for (Map.Entry<String,Object> entry : jsParams.entrySet()) {
            gtParams.put(
                entry.getKey(), 
                new Parameter(
                    entry.getKey(), 
                    (Class)entry.getValue(),
                    Text.text(entry.getKey()),
                    Text.text(entry.getKey())
                )
            );
        }

        return gtParams;
    }

    private static Scriptable mapToJsObject(Map<String,Object> map, Scriptable scope) {
        Context cx = Context.enter();
        Scriptable obj = cx.newObject(scope);
        try {
            for (Map.Entry<String,Object> entry : map.entrySet()) {
                ScriptableObject.putProperty(
                    obj, 
                    entry.getKey(), 
                    cx.javaToJS(entry.getValue(), scope)
                );
            }
        } finally { 
            Context.exit();
        }
        return obj;
    }

    private static Map<String, Object> jsObjectToMap(Scriptable obj) {
        Object[] ids = obj.getIds();
        Map<String, Object> map = new HashMap<String, Object>();
        for (Object idObj : ids) {
            String id = (String)idObj;
            Object value = obj.get(id, obj);

            if (value instanceof Wrapper) {
                map.put(id, ((Wrapper)value).unwrap());
            } else if (value instanceof Function) {
                // ignore functions?
            } else if (value instanceof Scriptable) {
                map.put(id, jsObjectToMap((Scriptable)value));
            } else {
                map.put(id, value);
            }
        }
        return map;
    }
}
