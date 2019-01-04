/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.Arrays;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.EmptyPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.web.role.RoleServiceChoice;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.wicket.HelpLink;

/**
 * Configuration panel for {@link GeoServerPreAuthenticatedUserNameFilter}.
 *
 * @author mcr
 */
public abstract class PreAuthenticatedUserNameFilterPanel<
                T extends PreAuthenticatedUserNameFilterConfig>
        extends AuthenticationFilterPanel<T> {

    DropDownChoice<RoleSource> roleSourceChoice;

    public PreAuthenticatedUserNameFilterPanel(String id, IModel<T> model) {
        super(id, model);

        add(new HelpLink("roleSourceHelp", this).setDialog(dialog));

        createRoleSourceDropDown();

        roleSourceChoice.setNullValid(false);

        roleSourceChoice.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Panel p = getRoleSourcePanel(roleSourceChoice.getModelObject());

                        WebMarkupContainer c = (WebMarkupContainer) get("container");
                        c.addOrReplace(p);
                        target.add(c);
                    }
                });

        WebMarkupContainer container = new WebMarkupContainer("container");
        add(container.setOutputMarkupId(true));

        // show correct panel for existing configuration
        RoleSource rs = model.getObject().getRoleSource();
        addRoleSourceDropDown(container, rs);
    }

    protected Panel getRoleSourcePanel(RoleSource model) {
        if (PreAuthenticatedUserNameRoleSource.UserGroupService.equals(model)) {
            return new UserGroupServicePanel("panel");
        } else if (PreAuthenticatedUserNameRoleSource.RoleService.equals(model)) {
            return new RoleServicePanel("panel");
        } else if (PreAuthenticatedUserNameRoleSource.Header.equals(model)) {
            return new HeaderPanel("panel");
        }
        return new EmptyPanel("panel");
    }

    protected void createRoleSourceDropDown() {
        add(
                roleSourceChoice =
                        new DropDownChoice<RoleSource>(
                                "roleSource",
                                Arrays.asList(PreAuthenticatedUserNameRoleSource.values()),
                                new RoleSourceChoiceRenderer()));
    }

    protected void addRoleSourceDropDown(WebMarkupContainer container, RoleSource rs) {
        container.addOrReplace(getRoleSourcePanel(rs));
    }

    static class HeaderPanel extends Panel {
        public HeaderPanel(String id) {
            super(id, new Model());
            add(new TextField("rolesHeaderAttribute").setRequired(true));
        }
    }

    static class UserGroupServicePanel extends Panel {
        public UserGroupServicePanel(String id) {
            super(id, new Model());
            add(new UserGroupServiceChoice("userGroupServiceName").setRequired(true));
        }
    }

    static class RoleServicePanel extends Panel {
        public RoleServicePanel(String id) {
            super(id, new Model());
            add(new RoleServiceChoice("roleServiceName"));
        }
    }
}
