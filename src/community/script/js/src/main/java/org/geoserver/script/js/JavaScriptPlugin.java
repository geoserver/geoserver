/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import org.geoserver.platform.resource.Resource;
import org.geoserver.script.ScriptManager;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.script.app.AppHook;
import org.geoserver.script.function.FunctionHook;
import org.geoserver.script.js.engine.CommonJSEngineFactory;
import org.geoserver.script.wps.WpsHook;
import org.geotools.util.logging.Logging;

public class JavaScriptPlugin extends ScriptPlugin {

    private static final long serialVersionUID = 1L;
    private Logger LOGGER = Logging.getLogger("org.geoserver.script.js");
    private ScriptManager scriptMgr;

    protected JavaScriptPlugin() {
        super("js", CommonJSEngineFactory.class);
    }

    @Override
    public String getId() {
        return "javascript";
    }

    @Override
    public String getDisplayName() {
        return "JavaScript";
    }

    @Override
    public void init(ScriptManager scriptMgr) throws Exception {
        super.init(scriptMgr);
        this.scriptMgr = scriptMgr;
        scriptMgr
                .getEngineManager()
                .registerEngineExtension("js", new CommonJSEngineFactory(getModulePaths()));
    }

    @Override
    public void initScriptEngine(ScriptEngine engine) {
        super.initScriptEngine(engine);
        Bindings scope = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        scope.put("LOGGER", LOGGER);
    }

    /**
     * Returns a list of paths to JavaScript modules. This includes modules bundled with this
     * extension in addition to modules in the "scripts/lib/js" directory of the data dir.
     */
    public List<String> getModulePaths() {
        // GeoScript modules
        URL geoscriptModuleUrl = getClass().getClassLoader().getResource("org/geoscript/js/lib");
        String geoscriptModulePath;
        try {
            geoscriptModulePath = geoscriptModuleUrl.toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Trouble evaluating GeoScript module path.", e);
        }

        // GeoServer modules
        URL geoserverModuleUrl = getClass().getResource("modules");
        String geoserverModulePath;
        try {
            geoserverModulePath = geoserverModuleUrl.toURI().toString();
        } catch (URISyntaxException e) {
            throw new RuntimeException("Trouble evaluating GeoServer module path.", e);
        }

        // User modules
        Resource libRoot;
        try {
            libRoot = scriptMgr.script("lib/" + "js");
        } catch (IllegalStateException e) {
            throw new RuntimeException("Trouble getting JavaScript library root.", e);
        }
        String userModulePath = libRoot.dir().toURI().toString();

        return (List<String>)
                Arrays.asList(geoscriptModulePath, geoserverModulePath, userModulePath);
    }

    @Override
    public WpsHook createWpsHook() {
        return new JavaScriptWpsHook(this);
    }

    @Override
    public FunctionHook createFunctionHook() {
        return new JavaScriptFunctionHook(this);
    }

    @Override
    public AppHook createAppHook() {
        return new JavaScriptAppHook(this);
    }
}
