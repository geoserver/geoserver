/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.components;

import static org.junit.Assert.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.markup.repeater.OddEvenItem;
import org.geoserver.acl.domain.adminrules.AdminRule;
import org.geoserver.acl.domain.adminrules.AdminRuleAdminService;
import org.geoserver.acl.plugin.web.adminrules.model.AdminRulesTableDataProvider;
import org.geoserver.acl.plugin.web.adminrules.model.MutableAdminRule;
import org.geoserver.acl.plugin.web.support.AclWicketTestSupport;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.junit.Test;

@SuppressWarnings("unused")
public class RulesTablePanelTest extends AclWicketTestSupport {

    List<AdminRule> createTestRules(final int count) {
        AdminRuleAdminService adminService = adminService();
        assertEquals(0, adminService.count());
        IntStream.rangeClosed(1, count).mapToObj(this::testRule).forEach(adminService::insert);
        return adminService.getAll().collect(Collectors.toList());
    }

    private AdminRule testRule(int i) {
        return AdminRule.user().withRolename("ROLE_" + i).withWorkspace("workspace-" + i);
    }

    @Test
    public void testEmpty() {
        RulesDataProvider<MutableAdminRule> data = new AdminRulesTableDataProvider();
        assertEquals(0, data.size());

        RulesTablePanel<MutableAdminRule> table =
                tester.startComponentInPage(new RulesTablePanel<>("rulesPanel", data));

        // print(table, false, false);
        assertEquals(0, table.getDataProvider().size());

        tester.assertNoErrorMessage();
        // selectable = true
        tester.assertVisible("rulesPanel:listContainer:selectAllContainer:selectAll");
    }

    @Test
    public void testRows() {
        createTestRules(10);

        AdminRulesTableDataProvider data = new AdminRulesTableDataProvider();
        assertEquals(10, data.size());

        RulesTablePanel<MutableAdminRule> table =
                tester.startComponentInPage(new RulesTablePanel<>("rulesPanel", data));

        assertEquals(10, table.getDataProvider().size());

        tester.assertNoErrorMessage();
        // print(table, true, true);

        List<Property<MutableAdminRule>> expectedProperties = data.getProperties();
        for (int i = 1; i <= 10; i++) {
            String itemPath = "rulesPanel:listContainer:items:" + i;
            tester.assertComponent(itemPath, OddEvenItem.class);

            @SuppressWarnings("unchecked")
            ListView<MutableAdminRule> listView =
                    (ListView<MutableAdminRule>) tester.getComponentFromLastRenderedPage(itemPath + ":itemProperties");
            assertNotNull(listView);

            tester.assertComponent(itemPath + ":itemProperties:5:component:up", ImageAjaxLink.class);
            tester.assertComponent(itemPath + ":itemProperties:5:component:down", ImageAjaxLink.class);
            tester.assertComponent(itemPath + ":itemProperties:5:component:edit", ImageAjaxLink.class);
        }
    }

    @Test
    public void testUpDown() {
        List<AdminRule> rules = createTestRules(3);
        List<MutableAdminRule> mutableRules =
                rules.stream().map(MutableAdminRule::new).collect(Collectors.toList());

        AdminRulesTableDataProvider data = spy(new AdminRulesTableDataProvider());

        RulesTablePanel<MutableAdminRule> table =
                tester.startComponentInPage(new RulesTablePanel<>("rulesPanel", data));

        tester.assertNoErrorMessage();

        // move item 1 down
        tester.clickLink("rulesPanel:listContainer:items:1:itemProperties:5:component:down:link");
        MutableAdminRule rule = mutableRules.get(0).clone();
        rule.setPriority(2); // priority is changed after moveDown
        verify(data, times(1)).moveDown(eq(rule));

        // move item 1 up
        //        print(table, true, true);
        // tester.clickLink("rulesPanel:listContainer:items:3:itemProperties:5:component:up:link");
        // note we have to use items:6 instead of items:3 since the table is not reusing items and
        // hence creates items with new ids on each update
        tester.clickLink("rulesPanel:listContainer:items:6:itemProperties:5:component:up:link");
        rule = mutableRules.get(2).clone();
        assertEquals(3, rule.getPriority());
        rule.setPriority(2); // priority is changed after moveUp
        verify(data, times(1)).moveUp(eq(rule));
    }
}
