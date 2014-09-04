/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.js;

import java.io.File;
import java.util.Collections;

import org.geoserver.data.test.MockData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.script.ScriptIntTestSupport;
import org.geoserver.script.function.ScriptFunctionFactory;
import org.opengis.filter.expression.Function;

public class JavaScriptFunctionTest extends ScriptIntTestSupport {

    ScriptFunctionFactory functionFactory;
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        File fromDir = new File(getClass().getResource("scripts").getFile());
        File toDir = new File(dataDirectory.getDataDirectoryRoot(), "scripts");
        IOUtils.deepCopy(fromDir, toDir);
        super.populateDataDirectory(dataDirectory);
    }

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        functionFactory = new ScriptFunctionFactory(getScriptManager());
    }
    
    @SuppressWarnings("unchecked")
    public void testFactorial() {
        Function f = functionFactory.function("factorial", Collections.EMPTY_LIST, null);
        assertNotNull(f);
        assertEquals(120, ((Number)f.evaluate(5)).intValue());
    }

}
