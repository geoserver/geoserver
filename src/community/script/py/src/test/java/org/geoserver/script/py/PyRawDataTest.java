/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.util.Map;
import org.geoserver.script.wps.ScriptProcessFactory;
import org.geoserver.script.wps.ScriptProcessTestSupport;
import org.geoserver.wps.process.RawData;
import org.geotools.data.Parameter;
import org.opengis.feature.type.Name;

public class PyRawDataTest extends ScriptProcessTestSupport {

    @Override
    public String getExtension() {
        return "py";
    }

    @Override
    public String getProcessName() {
        return "raw";
    }

    public void testName() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        assertEquals(1, pf.getNames().size());

        Name name = pf.getNames().iterator().next();
        assertEquals("raw", name.getLocalPart());
    }

    public void testProcessDescription() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name raw = pf.getNames().iterator().next();

        // check inputs
        Map<String, Parameter<?>> inputs = pf.getParameterInfo(raw);
        assertNotNull(inputs);
        assertEquals(2, inputs.size());

        Parameter<?> param =
                checkParameter(inputs, "input", RawData.class, "The raw data input", 1, 1);
        assertEquals("application/json,text/xml", param.metadata.get("mimeTypes"));
        checkParameter(
                inputs, "outputMimeType", String.class, "The user chosen output mime type", 0, 1);

        // check outputs
        Map<String, Parameter<?>> resultInfo = pf.getResultInfo(raw, null);
        assertEquals(1, resultInfo.size());
        param = checkParameter(resultInfo, "result", RawData.class, "The output", 1, 1);
        assertEquals("application/json,text/xml", param.metadata.get("mimeTypes"));
        assertEquals("outputMimeType", param.metadata.get("chosenMimeType"));
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

    public static void main(String[] args) {
        System.out.println(Integer.MAX_VALUE);
    }
}
