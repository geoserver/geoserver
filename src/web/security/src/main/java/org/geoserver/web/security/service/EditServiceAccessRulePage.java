/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.service;

import java.util.logging.Level;

import org.geoserver.security.impl.ServiceAccessRule;
import org.geoserver.security.impl.ServiceAccessRuleDAO;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Edits an existing rule
 */
public class EditServiceAccessRulePage extends AbstractServiceAccessRulePage {
    
    public EditServiceAccessRulePage(ServiceAccessRule rule) {
        super(rule);
    }

    @Override
    protected void onFormSubmit() {
        try {
            ServiceAccessRuleDAO dao = ServiceAccessRuleDAO.get();
            dao.addRule((ServiceAccessRule) getDefaultModelObject()); 
            dao.storeRules();
            setResponsePage(ServiceAccessRulePage.class);
        } catch(Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving service", e);
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
        }
    }
    

}
