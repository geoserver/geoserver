/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import static org.junit.Assert.*;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.ErrorLevelFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class HTTPLayerConfigTest extends GeoServerWicketTestSupport {

    LayerInfo polygons;
    FormTestPage page;

    @Before
    public void init() {
        polygons = getCatalog().getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new HTTPLayerConfig(id, new Model(polygons));
                            }
                        });
        tester.startPage(page);
    }

    @Test
    public void testDefaults() {
        tester.assertRenderedPage(FormTestPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.submit();
        assertEquals(
                0,
                page.getSession()
                        .getFeedbackMessages()
                        .messages(new ErrorLevelFeedbackMessageFilter(FeedbackMessage.ERROR))
                        .size());
    }

    @Test
    public void testInvalid() {
        final LayerInfo polygons =
                getCatalog().getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        FormTestPage page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new HTTPLayerConfig(id, new Model(polygons));
                            }
                        });
        tester.startPage(page);
        tester.assertComponent("form:panel:cacheAgeMax", TextField.class);

        tester.assertRenderedPage(FormTestPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:cacheAgeMax", "-20");
        ft.submit();
        tester.assertErrorMessages(
                "The value of 'cacheAgeMax' must be between 0 and 9223372036854775807.");
    }

    @Test
    public void testValid() {
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:cacheAgeMax", "3600");
        ft.submit();
        // System.out.println(page.getSession().getFeedbackMessages());
        assertEquals(
                0,
                page.getSession()
                        .getFeedbackMessages()
                        .messages(new ErrorLevelFeedbackMessageFilter(FeedbackMessage.ERROR))
                        .size());
        // System.out.println(polygons.getResource().getMetadata().get("cacheAgeMax").getClass());
        assertEquals(
                Integer.valueOf(3600),
                polygons.getResource().getMetadata().get("cacheAgeMax", Integer.class));
    }
}
