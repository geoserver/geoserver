/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import static org.geoserver.cluster.JmsEventsListener.getMessagesForHandler;
import static org.hamcrest.CoreMatchers.anyOf;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.Arrays;
import java.util.List;
import javax.jms.Message;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.cluster.impl.handlers.catalog.CatalogUtils;
import org.geoserver.cluster.impl.handlers.catalog.JMSCatalogAddEventHandlerSPI;
import org.geoserver.data.test.MockData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.feature.NameImpl;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Tests related with layer groups events. */
public final class JmsLayerGroupsTest extends GeoServerSystemTestSupport {

    private static final String TEST_LAYER_GROUP_NAME = "test_layer_group";

    private static final String CATALOG_ADD_EVENT_HANDLER_KEY = "JMSCatalogAddEventHandlerSPI";

    private static JMSEventHandler<String, CatalogEvent> addEventHandler;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        // adding our test spring context
        springContextLocations.add("classpath:TestContext.xml");
    }

    @Before
    public void beforeTest() {
        // initiate the catalog add event handler
        addEventHandler =
                GeoServerExtensions.bean(JMSCatalogAddEventHandlerSPI.class).createHandler();
    }

    @After
    public void afterTest() {
        // remove the test layer group
        removeTestLayerGroup();
        // clear all pending events
        JmsEventsListener.clear();
    }

    @Test
    public void testAddLayerGroup() throws Exception {
        // create the layer group
        createTetLayerGroup();
        // wait for a catalog add event
        List<Message> messages =
                JmsEventsListener.getMessagesByHandlerKey(
                        5000, (selected) -> selected.size() >= 2, CATALOG_ADD_EVENT_HANDLER_KEY);
        // remove the test layer group to force a complete deserialization
        removeTestLayerGroup();
        // let's see if we got the correct event
        assertThat(messages.size(), is(1));
        List<CatalogEvent> layerGroupAddEvent =
                getMessagesForHandler(messages, CATALOG_ADD_EVENT_HANDLER_KEY, addEventHandler);
        assertThat(layerGroupAddEvent.size(), is(1));
        assertThat(layerGroupAddEvent.get(0).getSource(), instanceOf(LayerGroupInfo.class));
        LayerGroupInfo layerGroup = (LayerGroupInfo) layerGroupAddEvent.get(0).getSource();
        CatalogUtils.localizeLayerGroup(layerGroup, getCatalog());
        // checking the published layer group
        assertThat(layerGroup.getName(), is(TEST_LAYER_GROUP_NAME));
        List<PublishedInfo> content = layerGroup.getLayers();
        assertThat(content.size(), is(2));
        // checking that the layer group contains the correct layers
        for (PublishedInfo item : content) {
            assertThat(item, instanceOf(LayerInfo.class));
            LayerInfo layer = (LayerInfo) item;
            assertThat(
                    layer.getName(),
                    anyOf(
                            is(MockData.ROAD_SEGMENTS.getLocalPart()),
                            is(MockData.BRIDGES.getLocalPart())));
            FeatureTypeInfo resource = (FeatureTypeInfo) layer.getResource();
            // check that the transient catalog variable has initiated properly
            assertThat(resource.getStore().getCatalog(), notNullValue());
        }
    }

    private void removeTestLayerGroup() {
        // search the test layer group in the catalog
        Catalog catalog = getCatalog();
        LayerGroupInfo layerGroup = catalog.getLayerGroupByName(TEST_LAYER_GROUP_NAME);
        if (layerGroup != null) {
            // the test layer group exists so let's remove it
            catalog.remove(layerGroup);
        }
    }

    private void createTetLayerGroup() {
        Catalog catalog = getCatalog();
        // preparing the list of layers
        LayerInfo roads = catalog.getLayerByName(new NameImpl(MockData.ROAD_SEGMENTS));
        LayerInfo bridges = catalog.getLayerByName(new NameImpl(MockData.BRIDGES));
        List<PublishedInfo> layers = Arrays.asList(roads, bridges);
        // creating the layer group
        LayerGroupInfoImpl layerGroup = new LayerGroupInfoImpl();
        layerGroup.setId(TEST_LAYER_GROUP_NAME);
        layerGroup.setName(TEST_LAYER_GROUP_NAME);
        layerGroup.setLayers(layers);
        // adding a style, jms catalog handler expect the layer group to have styles
        layerGroup.setStyles(Arrays.asList(roads.getDefaultStyle(), bridges.getDefaultStyle()));
        // save the layer group
        catalog.add(layerGroup);
    }
}
