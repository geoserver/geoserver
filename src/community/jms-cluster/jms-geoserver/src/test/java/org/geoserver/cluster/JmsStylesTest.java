/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.cluster.impl.handlers.DocumentFile;
import org.geoserver.cluster.impl.handlers.catalog.JMSCatalogAddEventHandlerSPI;
import org.geoserver.cluster.impl.handlers.catalog.JMSCatalogStylesFileHandlerSPI;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import javax.jms.Message;
import java.util.List;

import static org.geoserver.cluster.JmsEventsListener.getMessagesForHandler;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

/**
 * Tests related with styles events.
 */
public final class JmsStylesTest extends GeoServerSystemTestSupport {

    private static final String TEST_STYLE_NAME = "test_style";
    private static final String TEST_STYLE_FILE = "/test_style.sld";

    private static final String CATALOG_ADD_EVENT_HANDLER_KEY = "JMSCatalogAddEventHandlerSPI";
    private static final String CATALOG_STYLES_FILE_EVENT_HANDLER_KEY = "JMSCatalogStylesFileHandlerSPI";

    private static JMSEventHandler<String, DocumentFile> styleFileHandler;
    private static JMSEventHandler<String, CatalogEvent> addEventHandler;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        // adding our test spring context
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Before
    public void beforeTest() {
        // initiate the handlers related to styles
        styleFileHandler = GeoServerExtensions.bean(JMSCatalogStylesFileHandlerSPI.class).createHandler();
        addEventHandler = GeoServerExtensions.bean(JMSCatalogAddEventHandlerSPI.class).createHandler();
    }

    @After
    public void afterTest() {
        // search the test style in the catalog
        Catalog catalog = getCatalog();
        StyleInfo style = catalog.getStyleByName(TEST_STYLE_NAME);
        if (style != null) {
            // the test style exists so let's remove it
            catalog.remove(style);
        }
        // clear all pending events
        JmsEventsListener.clear();
    }

    @Test
    public void testAddStyle() throws Exception {
        // add the test to the style catalog
        getTestData().addStyle(TEST_STYLE_NAME, TEST_STYLE_FILE, this.getClass(), getCatalog());
        // waiting for a catalog add event and a style file event
        List<Message> messages = JmsEventsListener.getMessagesByHandlerKey(5000,
                (selected) -> selected.size() >= 2,
                CATALOG_ADD_EVENT_HANDLER_KEY, CATALOG_STYLES_FILE_EVENT_HANDLER_KEY);
        // let's check if the new added style was correctly published
        assertThat(messages.size(), is(2));
        // checking that the correct style file was published
        List<DocumentFile> styleFile = getMessagesForHandler(messages, CATALOG_STYLES_FILE_EVENT_HANDLER_KEY, styleFileHandler);
        assertThat(styleFile.size(), is(1));
        assertThat(styleFile.get(0).getResourceName(), is("test_style.sld"));
        // checking that the correct catalog style was published
        List<CatalogEvent> styleAddEvent = getMessagesForHandler(messages, CATALOG_ADD_EVENT_HANDLER_KEY, addEventHandler);
        assertThat(styleAddEvent.size(), is(1));
        assertThat(styleAddEvent.get(0).getSource(), instanceOf(StyleInfo.class));
        StyleInfo style = (StyleInfo) styleAddEvent.get(0).getSource();
        assertThat(style.getName(), is(TEST_STYLE_NAME));
    }
}
