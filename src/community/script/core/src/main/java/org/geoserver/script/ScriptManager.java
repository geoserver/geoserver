/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import org.apache.commons.io.FilenameUtils;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.resource.Files;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geoserver.platform.resource.Resources;
import org.geoserver.script.app.AppHook;
import org.geoserver.script.function.FunctionHook;
import org.geoserver.script.wfs.WfsTxHook;
import org.geoserver.script.wps.WpsHook;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * Facade for the scripting subsystem, providing methods for obtaining script context and managing
 * scripts.
 *
 * @author Justin Deoliveira, OpenGeo
 */
@Component
public class ScriptManager implements InitializingBean {

    static Logger LOGGER = Logging.getLogger(ScriptManager.class);

    GeoServerDataDirectory dataDir;
    ScriptEngineManager engineMgr;
    GeoServerSecurityManager secMgr;

    volatile List<ScriptPlugin> plugins;
    Cache<Long, ScriptSession> sessions;

    public ScriptManager(GeoServerDataDirectory dataDir) {
        this.dataDir = dataDir;
        engineMgr = new ScriptEngineManager();
        sessions =
                CacheBuilder.newBuilder()
                        .maximumSize(10)
                        .expireAfterAccess(10, TimeUnit.MINUTES)
                        .build();
    }

    public GeoServerDataDirectory getDataDirectory() {
        return dataDir;
    }

    public GeoServerSecurityManager getSecurityManager() {
        return secMgr != null ? secMgr : GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    public void setSecurityManager(GeoServerSecurityManager secMgr) {
        this.secMgr = secMgr;
    }

    /** Returns the underlying engine manager used to create and manage script engines. */
    public ScriptEngineManager getEngineManager() {
        return engineMgr;
    }

    /** Returns list of available script plugins. */
    public List<ScriptPlugin> getPlugins() {
        return plugins();
    }

    /** The root "scripts" directory, located directly under the root of the data directory. */
    public Resource script() {
        return dataDir.get("scripts");
    }

    /** Gets a script directory located at the specified path */
    public Resource script(String... path) {
        return script().get(Paths.path(path));
    }

    /** The root "scripts" directory, located directly under the root of the data directory. */
    public Resource app() {
        return dataDir.get("scripts/apps");
    }

    /** The root "scripts" directory, located directly under the root of the data directory. */
    public Resource app(String path) {
        return app().get(path);
    }

    /**
     * The root "scripts" directory, located directly under the root of the data directory.
     *
     * @deprecated use {@link #script()}
     */
    @Deprecated
    public File getScriptRoot() throws IOException {
        return script().dir();
    }

    /**
     * Finds a script directory located at the specified path, returning <code>null</code> if no
     * such directory exists.
     *
     * @deprecated use {@link #script(String)}
     */
    @Deprecated
    public File findScriptDir(String path) throws IOException {
        Resource r = script(path);
        if (r.getType() == Type.RESOURCE) {
            return r.dir();
        }
        return null;
    }

    /**
     * Finds a script directory located at the specified path, creating the directory if it does not
     * already exist.
     *
     * @deprecated use {@link #script(String...)}
     */
    @Deprecated
    public File findOrCreateScriptDir(String path) {
        return script(path).dir();
    }

    /**
     * Finds a script file with the specified filename located in the specified directory path,
     * returning <code>null</code> if the file does not exist.
     *
     * @deprecated use {@link #script(String...)}
     */
    @Deprecated
    public File findScriptFile(String dirPath, String filename) throws IOException {
        Resource res = script(dirPath + File.separator + filename);
        return Resources.exists(res) ? res.file() : null;
    }

    /**
     * Finds a script file at the specified path, returning <code>null</code> if the file does not
     * exist.
     *
     * @deprecated use {@link #script(String...)}
     */
    @Deprecated
    public File findScriptFile(String path) throws IOException {
        Resource res = script(path);
        return Resources.exists(res) ? res.file() : null;
    }

    /**
     * Finds a script file at the specified path, creating it if necessary.
     *
     * @deprecated use {@link #script(String...)}
     */
    @Deprecated
    public File findOrCreateScriptFile(String path) throws IOException {
        return script(path).file();
    }

    /**
     * The root "apps" directory, located directly under {@link #getScriptRoot()}.
     *
     * @deprecated use {@link #app()}
     */
    @Deprecated
    public File getAppRoot() throws IOException {
        return app().dir();
    }

    /**
     * Finds a named app dir, returning <code>null</code> if the directory does not exist.
     *
     * @deprecated use {@link #app(String)}
     */
    @Deprecated
    public File findAppDir(String app) throws IOException {
        return app(app).dir();
    }

    /**
     * Finds a named app dir, creating if it does not already exist.
     *
     * @deprecated use {@link #app(String)}
     */
    @Deprecated
    public File findOrCreateAppDir(String app) throws IOException {
        return app(app).dir();
    }

    /** Find the main script File */
    public Resource findAppMainScript(Resource appDir) {
        Resource main = null;
        if (appDir != null) {
            for (Resource f : appDir.list()) {
                if ("main".equals(FilenameUtils.getBaseName(f.name()))) {
                    main = f;
                    break;
                }
            }
        }
        return main;
    }

    /**
     * Find the main script File
     *
     * @deprecated use {@link #findAppMainScript(Resource)}
     */
    @Deprecated
    public File findAppMainScript(File appDir) {
        return findAppMainScript(Files.asResource(appDir)).file();
    }

    /** The root "wps" directory, located directly under {@link #script()} */
    public Resource wps() throws IOException {
        return script("wps");
    }

    /** The root "wfs/tx" directory, located directly under {@link #script()} */
    public Resource wfsTx() throws IOException {
        return script("scripts", "wfs", "tx");
    }

    /** The root "function" directory, located directly under {@link #script()} */
    public Resource function() {
        return script("function");
    }

    public Resource lib(String ext) throws IOException {
        return script("lib").get(ext);
    }

    /**
     * The root "wps" directory, located directly under {@link #getScriptRoot()}
     *
     * @deprecated use {@link #wps()}
     */
    @Deprecated
    public File getWpsRoot() throws IOException {
        return wps().dir();
    }

    /**
     * The root "wfs/tx" directory, located directly under {@link #getScriptRoot()}
     *
     * @deprecated use {@link #wfsTx()}
     */
    @Deprecated
    public File getWfsTxRoot() throws IOException {
        return wfsTx().dir();
    }

    /**
     * The root "function" directory, located directly under {@link #getScriptRoot()}
     *
     * @deprecated use {@link #function()}
     */
    @Deprecated
    public File getFunctionRoot() throws IOException {
        return function().dir();
    }

    /** @deprecated use {@link #lib(String)} */
    @Deprecated
    public File getLibRoot(String ext) throws IOException {
        return script("lib/" + ext).dir();
    }

    /** Creates a new script engine for the specified script file. */
    public ScriptEngine createNewEngine(Resource script) {
        return createNewEngine(ext(script.name()));
    }

    /** Creates a new script engine for the specified script file. */
    @Deprecated
    public ScriptEngine createNewEngine(File script) {
        return createNewEngine(ext(script.getName()));
    }

    /** Creates a new script engine for the specified file extension. */
    public ScriptEngine createNewEngine(String ext) {
        if (ext == null) {
            return null;
        }
        return initEngine(engineMgr.getEngineByExtension(ext));
    }

    public ScriptSession createNewSession(String ext) {
        ScriptSession session = new ScriptSession(createNewEngine(ext), ext);
        sessions.put(session.getId(), session);
        return session;
    }

    public ScriptSession findSession(long id) {
        return sessions.getIfPresent(id);
    }

    public List<ScriptSession> findSessions(String ext) {
        List<ScriptSession> sids = new ArrayList<ScriptSession>();
        for (Map.Entry<Long, ScriptSession> e : sessions.asMap().entrySet()) {
            if (ext != null && !ext.equalsIgnoreCase(e.getValue().getExtension())) {
                continue;
            }

            sids.add(e.getValue());
        }

        return sids;
    }

    /*
     * Initializes a new script engine by looking up the plugin matching the engines factory.
     */
    ScriptEngine initEngine(ScriptEngine engine) {
        if (engine == null) {
            return null;
        }

        for (ScriptPlugin plugin : plugins()) {
            if (plugin.getScriptEngineFactoryClass().isInstance(engine.getFactory())) {
                plugin.initScriptEngine(engine);
                break;
            }
        }

        return engine;
    }

    /**
     * Looks up the app hook for the specified script returning <code>null</code> if no such hook
     * can be found.
     *
     * <p>This method works by looking up all {@link ScriptPlugin} instances and delegating to
     * {@link ScriptPlugin#createAppHook()}.
     */
    public AppHook lookupAppHook(Resource script) {
        ScriptPlugin p = plugin(script.name());
        return p != null ? p.createAppHook() : new AppHook(null);
    }

    /**
     * Looks up the wps hook for the specified script returning <code>null</code> if no such hook
     * can be found.
     *
     * <p>This method works by looking up all {@link ScriptPlugin} instances and delegating to
     * {@link ScriptPlugin#createWpsHook()}.
     */
    public WpsHook lookupWpsHook(Resource script) {
        ScriptPlugin p = plugin(script.name());
        return p != null ? p.createWpsHook() : null;
    }

    /**
     * Looks up the filter hook for the specified script returning <code>null</code> if no such hook
     * can be found.
     *
     * <p>This method works by looking up all {@link ScriptPlugin} instances and delegating to
     * {@link ScriptPlugin#createFunctionHook()}.
     */
    public FunctionHook lookupFilterHook(Resource script) {
        ScriptPlugin p = plugin(script.name());
        return p != null ? p.createFunctionHook() : new FunctionHook(null);
    }

    /**
     * Looks up the wfs tx hook for the specified script returning <code>null</code> if no such hook
     * can be found.
     *
     * <p>This method works by looking up all {@link ScriptPlugin} instances and delegating to
     * {@link ScriptPlugin#createWfsTxHook()}.
     */
    public WfsTxHook lookupWfsTxHook(Resource script) {
        ScriptPlugin p = plugin(script.name());
        return p != null ? p.createWfsTxHook() : new WfsTxHook(null);
    }

    public String lookupPluginId(Resource script) {
        ScriptPlugin p = plugin(script.name());
        return p != null ? p.getId() : null;
    }

    public String lookupPluginDisplayName(Resource script) {
        ScriptPlugin p = plugin(script.name());
        return p != null ? p.getDisplayName() : null;
    }

    public String lookupPluginEditorMode(Resource script) {
        ScriptPlugin p = plugin(script.name());
        return p != null ? p.getEditorMode() : null;
    }

    public boolean hasEngineForExtension(Resource ext) {
        for (ScriptEngineFactory f : engineMgr.getEngineFactories()) {
            if (f.getExtensions().contains(ext.name())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Looks up the app hook for the specified script returning <code>null</code> if no such hook
     * can be found.
     *
     * <p>This method works by looking up all {@link ScriptPlugin} instances and delegating to
     * {@link ScriptPlugin#createAppHook()}.
     */
    @Deprecated
    public AppHook lookupAppHook(File script) {
        ScriptPlugin p = plugin(script.getName());
        return p != null ? p.createAppHook() : new AppHook(null);
    }

    /**
     * Looks up the wps hook for the specified script returning <code>null</code> if no such hook
     * can be found.
     *
     * <p>This method works by looking up all {@link ScriptPlugin} instances and delegating to
     * {@link ScriptPlugin#createWpsHook()}.
     */
    @Deprecated
    public WpsHook lookupWpsHook(File script) {
        ScriptPlugin p = plugin(script.getName());
        return p != null ? p.createWpsHook() : null;
    }

    /**
     * Looks up the filter hook for the specified script returning <code>null</code> if no such hook
     * can be found.
     *
     * <p>This method works by looking up all {@link ScriptPlugin} instances and delegating to
     * {@link ScriptPlugin#createFunctionHook()}.
     */
    @Deprecated
    public FunctionHook lookupFilterHook(File script) {
        ScriptPlugin p = plugin(script.getName());
        return p != null ? p.createFunctionHook() : new FunctionHook(null);
    }

    /**
     * Looks up the wfs tx hook for the specified script returning <code>null</code> if no such hook
     * can be found.
     *
     * <p>This method works by looking up all {@link ScriptPlugin} instances and delegating to
     * {@link ScriptPlugin#createWfsTxHook()}.
     */
    @Deprecated
    public WfsTxHook lookupWfsTxHook(File script) {
        ScriptPlugin p = plugin(script.getName());
        return p != null ? p.createWfsTxHook() : new WfsTxHook(null);
    }

    @Deprecated
    public String lookupPluginId(File script) {
        ScriptPlugin p = plugin(script.getName());
        return p != null ? p.getId() : null;
    }

    @Deprecated
    public String lookupPluginDisplayName(File script) {
        ScriptPlugin p = plugin(script.getName());
        return p != null ? p.getDisplayName() : null;
    }

    @Deprecated
    public String lookupPluginEditorMode(File script) {
        ScriptPlugin p = plugin(script.getName());
        return p != null ? p.getEditorMode() : null;
    }

    public boolean hasEngineForExtension(String ext) {
        for (ScriptEngineFactory f : engineMgr.getEngineFactories()) {
            if (f.getExtensions().contains(ext)) {
                return true;
            }
        }
        return false;
    }

    /*
     * Looks up all {@link ScriptPlugin} instances in the application context.
     */
    List<ScriptPlugin> plugins() {
        if (plugins == null) {
            synchronized (this) {
                if (plugins == null) {
                    plugins = GeoServerExtensions.extensions(ScriptPlugin.class);
                    for (ScriptPlugin plugin : plugins) {
                        try {
                            plugin.init(this);
                        } catch (Exception e) {
                            LOGGER.log(Level.SEVERE, "Error initializing plugin", e);
                        }
                    }
                }
            }
        }
        return plugins;
    }

    /*
     * Looks up the plugin for the specified script.
     */
    ScriptPlugin plugin(String scriptName) {
        String ext = ext(scriptName);

        for (ScriptPlugin plugin : plugins()) {
            if (ext.equalsIgnoreCase(plugin.getExtension())) {
                return plugin;
            }
        }

        return null;
    }

    /*
     * Helper method for extracting extension from filename throwing exception if the file has no
     * extension.
     */
    String ext(String scriptName) throws IllegalArgumentException {
        String ext = FilenameUtils.getExtension(scriptName);
        if (ext == null) {
            throw new IllegalArgumentException(scriptName + " has no extension");
        }
        return ext;
    }

    /**
     * Find the File based on the name, ScriptType and extension. The File and it's parent
     * directories do not have to exist, they will be created.
     *
     * @param name The name of the script
     * @param type The ScriptType (wps, function wfstx, app)
     * @param extension The extension (js, py, groovy)
     * @return The script File
     */
    public Resource scriptFile(String name, ScriptType type, String extension) throws IOException {
        Resource dir = null;
        if (type == ScriptType.WPS) {
            dir = this.wps();
        } else if (type == ScriptType.FUNCTION) {
            dir = this.function();
        } else if (type == ScriptType.WFSTX) {
            dir = this.wfsTx();
        } else if (type == ScriptType.APP) {
            dir = this.app();
        }
        if (type == ScriptType.APP) {
            Resource appDir = dir.get(name);
            return appDir.get("main." + extension);
        } else {
            return dir.get(name + "." + extension);
        }
    }

    /**
     * Find the File based on the name, ScriptType and extension. The File and it's parent
     * directories do not have to exist, they will be created.
     *
     * @param name The name of the script
     * @param type The ScriptType (wps, function wfstx, app)
     * @param extension The extension (js, py, groovy)
     * @return The script File @Depecrated (use {@link #scriptFile()})
     */
    @Deprecated
    public File findScriptFile(String name, ScriptType type, String extension) throws IOException {
        return scriptFile(name, type, extension).file();
    }

    /**
     * Determine the ScriptType for the File
     *
     * @param file The File
     * @return The ScriptType
     */
    public ScriptType getScriptType(Resource file) {
        Resource dir = file.parent();
        if (dir.name().equals("function")) {
            return ScriptType.FUNCTION;
        } else if (dir.name().equals("tx") && dir.parent().name().equals("wfs")) {
            return ScriptType.WFSTX;
        } else if (dir.name().equals("wps") || dir.parent().name().equals("wps")) {
            return ScriptType.WPS;
        } else if (dir.parent().name().equals("apps")) {
            return ScriptType.APP;
        } else {
            throw new IllegalArgumentException("Can't determine ScriptType for " + file + "'!");
        }
    }

    /**
     * Determine the ScriptType for the File
     *
     * @param file The File
     * @return The ScriptType
     */
    @Deprecated
    public ScriptType getScriptType(File file) {
        File dir = file.getParentFile();
        if (dir.getName().equals("function")) {
            return ScriptType.FUNCTION;
        } else if (dir.getName().equals("tx") && dir.getParentFile().getName().equals("wfs")) {
            return ScriptType.WFSTX;
        } else if (dir.getName().equals("wps") || dir.getParentFile().getName().equals("wps")) {
            return ScriptType.WPS;
        } else if (dir.getParentFile().getName().equals("apps")) {
            return ScriptType.APP;
        } else {
            throw new IllegalArgumentException("Can't determine ScriptType for " + file + "'!");
        }
    }

    /**
     * Look up the editor mode by File extension
     *
     * @param ext The file extension (js, groovy, py)
     * @return The codemirror editor mode or null
     */
    public String lookupEditorModeByExtension(String ext) {
        ScriptPlugin p = null;
        for (ScriptPlugin plugin : plugins()) {
            if (ext.equalsIgnoreCase(plugin.getExtension())) {
                p = plugin;
            }
        }
        if (p != null) {
            return p.getEditorMode();
        } else {
            return null;
        }
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        plugins();
    }
}
