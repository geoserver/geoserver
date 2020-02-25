/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.model.CompoundPropertyModel;
import org.geoserver.security.password.MasterPasswordConfig;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.web.wicket.HelpLink;

public class PasswordPage extends AbstractSecurityPage {

    public PasswordPage() {
        Form form = new Form("form", new CompoundPropertyModel(new MasterPasswordConfigModel()));
        add(form);

        form.add(new MasterPasswordProviderChoice("providerName"));
        form.add(
                new Link("changePassword") {
                    @Override
                    public void onClick() {
                        MasterPasswordChangePage page = new MasterPasswordChangePage();
                        page.setReturnPage(getPage());
                        setResponsePage(page);
                    }
                });

        form.add(
                new Link("masterPasswordInfo") {
                    @Override
                    public void onClick() {
                        MasterPasswordInfoPage page = new MasterPasswordInfoPage();
                        page.setReturnPage(getPage());
                        setResponsePage(page);
                    }
                });

        form.add(new MasterPasswordProvidersPanel("masterPasswordProviders"));
        form.add(new HelpLink("masterPasswordProvidersHelp").setDialog(dialog));
        form.add(new PasswordPoliciesPanel("passwordPolicies"));
        form.add(new HelpLink("passwordPoliciesHelp").setDialog(dialog));

        form.add(
                new SubmitLink("save", form) {
                    @Override
                    public void onSubmit() {
                        MasterPasswordConfig config =
                                (MasterPasswordConfig) getForm().getModelObject();
                        try {
                            getSecurityManager().saveMasterPasswordConfig(config);
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
