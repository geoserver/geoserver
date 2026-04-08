/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.GeofenceServerAdminPageTest)
 */
package org.geoserver.acl.plugin.web.adminrules;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.geoserver.acl.domain.adminrules.AdminRule;
import org.geoserver.acl.plugin.web.adminrules.model.MutableAdminRule;
import org.geoserver.acl.plugin.web.components.RulesTablePanel;
import org.geoserver.acl.plugin.web.support.AclWicketTestSupport;
import org.junit.Test;
import org.springframework.util.SerializationUtils;

public class AdminRulesACLPageTest extends AclWicketTestSupport {

    @Test
    public void testPageComponents() {
        adminService().insert(AdminRule.user());
        adminService().insert(AdminRule.admin());

        tester.startPage(AdminRulesACLPage.class);

        tester.assertRenderedPage(AdminRulesACLPage.class);
        tester.assertComponent("headerPanel:addNew", AjaxLink.class);
        tester.assertComponent("headerPanel:removeSelected", AjaxLink.class);
        tester.assertComponent("rulesPanel", RulesTablePanel.class);

        // check the rules model
        @SuppressWarnings("unchecked")
        RulesTablePanel<MutableAdminRule> rulesPanel =
                (RulesTablePanel<MutableAdminRule>) tester.getComponentFromLastRenderedPage("rulesPanel");
        assertEquals(2, rulesPanel.getDataProvider().size());
    }

    @Test
    public void testAddNewRuleLink() {
        tester.startPage(AdminRulesACLPage.class);

        tester.clickLink("headerPanel:addNew");
        tester.assertRenderedPage(AdminRuleEditPage.class);
    }

    @Test
    public void testSerializable() {
        adminService().insert(AdminRule.user());
        adminService().insert(AdminRule.admin());

        AdminRulesACLPage page = tester.startPage(AdminRulesACLPage.class);

        byte[] serialized = SerializationUtils.serialize(page);
        @SuppressWarnings("deprecation")
        AdminRulesACLPage deserialized = (AdminRulesACLPage) SerializationUtils.deserialize(serialized);
        assertNotNull(deserialized);
    }
}
