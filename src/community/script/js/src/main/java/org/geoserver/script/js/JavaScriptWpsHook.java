/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.util.Map;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoscript.js.process.MetaProcess;
import org.geoserver.script.wps.WpsHook;
import org.geotools.data.Parameter;
import org.geotools.util.logging.Logging;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Wrapper;

public class JavaScriptWpsHook extends WpsHook {

    static Logger LOGGER = Logging.getLogger("org.geoserver.script.js");

    public JavaScriptWpsHook(JavaScriptPlugin plugin) {
        super(plugin);
    }

    @Override
    public String getTitle(ScriptEngine engine) throws ScriptException {
        String filename = (String) engine.get(ScriptEngine.FILENAME);
        if (filename == null) {
            filename = "<Unknown Source>";
        }
        MetaProcess process = getProcess(engine);
        String title = process.getTitle();
        if (title == null) {
            LOGGER.warning("Process missing required title in " + filename);
            // TODO provide process name
            title = "Untitled";
        }
        return title;
    }

    @Override
    public String getDescription(ScriptEngine engine) throws ScriptException {
        return getProcess(engine).getDescription();
    }

    @Override
    public Map<String, Parameter<?>> getInputs(ScriptEngine engine) throws ScriptException {
        return getProcess(engine).getInputs();
    }

    @Override
    public Map<String, Parameter<?>> getOutputs(ScriptEngine engine) throws ScriptException {
        return getProcess(engine).getOutputs();
    }

    @Override
    public Map<String, Object> run(Map<String, Object> input, ScriptEngine engine)
            throws ScriptException {
        return getProcess(engine).execute(input, null);
    }

    private MetaProcess getProcess(ScriptEngine engine) {
        String filename = (String) engine.get(ScriptEngine.FILENAME);
        if (filename == null) {
            filename = "<Unknown Source>";
        }
        Object exportsObj = engine.get("exports");
        Scriptable exports = null;
        if (exportsObj instanceof Scriptable) {
            exports = (Scriptable) exportsObj;
        } else {
            throw new RuntimeException("Couldn't get exports for process in " + filename);
        }
        Object processObj = exports.get("process", exports);
        MetaProcess process = null;
        if (processObj instanceof Wrapper) {
            process = (MetaProcess) ((Wrapper) processObj).unwrap();
        } else {
            throw new RuntimeException("Missing 'process' exports from " + filename);
        }
        return process;
    }
}
