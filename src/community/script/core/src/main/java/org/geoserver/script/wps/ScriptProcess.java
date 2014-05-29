/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.apache.commons.io.FilenameUtils;
import org.geoserver.platform.FileWatcher;
import org.geoserver.script.ScriptFileWatcher;
import org.geoserver.script.ScriptManager;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.geotools.process.ProcessException;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;

/**
 * Implementation of {@link Process} backed by a script.
 * <p>
 * This class does its work by delegating all methods to the {@link WpsHook} interface. This 
 * class maintains a link to the backing script {@link File} and uses a {@link FileWatcher} to 
 * detect changes to the underlying script. When changed a new {@link ScriptEngine} is created and 
 * the underlying script is reloaded. 
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ScriptProcess implements Process {

    /** process name*/
    Name name;
    
    /** watcher for file changes */
    ScriptFileWatcher fw;
    
    /** script manager */
    ScriptManager scriptMgr;

    /** the hook for interacting with the script */
    WpsHook hook;

    ScriptProcess(File script, ScriptManager scriptMgr) {
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
        return hook.getInputs(fw.readIfModified());
    }

    public Map<String, Parameter<?>> getOutputs() throws ScriptException, IOException {
        return hook.getOutputs(fw.readIfModified());
    }

    @Override
    public Map<String, Object> execute(Map<String, Object> input,
            ProgressListener monitor) throws ProcessException {

        try {
            return hook.run(input, fw.readIfModified());
        } catch (Exception e) {
            throw new ProcessException(e);
        }
    }

    public File getScript() {
        return fw.getFile();
    }

    public Name getName() throws IOException {
        return new NameImpl(hook.getNamespace(fw.readIfModified(), getScript()),
                FilenameUtils.getBaseName(getScript().getName()));
    }
}
