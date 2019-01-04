/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.function;

import java.io.File;
import java.util.Collections;
import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptTestSupport;
import org.opengis.filter.expression.Function;

public class ScriptFunctionTest extends ScriptTestSupport {

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        File function = scriptMgr.function().dir();
        File script = new File(function, "factorial." + getExtension());

        FileUtils.copyURLToFile(getClass().getResource(script.getName()), script);
    }

    protected String getExtension() {
        return "js";
    }

    public void testName() throws Exception {
        ScriptFunctionFactory sff = new ScriptFunctionFactory(scriptMgr);
        assertEquals(1, sff.getFunctionNames().size());
        assertEquals("factorial", sff.getFunctionNames().get(0).getName());
    }

    public void testRun() throws Exception {
        ScriptFunctionFactory sff = new ScriptFunctionFactory(scriptMgr);

        Function f = sff.function("factorial", Collections.EMPTY_LIST, null);
        assertNotNull(f);
        assertEquals(120, ((Number) f.evaluate(5)).intValue());
    }
}
