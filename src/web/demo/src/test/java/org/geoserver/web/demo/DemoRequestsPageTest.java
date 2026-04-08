/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang3.SerializationUtils;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.platform.resource.Files;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.test.TestData;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Gabriel Roldan
 * @verion $Id$
 */
public class DemoRequestsPageTest extends GeoServerWicketTestSupport {

    private File demoDir;

    @Before
    public void setUp() throws Exception {
        demoDir = TestData.file(this, "demo-requests");
        tester.startPage(new DemoRequestsPage(Files.asResource(demoDir)));
    }

    /** Kind of smoke test to make sure the page structure was correctly set up once loaded */
    @Test
    public void testStructure() {
        // print(tester.getLastRenderedPage(), true, true);

        assertTrue(tester.getLastRenderedPage() instanceof DemoRequestsPage);

        tester.assertComponent("demoRequestsForm", Form.class);
        tester.assertComponent("demoRequestsForm:demoRequestsList", DropDownChoice.class);
        tester.assertComponent("demoRequestsForm:url", TextField.class);
        tester.assertComponent("demoRequestsForm:body:editorContainer:editorParent:editor", TextArea.class);
        tester.assertComponent("demoRequestsForm:username", TextField.class);
        tester.assertComponent("demoRequestsForm:password", PasswordTextField.class);
    }

    @Test
    public void testPrettyXML() {
        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");

        requestFormTester.select("demoRequestsList", 0);
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        DemoRequest model =
                (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();
        assertTrue(model.isPrettyXML());

        requestFormTester.setValue("prettyXML", false);

        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        model = (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();
        assertFalse(model.isPrettyXML());
    }

    @Test
    public void testOpenNewPage() {
        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");
        requestFormTester.select("demoRequestsList", 0);
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        DemoRequest model =
                (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();
        assertFalse(model.isOpenNewWindow());

        requestFormTester.setValue("openNewWindow", true);

        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        model = (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();
        assertTrue(model.isOpenNewWindow());
    }

    @Test
    public void testPageParams() {
        PageParameters parameters = new PageParameters();
        parameters.add("url", "myurl");
        parameters.add("xml", "myxml");

        tester.startPage(new DemoRequestsPage(Files.asResource(demoDir), parameters));
        DemoRequest model =
                (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();

        assertEquals("myurl", model.getRequestUrl());
        assertEquals("myxml", model.getRequestBody());
    }

    @Test
    public void testAuth() {
        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");

        requestFormTester.setValue("username", "UserName");
        requestFormTester.setValue("password", "PassWord");

        requestFormTester.select("demoRequestsList", 0);
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        DemoRequest model =
                (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();
        assertEquals("UserName", model.getUserName());
        assertEquals("PassWord", model.getPassword());

        requestFormTester.setValue("username", "UserName2");
        requestFormTester.setValue("password", "PassWord2");

        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        model = (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();
        assertEquals("UserName2", model.getUserName());
        assertEquals("PassWord2", model.getPassword());
    }

    @Test
    public void testDropDownChanged() {
        final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");
        requestFormTester.select("demoRequestsList", 0);
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");

        DemoRequest model =
                (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();

        assertEquals("WFS_getFeature-1.1.xml", model.getRequestFileName());
        assertEquals("http://localhost/context/wfs", model.getRequestUrl());
        assertTrue(model.getRequestBody().startsWith("<!--"));

        requestFormTester.select("demoRequestsList", 1);
        tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");
        model = (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();

        assertEquals("WMS_describeLayer.url", model.getRequestFileName());
        assertEquals(
                "http://localhost/context/wms?request=DescribeLayer&version=1.1.1&layers=topp:states\n",
                model.getRequestUrl());
        assertTrue(model.getRequestBody() == null || model.getRequestBody().isEmpty());
    }

    @Test
    public void testDemoListLoaded() {
        // print(tester.getLastRenderedPage(), true, true);

        /*
         * Expected choices are the file names in the demo requests dir
         * (/src/test/resources/test-data/demo-requests in this case)
         */
        final List<String> expectedList =
                Arrays.asList(new String[] {"WFS_getFeature-1.1.xml", "WMS_describeLayer.url"});

        DropDownChoice dropDown =
                (DropDownChoice) tester.getComponentFromLastRenderedPage("demoRequestsForm:demoRequestsList");
        List choices = dropDown.getChoices();
        assertEquals(expectedList, choices);
    }

    @Test
    public void testProxyBaseUrl() {
        // setup the proxy base url
        GeoServerInfo global = getGeoServer().getGlobal();
        String proxyBaseUrl = "http://www.geoserver.org/test_gs";
        global.getSettings().setProxyBaseUrl(proxyBaseUrl);
        try {
            getGeoServer().save(global);

            final FormTester requestFormTester = tester.newFormTester("demoRequestsForm");
            final String requestName = "WMS_describeLayer.url";
            requestFormTester.select("demoRequestsList", 1);

            /*
             * There's an AjaxFormSubmitBehavior attached to onchange so force it
             */
            tester.executeAjaxEvent("demoRequestsForm:demoRequestsList", "change");
            tester.assertModelValue("demoRequestsForm:demoRequestsList", requestName);

            DemoRequest model =
                    (DemoRequest) tester.getLastRenderedPage().getDefaultModel().getObject();

            String requestFileName = model.getRequestFileName();
            String requestUrl = model.getRequestUrl();

            assertEquals(requestName, requestFileName);
            assertTrue(requestUrl.startsWith(proxyBaseUrl + "/wms"));
        } finally {
            global.getSettings().setProxyBaseUrl(null);
            getGeoServer().save(global);
        }
    }

    @Test
    public void testSerializable() {
        DemoRequestsPage page = new DemoRequestsPage();
        DemoRequestsPage page2 = (DemoRequestsPage) SerializationUtils.deserialize(SerializationUtils.serialize(page));
        assertEquals(page.demoDir, page2.demoDir);
    }
}
