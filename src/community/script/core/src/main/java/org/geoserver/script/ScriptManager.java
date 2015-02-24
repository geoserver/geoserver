/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

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
import org.geoserver.script.app.AppHook;
import org.geoserver.script.function.FunctionHook;
import org.geoserver.script.wfs.WfsTxHook;
import org.geoserver.script.wps.WpsHook;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.logging.Logging;
import org.springframework.beans.factory.InitializingBean;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

/**
 * Facade for the scripting subsystem, providing methods for obtaining script context and managing
 * scripts. 
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
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
        sessions = CacheBuilder.newBuilder()
            .maximumSize(10).expireAfterAccess(10, TimeUnit.MINUTES).build();
    }

    public GeoServerDataDirectory getDataDirectory() {
        return dataDir;
    }

    public GeoServerSecurityManager getSecurityManager() {
        return secMgr != null ? secMgr :GeoServerExtensions.bean(GeoServerSecurityManager.class);
    }

    public void setSecurityManager(GeoServerSecurityManager secMgr) {
        this.secMgr = secMgr;
    }

    /**
     * Returns the underlying engine manager used to create and manage script engines.
     */
    public ScriptEngineManager getEngineManager() {
        return engineMgr;
    }

    /**
     * Returns list of available script plugins.
     */
    public List<ScriptPlugin> getPlugins() {
        return plugins();
    }

    /**
     * The root "scripts" directory, located directly under the root of the data directory. 
     */
    public File getScriptRoot() throws IOException {
        return dataDir.findOrCreateDir("scripts");
    }

    /**
     * Finds a script directory located at the specified path, returning <code>null</code> if no 
     * such directory exists.
     * 
     */
    public File findScriptDir(String path) throws IOException {
        File f = new File(getScriptRoot(), path);
        if (f.exists() && f.isDirectory()) {
            return f;
        }
        return null;
    }

    /**
     * Finds a script directory located at the specified path, creating the directory if it does not
     * already exist.
     * 
     */
    public File findOrCreateScriptDir(String path) throws IOException {
        File f = findScriptDir(path);
        if (f != null) {
            return f;
        }

        f = new File(getScriptRoot(), path);
        if (!f.mkdirs()) {
            throw new IOException("Unable to create directory " + f.getPath());
        }

        return f;
    }

    /**
     * Finds a script file with the specified filename located  in the specified directory path, 
     * returning <code>null</code> if the file does not exist.
     */
    public File findScriptFile(String dirPath, String filename) throws IOException {
        return findScriptFile(dirPath + File.separator + filename);
    }

    /**
     * Finds a script file at the specified path, returning <code>null</code> if the file does not 
     * exist.
     */
    public File findScriptFile(String path) throws IOException {
        File f = new File(getScriptRoot(), path);
        return f.exists() ? f : null;
    }

    /**
     * Finds a script file at the specified path, creating it if necessary.
     */
    public File findOrCreateScriptFile(String path) throws IOException {
        File f = new File(getScriptRoot(), path);
        if (!f.getParentFile().exists()) {
            f.getParentFile().mkdirs();
        }
        return f;
    }

    /**
     * The root "apps" directory, located directly under {@link #getScriptRoot()}.
     */
    public File getAppRoot() throws IOException {
        return dataDir.findOrCreateDir("scripts", "apps");
    }

    /**
     * Finds a named app dir, returning <code>null</code> if the directory does not exist.
     */
    public File findAppDir(String app) throws IOException {
        return findScriptDir("apps" + File.separator + app);
    }

    /**
     * Finds a named app dir, creating if it does not already exist.
     */
    public File findOrCreateAppDir(String app) throws IOException {
        return findOrCreateScriptDir("apps" + File.separator + app);
    }

    /**
     * Find the main script File
     */
    public File findAppMainScript(File appDir) {
        File main = null;
        for (File f : appDir.listFiles()) {
            if ("main".equals(FilenameUtils.getBaseName(f.getName()))) {
                main = f;
                break;
            }
        }
        return main;
    }
    
    /**
     * The root "wps" directory, located directly under {@link #getScriptRoot()} 
     */
    public File getWpsRoot() throws IOException {
        return dataDir.findOrCreateDir("scripts", "wps");
    }

    /**
     * The root "wfs/tx" directory, located directly under {@link #getScriptRoot()} 
     */
    public File getWfsTxRoot() throws IOException {
        return dataDir.findOrCreateDir("scripts", "wfs", "tx");
    }

    /**
     * The root "function" directory, located directly under {@link #getScriptRoot()} 
     */
    public File getFunctionRoot() throws IOException {
        return dataDir.findOrCreateDir("scripts", "function");
    }

    public File getLibRoot(String ext) throws IOException {
        return findOrCreateScriptDir("lib/" + ext);
    }

    /**
     * Creates a new script engine for the specified script file.
     */
    public ScriptEngine createNewEngine(File script) {
        return createNewEngine(ext(script));
    }

    /**
     * Creates a new script engine for the specified file extension.
     */
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
     * Looks up the app hook for the specified script returning <code>null</code> if no such 
     * hook can be found.
     * <p>
     * This method works by looking up all {@link ScriptPlugin} instances and delegating to 
     * {@link ScriptPlugin#createAppHook()}.
     * </p>
     */
    public AppHook lookupAppHook(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.createAppHook() : new AppHook(null);
    }

    /**
     * Looks up the wps hook for the specified script returning <code>null</code> if no such 
     * hook can be found.
     * <p>
     * This method works by looking up all {@link ScriptPlugin} instances and delegating to 
     * {@link ScriptPlugin#createWpsHook()}.
     * </p>
     */
    public WpsHook lookupWpsHook(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.createWpsHook() : null;
    }

    /**
     * Looks up the filter hook for the specified script returning <code>null</code> if no such 
     * hook can be found.
     * <p>
     * This method works by looking up all {@link ScriptPlugin} instances and delegating to 
     * {@link ScriptPlugin#createFunctionHook()}.
     * </p>
     */
    public FunctionHook lookupFilterHook(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.createFunctionHook() : new FunctionHook(null);
    }

    /**
     * Looks up the wfs tx hook for the specified script returning <code>null</code> if no such 
     * hook can be found.
     * <p>
     * This method works by looking up all {@link ScriptPlugin} instances and delegating to 
     * {@link ScriptPlugin#createWfsTxHook()}.
     * </p>
     */
    public WfsTxHook lookupWfsTxHook(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.createWfsTxHook() : new WfsTxHook(null);
    }

    public String lookupPluginId(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.getId() : null;
    }

    public String lookupPluginDisplayName(File script) {
        ScriptPlugin p = plugin(script);
        return p != null ? p.getDisplayName() : null;
    }

    public String lookupPluginEditorMode(File script) {
        ScriptPlugin p = plugin(script);
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
    ScriptPlugin plugin(File script) {
        String ext = ext(script);

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
    String ext(File script) throws IllegalArgumentException {
        String ext = FilenameUtils.getExtension(script.getName());
        if (ext == null) {
            throw new IllegalArgumentException(script.getName() + " has no extension");
        }
        return ext;
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        plugins();
    }
}