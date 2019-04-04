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

/** Edits an existing rule */
public class EditDataAccessRulePage extends AbstractDataAccessRulePage {

    DataAccessRule orig;

    public EditDataAccessRulePage(DataAccessRule rule) {
        // pass a clone into parent to avoid changing original
        super(new DataAccessRule(rule));

        // save original
        this.orig = rule;
    }

    @Override
    protected void onFormSubmit(DataAccessRule rule) {
        try {
            DataAccessRuleDAO dao = DataAccessRuleDAO.get();

            // we cannot update the original because it might have been serialized
            // and thus detached, we'll update the rule that is the same as the original one instead
            dao.getRules()
                    .forEach(
                            r -> {
                                if (r.equals(orig)) {
                                    r.setRoot(rule.getRoot());
                                    r.setGlobalGroupRule(rule.isGlobalGroupRule());
                                    r.setLayer(rule.getLayer());
                                    r.setAccessMode(rule.getAccessMode());
                                    r.getRoles().clear();
                                    r.getRoles().addAll(rule.getRoles());
                                }
                            });
            dao.storeRules();
            doReturn(DataSecurityPage.class);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred while saving rule ", e);
            error(new ParamResourceModel("saveError", getPage(), e.getMessage()));
        }
    }
}
