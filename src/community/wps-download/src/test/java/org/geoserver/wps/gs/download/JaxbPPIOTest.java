package org.geoserver.wps.gs.download;

import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.util.Map;
import org.junit.Test;

public class JaxbPPIOTest {

    @Test
    public void testParseLayer() throws Exception {
        String xml =
                "<Layer xmlns=\""
                        + AbstractParametricEntity.NAMESPACE
                        + "\">\n"
                        + "  <Capabilities>http://demo.geo-solutions.it/geoserver/ows?REQUEST=GetCapabilities&amp;SERVICE=WMS\n"
                        + "  </Capabilities>\n"
                        + "  <Name>tiger:giant_polygon</Name>\n"
                        + "  <Parameter key=\"format\">image/png8</Parameter>\n"
                        + "</Layer>\n";

        JaxbPPIO ppio = new JaxbPPIO(Layer.class, null);
        Layer layer = (Layer) ppio.decode(new ByteArrayInputStream(xml.getBytes()));
        assertEquals("tiger:giant_polygon", layer.getName());
        assertEquals(
                "http://demo.geo-solutions.it/geoserver/ows?REQUEST=GetCapabilities&SERVICE=WMS",
                layer.getCapabilities());
        Map<String, String> parameters = layer.getParametersMap();
        assertEquals(1, parameters.size());
        assertEquals("image/png8", parameters.get("format"));
    }

    @Test
    public void testParseFormat() throws Exception {
        String xml =
                "<Format xmlns=\""
                        + AbstractParametricEntity.NAMESPACE
                        + "\">\n"
                        + "  <Name>image/jpeg</Name>\n"
                        + "  <Parameter key=\"image-quality\">80%</Parameter>\n"
                        + "</Format>\n";

        JaxbPPIO ppio = new JaxbPPIO(Format.class, null);
        Format format = (Format) ppio.decode(new ByteArrayInputStream(xml.getBytes()));
        assertEquals("image/jpeg", format.getName());
        Map<String, String> parameters = format.getParametersMap();
        assertEquals(1, parameters.size());
        assertEquals("80%", parameters.get("image-quality"));
    }
}
