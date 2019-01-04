/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.service;

import java.util.logging.Level;
import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.web.wicket.ParamResourceModel;

/** Edits an existing rule */
public class EditServiceAccessRulePage extends AbstractServiceAccessRulePage {

    ServiceAccessRule orig;

    public EditServiceAccessRulePage(ServiceAccessRule rule) {
        super(new ServiceAccessRule(rule));

        // save the original
        this.orig = rule;

        // set drop downs to disabled
        serviceChoice.setEnabled(false);
        methodChoice.setEnabled(false);
    }

    @Override
    protected void onFormSubmit(ServiceAccessRule rule) {
        try {
            ServiceAccessRuleDAO dao = ServiceAccessRuleDAO.get();

            // update the original
            orig.getRoles().clear();
            orig.getRoles().addAll(rolesFormComponent.getRolesNamesForStoring());

            dao.storeRules();
            doReturn(ServiceAccessRulePage.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving rule ", e);
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
        }
    }
}
