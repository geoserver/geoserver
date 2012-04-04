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
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.config.X509CertificateAuthenticationFilterConfig;
import org.geoserver.security.config.X509CertificateAuthenticationFilterConfig.RoleSource;
import org.geoserver.security.filter.GeoServerX509CertificateAuthenticationFilter;
import org.geoserver.security.web.role.RoleServiceChoice;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;

/**
 * Configuration panel for {@link GeoServerX509CertificateAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class X509AuthFilterPanel 
    extends AuthenticationFilterPanel<X509CertificateAuthenticationFilterConfig> {

    DropDownChoice<RoleSource> roleSourceChoice;

    public X509AuthFilterPanel(String id, IModel<X509CertificateAuthenticationFilterConfig> model) {
        super(id, model);

        add(roleSourceChoice = 
            new DropDownChoice<RoleSource>("roleSource", Arrays.asList(RoleSource.values()), 
            new EnumChoiceRenderer<RoleSource>()));

        roleSourceChoice.setNullValid(false);
        roleSourceChoice.add(new OnChangeAjaxBehavior() {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                Panel p = roleSourceChoice.getModelObject() == RoleSource.UserGroupService ? 
                    new UserGroupServicePanel("panel") : new RoleServicePanel("panel");
                WebMarkupContainer c = (WebMarkupContainer)get("container"); 
                c.addOrReplace(p);
                target.addComponent(c);
            }
        });

        WebMarkupContainer container = new WebMarkupContainer("container");
        add(container.setOutputMarkupId(true));

        container.add(new UserGroupServicePanel("panel"));
    }

    static class UserGroupServicePanel extends Panel {
        public UserGroupServicePanel(String id) {
            super(id, new Model());
            add(new UserGroupServiceChoice("userGroupServiceName"));
        }
    }

    static class RoleServicePanel extends Panel {
        public RoleServicePanel(String id) {
            super(id, new Model());
            add(new RoleServiceChoice("roleServiceName"));
        }
    }
}
