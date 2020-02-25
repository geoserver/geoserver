/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.util.HashMap;
import java.util.Map;
import org.geoserver.script.wps.ScriptProcessFactory;
import org.geoserver.script.wps.ScriptProcessTestSupport;
import org.geoserver.wps.WPSException;
import org.geotools.data.Parameter;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.process.ProcessException;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.WKTReader;
import org.opengis.feature.type.Name;
import org.opengis.util.ProgressListener;
import org.python.core.PyException;

public class PyMonitorTest extends ScriptProcessTestSupport {

    @Override
    public String getExtension() {
        return "py";
    }

    @Override
    public String getProcessName() {
        return "buffer-monitor";
    }

    public void testName() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        assertEquals(1, pf.getNames().size());

        Name name = pf.getNames().iterator().next();
        assertEquals("buffer-monitor", name.getLocalPart());
    }

    public void testInputs() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name buffer = pf.getNames().iterator().next();

        Map<String, Parameter<?>> inputs = pf.getParameterInfo(buffer);
        assertNotNull(inputs);
        assertEquals(2, inputs.size());

        checkParameter(inputs, "geom", Geometry.class, "The geometry to buffer", 1, 1);
        checkParameter(inputs, "distance", Number.class, "The buffer distance", 1, 1);
    }

    public Parameter<?> checkParameter(
            Map<String, Parameter<?>> parameters,
            String name,
            Class type,
            String description,
            int minOccurs,
            int maxOccurs) {
        assertTrue(parameters.containsKey(name));
        Parameter<?> param = parameters.get(name);
        assertTrue(type.isAssignableFrom(param.type));
        assertEquals(description, param.description.toString());
        assertEquals(minOccurs, param.minOccurs);
        assertEquals(maxOccurs, param.maxOccurs);

        return param;
    }

    public void testRun() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name name = pf.getNames().iterator().next();
        assertEquals(getNamespace(), name.getNamespaceURI());
        assertEquals(getProcessName(), name.getLocalPart());

        org.geotools.process.Process p = pf.create(name);

        Geometry g = new WKTReader().read("POINT(0 0)");

        Map inputs = new HashMap();
        inputs.put("geom", g);
        inputs.put("distance", 1);

        ProgressListener listener = new DefaultProgressListener();
        Map outputs = p.execute(inputs, listener);
        Geometry h = (Geometry) outputs.get("result");
        assertTrue(h.equals(g.buffer(1)));
        assertEquals("The task", listener.getTask().toString());
        assertEquals(10f, listener.getProgress());
    }

    public void testException() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name name = pf.getNames().iterator().next();
        assertEquals(getNamespace(), name.getNamespaceURI());
        assertEquals(getProcessName(), name.getLocalPart());

        org.geotools.process.Process p = pf.create(name);

        Geometry g = new WKTReader().read("POINT(0 0)");

        Map inputs = new HashMap();
        inputs.put("geom", g);
        inputs.put("distance", -11);

        try {
            ProgressListener listener = new DefaultProgressListener();
            p.execute(inputs, listener);
            fail("Should have thrown a WPSException");
        } catch (ProcessException processException) {
            PyException pyException = (PyException) processException.getCause();
            WPSException e = (WPSException) pyException.getCause();
            assertEquals("Forbidden", e.getMessage());
            assertEquals("userInput", e.getCode());
            assertEquals("distance", e.getLocator());
        }
    }
}
