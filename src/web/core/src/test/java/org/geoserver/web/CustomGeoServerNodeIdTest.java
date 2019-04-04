/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.List;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.model.Model;
import org.geoserver.GeoServerNodeData;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class CustomGeoServerNodeIdTest extends GeoServerWicketTestSupport {

    @BeforeClass
    @AfterClass
    public static void cleanupNodeInfo() {
        GeoServerBasePage.NODE_INFO = null;
    }

    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/custom-gs-node-id-ctx.xml");
    }

    @Test
    public void testNodeInfoInvisible() throws Exception {
        CustomNodeInfo.ID = null;
        DefaultGeoServerNodeInfo.initializeFromEnviroment();

        tester.startPage(GeoServerHomePage.class);
        tester.assertInvisible("nodeIdContainer");
    }

    @Test
    public void testNodeInfoVisible() throws Exception {
        CustomNodeInfo.ID = "testId";

        tester.startPage(GeoServerHomePage.class);
        tester.assertVisible("nodeIdContainer");
        tester.assertModelValue("nodeIdContainer:nodeId", "testId");
    }

    public static class CustomNodeInfo implements GeoServerNodeInfo {
        static String ID = null;
        static String STYLE = null;

        @Override
        public String getId() {
            return ID;
        }

        @Override
        public GeoServerNodeData getData() {
            return new GeoServerNodeData(ID, STYLE);
        }

        @Override
        public void customize(WebMarkupContainer nodeInfoContainer) {
            if (STYLE != null) {
                nodeInfoContainer.add(
                        new AttributeAppender("style", new Model<String>(STYLE), ";"));
            }
        }
    }
}
