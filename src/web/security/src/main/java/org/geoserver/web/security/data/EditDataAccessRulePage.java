/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.data;

import java.util.logging.Level;

import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Edits an existing rule
 */
public class EditDataAccessRulePage extends AbstractDataAccessRulePage {

    public EditDataAccessRulePage(DataAccessRule rule) {
        super(rule);
    }

    @Override
    protected void onFormSubmit() {
        try {
            DataAccessRuleDAO dao = DataAccessRuleDAO.get();
            dao.addRule((DataAccessRule) getDefaultModelObject());
            dao.storeRules();
            setResponsePage(DataAccessRulePage.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving user", e);
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
        }
    }

}
