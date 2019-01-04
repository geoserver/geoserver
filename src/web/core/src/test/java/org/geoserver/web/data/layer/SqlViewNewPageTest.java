/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.data.layer;

import java.io.IOException;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.apache.wicket.util.tester.FormTester;
import org.junit.Test;

public class SqlViewNewPageTest extends AbstractSqlViewPageTest {

    @Test
    public void testSqlViewManyParameters() throws IOException {
        login();

        PageParameters pp = new PageParameters();
        pp.add(SQLViewAbstractPage.WORKSPACE, getCatalog().getDefaultWorkspace().getName());
        pp.add(SQLViewAbstractPage.DATASTORE, AbstractSqlViewPageTest.STORE_NAME);
        tester.startPage(new SQLViewNewPage(pp));

        FormTester form = tester.newFormTester("form");
        form.setValue("sql", "SELECT * FROM \"Forests\" where name = %FOO%");
        tester.clickLink("form:guessParams", true);

        // print(tester.getLastRenderedPage(), true, true);

        // check it did not crash and the param has been guessed
        tester.assertModelValue(
                "form:parameters:listContainer:items:1:itemProperties:0:component:text", "FOO");
    }
}
