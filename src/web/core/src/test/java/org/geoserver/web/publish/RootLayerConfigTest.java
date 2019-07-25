/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.publish;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.Component;
import org.apache.wicket.feedback.ErrorLevelFeedbackMessageFilter;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Before;
import org.junit.Test;

public class RootLayerConfigTest extends GeoServerWicketTestSupport {
    LayerInfo polygons;
    FormTestPage page;

    @Before
    public void init() {
        polygons = getCatalog().getLayerByName(MockData.BASIC_POLYGONS.getLocalPart());
        page =
                new FormTestPage(
                        new ComponentBuilder() {

                            public Component buildComponent(String id) {
                                return new RootLayerConfig(id, new Model(polygons));
                            }
                        });
        tester.startPage(page);
    }

    @Test
    public void testRootLayerRemoveWMSGlobal() {
        tester.assertRenderedPage(FormTestPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.select("panel:rootLayer", 0);
        ft.submit();
        assertEquals(
                0,
                page.getSession()
                        .getFeedbackMessages()
                        .messages(new ErrorLevelFeedbackMessageFilter(FeedbackMessage.ERROR))
                        .size());
        assertNull(polygons.getMetadata().get(PublishedInfo.ROOT_IN_CAPABILITIES, Boolean.class));
    }

    @Test
    public void testRootLayerRemoveYes() {
        tester.assertRenderedPage(FormTestPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.select("panel:rootLayer", 1);
        ft.submit();
        assertEquals(
                0,
                page.getSession()
                        .getFeedbackMessages()
                        .messages(new ErrorLevelFeedbackMessageFilter(FeedbackMessage.ERROR))
                        .size());
        assertTrue(polygons.getMetadata().get(PublishedInfo.ROOT_IN_CAPABILITIES, Boolean.class));
    }

    @Test
    public void testRootLayerRemoveNo() {
        tester.assertRenderedPage(FormTestPage.class);
        FormTester ft = tester.newFormTester("form");
        ft.select("panel:rootLayer", 2);
        ft.submit();
        assertEquals(
                0,
                page.getSession()
                        .getFeedbackMessages()
                        .messages(new ErrorLevelFeedbackMessageFilter(FeedbackMessage.ERROR))
                        .size());
        assertFalse(polygons.getMetadata().get(PublishedInfo.ROOT_IN_CAPABILITIES, Boolean.class));
    }
}
