/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wfs;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;
import javax.script.ScriptEngine;
import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptIntTestSupport;
import org.geoserver.wfs.WFSException;
import org.geoserver.wfs.request.TransactionRequest;
import org.geoserver.wfs.request.TransactionResponse;

public abstract class WfsTxHookTest extends ScriptIntTestSupport {

    public abstract String getExtension();

    File copyOverFile(String basename) throws Exception {
        File wfsTx = scriptMgr.wfsTx().dir();
        File f = new File(wfsTx, basename + "." + getExtension());

        FileUtils.copyURLToFile(getClass().getResource(f.getName()), f);
        return f;
    }

    public void testLookupHook() throws Exception {
        File script = copyOverFile("tx");
        assertNotNull(scriptMgr.lookupWfsTxHook(script));
    }

    public void testHookExecution() throws Exception {
        File script = copyOverFile("tx");
        TransactionRequest tx = new TransactionRequest.WFS11(null);
        TransactionResponse res = new TransactionResponse.WFS11(null);

        Map context = new HashMap();

        ScriptEngine eng = scriptMgr.createNewEngine(script);
        eng.eval(new FileReader(script));

        WfsTxHook hook = getScriptManager().lookupWfsTxHook(script);
        hook.handleBefore(eng, tx, context);
        hook.handlePreInsert(eng, null, tx, context);
        hook.handlePostInsert(eng, null, tx, context);
        hook.handlePreUpdate(eng, null, null, tx, context);
        hook.handlePostUpdate(eng, null, null, tx, context);
        hook.handlePreDelete(eng, null, tx, context);
        hook.handlePostDelete(eng, null, tx, context);
        hook.handlePreCommit(eng, tx, context);
        hook.handlePostCommit(eng, tx, res, context);
        hook.handleAbort(eng, tx, res, context);

        assertTrue(context.containsKey("before"));
        assertTrue(context.containsKey("preInsert"));
        assertTrue(context.containsKey("postInsert"));
        assertTrue(context.containsKey("preUpdate"));
        assertTrue(context.containsKey("postUpdate"));
        assertTrue(context.containsKey("preDelete"));
        assertTrue(context.containsKey("postDelete"));
        assertTrue(context.containsKey("preCommit"));
        assertTrue(context.containsKey("postCommit"));
        assertTrue(context.containsKey("abort"));
    }

    public void testHookError() throws Exception {
        File script = copyOverFile("tx-error");

        TransactionRequest tx = new TransactionRequest.WFS11(null);
        TransactionResponse res = new TransactionResponse.WFS11(null);

        Map context = new HashMap();

        ScriptEngine eng = scriptMgr.createNewEngine(script);
        eng.eval(new FileReader(script));

        WfsTxHook hook = getScriptManager().lookupWfsTxHook(script);
        try {
            hook.handleBefore(eng, tx, context);
            fail("exected WFS exception");
        } catch (WFSException e) {
            assertEquals("before exception", e.getMessage());
        }
    }
}
