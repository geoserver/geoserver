/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.Arrays;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.RoleSource;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.web.role.RoleServiceChoice;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.HelpLink;

/**
 * Configuration panel for {@link GeoServerPreAuthenticatedUserNameFilter}.
 * 
 * @author mcr
 */
public abstract class PreAuthenticatedUserNameFilterPanel<T extends PreAuthenticatedUserNameFilterConfig> 
    extends AuthenticationFilterPanel<T> {

    DropDownChoice<RoleSource> roleSourceChoice;

    public PreAuthenticatedUserNameFilterPanel(String id, IModel<T> model) {
        super(id, model);
                                        
        add(new HelpLink("roleSourceHelp",this).setDialog(dialog));
        
        add(roleSourceChoice = 
            new DropDownChoice<RoleSource>("roleSource", Arrays.asList(RoleSource.values()),
            new EnumChoiceRenderer<RoleSource>()));

        roleSourceChoice.setNullValid(false);
        
        roleSourceChoice.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Panel p;
                switch(roleSourceChoice.getModelObject()) {
                case UserGroupService:
                    p = new UserGroupServicePanel("panel");
                    break;
                case RoleService:
                    p = new RoleServicePanel("panel");
                    break;
                default:
                    p = new HeaderPanel("panel");
                }

                WebMarkupContainer c = (WebMarkupContainer)get("container"); 
                c.addOrReplace(p);
                target.addComponent(c);
            }
        });
        

        WebMarkupContainer container = new WebMarkupContainer("container");
        add(container.setOutputMarkupId(true));

        // show correct panel for existing configuration
        RoleSource rs = model.getObject().getRoleSource();
        if (RoleSource.Header.equals(rs))
            container.addOrReplace(new HeaderPanel("panel"));
        if (RoleSource.UserGroupService.equals(rs))
            container.addOrReplace(new UserGroupServicePanel("panel"));
        if (RoleSource.RoleService.equals(rs))
            container.addOrReplace(new RoleServicePanel("panel"));
        if (rs==null)
            container.add(new UserGroupServicePanel("panel"));
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
