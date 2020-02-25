/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import java.io.IOException;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.password.MasterPasswordConfig;
import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.web.AbstractSecurityPage;

public class MasterPasswordChangePage extends AbstractSecurityPage {

    public MasterPasswordChangePage() {
        MasterPasswordConfigModel configModel = new MasterPasswordConfigModel();

        Form form = new Form("form", new CompoundPropertyModel(configModel));
        add(form);

        form.add(new Label("providerName"));

        MasterPasswordConfig config = configModel.getObject();
        MasterPasswordProviderConfig providerConfig = null;
        try {
            providerConfig =
                    getSecurityManager()
                            .loadMasterPassswordProviderConfig(config.getProviderName());
        } catch (IOException e) {
            throw new WicketRuntimeException(e);
        }

        // TODO: this will cause the master password to stored as a string in plain text, without
        // the
        // ability to scramble it... not much we can do because wicket works with strings...
        // potentially look into a way to store as char or byte array so string never gets
        // created
        form.add(new PasswordTextField("currentPassword", new Model()));
        form.add(
                new PasswordTextField("newPassword", new Model())
                        .setEnabled(!providerConfig.isReadOnly()));
        form.add(new PasswordTextField("newPasswordConfirm", new Model()));

        form.add(
                new SubmitLink("save", form) {
                    @Override
                    public void onSubmit() {
                        Form f = getForm();
                        // @Justin, we cannot use getDefaultModelObjectAsString() because of special
                        // chars.
                        // example: The password "mcrmcr&1" is converted to "mcrmcr&amp;1".
                        String currPasswd =
                                // f.get("currentPassword").getDefaultModelObjectAsString();
                                (String) f.get("currentPassword").getDefaultModelObject();
                        String newPasswd =
                                // f.get("newPassword").getDefaultModelObjectAsString();
                                (String) f.get("newPassword").getDefaultModelObject();
                        String newPasswdConfirm =
                                // f.get("newPasswordConfirm").getDefaultModelObjectAsString();
                                (String) f.get("newPasswordConfirm").getDefaultModelObject();

                        MasterPasswordConfig mpConfig =
                                (MasterPasswordConfig) getForm().getModelObject();
                        try {
                            getSecurityManager()
                                    .saveMasterPasswordConfig(
                                            mpConfig,
                                            currPasswd.toCharArray(),
                                            newPasswd != null ? newPasswd.toCharArray() : null,
                                            newPasswdConfirm.toCharArray());
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
}
