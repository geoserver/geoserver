/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.web.SecurityNamedServicePanel;
import org.geoserver.security.web.SecurityNamedServiceTabbedPanel;
import org.geoserver.security.web.group.GroupPanel;
import org.geoserver.security.web.user.UserPanel;
import org.geoserver.security.web.passwd.PasswordEncoderChoice;
import org.geoserver.security.web.passwd.PasswordPolicyChoice;

/**
 * Base class for user group service panels.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 * @param <T>
 */
public class UserGroupServicePanel<T extends SecurityUserGroupServiceConfig> 
    extends SecurityNamedServicePanel<T> implements SecurityNamedServiceTabbedPanel<T> {

    public UserGroupServicePanel(String id, IModel<T> model) {
        super(id, model);

        add(new PasswordEncoderChoice("passwordEncoderName"));
        add(new PasswordPolicyChoice("passwordPolicyName"));
    }

    @Override
    public List<ITab> createTabs(final IModel<T> model) {
        List<ITab> tabs = new ArrayList<ITab>();
        tabs.add(new AbstractTab(new StringResourceModel("users", this, null)) {
            @Override
            public Panel getPanel(String panelId) {
                return new UserPanel(panelId, model.getObject().getName());
            }
        });
        tabs.add(new AbstractTab(new StringResourceModel("groups", this, null)) {
            @Override
            public Panel getPanel(String panelId) {
                return new GroupPanel(panelId, model.getObject().getName());
            }
        });
        return tabs;
    }

    @Override
    public void doSave(T config) throws Exception {
        getSecurityManager().saveUserGroupService(config);
    }

    public void doLoad(T config) throws Exception {
        getSecurityManager().loadUserGroupServiceConfig(config.getName());
    }

}
