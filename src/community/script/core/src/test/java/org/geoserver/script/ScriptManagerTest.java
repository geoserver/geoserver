package org.geoserver.script;

import javax.script.ScriptEngine;

public class ScriptManagerTest extends ScriptTestSupport {

    public void testGetEngineManager() throws Exception {
        ScriptEngine engine = scriptMgr.getEngineManager().getEngineByName("JavaScript");
        engine.eval("print ('Hello');");
    }
}
