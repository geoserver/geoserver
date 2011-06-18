package org.geoserver.web.demo;

import org.geoserver.web.GeoServerWicketTestSupport;

public class SRSListPageTest extends GeoServerWicketTestSupport {
    public void testBasicPage() throws Exception {
        tester.startPage(SRSListPage.class);
        tester.assertLabel("srsListPanel:table:listContainer:items:1:itemProperties:0:component:link:label","2000");
        tester.clickLink("srsListPanel:table:listContainer:items:1:itemProperties:0:component:link");
        tester.assertRenderedPage(SRSDescriptionPage.class);
    }
}
