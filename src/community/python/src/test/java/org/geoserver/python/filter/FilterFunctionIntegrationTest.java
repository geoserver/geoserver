/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python.filter;

import java.awt.image.BufferedImage;
import java.io.File;

import javax.imageio.ImageIO;

import junit.framework.Test;

import org.apache.commons.io.FileUtils;
import org.geoserver.python.Python;
import org.geoserver.test.GeoServerTestSupport;
import org.geoserver.wms.WMSTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class FilterFunctionIntegrationTest extends GeoServerTestSupport {

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new FilterFunctionIntegrationTest());
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        Python py = (Python) applicationContext.getBean("python");
        FileUtils.copyURLToFile(getClass().getResource("wfs.py"), new File(py.getFilterRoot(), "wfs.py"));
        FileUtils.copyURLToFile(getClass().getResource("sld.py"), new File(py.getFilterRoot(), "sld.py"));
    }

    public void testWFS() throws Exception {
        String xml = 
        "<wfs:GetFeature " + "service=\"WFS\" version=\"1.1.0\" "
         + "xmlns:sf=\"http://cite.opengeospatial.org/gmlsf\" "
         + "xmlns:wfs=\"http://www.opengis.net/wfs\" "
         + "xmlns:ogc=\"http://www.opengis.net/ogc\" > "
         + "<wfs:Query typeName=\"sf:PrimitiveGeoFeature\"> "
         +   "<ogc:Filter>"
         +    "<ogc:PropertyIsEqualTo> "
         +      "<ogc:Function name=\"myFilter\">"
         +      "</ogc:Function> "
         +      "<ogc:Literal>true</ogc:Literal> "
         +    "</ogc:PropertyIsEqualTo> "
         +   "</ogc:Filter> "
         + "</wfs:Query>"
         +"</wfs:GetFeature>";
        print(postAsDOM("wfs", xml));
    }

    public void testSLD() throws Exception {
        String sld = 
            "<StyledLayerDescriptor xmlns:ogc='http://www.opengis.net/ogc'>"+
            " <UserLayer> "+
            "  <Name>BasicPolygons</Name>" + 
            "  <UserStyle> "+
            "   <Name>UserSelection</Name> "+
            "   <FeatureTypeStyle> "+
            "    <Rule> "+
            " <PolygonSymbolizer>"+
          "    <Geometry>"+
          "       <ogc:Function name='myBuffer'>"+
          "         <ogc:PropertyName>the_geom</ogc:PropertyName>"+
          "       </ogc:Function>"+
          "    </Geometry>"+
          "    <Fill>"+
          "      <CssParameter name='fill'>#777777</CssParameter>"+
          "    </Fill>"+
          "    <Stroke>"+
          "     <CssParameter name='stroke'>#000000</CssParameter>"+
          "     <CssParameter name='stroke-width'>2</CssParameter>"+
          "   </Stroke>"+
          "  </PolygonSymbolizer>"+
            "    </Rule> "+
            "   </FeatureTypeStyle> "+
            "  </UserStyle> "+
            " </UserLayer> "+
            "</StyledLayerDescriptor>";

        MockHttpServletResponse response = getAsServletResponse("wms?request=GetMap&version=1.1.1" +
            "&bbox=-10,-10,10,10&format=image/png&&width=500&height=500&srs=EPSG:4326" +
            "&SLD_BODY=" + sld.replaceAll("=", "%3D"));
        
        assertEquals("image/png", response.getContentType());
        BufferedImage img = ImageIO.read(getBinaryInputStream(response));
        WMSTestSupport.showImage("test", img);
    }
}
