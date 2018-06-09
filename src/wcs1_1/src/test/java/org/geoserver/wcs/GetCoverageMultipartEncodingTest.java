/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.geoserver.data.test.MockData.TASMANIA_BM;
import static org.junit.Assert.*;

import java.io.IOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import javax.mail.BodyPart;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import org.geoserver.wcs.responses.GeoTIFFCoverageResponseDelegate;
import org.geoserver.wcs.test.WCSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

public class GetCoverageMultipartEncodingTest extends WCSTestSupport {

    // @Override
    // protected String getDefaultLogConfiguration() {
    // return "/DEFAULT_LOGGING.properties";
    // }

    @Test
    public void testKvpBasic() throws Exception {
        String request =
                "wcs?service=WCS&version=1.1.1&request=GetCoverage"
                        + "&identifier="
                        + getLayerId(TASMANIA_BM)
                        + "&BoundingBox=-90,-180,90,180,urn:ogc:def:crs:EPSG:4326"
                        + "&GridBaseCRS=urn:ogc:def:crs:EPSG:4326"
                        + "&format=geotiff";
        MockHttpServletResponse response = getAsServletResponse(request);
        // System.out.println(response.getOutputStreamContent());
        // make sure we got a multipart
        String contentType = response.getContentType();
        assertTrue(contentType.matches("multipart/related;\\s*boundary=\".*\""));
        // Tomcat 7 does not like to have newlines in the http headers, and it's right, they are not
        // allowed
        assertFalse(contentType.contains("\n"));
        assertFalse(contentType.contains("\r"));

        // parse the multipart, check there are two parts
        Multipart multipart = getMultipart(response);
        assertEquals(2, multipart.getCount());

        // now check the first part is a proper description
        BodyPart coveragesPart = multipart.getBodyPart(0);
        assertEquals("text/xml", coveragesPart.getContentType());
        // System.out.println("Coverages part: " + coveragesPart.getContent());
        assertEquals("<urn:ogc:wcs:1.1:coverages>", coveragesPart.getHeader("Content-ID")[0]);
        // read the xml document into a dom
        Document dom = dom(coveragesPart.getDataHandler().getInputStream());
        checkValidationErrors(dom, WCS11_SCHEMA);
        assertXpathEvaluatesTo(
                TASMANIA_BM.getLocalPart(), "wcs:Coverages/wcs:Coverage/ows:Title", dom);

        // the second part is the actual coverage
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals(
                GeoTIFFCoverageResponseDelegate.GEOTIFF_CONTENT_TYPE,
                coveragePart.getContentType());
        assertEquals("<theCoverage>", coveragePart.getHeader("Content-ID")[0]);
    }

    /**
     * ArcGrid cannot encode rotate coverages, yet due to a bug the output was a garbled mime
     * multipart instead of a service exception. This makes sure an exception is returned instead.
     */
    @Test
    public void testArcgridException() throws Exception {
        String request =
                "wcs?service=WCS&version=1.1.1&request=GetCoverage&identifier="
                        + getLayerId(TASMANIA_BM)
                        + "&format=application/arcgrid"
                        + "&boundingbox=-90,-180,90,180,urn:ogc:def:crs:EPSG:6.6:4326";
        Document dom = getAsDOM(request);
        checkOws11Exception(dom);
    }

    @Test
    public void testTiffOutput() throws Exception {
        String request =
                "wcs?service=WCS&version=1.1.1&request=GetCoverage"
                        + "&identifier="
                        + getLayerId(TASMANIA_BM)
                        + "&BoundingBox=-90,-180,90,180,urn:ogc:def:crs:EPSG:4326"
                        + "&GridBaseCRS=urn:ogc:def:crs:EPSG:4326"
                        + "&format=image/tiff";
        MockHttpServletResponse response = getAsServletResponse(request);

        // parse the multipart, check there are two parts
        Multipart multipart = getMultipart(response);
        assertEquals(2, multipart.getCount());
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("image/tiff", coveragePart.getContentType());
        assertEquals("<theCoverage>", coveragePart.getHeader("Content-ID")[0]);

        // make sure we can read the coverage back
        ImageReader reader = ImageIO.getImageReadersByFormatName("tiff").next();
        ImageInputStream iis = ImageIO.createImageInputStream(coveragePart.getInputStream());
        reader.setInput(iis);
        reader.read(0);
        iis.close();
    }

    @Test
    public void testPngOutput() throws Exception {
        String request =
                "wcs?service=WCS&version=1.1.1&request=GetCoverage"
                        + "&identifier="
                        + getLayerId(TASMANIA_BM)
                        + "&BoundingBox=-90,-180,90,180,urn:ogc:def:crs:EPSG:4326"
                        + "&GridBaseCRS=urn:ogc:def:crs:EPSG:4326"
                        + "&format=image/png";
        MockHttpServletResponse response = getAsServletResponse(request);

        // parse the multipart, check there are two parts
        Multipart multipart = getMultipart(response);
        assertEquals(2, multipart.getCount());
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals("image/png", coveragePart.getContentType());
        assertEquals("<theCoverage>", coveragePart.getHeader("Content-ID")[0]);

        // make sure we can read the coverage back
        ImageReader reader = ImageIO.getImageReadersByFormatName("png").next();
        reader.setInput(ImageIO.createImageInputStream(coveragePart.getInputStream()));
        reader.read(0);
    }

    @Test
    public void testGeotiffNamesGalore() throws Exception {
        String requestBase =
                "wcs?service=WCS&version=1.1.1&request=GetCoverage"
                        + "&identifier="
                        + getLayerId(TASMANIA_BM)
                        + "&BoundingBox=-90,-180,90,180,urn:ogc:def:crs:EPSG:4326"
                        + "&GridBaseCRS=urn:ogc:def:crs:EPSG:4326";
        ensureTiffFormat(getAsServletResponse(requestBase + "&format=geotiff"));
        ensureTiffFormat(getAsServletResponse(requestBase + "&format=image/geotiff"));
        ensureTiffFormat(getAsServletResponse(requestBase + "&format=image/tiff"));
    }

    private void ensureTiffFormat(MockHttpServletResponse response)
            throws MessagingException, IOException {
        // make sure we got a multipart
        assertTrue(
                "Content type not mulipart but " + response.getContentType(),
                response.getContentType().matches("multipart/related;\\s*boundary=\".*\""));

        // parse the multipart, check the second part is a geotiff
        Multipart multipart = getMultipart(response);
        BodyPart coveragePart = multipart.getBodyPart(1);
        assertEquals(
                GeoTIFFCoverageResponseDelegate.GEOTIFF_CONTENT_TYPE,
                coveragePart.getContentType());
    }
}
