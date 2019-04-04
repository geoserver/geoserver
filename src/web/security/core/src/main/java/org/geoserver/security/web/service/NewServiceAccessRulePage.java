/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import java.util.logging.Level;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.web.wicket.ParamResourceModel;

/** Adds a new rule to the data access set */
@SuppressWarnings("serial")
public class NewServiceAccessRulePage extends AbstractServiceAccessRulePage {

    public NewServiceAccessRulePage() {
        super(new ServiceAccessRule());

        ((Form) get("form")).add(new DuplicateRuleValidator());
    }

    @Override
    protected void onFormSubmit(ServiceAccessRule rule) {
        try {
            ServiceAccessRuleDAO dao = ServiceAccessRuleDAO.get();
            dao.addRule(rule);
            dao.storeRules();
            doReturn(ServiceAccessRulePage.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving service rule", e);
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
        }
    }

    /** Checks the same rule has not been entered before */
    class DuplicateRuleValidator extends AbstractFormValidator {

        public void validate(Form<?> form) {
            // only validate on final submit
            if (form.findSubmittingButton() != form.get("save")) {
                return;
            }

            updateModels();
            ServiceAccessRule rule = (ServiceAccessRule) form.getModelObject();
            if (ServiceAccessRuleDAO.get().getRules().contains(rule)) {
                form.error(
                        new ParamResourceModel("duplicateRule", getPage(), rule.getKey())
                                .getString());
            }
        }

        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent[] {serviceChoice, methodChoice, rolesFormComponent};
        }
    }
}
