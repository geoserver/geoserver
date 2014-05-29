package org.geoserver.script.py;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.script.wps.ScriptProcessFactory;
import org.geoserver.script.wps.ScriptProcessTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.process.Process;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.io.WKTReader;

public class PyCustomPrefixTest extends ScriptProcessTestSupport {

    @Override
    public String getExtension() {
        return "py";
    }

    @Override
    public String getProcessName() {
        return "buffer-prefix";
    }

    public void testName() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        assertEquals(1, pf.getNames().size());

        Name name = pf.getNames().iterator().next();
        assertEquals("buffer-prefix", name.getLocalPart());
        assertEquals("abc", name.getNamespaceURI());
    }

    public void testRun() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Process process = pf.create(new NameImpl("abc", "buffer-prefix"));
        Map<String, Object> inputs = new HashMap<String, Object>();
        inputs.put("geom", new WKTReader().read("POINT(0 0)"));
        inputs.put("distance", 10);
        Map<String, Object> outputs = process.execute(inputs, null);
        assertEquals(1, outputs.size());
        Object result = outputs.get("result");
        assertTrue(result instanceof Polygon);
    }

}
