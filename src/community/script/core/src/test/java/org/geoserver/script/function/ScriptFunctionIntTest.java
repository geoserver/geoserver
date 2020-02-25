/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.function;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;
import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptIntTestSupport;
import org.geoserver.wms.WMSTestSupport;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class ScriptFunctionIntTest extends ScriptIntTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();

        File script = new File(getScriptManager().function().dir(), "wfs.js");
        FileUtils.copyURLToFile(getClass().getResource(script.getName()), script);

        script = new File(getScriptManager().function().dir(), "sld.js");
        FileUtils.copyURLToFile(getClass().getResource(script.getName()), script);
    }

    public void testWFS() throws Exception {
        String xml =
                "<wfs:GetFeature "
                        + "service=\"WFS\" version=\"1.1.0\" "
                        + "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
                        + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
                        + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
                        + "<wfs:Query typeName=\"sf:PrimitiveGeoFeature\"> "
                        + "<ogc:Filter>"
                        + "<ogc:PropertyIsEqualTo> "
                        + "<ogc:Function name=\"wfs\">"
                        + "</ogc:Function> "
                        + "<ogc:Literal>true</ogc:Literal> "
                        + "</ogc:PropertyIsEqualTo> "
                        + "</ogc:Filter> "
                        + "</wfs:Query>"
                        + "</wfs:GetFeature>";
        Document dom = postAsDOM("wfs", xml);
        assertEquals(1, dom.getElementsByTagName("sf:intProperty").getLength());
        assertEquals(
                "180",
                dom.getElementsByTagName("sf:intProperty").item(0).getFirstChild().getNodeValue());
    }

    public void testSLD() throws Exception {
        String sld =
                "<StyledLayerDescriptor xmlns:ogc='http://www.opengis.net/ogc'>"
                        + " <UserLayer> "
                        + "  <Name>BasicPolygons</Name>"
                        + "  <UserStyle> "
                        + "   <Name>UserSelection</Name> "
                        + "   <FeatureTypeStyle> "
                        + "    <Rule> "
                        + " <PolygonSymbolizer>"
                        + "    <Geometry>"
                        + "       <ogc:Function name='sld'>"
                        + "         <ogc:PropertyName>the_geom</ogc:PropertyName>"
                        + "       </ogc:Function>"
                        + "    </Geometry>"
                        + "    <Fill>"
                        + "      <CssParameter name='fill'>#777777</CssParameter>"
                        + "    </Fill>"
                        + "    <Stroke>"
                        + "     <CssParameter name='stroke'>#000000</CssParameter>"
                        + "     <CssParameter name='stroke-width'>2</CssParameter>"
                        + "   </Stroke>"
                        + "  </PolygonSymbolizer>"
                        + "    </Rule> "
                        + "   </FeatureTypeStyle> "
                        + "  </UserStyle> "
                        + " </UserLayer> "
                        + "</StyledLayerDescriptor>";

        MockHttpServletResponse response =
                getAsServletResponse(
                        "wms?request=GetMap&version=1.1.1"
                                + "&bbox=-10,-10,10,10&format=image/png&&width=500&height=500&srs=EPSG:4326"
                                + "&SLD_BODY="
                                + sld.replaceAll("=", "%3D"));

        assertEquals("image/png", response.getContentType());
        BufferedImage img = ImageIO.read(getBinaryInputStream(response));
        WMSTestSupport.showImage("test", img);
    }
}
