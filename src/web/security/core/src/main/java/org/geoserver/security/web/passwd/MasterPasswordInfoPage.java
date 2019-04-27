/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import java.io.File;
import java.io.IOException;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.platform.resource.Files;
import org.geoserver.security.web.AbstractSecurityPage;
import org.springframework.util.StringUtils;

/**
 * This class has package visibility due to security reasons.
 *
 * @author christian
 */
class MasterPasswordInfoPage extends AbstractSecurityPage {

    String fileName;

    MasterPasswordInfoPage() {

        Form form = new Form("form", new CompoundPropertyModel(this));
        add(form);
        form.add(new TextField<String>("fileName"));

        form.add(
                new SubmitLink("save", form) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        if (StringUtils.hasLength(fileName) == false) {
                            error(new StringResourceModel("fileNameEmpty", this, null).getString());
                            return;
                        }
                        try {
                            if (dumpMasterPassword()) {
                                info(
                                        new StringResourceModel("dumpInfo", this)
                                                .setParameters(
                                                        new File(fileName).getCanonicalFile())
                                                .getString());
                            } else error(new StringResourceModel("unauthorized", this).getString());
                        } catch (Exception e) {
                            error(e);
                        }
                    }
                });
        form.add(
                new AjaxLink("back") {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doReturn();
                    }
                });
    }

    boolean dumpMasterPassword() throws IOException {
        return getSecurityManager().dumpMasterPassword(Files.asResource(new File(fileName)));
    }
}
