/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.py;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import org.geoserver.script.wps.ScriptProcessFactory;
import org.geoserver.script.wps.ScriptProcessTestSupport;
import org.geotools.data.Parameter;
import org.locationtech.jts.geom.Geometry;
import org.opengis.feature.type.Name;

public class PyExtraMetadataTest extends ScriptProcessTestSupport {

    @Override
    public String getExtension() {
        return "py";
    }

    @Override
    public String getProcessName() {
        return "buffer-ex";
    }

    public void testName() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        assertEquals(1, pf.getNames().size());

        Name name = pf.getNames().iterator().next();
        assertEquals("buffer-ex", name.getLocalPart());
    }

    public void testInputs() throws Exception {
        ScriptProcessFactory pf = new ScriptProcessFactory(scriptMgr);
        Name buffer = pf.getNames().iterator().next();

        Map<String, Parameter<?>> inputs = pf.getParameterInfo(buffer);
        assertNotNull(inputs);
        assertEquals(4, inputs.size());

        checkParameter(inputs, "geom", Geometry.class, "The geometry to buffer", 1, 1);
        checkParameter(inputs, "distance", Number.class, "The buffer distance", 1, 1);
        Parameter<?> capStyle =
                checkParameter(
                        inputs, "capStyle", String.class, "The style of buffer endings", 0, 1);
        assertEquals("round", capStyle.sample);
        List<String> options = (List<String>) capStyle.metadata.get(Parameter.OPTIONS);
        assertNotNull(options);
        assertEquals(Arrays.asList("round", "flat", "square"), options);
        Parameter<?> quadrantSegments =
                checkParameter(
                        inputs, "quadrantSegments", Integer.class, "Number of segments", 0, 1);
        assertEquals(8, quadrantSegments.sample);
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
}
