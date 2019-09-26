/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.request.mapper.parameter.INamedParameters.Type;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Test;
import org.opengis.filter.Filter;

public class StylePageTest extends GeoServerWicketTestSupport {

    @Test
    public void testPageLoad() {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
    }

    @Test
    public void testStyleProvider() {
        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);

        // Get the StyleProvider

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        Catalog catalog = getCatalog();
        assertEquals(dv.size(), catalog.getStyles().size());
        IDataProvider dataProvider = dv.getDataProvider();

        // Ensure the data provider is an instance of StoreProvider
        assertTrue(dataProvider instanceof StyleProvider);

        // Cast to StoreProvider
        StyleProvider provider = (StyleProvider) dataProvider;

        // Ensure that an unsupportedException is thrown when requesting the Items directly
        boolean catchedException = false;
        try {
            provider.getItems();
        } catch (UnsupportedOperationException e) {
            catchedException = true;
        }

        // Ensure the exception is cacthed
        assertTrue(catchedException);

        StyleInfo actual = provider.iterator(0, 1).next();
        CloseableIterator<StyleInfo> list =
                catalog.list(
                        StyleInfo.class, Filter.INCLUDE, 0, 1, Predicates.sortBy("name", true));
        assertTrue(list.hasNext());
        StyleInfo expected = list.next();

        // Close the iterator
        try {
            if (list != null) {
                list.close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        // Ensure equality
        assertEquals(expected, actual);
    }

    @Test
    public void testIsDefaultStyle() {
        Catalog cat = getCatalog();
        assertTrue(StylePage.isDefaultStyle(cat.getStyleByName("line")));

        StyleInfo s = cat.getFactory().createStyle();
        s.setName("line");
        s.setFilename("line.sld");
        s.setWorkspace(cat.getDefaultWorkspace());

        assertFalse(StylePage.isDefaultStyle(s));
    }

    @Test
    public void testTimeColumnsToggle() {
        GeoServerInfo info = getGeoServerApplication().getGeoServer().getGlobal();
        info.getSettings().setShowCreatedTimeColumnsInAdminList(true);
        info.getSettings().setShowModifiedTimeColumnsInAdminList(true);
        getGeoServerApplication().getGeoServer().save(info);

        login();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);

        // Get the StyleProvider

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        Catalog catalog = getCatalog();
        assertEquals(dv.size(), catalog.getStyles().size());
        IDataProvider dataProvider = dv.getDataProvider();

        // Ensure the data provider is an instance of StoreProvider
        assertTrue(dataProvider instanceof StyleProvider);

        // Cast to StoreProvider
        StyleProvider provider = (StyleProvider) dataProvider;
        // should have these columns
        assertTrue(provider.getProperties().contains(StyleProvider.CREATED_TIMESTAMP));
        assertTrue(provider.getProperties().contains(StyleProvider.MODIFIED_TIMESTAMP));
    }

    @Test
    public void testFilter() {
        login();
        Catalog catalog = getCatalog();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        assertEquals(dv.size(), catalog.getStyles().size());
        // apply filter by only viewing style with name polygon
        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "polygon");
        ft.submit("submit");

        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), 2);
        tester.assertVisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "polygon");
        // navigate to a style page for any style
        tester.startPage(new StyleEditPage(catalog.getStyles().get(0)));
        tester.assertRenderedPage(StyleEditPage.class);
        tester.assertNoErrorMessage();

        // click submit and go back to Style page
        tester.executeAjaxEvent("submit", "click");

        // verify when user navigates back to Layer Page
        // the clear link is visible and filter is populated in text field
        // and table is in filtered state
        tester.assertRenderedPage(StylePage.class);
        tester.assertVisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "polygon");
        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), 2);

        // clear the filter by click the Clear button
        tester.clickLink("table:filterForm:clear", true);
        //        // verify clear button has disappeared and filter is set to empty
        tester.assertInvisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "");
        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), catalog.getStyles().size());
    }

    @Test
    public void testFilterReset() {
        login();
        Catalog catalog = getCatalog();
        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), catalog.getStyles().size());
        // apply filter by only viewing style with name polygon
        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "polygon");
        ft.submit("submit");

        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");
        assertEquals(dv.size(), 2);
        tester.assertVisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", "polygon");

        // simulate click from left menu which passes
        // show the page with no filter
        PageParameters pageParms = new PageParameters();
        pageParms.set(GeoServerTablePanel.FILTER_PARAM, false, Type.PATH);
        tester.startPage(StylePage.class, pageParms);
        tester.assertRenderedPage(StylePage.class);
        tester.assertNoErrorMessage();

        tester.assertInvisible("table:filterForm:clear");
        tester.assertModelValue("table:filterForm:filter", null);
        dv = (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        assertEquals(dv.size(), catalog.getStyles().size());
    }
}
