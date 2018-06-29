/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.geotools.data.Parameter;
import org.geotools.feature.NameImpl;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.type.Name;

public abstract class ScriptProcessTest extends ScriptProcessTestSupport {

    @Override
    public String getProcessName() {
        return "buffer";
    }

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
        assertEquals(getNamespace(), buffer.getNamespaceURI());
        assertEquals(getProcessName(), buffer.getLocalPart());

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
            Name buffer = new NameImpl(getNamespace(), pname);

            org.geotools.process.Process p = pf.create(buffer);

            Map inputs = new HashMap();
            inputs.put("geom", new WKTReader().read("POINT(0 0)"));
            inputs.put("distance", 1d);

            Map outputs = p.execute(inputs, null);
            assertEquals(2, outputs.size());

            assertNotNull((Geometry) outputs.get("geom"));
            assertEquals(1d, (Double) outputs.get("distance"), 0.1);
        } else {
            System.out.println("Script " + pname + " does not exist, skipping test");
        }
    }
}
