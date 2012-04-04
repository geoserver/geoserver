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
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.RoleSource;
import org.geoserver.security.config.RequestHeaderAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerRequestHeaderAuthenticationFilter;
import org.geoserver.security.web.role.RoleServiceChoice;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;

/**
 * Configuration panel for {@link GeoServerRequestHeaderAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class HeaderAuthFilterPanel 
    extends AuthenticationFilterPanel<RequestHeaderAuthenticationFilterConfig> {

    DropDownChoice<RoleSource> roleSourceChoice;

    public HeaderAuthFilterPanel(String id, IModel<RequestHeaderAuthenticationFilterConfig> model) {
        super(id, model);

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

        container.add(new UserGroupServicePanel("panel"));
    }

    static class HeaderPanel extends Panel {
        public HeaderPanel(String id) {
            super(id, new Model());
            add(new TextField("principalHeaderAttribute").setRequired(true));
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
