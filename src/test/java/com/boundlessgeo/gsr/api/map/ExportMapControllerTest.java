package com.boundlessgeo.gsr.api.map;

import static org.junit.Assert.assertTrue;

import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.File;

import javax.imageio.ImageIO;

import org.geoserver.data.test.SystemTestData;
import org.geotools.image.test.ImageAssert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.boundlessgeo.gsr.controller.ControllerTest;

/**
 * Basic tests for export map
 */
public class ExportMapControllerTest extends ControllerTest {

    @Ignore
    @Test
    public void exportMap() throws Exception {
        String exportMapUrl = getBaseURL() + SystemTestData.BASIC_POLYGONS.getPrefix() + "/MapServer/export?f=image&bbox=-180.0,-90.0,180.0,90.0&layers=show:" + SystemTestData.BASIC_POLYGONS
            .getLocalPart() + "&size=150,150&format=png";
        MockHttpServletResponse servletResponse = getAsServletResponse(exportMapUrl);
        RenderedImage image = ImageIO.read(new ByteArrayInputStream(servletResponse.getContentAsByteArray()));
        File resultsFile = new File("src/test/resources/images/export_result1.png");
        ImageAssert.assertEquals(resultsFile, image, 20);
    }

    @Test
    public void exportMapSpecificLayer() throws Exception {
        String exportMapUrl = getBaseURL() + SystemTestData.BASIC_POLYGONS.getPrefix()
            + "/MapServer/0/export?f=image&bbox=-180.0,-90.0,180.0,90.0&layers&size=150,150&format=png";
        MockHttpServletResponse servletResponse = getAsServletResponse(exportMapUrl);
        System.out.println(servletResponse.getErrorMessage());
        assertTrue("Response code must be good: " + servletResponse.getStatus(),
            servletResponse.getStatus() >= 200 && servletResponse.getStatus() < 300);
        RenderedImage image = ImageIO.read(new ByteArrayInputStream(servletResponse.getContentAsByteArray()));
        File resultsFile = new File("src/test/resources/images/export_result1.png");
        ImageAssert.assertEquals(resultsFile, image, 20);
    }

    @Test
    public void exportMapJSON() throws Exception {
        String exportMapUrl = getBaseURL() + SystemTestData.BASIC_POLYGONS.getPrefix()
            + "/MapServer/export?f=json&bbox=-180.0,-90.0,180.0,90.0&layers=show:" + SystemTestData.BASIC_POLYGONS
            .getLocalPart() + "&size=150,150";

        MockHttpServletRequest request = createRequest(exportMapUrl);
        request.setMethod( "GET" );
        request.setContent(new byte[]{});
        request.addHeader("Accept", "application/json");
        MockHttpServletResponse servletResponse = dispatch(request, null);

        assertTrue(servletResponse.getContentAsString().contains("f=image")
            && servletResponse.getContentAsString().contains("format=png"));
    }

}