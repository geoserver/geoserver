/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import javax.imageio.ImageIO;
import org.apache.commons.io.IOUtils;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class SLDWithInlineFeatureTest extends GeoServerSystemTestSupport {

    @Test
    public void testSLDWithInlineFeatureWMS() throws Exception {
        BufferedReader reader =
                new BufferedReader(
                        new InputStreamReader(
                                getClass().getResourceAsStream("SLDWithInlineFeature.xml")));
        String line;
        StringBuilder builder = new StringBuilder();

        while ((line = reader.readLine()) != null) {
            builder.append(line);
        }

        assertStatusCodeForPost(200, "wms", builder.toString(), "text/xml");

        // this is the test; an exception will be thrown if no image was rendered
        BufferedImage image =
                ImageIO.read(
                        getBinaryInputStream(postAsServletResponse("wms", builder.toString())));

        assertNotNull(image);
    }

    @Test
    public void testGetMapPostEntityExpansion() throws Exception {
        String body =
                IOUtils.toString(
                        getClass().getResourceAsStream("GetMapExternalEntity.xml"), "UTF-8");
        MockHttpServletResponse response = postAsServletResponse("wms", body);
        // should fail with an error message pointing at entity resolution
        assertEquals("application/vnd.ogc.se_xml", response.getContentType());
        final String content = response.getContentAsString();
        assertThat(content, containsString("Entity resolution disallowed"));
        assertThat(content, containsString("/this/file/does/not/exist"));
    }

    @Test
    public void testSLDBody() throws Exception {
        String request =
                "wms?FORMAT=image/png&TRANSPARENT=TRUE&HEIGHT=406&WIDTH=810&REQUEST=GetMap&SRS=EPSG:4326&VERSION=1.1.1&BBOX=-120,-120,120,120&SLD_BODY=%3C%3Fxml%20version%3D%221.0%22%20encoding%3D%22UTF-8%22%3F%3E%3CStyledLayerDescriptor%20version%3D%221.0.0%22%20xmlns%3Agml%3D%22http%3A%2F%2Fwww.opengis.net%2Fgml%22%20xmlns%3Aogc%3D%22http%3A%2F%2Fwww.opengis.net%2Fogc%22%20xmlns%3D%22http%3A%2F%2Fwww.opengis.net%2Fsld%22%3E%3CUserLayer%3E%3CName%3Ejunk%3C%2FName%3E%3CInlineFeature%3E%3CFeatureCollection%3E%3CfeatureMember%3E%3CBodyPart%3E%3CType%3EFace%3C%2FType%3E%3CpolygonProperty%3E%3Cgml%3APolygon%3E%3Cgml%3AouterBoundaryIs%3E%3Cgml%3ALinearRing%3E%3Cgml%3Acoordinates%3E-10%2C10%2010%2C10%2010%2C-10%20-10%2C-10%20-10%2C10%3C%2Fgml%3Acoordinates%3E%3C%2Fgml%3ALinearRing%3E%3C%2Fgml%3AouterBoundaryIs%3E%3C%2Fgml%3APolygon%3E%3C%2FpolygonProperty%3E%3C%2FBodyPart%3E%3C%2FfeatureMember%3E%3C%2FFeatureCollection%3E%3C%2FInlineFeature%3E%3CLayerFeatureConstraints%3E%3CFeatureTypeConstraint%3E%3C%2FFeatureTypeConstraint%3E%3C%2FLayerFeatureConstraints%3E%3CUserStyle%3E%3CFeatureTypeStyle%3E%3CRule%3E%3CPolygonSymbolizer%3E%3CFill%3E%3CCssParameter%20name%3D%22fill%22%3E%3Cogc%3ALiteral%3E%23F00620%3C%2Fogc%3ALiteral%3E%3C%2FCssParameter%3E%3CCssParameter%20name%3D%22fill-opacity%22%3E%3Cogc%3ALiteral%3E1.0%3C%2Fogc%3ALiteral%3E%3C%2FCssParameter%3E%3C%2FFill%3E%3CStroke%3E%3CCssParameter%20name%3D%22stroke%22%3E%3Cogc%3ALiteral%3E%23FF0000%3C%2Fogc%3ALiteral%3E%3C%2FCssParameter%3E%3C%2FStroke%3E%3C%2FPolygonSymbolizer%3E%3C%2FRule%3E%3C%2FFeatureTypeStyle%3E%3C%2FUserStyle%3E%3C%2FUserLayer%3E%3C%2FStyledLayerDescriptor%3E";
        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals("image/png", response.getContentType());

        // this is the test; an exception will be thrown if no image was rendered
        BufferedImage image = ImageIO.read(getBinaryInputStream(getAsServletResponse(request)));

        assertNotNull(image);
    }
}
