/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.apache.wicket.util.tester.WicketTester;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.junit.Before;
import org.junit.Test;

public class GeoServerTablePanelTest {
    WicketTester tester;

    static final int TOTAL_ITEMS = 40;
    static final int DEFAULT_ITEMS_PER_PAGE = 25;

    @Before
    public void setUp() throws Exception {
        tester = new WicketTester();
    }

    @Test
    public void testBasicTable() throws Exception {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new IntegerTable(id, false)));
        tester.assertComponent("form:panel", IntegerTable.class);

        // check the contents are as expected
        String firstLabelPath = "form:panel:listContainer:items:1:itemProperties:0:component";
        tester.assertComponent(firstLabelPath, Label.class);
        assertEquals(
                Integer.valueOf(0),
                tester.getComponentFromLastRenderedPage(firstLabelPath).getDefaultModelObject());

        // check we actually rendered 10 rows
        DataView dv =
                (DataView)
                        tester.getComponentFromLastRenderedPage("form:panel:listContainer:items");
        assertEquals(DEFAULT_ITEMS_PER_PAGE, dv.size());
    }

    @Test
    public void testFullSelection() throws Exception {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new IntegerTable(id, true)));
        tester.assertComponent("form:panel", IntegerTable.class);
        IntegerTable table = (IntegerTable) tester.getComponentFromLastRenderedPage("form:panel");

        // check the select all check and the row check are there
        String selectAllPath = "form:panel:listContainer:selectAllContainer:selectAll";
        String selectFirstPath = "form:panel:listContainer:items:1:selectItemContainer:selectItem";
        tester.assertComponent(selectAllPath, CheckBox.class);
        tester.assertComponent(selectFirstPath, CheckBox.class);

        // test full selection
        assertEquals(0, table.getSelection().size());
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:listContainer:selectAllContainer:selectAll", "true");
        tester.executeAjaxEvent(selectAllPath, "click");
        assertEquals(DEFAULT_ITEMS_PER_PAGE, table.getSelection().size());
        assertEquals(Integer.valueOf(0), table.getSelection().get(0));

        // reset selection
        table.setSelection(false);
        assertEquals(0, table.getSelection().size());
    }

    @Test
    public void testSingleSelection() throws Exception {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new IntegerTable(id, true)));
        tester.assertComponent("form:panel", IntegerTable.class);
        IntegerTable table = (IntegerTable) tester.getComponentFromLastRenderedPage("form:panel");
        assertEquals(0, table.getSelection().size());

        // select just one
        FormTester ft = tester.newFormTester("form");
        ft.setValue("panel:listContainer:items:1:selectItemContainer:selectItem", "true");
        ft.setValue("panel:listContainer:items:7:selectItemContainer:selectItem", "true");
        ft.submit();
        assertEquals(2, table.getSelection().size());
        assertEquals(Integer.valueOf(0), table.getSelection().get(0));
        assertEquals(Integer.valueOf(6), table.getSelection().get(1));
    }

    @Test
    public void testSingleSelectionByObjectAndIndex() throws Exception {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new IntegerTable(id, true)));
        tester.assertComponent("form:panel", IntegerTable.class);

        IntegerTable table = (IntegerTable) tester.getComponentFromLastRenderedPage("form:panel");
        assertEquals(0, table.getSelection().size());

        table.selectObject(Integer.valueOf(5));
        assertEquals(1, table.getSelection().size());
        assertEquals(Integer.valueOf(5), table.getSelection().get(0));

        table.selectObject(7);
        assertEquals(2, table.getSelection().size());
        assertEquals(Integer.valueOf(5), table.getSelection().get(0));
        assertEquals(Integer.valueOf(7), table.getSelection().get(1));
    }

    @Test
    public void testFilter() {
        tester.startPage(new FormTestPage((ComponentBuilder) id -> new IntegerTable(id, true)));

        // verify the initial state
        tester.assertComponent("form:panel", IntegerTable.class);

        DataView dv =
                (DataView)
                        tester.getComponentFromLastRenderedPage("form:panel:listContainer:items");
        assertEquals(25, dv.size());

        String filterLabelPath = "form:panel:filterForm:navigatorTop:filterMatch";
        tester.assertComponent(filterLabelPath, Label.class);
        tester.assertLabel(filterLabelPath, "1 -&gt; 25 of 40");

        // search by "5"
        FormTester ft = tester.newFormTester("form:panel:filterForm");
        ft.setValue("filter", "5");
        ft.submit("submit");

        // verify the search returned the expected number of results
        dv = (DataView) tester.getComponentFromLastRenderedPage("form:panel:listContainer:items");
        assertEquals(4, dv.size());
        // verify the label was updated correctly
        tester.assertLabel("form:panel:filterForm:navigatorTop:filterMatch", "1 -&gt; 4 of 4/40");
        // verify the clear label appears
        tester.assertVisible("form:panel:filterForm:clear");

        // test clearing the filter by clicking on the Clear link
        tester.clickLink("form:panel:filterForm:clear", true);
        // verify clear button has disppeared
        tester.assertInvisible("form:panel:filterForm:clear");
        // verify filter text field is empty after clicking Clear
        tester.assertModelValue("form:panel:filterForm:filter", "");
        dv = (DataView) tester.getComponentFromLastRenderedPage("form:panel:listContainer:items");
        assertEquals(25, dv.size());
    }

    static class IntegerTable extends GeoServerTablePanel<Integer> {

        public IntegerTable(String id, boolean selectable) {
            super(id, new IntegerProvider(), selectable);
        }

        @Override
        protected Component getComponentForProperty(
                String id, IModel<Integer> itemModel, Property<Integer> property) {
            if (property == IntegerProvider.IDX) {
                return new Label(id, itemModel);
            }
            return null;
        }

        @Override
        protected IModel getPropertyTitle(Property<Integer> property) {
            return new Model(property.getName());
        }

        @Override
        IModel showingAllRecords(long first, long last, long size) {
            return new Model(first + " -> " + last + " of " + size);
        }

        @Override
        IModel matchedXOutOfY(long first, long last, long size, long fullSize) {
            return new Model(first + " -> " + last + " of " + size + "/" + fullSize);
        }
    }

    static class IntegerProvider extends GeoServerDataProvider<Integer> {

        static final Property<Integer> IDX =
                new Property<Integer>() {
                    @Override
                    public String getName() {
                        return "idx";
                    }

                    @Override
                    public Object getPropertyValue(Integer item) {
                        return item;
                    }

                    @Override
                    public IModel<?> getModel(IModel<Integer> itemModel) {
                        return null;
                    }

                    @Override
                    public Comparator<Integer> getComparator() {
                        return null;
                    }

                    @Override
                    public boolean isVisible() {
                        return true;
                    }

                    @Override
                    public boolean isSearchable() {
                        return true;
                    }
                };

        @Override
        protected List<Integer> getItems() {
            List<Integer> result = new ArrayList<Integer>();
            for (int i = 0; i < TOTAL_ITEMS; i++) {
                result.add(i);
            }
            return result;
        }

        @Override
        protected List<org.geoserver.web.wicket.GeoServerDataProvider.Property<Integer>>
                getProperties() {
            return Collections.singletonList(IDX);
        }
    }
}
