/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.web.user;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.web.AbstractConfirmRemovalPanelTest;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;
import org.junit.Before;
import org.junit.Test;

public class ConfirmRemovalUserPanelTest extends AbstractConfirmRemovalPanelTest<GeoServerUser> {
    private static final long serialVersionUID = 1L;

    protected boolean disassociateRoles = false;

    protected void setupPanel(final List<GeoServerUser> roots) {
        tester.startPage(
                new FormTestPage(
                        new ComponentBuilder() {
                            private static final long serialVersionUID = 1L;

                            public Component buildComponent(String id) {
                                Model<Boolean> model = new Model<Boolean>(disassociateRoles);
                                return new ConfirmRemovalUserPanel(
                                        id, model, roots.toArray(new GeoServerUser[roots.size()])) {
                                    @Override
                                    protected IModel<String> canRemove(GeoServerUser data) {
                                        SelectionUserRemovalLink link =
                                                new SelectionUserRemovalLink(
                                                        getUserGroupServiceName(),
                                                        "XXX",
                                                        null,
                                                        null,
                                                        disassociateRoles);
                                        return link.canRemove(data);
                                    }

                                    private static final long serialVersionUID = 1L;
                                };
                            }
                        }));
    }

    @Before
    public void init() throws Exception {
        initializeForXML();
        clearServices();
    }

    @Test
    public void testRemoveUser() throws Exception {
        disassociateRoles = false;
        removeObject();
    }

    @Test
    public void testRemoveUserWithRoles() throws Exception {
        disassociateRoles = true;
        removeObject();
    }

    @Override
    protected GeoServerUser getRemoveableObject() throws Exception {
        return ugService.getUserByUsername("admin");
    }

    @Override
    protected GeoServerUser getProblematicObject() throws Exception {
        return null;
    }

    @Override
    protected String getProblematicObjectRegExp() throws Exception {
        return "";
    }

    @Override
    protected String getRemoveableObjectRegExp() throws Exception {
        if (disassociateRoles)
            return ".*"
                    + getRemoveableObject().getUsername()
                    + ".*"
                    + GeoServerRole.ADMIN_ROLE
                    + ".*";
        else return ".*" + getRemoveableObject().getUsername() + ".*";
    }
}
