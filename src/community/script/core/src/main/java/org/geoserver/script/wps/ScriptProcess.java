/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;
import org.geoserver.script.ScriptFileWatcher;
import org.geoserver.script.ScriptManager;
import org.geotools.data.Parameter;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * Implementation of {@link Process} backed by a script.
 *
 * <p>This class does its work by delegating all methods to the {@link WpsHook} interface. This
 * class maintains a link to the backing script {@link File} and uses a {@link FileWatcher} to
 * detect changes to the underlying script. When changed a new {@link ScriptEngine} is created and
 * the underlying script is reloaded.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ScriptProcess implements Process {

    /** The name of the input that, by convention, receives the StatusMonitor */
    static final String MONITOR = "monitor";

    /** process name */
    Name name;

    /** watcher for file changes */
    ScriptFileWatcher fw;

    /** script manager */
    ScriptManager scriptMgr;

    /** the hook for interacting with the script */
    WpsHook hook;

    ScriptProcess(Name name, Resource script, ScriptManager scriptMgr) {
        this.name = name;
        this.scriptMgr = scriptMgr;

        hook = scriptMgr.lookupWpsHook(script);
        fw = new ScriptFileWatcher(script, scriptMgr);
    }

    @Deprecated
    ScriptProcess(Name name, File script, ScriptManager scriptMgr) {
        this.name = name;
        this.scriptMgr = scriptMgr;

        hook = scriptMgr.lookupWpsHook(script);
        fw = new ScriptFileWatcher(script, scriptMgr);
    }

    public String getTitle() throws ScriptException, IOException {
        return hook.getTitle(fw.readIfModified());
    }

    String getVersion() throws ScriptException, IOException {
        return hook.getVersion(fw.readIfModified());
    }

    public String getDescription() throws ScriptException, IOException {
        return hook.getDescription(fw.readIfModified());
    }

    public Map<String, Parameter<?>> getInputs() throws ScriptException, IOException {
        Map<String, Parameter<?>> inputs = hook.getInputs(fw.readIfModified());
        if (inputs == null) {
            return null;
        }

        if (inputs.get(MONITOR) == null) {
            return inputs;
        }
        // remove the monitor
        Map<String, Parameter<?>> result = new HashMap<String, Parameter<?>>();
        result.putAll(inputs);
        result.remove(MONITOR);
        return result;
    }

    public Map<String, Parameter<?>> getOutputs() throws ScriptException, IOException {
        return hook.getOutputs(fw.readIfModified());
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input, ProgressListener listener)
            throws ProcessException {

        try {
            if (listener != null) {
                Map<String, Parameter<?>> inputParams = hook.getInputs(fw.readIfModified());
                if (inputParams.get(MONITOR) != null) {
                    StatusMonitor monitor = new StatusMonitor(listener);
                    Map<String, Object> inputReplacement = new HashMap<String, Object>(input);
                    inputReplacement.put(MONITOR, monitor);
                    input = inputReplacement;
                }
            }
            return hook.run(input, fw.readIfModified());
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }
}
