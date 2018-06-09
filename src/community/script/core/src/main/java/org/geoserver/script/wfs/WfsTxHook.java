/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wfs;

import java.util.Map;
import javax.script.ScriptEngine;
import javax.script.ScriptException;
import org.geoserver.script.ScriptHook;
import org.geoserver.script.ScriptPlugin;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;
import org.geotools.feature.FeatureCollection;

public class WfsTxHook extends ScriptHook {

    public WfsTxHook(ScriptPlugin plugin) {
        super(plugin);
    }

    public Integer getPriority(ScriptEngine engine) throws ScriptException {
        return lookup(engine, "priority", Integer.class, false);
    }

    public final void handleBefore(ScriptEngine engine, TransactionRequest tx, Map<?, ?> context)
            throws ScriptException {
        try {
            doHandleBefore(engine, tx, context);
        } catch (ScriptException e) {
            unWrapAndThrowWfsException(e);
        }
    }

    //
    // before
    //

    protected void doHandleBefore(ScriptEngine engine, TransactionRequest tx, Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "before", tx, context);
    }

    //
    // preInsert
    //

    public final void handlePreInsert(
            ScriptEngine engine,
            FeatureCollection inserted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        try {
            doHandlePreInsert(engine, inserted, tx, context);
        } catch (ScriptException e) {
            unWrapAndThrowWfsException(e);
        }
    }

    protected void doHandlePreInsert(
            ScriptEngine engine,
            FeatureCollection inserted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "preInsert", inserted, tx, context);
    }

    //
    // postInsert
    //

    public final void handlePostInsert(
            ScriptEngine engine,
            FeatureCollection inserted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        try {
            doHandlePostInsert(engine, inserted, tx, context);
        } catch (ScriptException e) {
            unWrapAndThrowWfsException(e);
        }
    }

    protected void doHandlePostInsert(
            ScriptEngine engine,
            FeatureCollection inserted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "postInsert", inserted, tx, context);
    }

    //
    // preUpdate
    //

    public final void handlePreUpdate(
            ScriptEngine engine,
            FeatureCollection updated,
            Map<String, Object> changed,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        try {
            doHandlePreUpdate(engine, updated, changed, tx, context);
        } catch (ScriptException e) {
            unWrapAndThrowWfsException(e);
        }
    }

    protected void doHandlePreUpdate(
            ScriptEngine engine,
            FeatureCollection updated,
            Map<String, Object> props,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "preUpdate", updated, props, tx, context);
    }

    //
    // postUpdate
    //

    public final void handlePostUpdate(
            ScriptEngine engine,
            FeatureCollection updated,
            Map<String, Object> props,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        try {
            doHandlePostUpdate(engine, updated, props, tx, context);
        } catch (ScriptException e) {
            unWrapAndThrowWfsException(e);
        }
    }

    protected void doHandlePostUpdate(
            ScriptEngine engine,
            FeatureCollection updated,
            Map<String, Object> props,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "postUpdate", updated, props, tx, context);
    }

    //
    // preDelete
    //

    public final void handlePreDelete(
            ScriptEngine engine,
            FeatureCollection deleted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        try {
            doHandlePreDelete(engine, deleted, tx, context);
        } catch (ScriptException e) {
            unWrapAndThrowWfsException(e);
        }
    }

    protected void doHandlePreDelete(
            ScriptEngine engine,
            FeatureCollection deleted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "preDelete", deleted, tx, context);
    }

    //
    // postDelete
    //

    public final void handlePostDelete(
            ScriptEngine engine,
            FeatureCollection deleted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        try {
            doHandlePostDelete(engine, deleted, tx, context);
        } catch (ScriptException e) {
            unWrapAndThrowWfsException(e);
        }
    }

    protected void doHandlePostDelete(
            ScriptEngine engine,
            FeatureCollection deleted,
            TransactionRequest tx,
            Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "postDelete", deleted, tx, context);
    }

    //
    // preCommit
    //

    public final void handlePreCommit(ScriptEngine engine, TransactionRequest tx, Map<?, ?> context)
            throws ScriptException {
        doHandlePreCommit(engine, tx, context);
    }

    protected void doHandlePreCommit(ScriptEngine engine, TransactionRequest tx, Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "preCommit", tx, context);
    }

    //
    // postCommit
    //

    public void handlePostCommit(
            ScriptEngine engine,
            TransactionRequest tx,
            TransactionResponse result,
            Map<?, ?> context)
            throws ScriptException {
        doHandlePostCommit(engine, tx, result, context);
    }

    protected void doHandlePostCommit(
            ScriptEngine engine,
            TransactionRequest tx,
            TransactionResponse result,
            Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "postCommit", tx, result, context);
    }

    //
    // abort
    //

    public void handleAbort(
            ScriptEngine engine,
            TransactionRequest tx,
            TransactionResponse result,
            Map<?, ?> context)
            throws ScriptException {
        doHandleAbort(engine, tx, result, context);
    }

    protected void doHandleAbort(
            ScriptEngine engine,
            TransactionRequest tx,
            TransactionResponse result,
            Map<?, ?> context)
            throws ScriptException {
        invokeOptional(engine, "abort", tx, result, context);
    }

    protected void unWrapAndThrowWfsException(ScriptException e) throws ScriptException {
        // unwind the exception looknig for a wfs exception
        Throwable t = e.getCause();
        while (t != null) {
            if (t instanceof WFSException) {
                throw (WFSException) t;
            }
            t = t.getCause();
        }
        throw e;
    }
}
