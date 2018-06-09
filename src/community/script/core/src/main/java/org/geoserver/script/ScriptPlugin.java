/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import java.io.Serializable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.geoserver.script.app.AppHook;
import org.geoserver.script.function.FunctionHook;
import org.geoserver.script.wfs.WfsTxHook;
import org.geoserver.script.wps.WpsHook;
import org.springframework.stereotype.Component;

/**
 * Base class for script plugins.
 *
 * <p>Instances of this class must be registered in the application context.
 *
 * @author Justin Deoliveira, OpenGeo
 */
@Component
public abstract class ScriptPlugin implements Serializable {

    String extension;
    Class<? extends ScriptEngineFactory> scriptEngineFactoryClass;

    /**
     * Constructor.
     *
     * @param extension The associated extension for the plugin.
     * @param factoryClass The associated jsr-223 script engine factory class.
     */
    protected ScriptPlugin(String extension, Class<? extends ScriptEngineFactory> factoryClass) {
        this.extension = extension;
        this.scriptEngineFactoryClass = factoryClass;
    }

    public void init(ScriptManager scriptMgr) throws Exception {}

    /** The associated extension for the script plugin, examples: "py", "js", "rb", etc... */
    public String getExtension() {
        return extension;
    }

    /** The id of the script plugin, examples: "python", "javascript", "ruby", etc... */
    public abstract String getId();

    /**
     * The id of the script plugin, meant for display, examples: "Python", "JavaScript", "Ruby",
     * etc...
     */
    public abstract String getDisplayName();

    /**
     * The value of the mode parameter to use for the CodeMirror editor.
     *
     * <p>Subclasses may override, the default for this method is to return {@link #getId()}.
     */
    public String getEditorMode() {
        return getId();
    }

    /** The associated script engine factory for the script plugin. */
    public Class<? extends ScriptEngineFactory> getScriptEngineFactoryClass() {
        return scriptEngineFactoryClass;
    }

    /**
     * Callback to initialize a new script engine.
     *
     * <p>This method is called whenever a new script engine is created and before any scripts are
     * created. Plugins may use this method to set up any context they wish to make avialable to
     * scripts running in the engine. This default implementation does nothing.
     */
    public void initScriptEngine(ScriptEngine engine) {}

    /**
     * Creates the hook for "app" requests.
     *
     * <p>This default implementation returns <code>null</code>, subclass should override in order
     * to implement a custom app hook.
     */
    public AppHook createAppHook() {
        return new AppHook(this);
    }

    /**
     * Creates the hook for wps processes.
     *
     * <p>This default implementation returns <code>null</code>, subclass should override in order
     * to implement a custom hook.
     */
    public WpsHook createWpsHook() {
        return new WpsHook(this);
    }

    /**
     * Creates the hook for functions.
     *
     * <p>This default implementation returns <code>null</code>, subclass should override in order
     * to implement a custom hook.
     */
    public FunctionHook createFunctionHook() {
        return new FunctionHook(this);
    }

    /**
     * Creates the hook for WFS transactions.
     *
     * <p>This default implementation returns a default implementation, subclass should override in
     * order to implement a custom hook.
     */
    public WfsTxHook createWfsTxHook() {
        return new WfsTxHook(this);
    }
}
