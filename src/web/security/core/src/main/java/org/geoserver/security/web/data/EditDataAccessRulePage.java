/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import java.util.logging.Level;

import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.DataAccessRuleDAO;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Edits an existing rule
 */
public class EditDataAccessRulePage extends AbstractDataAccessRulePage {

    DataAccessRule orig;

    public EditDataAccessRulePage(DataAccessRule rule) {
        //pass a clone into parent to avoid changing original
        super(new DataAccessRule(rule));

        //save original
        this.orig = rule;
    }

    @Override
    protected void onFormSubmit(DataAccessRule rule) {
        try {
            DataAccessRuleDAO dao = DataAccessRuleDAO.get();

            //update original
            orig.setWorkspace(rule.getWorkspace());
            orig.setLayer(rule.getLayer());
            orig.setAccessMode(rule.getAccessMode());
            orig.getRoles().clear();
            orig.getRoles().addAll(rule.getRoles());

            dao.storeRules();
            doReturn(DataSecurityPage.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving rule ", e);
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
        }
    }

}
