/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules;

import org.geoserver.acl.plugin.web.accessrules.model.DataAccessRuleEditModel;
import org.geoserver.acl.plugin.web.support.AclWicketTestSupport;
import org.junit.Test;

public class DataAccessRuleEditPanelTest extends AclWicketTestSupport {

    @Test
    public void test() {
        DataAccessRuleEditModel pageModel = new DataAccessRuleEditModel();
        @SuppressWarnings("unused")
        DataAccessRuleEditPanel panel =
                tester.startComponentInPage(new DataAccessRuleEditPanel("testPanel", pageModel));
        tester.assertNoErrorMessage();
        // print(panel, true, true);
    }
}
