/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.csp;

import java.util.List;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.security.csp.CSPConfiguration;
import org.geoserver.security.csp.CSPPolicy;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;

/** Page for creating/modifying {@link CSPPolicy} objects. */
public class CSPPolicyPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -5200150783568375701L;

    private TextField<String> nameField;

    private CSPConfiguration config;

    private CSPPolicy policy;

    public CSPPolicyPage(CSPPolicy policy, CSPConfiguration config) {
        this.config = config;
        this.policy = new CSPPolicy(policy);
        IModel<CSPPolicy> model = new Model<>(this.policy);
        Form<CSPPolicy> form = new Form<>("form", new CompoundPropertyModel<>(model));
        this.nameField = new TextField<>("name", new PropertyModel<>(model, "name"));
        this.nameField.add(new NameValidator());
        form.add(this.nameField.setRequired(true).setEnabled(this.policy.getName() == null));
        form.add(new TextArea<>("description", new PropertyModel<>(model, "description")));
        form.add(new CheckBox("enabled", new PropertyModel<>(model, "enabled")));
        form.add(new CSPRulePanel("rules", this.policy));
        form.add(new SubmitLink("save", form) {
            private static final long serialVersionUID = -5897975410833363747L;

            @Override
            public void onSubmit() {
                savePolicy();
            }
        });
        form.add(new Button("cancel") {
            private static final long serialVersionUID = 4579381111420468326L;

            @Override
            public void onSubmit() {
                doReturn();
            }
        });
        add(form);
    }

    private void savePolicy() {
        List<CSPPolicy> policies = this.config.getPolicies();
        if (this.nameField.isEnabled()) {
            policies.add(new CSPPolicy(this.policy));
        } else {
            for (int i = 0; i < policies.size(); i++) {
                if (policies.get(i).getName().equals(this.policy.getName())) {
                    policies.set(i, new CSPPolicy(this.policy));
                    break;
                }
            }
        }
        doReturn();
    }

    private class NameValidator implements IValidator<String> {

        private static final long serialVersionUID = 2516642715001094366L;

        @Override
        public void validate(IValidatable<String> validatable) {
            String name = validatable.getValue();
            if (CSPPolicyPage.this.nameField.isEnabled() && CSPPolicyPage.this.config.getPolicyByName(name) != null) {
                ParamResourceModel message = new ParamResourceModel("duplicatePolicy", CSPPolicyPage.this, name);
                validatable.error(new ValidationError(message.getString()));
            }
        }
    }
}
