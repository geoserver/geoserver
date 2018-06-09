/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxCheckBox;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.web.SecurityNamedServicePanel;

/**
 * Configuration panel for {@link PasswordPolicy}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class PasswordPolicyPanel extends SecurityNamedServicePanel<PasswordPolicyConfig> {

    MaxLengthPanel maxLengthPanel;

    public PasswordPolicyPanel(String id, IModel<PasswordPolicyConfig> model) {
        super(id, model);

        PasswordPolicyConfig pwPolicy = model.getObject();

        // add(new TextField("name").setRequired(true));
        add(new CheckBox("digitRequired"));
        add(new CheckBox("uppercaseRequired"));
        add(new CheckBox("lowercaseRequired"));
        add(new TextField<Integer>("minLength"));

        boolean unlimited = pwPolicy.getMaxLength() == -1;
        add(
                new AjaxCheckBox("unlimitedMaxLength", new Model(unlimited)) {

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        Boolean value = getModelObject();
                        maxLengthPanel.setVisible(!value);
                        if (value) {
                            maxLengthPanel.setUnlimited();
                        }
                        target.add(maxLengthPanel.getParent());
                    }
                });
        add(
                maxLengthPanel =
                        (MaxLengthPanel) new MaxLengthPanel("maxLength").setVisible(!unlimited));
    }

    public void doSave(PasswordPolicyConfig config) throws Exception {
        getSecurityManager().savePasswordPolicy(config);
    }

    @Override
    public void doLoad(PasswordPolicyConfig config) throws Exception {
        getSecurityManager().loadPasswordPolicyConfig(config.getName());
    }

    class MaxLengthPanel extends FormComponentPanel {

        public MaxLengthPanel(String id) {
            super(id, new Model());
            add(new TextField<Integer>("maxLength"));
            setOutputMarkupId(true);
        }

        public void setUnlimited() {
            get("maxLength").setDefaultModelObject(-1);
        }
    }
}
