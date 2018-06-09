/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js.engine;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.commonjs.module.Require;
import org.mozilla.javascript.commonjs.module.RequireBuilder;
import org.mozilla.javascript.commonjs.module.provider.SoftCachingModuleScriptProvider;
import org.mozilla.javascript.commonjs.module.provider.UrlModuleSourceProvider;
import org.mozilla.javascript.tools.shell.Global;

public class CommonJSEngineFactory implements ScriptEngineFactory {

    private static List<String> names;
    private static List<String> mimeTypes;
    private static List<String> extensions;

    static {
        names = new ArrayList<String>(7);
        names.add("rhino-nonjdk");
        names.add("js");
        names.add("rhino");
        names.add("JavaScript");
        names.add("javascript");
        names.add("ECMAScript");
        names.add("ecmascript");
        names = Collections.unmodifiableList(names);

        mimeTypes = new ArrayList<String>(4);
        mimeTypes.add("application/javascript");
        mimeTypes.add("application/ecmascript");
        mimeTypes.add("text/javascript");
        mimeTypes.add("text/ecmascript");
        mimeTypes = Collections.unmodifiableList(mimeTypes);

        extensions = new ArrayList<String>(1);
        extensions.add("js");
        extensions = Collections.unmodifiableList(extensions);
    }

    private Global global;
    private RequireBuilder requireBuilder;
    private List<String> modulePaths;

    public CommonJSEngineFactory(List<String> modulePaths) {
        this.modulePaths = modulePaths;
        Context cx = CommonJSEngine.enterContext();
        try {
            global = new Global();
            global.initStandardObjects(cx, true);
            global.installRequire(cx, modulePaths, true);
        } finally {
            Context.exit();
        }
    }

    /** Create a new require function using the shared global. */
    @SuppressWarnings("unused")
    private Require createRequire() {
        Require require = null;
        RequireBuilder builder = getRequireBuilder();
        Context cx = CommonJSEngine.enterContext();
        try {
            require = builder.createRequire(cx, global);
        } finally {
            Context.exit();
        }
        return require;
    }

    @Override
    public String getEngineName() {
        return (String) getParameter(ScriptEngine.ENGINE);
    }

    @Override
    public String getEngineVersion() {
        return (String) getParameter(ScriptEngine.ENGINE_VERSION);
    }

    @Override
    public List<String> getExtensions() {
        return extensions;
    }

    @Override
    public String getLanguageName() {
        return (String) getParameter(ScriptEngine.LANGUAGE);
    }

    @Override
    public String getLanguageVersion() {
        return (String) getParameter(ScriptEngine.LANGUAGE_VERSION);
    }

    @Override
    public String getMethodCallSyntax(String object, String method, String... args) {
        String syntax = object + "." + method + "(";
        int length = args.length;
        for (int i = 0; i < length; ++i) {
            syntax += args[i];
            if (i != length - 1) {
                syntax += ",";
            }
        }
        return syntax + ")";
    }

    @Override
    public List<String> getMimeTypes() {
        return mimeTypes;
    }

    @Override
    public List<String> getNames() {
        return names;
    }

    @Override
    public String getOutputStatement(String arg) {
        return "print(" + arg + ")";
    }

    @Override
    public Object getParameter(String key) {
        if (key.equals(ScriptEngine.NAME)) {
            return "javascript";
        } else if (key.equals(ScriptEngine.ENGINE)) {
            return "Mozilla Rhino";
        } else if (key.equals(ScriptEngine.ENGINE_VERSION)) {
            return "1.7R4";
        } else if (key.equals(ScriptEngine.LANGUAGE)) {
            return "ECMAScript";
        } else if (key.equals(ScriptEngine.LANGUAGE_VERSION)) {
            return "1.8";
        } else if (key.equals("THREADING")) {
            return "MULTITHREADED";
        } else {
            throw new IllegalArgumentException("Invalid key");
        }
    }

    @Override
    public String getProgram(String... statements) {
        int length = statements.length;
        String program = "";
        for (int i = 0; i < length; ++i) {
            program += statements[i] + ";";
        }
        return program;
    }

    /**
     * Creates and returns a shared require builder. This allows loaded modules to be cached. The
     * require builder is constructed with a module provider that reloads modules only when they
     * have changed on disk (with a 60 second interval). This require builder will be configured
     * with the module paths returned by {@link #getModulePahts()}.
     *
     * @return a shared require builder
     */
    private RequireBuilder getRequireBuilder() {
        if (requireBuilder == null) {
            synchronized (this) {
                if (requireBuilder == null) {
                    requireBuilder = new RequireBuilder();
                    requireBuilder.setSandboxed(false);
                    List<URI> uris = new ArrayList<URI>();
                    if (modulePaths != null) {
                        for (String path : modulePaths) {
                            try {
                                URI uri = new URI(path);
                                if (!uri.isAbsolute()) {
                                    // call resolve("") to canonify the path
                                    uri = new File(path).toURI().resolve("");
                                }
                                if (!uri.toString().endsWith("/")) {
                                    // make sure URI always terminates with slash to
                                    // avoid loading from unintended locations
                                    uri = new URI(uri + "/");
                                }
                                uris.add(uri);
                            } catch (URISyntaxException usx) {
                                throw new RuntimeException(usx);
                            }
                        }
                    }
                    requireBuilder.setModuleScriptProvider(
                            new SoftCachingModuleScriptProvider(
                                    new UrlModuleSourceProvider(uris, null)));
                }
            }
        }
        return requireBuilder;
    }

    @Override
    public ScriptEngine getScriptEngine() {
        return new CommonJSEngine(this);
    }

    public Global getGlobal() {
        return global;
    }
}
