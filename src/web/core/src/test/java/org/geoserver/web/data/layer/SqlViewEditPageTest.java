/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.sql.SQLException;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.SQLDialect;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.junit.Test;
import org.locationtech.jts.geom.Polygon;
import org.mockito.Mockito;

public class SqlViewEditPageTest extends AbstractSqlViewPageTest {

    @Test
    public void testSqlViewManyParameters() throws IOException {
        login();

        // build a virtual table with many parameters (fake one)
        StringBuilder sb = new StringBuilder("SELECT * FROM \"Forests\" WHERE \n");
        final int LOOPS = 50;
        for (int i = 0; i < LOOPS; i++) {
            sb.append("name = '%PARAM").append(i + 1).append("%'");
            if (i < LOOPS - 1) {
                sb.append(" OR ");
            }
            sb.append("\n");
        }

        // pick a random feature type
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName("Forests");
        VirtualTable vt = new VirtualTable("test", sb.toString());
        for (int i = 0; i < LOOPS; i++) {
            vt.addParameter(new VirtualTableParameter("PARAM" + (i + 1), "abc"));
        }
        info.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);

        // test that we can load the page
        tester.startPage(new SQLViewEditPage(info, null));
        tester.assertRenderedPage(SQLViewEditPage.class);
        tester.assertNoErrorMessage();

        // print(tester.getLastRenderedPage(), true, true);

        // check we have item 50 (numbered 49) in the html output
        Component component = tester.getComponentFromLastRenderedPage("form:parameters:listContainer:items:49");
        assertNotNull(component);
    }

    @Test
    public void testSqlViewConnectionLeak() throws IOException, SQLException {
        login();

        // build a virtual table
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName("Forests");
        VirtualTable vt = new VirtualTable("test", "SELECT * FROM \"Forests\"");
        info.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);
        FeatureTypeInfo ft = info;

        // this assumes the resource pool will not drop the store, which it shuld not indeed
        JDBCDataStore store = (JDBCDataStore) ft.getStore().getDataStore(null);
        SQLDialect spy = spy(store.getSQLDialect());
        doReturn(Polygon.class).when(spy).getMapping("VARBINARY"); // work around lack of geometry metadata
        store.setSQLDialect(spy);

        // start the tester page
        tester.startPage(new SQLViewEditPage(ft, null));
        tester.assertRenderedPage(SQLViewEditPage.class);
        tester.assertNoErrorMessage();

        FormTester formTester = tester.newFormTester("form");
        formTester.setValue("guessGeometrySrid", true);

        // loop enough times on the refresh that the connection pool should exhaust connections
        // if not released properly
        final int LOOPS = 100;
        for (int i = 0; i < LOOPS; i++) {
            tester.executeAjaxEvent("form:refresh", "click");
        }

        // check the createCRS has been called 50 times with a valid connection
        verify(spy, Mockito.atLeast(50)).createCRS(anyInt(), notNull());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testSqlViewLostEdits() throws IOException {
        login();

        // build a virtual table and configure it
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName("Forests");
        // no pk and no geometry, so that a refresh will find the same model
        VirtualTable vt = new VirtualTable("test", "SELECT * FROM \"Forests\"");
        info.getMetadata().put(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, vt);

        // load the page
        tester.startPage(new SQLViewEditPage(info, null));
        tester.assertRenderedPage(SQLViewEditPage.class);
        tester.assertNoErrorMessage();

        // refresh the attributes
        tester.clickLink("form:refresh", true);

        // now set the pk (cannot work on the geometry, H2 sql views do not recognize the geometry type)
        FormTester formTester = tester.newFormTester("form");
        DataView<SQLViewAttribute> dataView = (DataView<SQLViewAttribute>)
                tester.getComponentFromLastRenderedPage("form:attributes:listContainer:items");
        String pkComponentId = dataView.streamChildren()
                .filter(c -> {
                    SQLViewAttribute att = (SQLViewAttribute) c.getDefaultModelObject();
                    return att.getName().equals("fid_1");
                })
                .map(c -> c.getId())
                .findFirst()
                .orElseThrow();
        String pkPath = "attributes:listContainer:items:" + pkComponentId + ":itemProperties:3:component:identifier";
        formTester.setValue(pkPath, true);
        formTester.submitLink("ok", false);

        // check saving worked (without the fix the list would have been empty
        vt = info.getMetadata().get(FeatureTypeInfo.JDBC_VIRTUAL_TABLE, VirtualTable.class);
        assertEquals(List.of("fid_1"), vt.getPrimaryKeyColumns());
    }
}
