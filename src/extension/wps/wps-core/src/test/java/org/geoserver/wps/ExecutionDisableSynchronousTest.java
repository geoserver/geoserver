/* (c) 2014-2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertTrue;

import java.net.URLEncoder;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class ExecutionDisableSynchronousTest extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // need no layers for this test
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        WPSInfo wps = getGeoServer().getService(WPSInfo.class);

        // disable synchronous
        wps.setSynchronousDisabled(true);

        // save
        getGeoServer().save(wps);
    }

    @Test
    public void testSyncExecutionLimits() throws Exception {

        // submit synch request
        String request = "wps?service=WPS&version=1.0.0&request=Execute&Identifier=gs:Monkey&DataInputs="
                + urlEncode("id=x2");

        Document dom = getAsDOM(request);
        // print(dom, System.out);

        XMLAssert.assertXpathExists("//ows:ExceptionReport/ows:Exception", dom);

        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate(
                        "//ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);

        assertTrue(message.contains("Synchronous WPS requests are disabled"));
    }

    String urlEncode(String string) throws Exception {
        return URLEncoder.encode(string, "ASCII");
    }
}

