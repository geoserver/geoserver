/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.notNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.sql.SQLException;
import org.apache.wicket.Component;
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
        Component component =
                tester.getComponentFromLastRenderedPage("form:parameters:listContainer:items:49");
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
        doReturn(Polygon.class)
                .when(spy)
                .getMapping("VARBINARY"); // work around lack of geometry metadata
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
}
