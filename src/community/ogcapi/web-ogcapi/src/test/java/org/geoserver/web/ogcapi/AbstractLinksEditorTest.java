/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.util.List;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.LinkInfo;
import org.geoserver.ogcapi.impl.LinkInfoImpl;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public abstract class AbstractLinksEditorTest extends GeoServerWicketTestSupport {
    final String ITEM1 = ":table:listContainer:items:1";
    final String ITEM2 = ":table:listContainer:items:2";
    final String ITEM3 = ":table:listContainer:items:3";
    final String REL = ":itemProperties:2:component:text";
    final String TYPE = ":itemProperties:3:component:text";
    final String HREF = ":itemProperties:4:component:area";
    final String TITLE = ":itemProperties:5:component:area";
    final String SERVICE = ":itemProperties:6:component:service";
    final String REMOVE = ":itemProperties:7:component:link";
    protected LinkInfoImpl link;
    String EDITOR = "publishedinfo:tabs:panel:theList:0:content:linksEditor";
    String EDITOR_FT = "tabs:panel:theList:0:content:linksEditor";

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // not adding test data here
    }

    @Test
    public void testDisplayLinks() throws Exception {
        print(tester.getLastRenderedPage(), true, true);

        // check existing link
        tester.assertModelValue(EDITOR + ITEM1 + REL, link.getRel());
        tester.assertModelValue(EDITOR + ITEM1 + TYPE, link.getType());
        tester.assertModelValue(EDITOR + ITEM1 + HREF, link.getHref());
        tester.assertModelValue(EDITOR + ITEM1 + TITLE, link.getTitle());
        tester.assertModelValue(EDITOR + ITEM1 + SERVICE, link.getService());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testAddLink() throws Exception {
        // create a new link
        // (this generates a new set of ids for all link components, starting from 2)
        tester.clickLink(EDITOR + ":addLink");

        print(tester.getLastRenderedPage(), true, true);

        // fill the links
        FormTester ft = tester.newFormTester(getFormName());
        String rel = "describedBy";
        String type = "application/json";
        String href = "http://www.geoserver.org/lakes-schema.json";
        String title = "Lakes schema description";
        ft.setValue(EDITOR_FT + ITEM3 + REL, rel);
        ft.setValue(EDITOR_FT + ITEM3 + TYPE, type);
        ft.setValue(EDITOR_FT + ITEM3 + HREF, href);
        ft.setValue(EDITOR_FT + ITEM3 + TITLE, title);
        // service not set on purpose
        ft.submit("apply");
        tester.assertNoErrorMessage();

        List<LinkInfo> links = getLinks();
        assertEquals(2, links.size());
        assertEquals(link, links.get(0));
        LinkInfo created = links.get(1);
        assertEquals(rel, created.getRel());
        assertEquals(type, created.getType());
        assertEquals(href, created.getHref());
        assertEquals(title, created.getTitle());
        assertNull(created.getService());
    }

    @Test
    public void testRemoveLink() throws Exception {
        // remove the link
        tester.executeAjaxEvent(EDITOR + ITEM1 + REMOVE, "click");
        // submit the form
        tester.newFormTester(getFormName()).submit("apply");
        tester.assertNoErrorMessage();

        List<LinkInfo> links = getLinks();
        assertNull(links);
    }

    protected abstract List<LinkInfo> getLinks();

    protected abstract String getFormName();
}
