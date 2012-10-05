package org.geoserver.script.py;

import javax.script.ScriptException;

import org.geoserver.script.wfs.WfsTxHook;
import org.geoserver.wfs.WFSException;
import org.python.core.PyException;

public class PyWfsTxHook extends WfsTxHook {

    public PyWfsTxHook(PythonPlugin plugin) {
        super(plugin);
    }

    @Override
    protected void unWrapAndThrowWfsException(ScriptException e)
            throws ScriptException {
        if (e.getCause() instanceof PyException) {
            PyException pye = (PyException) e.getCause();
            if (pye.value != null) {
                Exception wfse = (Exception) pye.value.__tojava__(Exception.class);
                if (wfse instanceof Exception) {
                    throw (WFSException) wfse;
                }
            }
        }
        throw e;
    }
}
