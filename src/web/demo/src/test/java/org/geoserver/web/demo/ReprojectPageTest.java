/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.ParamResourceModel;
import org.junit.Test;

public class ReprojectPageTest extends GeoServerWicketTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // we don't need data configured in the catalog
        testData.setUpSecurity();
    }

    @Test
    public void testReprojectPoint() {
        tester.startPage(ReprojectPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("sourceCRS:srs", "EPSG:4326");
        form.setValue("targetCRS:srs", "EPSG:32632");
        form.setValue("sourceGeom", "12 45");
        form.submit();
        tester.clickLink("form:forward", true);

        assertEquals(ReprojectPage.class, tester.getLastRenderedPage().getClass());
        assertEquals(0, tester.getMessages(FeedbackMessage.ERROR).size());
        String tx =
                tester.getComponentFromLastRenderedPage("form:targetGeom")
                        .getDefaultModelObjectAsString();
        assertEquals("736446.0261038465 4987329.504699742", tx);
    }

    @Test
    public void testInvalidPoint() {
        tester.startPage(ReprojectPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("sourceCRS:srs", "EPSG:4326");
        form.setValue("targetCRS:srs", "EPSG:32632");
        form.setValue("sourceGeom", "12 a45a");
        form.submit();
        tester.clickLink("form:forward", true);

        assertEquals(ReprojectPage.class, tester.getLastRenderedPage().getClass());
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        String message =
                ((ValidationErrorFeedback) tester.getMessages(FeedbackMessage.ERROR).get(0))
                        .getMessage()
                        .toString();
        String expected = new ParamResourceModel("GeometryTextArea.parseError", null).getString();
        assertEquals(expected, message);
    }

    @Test
    public void testReprojectLinestring() {
        tester.startPage(ReprojectPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("sourceCRS:srs", "EPSG:4326");
        form.setValue("targetCRS:srs", "EPSG:32632");
        form.setValue("sourceGeom", "LINESTRING(12 45, 13 45)");
        form.submit();
        tester.clickLink("form:forward", true);

        assertEquals(ReprojectPage.class, tester.getLastRenderedPage().getClass());
        assertEquals(0, tester.getMessages(FeedbackMessage.ERROR).size());
        String tx =
                tester.getComponentFromLastRenderedPage("form:targetGeom")
                        .getDefaultModelObjectAsString();
        assertEquals(
                "LINESTRING (736446.0261038465 4987329.504699742, 815261.4271666661 4990738.261612577)",
                tx);
    }

    @Test
    public void testInvalidGeometry() {
        tester.startPage(ReprojectPage.class);
        FormTester form = tester.newFormTester("form");
        form.setValue("sourceCRS:srs", "EPSG:4326");
        form.setValue("targetCRS:srs", "EPSG:32632");
        form.setValue("sourceGeom", "LINESTRING(12 45, 13 45"); // missing ) at the end
        form.submit();
        tester.clickLink("form:forward", true);

        assertEquals(ReprojectPage.class, tester.getLastRenderedPage().getClass());
        assertEquals(1, tester.getMessages(FeedbackMessage.ERROR).size());
        String message =
                ((ValidationErrorFeedback) tester.getMessages(FeedbackMessage.ERROR).get(0))
                        .getMessage()
                        .toString();
        String expected = new ParamResourceModel("GeometryTextArea.parseError", null).getString();
        assertEquals(expected, message);
    }

    @Test
    public void testPageParams() {
        tester.startPage(
                ReprojectPage.class,
                new PageParameters().add("fromSRS", "EPSG:4326").add("toSRS", "EPSG:32632"));
        String source =
                tester.getComponentFromLastRenderedPage("form:sourceCRS:srs")
                        .getDefaultModelObjectAsString();
        String target =
                tester.getComponentFromLastRenderedPage("form:targetCRS:srs")
                        .getDefaultModelObjectAsString();
        assertEquals("EPSG:4326", source);
        assertEquals("EPSG:32632", target);
    }
}
