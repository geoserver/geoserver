/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.net.URLEncoder;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

public class ExecutionLimitsTest extends WPSTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // need no layers for this test
    }

    @Before
    public void setUpInternal() throws Exception {
        // make extra sure we don't have anything else going
        MonkeyProcess.clearCommands();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addWcs11Coverages(testData);

        // add some limits to the processes
        WPSInfo wps = getGeoServer().getService(WPSInfo.class);

        // global size limits
        wps.setMaxComplexInputSize(10);

        // max execution times
        wps.setMaxSynchronousExecutionTime(1);
        wps.setMaxAsynchronousExecutionTime(2);

        // save
        getGeoServer().save(wps);
    }

    @Test
    public void testAsyncExecutionLimits() throws Exception {
        // submit asynch request with no updates
        String request = "wps?service=WPS&version=1.0.0&request=Execute&Identifier=gs:Monkey&storeExecuteResponse=true&DataInputs="
                + urlEncode("id=x2");
        Document dom = getAsDOM(request);
        assertXpathExists("//wps:ProcessAccepted", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String fullStatusLocation = xpath.evaluate("//wps:ExecuteResponse/@statusLocation", dom);
        String statusLocation = fullStatusLocation.substring(fullStatusLocation.indexOf('?') - 3);

        // wait for more than the limit for asynch execution
        Thread.sleep(3000);

        // schedule an update that will make it fail
        MonkeyProcess.progress("x2", 50f, true);

        // make it end
        dom = waitForProcessEnd(statusLocation, 10);
        // print(dom);
        XMLAssert.assertXpathExists("//wps:Status/wps:ProcessFailed", dom);
        String message = xpath.evaluate(
                        "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);

        assertTrue(message.contains("went beyond the configured limits"));
        assertTrue(message.contains("maxExecutionTime 2 seconds"));
    }

    @Test
    public void testSyncExecutionLimits() throws Exception {
        // setup the set of commands for the monkey process
        MonkeyProcess.wait("x2", 2000);
        MonkeyProcess.progress("x2", 50f, false);

        // submit synch request
        String request = "wps?service=WPS&version=1.0.0&request=Execute&Identifier=gs:Monkey&DataInputs="
                + urlEncode("id=x2");
        Document dom = getAsDOM(request);
        // print(dom);
        XMLAssert.assertXpathExists("//wps:Status/wps:ProcessFailed", dom);
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String message = xpath.evaluate(
                        "//wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                        dom);

        assertTrue(message.contains("went beyond the configured limits"));
        assertTrue(message.contains("maxExecutionTime 1 seconds"));
    }

    String urlEncode(String string) throws Exception {
        return URLEncoder.encode(string, "ASCII");
    }

}
