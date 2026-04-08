/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.GeofenceServerPageTest)
 */
package org.geoserver.acl.plugin.web.accessrules;

import static org.junit.Assert.assertEquals;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.acl.plugin.web.adminrules.model.MutableAdminRule;
import org.geoserver.acl.plugin.web.support.AclWicketTestSupport;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.junit.Ignore;
import org.junit.Test;

public class AccessRulesACLPageTest extends AclWicketTestSupport {

    @Test
    public void testLoads() {
        tester.startPage(AccessRulesACLPage.class);
        tester.assertNoErrorMessage();
    }

    @Test
    @Ignore("TODO")
    public void testAddNewRuleLink() {
        tester.startPage(AccessRulesACLPage.class);
        tester.assertRenderedPage(AccessRulesACLPage.class);
        tester.assertComponent("addNew", AjaxLink.class);
        tester.clickLink("addNew");
        tester.assertRenderedPage(DataAccessRuleEditPage.class);

        // submit a new rule
        FormTester form = tester.newFormTester("form");
        form.submit("save");

        tester.assertRenderedPage(AccessRulesACLPage.class);

        // check the rules model
        @SuppressWarnings("unchecked")
        GeoServerTablePanel<MutableAdminRule> rulesPanel =
                (GeoServerTablePanel<MutableAdminRule>) tester.getComponentFromLastRenderedPage("rulesPanel");
        assertEquals(1, rulesPanel.getDataProvider().size());
    }
}
