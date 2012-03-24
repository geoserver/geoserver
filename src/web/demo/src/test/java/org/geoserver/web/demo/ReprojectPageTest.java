package org.geoserver.web.demo;

import java.io.Serializable;

import org.apache.wicket.PageParameters;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.ValidationErrorFeedback;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.ParamResourceModel;

public class ReprojectPageTest extends GeoServerWicketTestSupport {

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        // we don't need data configured in the catalog
    }

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
        String tx = tester.getComponentFromLastRenderedPage("form:targetGeom").getDefaultModelObjectAsString();
        assertEquals("736446.0261038465 4987329.504699742", tx);
    }
    
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
        String message = ((ValidationErrorFeedback) tester.getMessages(FeedbackMessage.ERROR).get(0)).getMessage();
        String expected = new ParamResourceModel("GeometryTextArea.parseError", null).getString();
        assertEquals(expected, message);
    }
    
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
        String tx = tester.getComponentFromLastRenderedPage("form:targetGeom").getDefaultModelObjectAsString();
        assertEquals("LINESTRING (736446.0261038465 4987329.504699742, 815261.4271666661 4990738.261612577)", tx);
    }
    
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
        String message = ((ValidationErrorFeedback) tester.getMessages(FeedbackMessage.ERROR).get(0)).getMessage();
        String expected = new ParamResourceModel("GeometryTextArea.parseError", null).getString();
        assertEquals(expected, message);
    }
    
    public void testPageParams() {
        tester.startPage(ReprojectPage.class, new PageParameters("fromSRS=EPSG:4326,toSRS=EPSG:32632"));
        String source = tester.getComponentFromLastRenderedPage("form:sourceCRS:srs").getDefaultModelObjectAsString();
        String target = tester.getComponentFromLastRenderedPage("form:targetCRS:srs").getDefaultModelObjectAsString();
        assertEquals("EPSG:4326", source);
        assertEquals("EPSG:32632", target);
    }
    
    
    
}
