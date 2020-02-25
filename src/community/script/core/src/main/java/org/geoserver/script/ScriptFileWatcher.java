/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoserver.platform.FileWatcher;
import org.geoserver.platform.resource.Resource;

/**
 * Special watcher that watches an underlying script and when changed creates a new {@link
 * ScriptEngine} instance evaluated with the contents of the modified script.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class ScriptFileWatcher extends FileWatcher<ScriptEngine> {

    ScriptManager scriptMgr;
    ScriptEngine engine;

    public ScriptFileWatcher(Resource resource, ScriptManager scriptMgr) {
        super(resource);
        this.scriptMgr = scriptMgr;
    }

    @Deprecated
    public ScriptFileWatcher(File file, ScriptManager scriptMgr) {
        super(file);
        this.scriptMgr = scriptMgr;
    }

    /**
     * Create a new script engine and evaluate the script if modified since the last call to read.
     * Otherwise return the existing engine.
     */
    public ScriptEngine readIfModified() throws IOException {
        if (isModified()) {
            return read();
        } else {
            return engine;
        }
    }

    @Override
    protected ScriptEngine parseFileContents(InputStream in) throws IOException {
        engine = scriptMgr.createNewEngine(getFile());
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        bindings.put(ScriptEngine.FILENAME, getFile().getPath());
        try {
            engine.eval(new InputStreamReader(in));
        } catch (ScriptException e) {
            throw new IOException(e);
        }
        return engine;
    }
}
