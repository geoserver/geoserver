/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.usergroup;

import java.util.ArrayList;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.extensions.markup.html.tabs.AbstractTab;
import org.apache.wicket.extensions.markup.html.tabs.ITab;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.Util;
import org.geoserver.security.web.SecurityNamedServicePanel;
import org.geoserver.security.web.SecurityNamedServiceTabbedPanel;
import org.geoserver.security.web.group.GroupPanel;
import org.geoserver.security.web.passwd.PasswordEncoderChoice;
import org.geoserver.security.web.passwd.PasswordPolicyChoice;
import org.geoserver.security.web.user.UserPanel;

/**
 * Base class for user group service panels.
 *
 * @author Justin Deoliveira, OpenGeo
 * @param <T>
 */
public class UserGroupServicePanel<T extends SecurityUserGroupServiceConfig>
        extends SecurityNamedServicePanel<T> implements SecurityNamedServiceTabbedPanel<T> {

    CheckBox recodeCheckBox = null;

    public UserGroupServicePanel(String id, IModel<T> model) {
        super(id, model);

        add(
                new PasswordEncoderChoice("passwordEncoderName")
                        .add(
                                new OnChangeAjaxBehavior() {
                                    @Override
                                    protected void onUpdate(AjaxRequestTarget target) {
                                        if (recodeCheckBox.isVisible()) {
                                            recodeCheckBox.setEnabled(true);
                                            target.add(recodeCheckBox);
                                        }
                                    }
                                }));

        boolean canCreateStore = false;
        SecurityUserGroupServiceConfig config = model.getObject();
        try {
            GeoServerUserGroupService s =
                    (GeoServerUserGroupService)
                            Class.forName(config.getClassName())
                                    .getDeclaredConstructor()
                                    .newInstance();
            canCreateStore = s.canCreateStore();
        } catch (Exception e) {
            // do nothing
        }

        recodeCheckBox = new CheckBox("recodeExistingPasswords", Model.of(false));
        recodeCheckBox.setOutputMarkupId(true);
        recodeCheckBox.setVisible(canCreateStore);
        recodeCheckBox.setEnabled(false);
        add(recodeCheckBox);
        add(new PasswordPolicyChoice("passwordPolicyName"));
    }

    @Override
    public List<ITab> createTabs(final IModel<T> model) {
        List<ITab> tabs = new ArrayList<ITab>();
        tabs.add(
                new AbstractTab(new StringResourceModel("users", this, null)) {
                    @Override
                    public Panel getPanel(String panelId) {
                        return new UserPanel(panelId, model.getObject().getName());
                    }
                });
        tabs.add(
                new AbstractTab(new StringResourceModel("groups", this, null)) {
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
        if (recodeCheckBox.getModelObject()) {
            GeoServerUserGroupService s =
                    getSecurityManager().loadUserGroupService(config.getName());
            if (s.canCreateStore()) {
                Util.recodePasswords(s.createStore());
            }
        }
    }

    public void doLoad(T config) throws Exception {
        getSecurityManager().loadUserGroupServiceConfig(config.getName());
    }
}
