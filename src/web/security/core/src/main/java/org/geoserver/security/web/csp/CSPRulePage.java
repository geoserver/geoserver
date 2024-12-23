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
import org.geoserver.security.csp.CSPPolicy;
import org.geoserver.security.csp.CSPRule;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.ParamResourceModel;

/** Page for creating/modifying {@link CSPRule} objects. */
public class CSPRulePage extends GeoServerSecuredPage {

    private static final long serialVersionUID = 8806565936027357459L;

    private TextField<String> nameField;

    private CSPPolicy policy;

    private CSPRule rule;

    public CSPRulePage(CSPRule rule, CSPPolicy policy) {
        this.policy = policy;
        this.rule = new CSPRule(rule);
        IModel<CSPRule> model = new Model<>(this.rule);
        Form<CSPRule> form = new Form<>("form", new CompoundPropertyModel<>(model));
        this.nameField = new TextField<>("name", new PropertyModel<>(model, "name"));
        this.nameField.add(new NameValidator());
        form.add(this.nameField.setRequired(true).setEnabled(this.rule.getName() == null));
        form.add(new TextArea<>("description", new PropertyModel<>(model, "description")));
        form.add(new CheckBox("enabled", new PropertyModel<>(model, "enabled")));
        form.add(new TextArea<String>("filter", new PropertyModel<>(model, "filter")).add(new FilterValidator()));
        form.add(new TextArea<>("directives", new PropertyModel<>(model, "directives")));
        form.add(new SubmitLink("save", form) {
            private static final long serialVersionUID = 7615174589339108727L;

            @Override
            public void onSubmit() {
                saveRule();
            }
        });
        form.add(new Button("cancel") {
            private static final long serialVersionUID = 5040891804626008259L;

            @Override
            public void onSubmit() {
                doReturn();
            }
        });
        add(form);
    }

    private void saveRule() {
        List<CSPRule> rules = this.policy.getRules();
        if (this.nameField.isEnabled()) {
            rules.add(new CSPRule(this.rule));
        } else {
            for (int i = 0; i < rules.size(); i++) {
                if (rules.get(i).getName().equals(this.rule.getName())) {
                    rules.set(i, new CSPRule(this.rule));
                    break;
                }
            }
        }
        doReturn();
    }

    private class NameValidator implements IValidator<String> {

        private static final long serialVersionUID = -6504547974652587614L;

        @Override
        public void validate(IValidatable<String> validatable) {
            String name = validatable.getValue();
            if (CSPRulePage.this.nameField.isEnabled() && CSPRulePage.this.policy.getRuleByName(name) != null) {
                ParamResourceModel message = new ParamResourceModel("duplicateRule", CSPRulePage.this, name);
                validatable.error(new ValidationError(message.getString()));
            }
        }
    }

    private class FilterValidator implements IValidator<String> {

        private static final long serialVersionUID = -4027375126421815304L;

        @Override
        public void validate(IValidatable<String> validatable) {
            try {
                CSPRule.parseFilter(validatable.getValue());
            } catch (Exception e) {
                String message = e.getMessage();
                if (e.getCause() != null) {
                    message += ": " + e.getCause().getMessage();
                }
                validatable.error(new ValidationError(message));
            }
        }
    }
}
