/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.util.Map;
import javax.script.Bindings;
import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoserver.script.wfs.WfsTxHook;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.feature.FeatureCollection;
import org.python.core.Py;
import org.python.core.PyException;
import org.python.core.PyFunction;
import org.python.core.PyObject;

public class PyWfsTxHook extends WfsTxHook {

    public PyWfsTxHook(PythonPlugin plugin) {
        super(plugin);
    }

    @Override
    protected void doHandlePreInsert(
            ScriptEngine engine,
            FeatureCollection inserted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        if (!findAndCall("preInsert", engine, inserted, tx, context)) {
            super.doHandlePreInsert(engine, inserted, tx, context);
        }
    }

    @Override
    protected void doHandlePostInsert(
            ScriptEngine engine,
            FeatureCollection inserted,
            TransactionRequest tx,
            java.util.Map<?, ?> context)
            throws ScriptException {
        if (!findAndCall("postInsert", engine, inserted, tx, context)) {
            super.doHandlePostInsert(engine, inserted, tx, context);
        }
    }

    @Override
    protected void doHandlePreUpdate(
            ScriptEngine engine,
            FeatureCollection updated,
            Map<String, Object> props,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {

        if (!findAndCall("preUpdate", engine, updated, props, tx, context)) {
            super.doHandlePreUpdate(engine, updated, props, tx, context);
        }
    }

    @Override
    protected void doHandlePostUpdate(
            ScriptEngine engine,
            FeatureCollection updated,
            Map<String, Object> props,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        if (!findAndCall("postUpdate", engine, updated, props, tx, context)) {
            super.doHandlePostUpdate(engine, updated, props, tx, context);
        }
    }

    @Override
    protected void doHandlePreDelete(
            ScriptEngine engine,
            FeatureCollection deleted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        if (!findAndCall("preDelete", engine, deleted, tx, context)) {
            super.doHandlePreDelete(engine, deleted, tx, context);
        }
    }

    @Override
    protected void doHandlePostDelete(
            ScriptEngine engine,
            FeatureCollection deleted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        if (!findAndCall("postDelete", engine, deleted, tx, context)) {
            super.doHandlePostDelete(engine, deleted, tx, context);
        }
    }

    @Override
    protected void doHandleBefore(ScriptEngine engine, TransactionRequest tx, Map<?, ?> context)
            throws ScriptException {
        if (!findAndCall("before", engine, tx, context)) {
            super.doHandleBefore(engine, tx, context);
        }
    }

    @Override
    protected void doHandlePreCommit(ScriptEngine engine, TransactionRequest tx, Map<?, ?> context)
            throws ScriptException {
        if (!findAndCall("preCommit", engine, tx, context)) {
            super.doHandlePreCommit(engine, tx, context);
        }
    }

    @Override
    protected void doHandlePostCommit(
            ScriptEngine engine,
            TransactionRequest tx,
            TransactionResponse result,
            Map<?, ?> context)
            throws ScriptException {
        if (!findAndCall("postCommit", engine, tx, result, context)) {
            super.doHandlePostCommit(engine, tx, result, context);
        }
    }

    @Override
    protected void doHandleAbort(
            ScriptEngine engine,
            TransactionRequest tx,
            TransactionResponse result,
            Map<?, ?> context)
            throws ScriptException {
        if (!findAndCall("abort", engine, tx, result, context)) {
            super.doHandleAbort(engine, tx, result, context);
        }
    }

    @Override
    protected void unWrapAndThrowWfsException(ScriptException e) throws ScriptException {
        if (e.getCause() instanceof PyException) {
            PyException pye = (PyException) e.getCause();
            if (pye.value != null) {
                Object wfse = pye.value.__tojava__(Exception.class);
                if (wfse instanceof WFSException) {
                    throw (WFSException) wfse;
                }
            }
        }
        throw e;
    }

    PyFunction findDecoratedFunction(ScriptEngine engine, String name) {
        Bindings bindings = engine.getBindings(ScriptContext.ENGINE_SCOPE);
        for (Object o : bindings.values()) {
            if (o instanceof PyFunction) {
                PyFunction f = (PyFunction) o;
                PyObject d = f.__findattr__("__decorator__");
                if (d instanceof PyFunction && name.equals(((PyFunction) d).__name__)) {
                    return f;
                }
            }
        }
        return null;
    }

    boolean findAndCall(String name, ScriptEngine engine, Object... args) {
        PyFunction f = findDecoratedFunction(engine, name);
        if (f != null) {
            call(f, args);
            return true;
        }
        return false;
    }

    Object call(PyFunction f, Object... args) {

        PyObject[] pyargs = new PyObject[args.length];
        for (int i = 0; i < args.length; i++) {
            pyargs[i] = Py.java2py(args[i]);
        }

        return f.__call__(pyargs);
    }
}
