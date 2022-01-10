/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.role;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractConfirmRemovalPanelTest;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Test;

public class ConfirmRemovalRolePanelTest extends AbstractConfirmRemovalPanelTest<GeoServerRole> {
    private static final long serialVersionUID = 1L;

    protected void setupPanel(final List<GeoServerRole> roots) {

        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(String id) {
                                return new ConfirmRemovalRolePanel(
                                        id, roots.toArray(new GeoServerRole[roots.size()])) {
                                    @Override
                                    protected IModel<String> canRemove(GeoServerRole data) {
                                        SelectionRoleRemovalLink link =
                                                new SelectionRoleRemovalLink(
                                                        getRoleServiceName(), "XXX", null, null);
                                        return link.canRemove(data);
                                    }

                                    private static final long serialVersionUID = 1L;
                                };
                            }
                        }));
    }

    @Test
    public void testRemoveRole() throws Exception {
        initializeForXML();
        removeObject();
    }

    @Override
    protected GeoServerRole getRemoveableObject() throws Exception {
        GeoServerRole role = gaService.getRoleByName("ROLE_NEW");
        if (role == null) {
            gaStore.addRole(role = gaStore.createRoleObject("ROLE_NEW"));
            gaStore.store();
        }
        return role;
    }

    @Override
    protected GeoServerRole getProblematicObject() throws Exception {
        return null;
    }

    @Override
    protected String getProblematicObjectRegExp() throws Exception {
        return null;
    }

    @Override
    protected String getRemoveableObjectRegExp() throws Exception {
        return ".*" + getRemoveableObject().getAuthority() + ".*";
    }
}
