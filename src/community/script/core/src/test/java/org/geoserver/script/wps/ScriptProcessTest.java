/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptIntTestSupport;
import org.geoserver.script.ScriptManager;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;

public abstract class ScriptProcessTest extends ScriptIntTestSupport {

    File script;
    
    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
    
        script = copyScriptIfExists("buffer");
    }

    File copyScriptIfExists(String baseName) throws IOException {
        File wps = scriptMgr.getWpsRoot();
        File script = new File(wps, baseName + "." + getExtension());

        URL u = getClass().getResource(script.getName());
        if (u != null) {
            FileUtils.copyURLToFile(u, script);
            return script;
        }
        return null;
    }

    public abstract String getExtension();

    public void testLookupHook() throws Exception {
        assertNotNull(getScriptManager().lookupWpsHook(script));
    }

    public void testName() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        assertEquals(1, pf.getNames().size());

        Name name = pf.getNames().iterator().next();
        assertEquals("buffer", name.getLocalPart());
    }

    public void testTitle() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name buffer = pf.getNames().iterator().next();
        assertEquals("Buffer", pf.getTitle(buffer).toString());
    }

    public void testDescription() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name buffer = pf.getNames().iterator().next();
        assertEquals("Buffers a geometry", pf.getDescription(buffer).toString());
    }

    public void testInputs() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name buffer = pf.getNames().iterator().next();
        
        Map<String, Parameter<?>> inputs = pf.getParameterInfo(buffer);
        assertNotNull(inputs);

        assertTrue(inputs.containsKey("geom"));
        assertTrue(Geometry.class.isAssignableFrom(inputs.get("geom").type));
        assertEquals("The geometry to buffer", inputs.get("geom").description.toString());

        assertTrue(inputs.containsKey("distance"));
        assertTrue(Number.class.isAssignableFrom(inputs.get("distance").type));
        assertEquals("The buffer distance", inputs.get("distance").description.toString());
    }

    public void testOutputs() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name buffer = pf.getNames().iterator().next();
        
        Map<String, Parameter<?>> outputs = pf.getResultInfo(buffer, null);
        assertNotNull(outputs);

        assertTrue(outputs.containsKey("result"));
        assertTrue(Geometry.class.isAssignableFrom(outputs.get("result").type));
        assertEquals("The buffered geometry", outputs.get("result").description.toString());
    }

    public void testRun() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name buffer = pf.getNames().iterator().next();

        org.geotools.process.Process p = pf.create(buffer);

        Geometry g = new WKTReader().read("POINT(0 0)");

        Map inputs = new HashMap();
        inputs.put("geom", g);
        inputs.put("distance", 1);

        Map outputs = p.execute(inputs, null);
        Geometry h = (Geometry) outputs.get("result");
        assertTrue(h.equals(g.buffer(1)));
    }

    public void testRunMultipleOutputs() throws Exception {
        String pname = "buffer-multipleOutputs";
        File script = copyScriptIfExists(pname);
        if (script != null) {
            ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
            Name buffer = new NameImpl(getExtension(), pname);

            org.geotools.process.Process p = pf.create(buffer);
            
            Map inputs = new HashMap();
            inputs.put("geom", new WKTReader().read("POINT(0 0)"));
            inputs.put("distance", 1d);

            Map outputs = p.execute(inputs, null);
            assertEquals(2, outputs.size());
            
            assertNotNull((Geometry) outputs.get("geom"));
            assertEquals(1d, (Double) outputs.get("distance"), 0.1);
        }
        else {
            System.out.println("Script " + pname + " does not exist, skipping test");
        }
    }

}
