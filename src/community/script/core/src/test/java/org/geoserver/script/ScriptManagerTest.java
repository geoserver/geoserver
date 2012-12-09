/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script;

import javax.script.ScriptEngine;

public class ScriptManagerTest extends ScriptTestSupport {

    public void testGetEngineManager() throws Exception {
        ScriptEngine engine = scriptMgr.getEngineManager().getEngineByName("JavaScript");
        engine.eval("print ('Hello');");
    }
}
