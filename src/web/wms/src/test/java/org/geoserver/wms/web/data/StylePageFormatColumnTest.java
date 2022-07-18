/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Map;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.markup.repeater.data.IDataProvider;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.StyleProperty;
import org.junit.Before;
import org.junit.Test;

public class StylePageFormatColumnTest extends StylePageTest {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        Map<StyleProperty, Object> propertiesSld10 = new HashMap<>();
        propertiesSld10.put(StyleProperty.FORMAT_VERSION, SLDHandler.VERSION_10);

        testData.addStyle(
                null,
                "testStyleFormatLabelSld10",
                "testStyleFormatLabelSld10.sld",
                StylePageTest.class,
                getCatalog(),
                propertiesSld10);

        Map<StyleProperty, Object> propertiesSld11 = new HashMap<>();
        propertiesSld11.put(StyleProperty.FORMAT_VERSION, SLDHandler.VERSION_11);

        testData.addStyle(
                null,
                "testStyleFormatLabelSld11",
                "testStyleFormatLabelSld11.sld",
                StylePageTest.class,
                getCatalog(),
                propertiesSld11);
    }

    @Before
    public void loadStylePage() {
        login();

        tester.startPage(StylePage.class);
        tester.assertRenderedPage(StylePage.class);
    }

    private int findFormatColumnIndex(DataView dv) {
        IDataProvider dataProvider = dv.getDataProvider();
        assertTrue(dataProvider instanceof StyleProvider);
        StyleProvider provider = (StyleProvider) dataProvider;
        return provider.getProperties().indexOf(StyleProvider.FORMAT);
    }

    private String findFormatLabelText(DataView dv, int formatColumnIndex) {
        Item i = (Item) dv.getItems().next();
        Label label =
                (Label)
                        i.get(
                                "itemProperties:"
                                        + formatColumnIndex
                                        + ":component:styleFormatLabel");

        return label.getDefaultModelObjectAsString();
    }

    @Test
    public void formatColumnPresenceTest() {

        Catalog catalog = getCatalog();
        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        assertEquals(dv.size(), catalog.getStyles().size());

        IDataProvider dataProvider = dv.getDataProvider();
        assertTrue(dataProvider instanceof StyleProvider);

        StyleProvider provider = (StyleProvider) dataProvider;
        assertTrue(provider.getProperties().contains(StyleProvider.FORMAT));
    }

    @Test
    public void testSLD11Label() {

        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "testStyleFormatLabelSld11");
        ft.submit("submit");

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        assertEquals(dv.size(), 1);

        int formatColumnIndex = findFormatColumnIndex(dv);
        assertTrue(formatColumnIndex > -1);

        String formatLabelText = findFormatLabelText(dv, formatColumnIndex);
        assertEqualsIgnoreNewLineStyle(formatLabelText, "SLD 1.1");
    }

    @Test
    public void testSLD10Label() {

        FormTester ft = tester.newFormTester("table:filterForm");
        ft.setValue("filter", "testStyleFormatLabelSld10");
        ft.submit("submit");

        DataView dv =
                (DataView) tester.getComponentFromLastRenderedPage("table:listContainer:items");

        assertEquals(dv.size(), 1);

        int formatColumnIndex = findFormatColumnIndex(dv);
        assertTrue(formatColumnIndex > -1);

        String formatLabelText = findFormatLabelText(dv, formatColumnIndex);
        assertEqualsIgnoreNewLineStyle(formatLabelText, "SLD 1.0");
    }
}
