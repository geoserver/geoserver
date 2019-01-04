/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.web.passwd.PasswordEncoderChoice;
import org.geoserver.security.web.role.RoleServiceChoice;
import org.geoserver.web.wicket.HelpLink;

/**
 * Main menu page for global security settings page.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class SecuritySettingsPage extends AbstractSecurityPage {

    public SecuritySettingsPage() {
        initComponents();
    }

    void initComponents() {
        Form<SecurityManagerConfig> form =
                new Form(
                        "form",
                        new CompoundPropertyModel<SecurityManagerConfig>(
                                new SecurityManagerConfigModel()));
        add(form);

        form.add(new RoleServiceChoice("roleServiceName"));

        form.add(new EncryptionPanel("encryption"));
        form.add(new HelpLink("encryptionHelp").setDialog(dialog));
        form.add(
                new SubmitLink("save", form) {
                    @Override
                    public void onSubmit() {
                        SecurityManagerConfig config =
                                (SecurityManagerConfig) getForm().getModelObject();
                        try {
                            getSecurityManager().saveSecurityConfig(config);
                            doReturn();
                        } catch (Exception e) {
                            error(e);
                        }
                    }
                });
        form.add(
                new AjaxLink("cancel") {
                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doReturn();
                    }
                });
    }

    class EncryptionPanel extends FormComponentPanel {

        public EncryptionPanel(String id) {
            super(id, new Model());

            GeoServerSecurityManager secMgr = getSecurityManager();
            if (secMgr.isStrongEncryptionAvailable()) {

                add(
                        new Label(
                                        "strongEncryptionMsg",
                                        new StringResourceModel("strongEncryption", this, null))
                                .add(new AttributeAppender("class", new Model("info-link"), " ")));
            } else {
                add(
                        new Label(
                                        "strongEncryptionMsg",
                                        new StringResourceModel("noStrongEncryption", this, null))
                                .add(
                                        new AttributeAppender(
                                                "class", new Model("warning-link"), " ")));
            }

            add(new CheckBox("encryptingUrlParams"));

            // load only reversible encoders
            add(
                    new PasswordEncoderChoice(
                            "configPasswordEncrypterName",
                            getSecurityManager().loadPasswordEncoders(null, true, null)));
        }
    }
}
