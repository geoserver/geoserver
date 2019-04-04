/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import java.util.logging.Level;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.wicket.ParamResourceModel;

/** Adds a new rule to the data access set */
@SuppressWarnings("serial")
public class NewDataAccessRulePage extends AbstractDataAccessRulePage {

    public NewDataAccessRulePage() {
        super(new DataAccessRule());

        ((Form) get("form")).add(new DuplicateRuleValidator());
    }

    @Override
    protected void onFormSubmit(DataAccessRule rule) {
        try {
            DataAccessRuleDAO dao = DataAccessRuleDAO.get();
            dao.addRule(rule);
            dao.storeRules();
            doReturn(DataSecurityPage.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving rule ", e);
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
        }
    }

    //    private String parseRole(String modelObjectAsString) {
    //        return modelObjectAsString.substring(1, modelObjectAsString.length() - 1);
    //    }

    /**
     * Checks the same rule has not been entered before
     *
     * @author aaime
     */
    class DuplicateRuleValidator extends AbstractFormValidator {
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent[] {
                rootChoice, layerChoice, accessModeChoice, rolesFormComponent
            };
        }

        public void validate(Form<?> form) {
            if (form.findSubmittingButton() != form.get("save")) { // only validate on final submit
                return;
            }

            updateModels();
            DataAccessRule rule = (DataAccessRule) form.getModelObject();
            // DataAccessRule rule = new DataAccessRule(model.getWorkspace(),
            //        model.getLayer(),model.getAccessMode(),
            //       rolesFormComponent.getRolesNamesForStoring());
            if (DataAccessRuleDAO.get().getRules().contains(rule)) {
                form.error(
                        new ParamResourceModel("duplicateRule", getPage(), rule.getKey())
                                .getString());
            }
        }
    }
}
