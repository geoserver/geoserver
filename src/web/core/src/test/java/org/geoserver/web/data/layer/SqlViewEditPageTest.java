/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import org.apache.wicket.Component;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.jdbc.VirtualTable;
import org.geotools.jdbc.VirtualTableParameter;
import org.junit.Test;

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
}
