/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js.engine;

import java.io.Reader;
import java.io.StringReader;
import javax.script.AbstractScriptEngine;
import javax.script.Bindings;
import javax.script.Invocable;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptException;
import javax.script.SimpleBindings;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.EcmaError;
import org.mozilla.javascript.Function;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.ScriptableObject;
import org.mozilla.javascript.tools.shell.Global;

public class CommonJSEngine extends AbstractScriptEngine implements Invocable {

    private CommonJSEngineFactory factory;

    public CommonJSEngine() {
        this(new CommonJSEngineFactory(null));
    }

    public CommonJSEngine(CommonJSEngineFactory factory) {
        this.factory = factory;
    }

    @Override
    public Bindings createBindings() {
        return new SimpleBindings();
    }

    @Override
    public Object eval(String script, ScriptContext context) throws ScriptException {
        if (script == null) {
            throw new NullPointerException("Null script");
        }
        return eval(new StringReader(script), context);
    }

    @Override
    public Object eval(Reader reader, ScriptContext context) throws ScriptException {
        String filename = (String) get(ScriptEngine.FILENAME);
        if (filename == null) {
            filename = "<Unknown Source>";
        }
        Object result;
        EngineScope scope = new EngineScope(context);
        Global global = getGlobal();
        scope.setParentScope(global);
        scope.setPrototype(global);
        Context cx = enterContext();
        try {
            scope.put("exports", scope, cx.newObject(global));
            result = cx.evaluateReader(scope, reader, filename, 1, null);
        } catch (EcmaError e) {
            throw new ScriptException(
                    e.getMessage(), e.sourceName(), e.lineNumber(), e.columnNumber());
        } catch (Exception e) {
            throw new ScriptException(e);
        } finally {
            Context.exit();
        }
        return result;
    }

    @Override
    public ScriptEngineFactory getFactory() {
        return factory;
    }

    private Global getGlobal() {
        return factory.getGlobal();
    }

    @Override
    public <T> T getInterface(Class<T> cls) {
        throw new RuntimeException("getInterface not implemented");
    }

    @Override
    public <T> T getInterface(Object thisObj, Class<T> cls) {
        throw new RuntimeException("getInterface not implemented");
    }

    @Override
    public Object invokeFunction(String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        return invokeMethod(null, name, args);
    }

    @Override
    public Object invokeMethod(Object thisObj, String name, Object... args)
            throws ScriptException, NoSuchMethodException {
        if (name == null) {
            throw new NullPointerException("Method name is null");
        }
        if (thisObj == null) {
            thisObj = getGlobal();
        } else {
            if (!(thisObj instanceof Scriptable)) {
                thisObj = Context.toObject(thisObj, getGlobal());
            }
        }
        Object methodObj = ScriptableObject.getProperty((Scriptable) thisObj, name);
        if (!(methodObj instanceof Function)) {
            throw new NoSuchMethodException("No such method: " + name);
        }
        Function method = (Function) methodObj;
        Scriptable scope = method.getParentScope();
        if (scope == null) {
            scope = getGlobal();
        }
        Context cx = enterContext();
        Object result;
        try {
            result = method.call(cx, scope, (Scriptable) thisObj, args);
        } finally {
            Context.exit();
        }
        return result;
    }

    /**
     * Associate a context with the current thread. This calls Context.enter() and sets the language
     * version to 1.8.
     *
     * @return a Context associated with the thread
     */
    public static Context enterContext() {
        Context cx = Context.enter();
        cx.setLanguageVersion(Context.VERSION_1_8);
        return cx;
    }
}
