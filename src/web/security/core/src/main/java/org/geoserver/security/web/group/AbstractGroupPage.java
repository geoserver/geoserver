/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.group;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.AbstractSecurityException;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.role.RolePaletteFormComponent;
import org.geoserver.web.wicket.ParamResourceModel;

/** Allows creation of a new user in users.properties */
@SuppressWarnings("serial")
public abstract class AbstractGroupPage extends AbstractSecurityPage {

    protected String userGroupServiceName;
    protected RolePaletteFormComponent rolePalette;

    protected AbstractGroupPage(String userGroupServiceName, final GeoServerUserGroup group) {
        this.userGroupServiceName = userGroupServiceName;

        boolean hasUserGroupStore = hasUserGroupStore(userGroupServiceName);
        boolean hasRoleStore = hasRoleStore(getSecurityManager().getActiveRoleService().getName());

        Form form = new Form("form", new CompoundPropertyModel(group));
        add(form);

        form.add(new TextField<String>("groupname").setEnabled(hasUserGroupStore));
        form.add(new CheckBox("enabled").setEnabled(hasUserGroupStore));

        List<GeoServerRole> roles;
        try {
            roles =
                    new ArrayList(
                            getSecurityManager()
                                    .getActiveRoleService()
                                    .getRolesForGroup(group.getGroupname()));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        form.add(
                rolePalette =
                        new RolePaletteFormComponent("roles", new Model((Serializable) roles)));
        rolePalette.setEnabled(hasRoleStore);

        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        try {
                            onFormSubmit(group);
                            setReturnPageDirtyAndReturn(true);
                        } catch (IOException e) {
                            if (e.getCause() instanceof AbstractSecurityException) {
                                error(e.getCause());
                            } else {
                                error(
                                        new ParamResourceModel(
                                                        "saveError", getPage(), e.getMessage())
                                                .getObject());
                            }
                            LOGGER.log(Level.SEVERE, "Error occurred while saving group", e);
                        }
                    }
                }.setEnabled(
                        hasUserGroupStore
                                || hasRoleStore(
                                        getSecurityManager().getActiveRoleService().getName())));

        // build the submit/cancel
        form.add(getCancelLink());
    }

    /** Implements the actual save action */
    protected abstract void onFormSubmit(GeoServerUserGroup group) throws IOException;
}
