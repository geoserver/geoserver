/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfsfreemarker.html;

import static org.junit.Assert.assertEquals;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Test for checking WFS GetFeature HTML templating */
public class GetFeatureOutputFormatTest extends WFSTestSupport {

    @Test
    public void testGetFeatureResponseMimeType() throws Exception {
        String request =
                "wfs?version=1.1.0&request=GetFeature&typeName=cite:Forests&outputFormat=text/html";

        MockHttpServletResponse response = getAsServletResponse(request);

        assertEquals("text/html", getBaseMimeType(response.getContentType()));
    }

    @Test
    public void testGetFeatureHtmlDefaultTemplate() throws Exception {
        String request =
                "wfs?version=1.1.0&request=GetFeature&typeName=cite:Forests&outputFormat=text/html";

        Document dom = getAsDOM(request);

        XMLAssert.assertXpathExists("/html/body/table", dom);
        XMLAssert.assertXpathEvaluatesTo("Forests", "/html/body/table/caption", dom);
        XMLAssert.assertXpathEvaluatesTo("2", "count(/html/body/table/tr)", dom);
        XMLAssert.assertXpathEvaluatesTo("Green Forest", "/html/body/table/tr[2]/td[3]", dom);
    }
}
