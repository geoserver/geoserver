/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.script.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.script.ScriptIntTestSupport;
import org.w3c.dom.Document;

public abstract class ScriptProcessIntTest extends ScriptIntTestSupport {

    @Override
    protected void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        File script = new File(getScriptManager().wps().dir(), "map." + getExtension());
        FileUtils.copyURLToFile(getClass().getResource(script.getName()), script);
    }

    protected abstract String getExtension();

    public void testMapResult() throws Exception {
        String ext = getExtension();
        String xml =
                "<wps:Execute service='WPS' version='1.0.0' xmlns:wps='http://www.opengis.net/wps/1.0.0' "
                        + "xmlns:ows='http://www.opengis.net/ows/1.1'>"
                        + "<ows:Identifier>"
                        + ext
                        + ":map</ows:Identifier>"
                        + "<wps:DataInputs>"
                        + "</wps:DataInputs>"
                        + "<wps:ResponseForm>"
                        + "<wps:RawDataOutput mimeType=\"application/xml\">"
                        + "<ows:Identifier>result</ows:Identifier>"
                        + "</wps:RawDataOutput>"
                        + "</wps:ResponseForm>"
                        + "</wps:Execute>";

        Document doc = postAsDOM("wps", xml);
        // print(doc);
        assertEquals("map", doc.getDocumentElement().getLocalName());

        assertXpathEvaluatesTo("widget", "/map/name", doc);
        assertXpathEvaluatesTo("12.99", "/map/price", doc);
    }
}
