/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.GeofenceRulePage)
 */
package org.geoserver.acl.plugin.web.accessrules;

import lombok.NonNull;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.geoserver.acl.plugin.web.accessrules.model.DataAccessRuleEditModel;
import org.geoserver.acl.plugin.web.accessrules.model.MutableRule;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * @author Niels Charlier - Originally as part of GeoFence's GeoServer extension
 * @author Gabriel Roldan (Camptocamp)
 * @see DataAccessRuleEditPanel
 */
@SuppressWarnings("serial")
public class DataAccessRuleEditPage extends GeoServerSecuredPage {

    private final Form<MutableRule> form;

    private @NonNull DataAccessRuleEditModel pageModel;

    public DataAccessRuleEditPage(@NonNull DataAccessRuleEditModel pageModel) {
        this.pageModel = pageModel;

        CompoundPropertyModel<MutableRule> model = pageModel.getModel();
        add(form = new Form<>("form", model));

        form.add(new DataAccessRuleEditPanel("ruleProperties", pageModel));

        // feedback panel for error messages
        form.add(new FeedbackPanel("feedback"));

        // build the submit/cancel
        form.add(saveLink());
        form.add(cancelLink());
    }

    private BookmarkablePageLink<MutableRule> cancelLink() {
        return new BookmarkablePageLink<>("cancel", AccessRulesACLPage.class);
    }

    private SubmitLink saveLink() {
        return new SubmitLink("save") {
            public @Override void onSubmit() {
                save();
            }
        };
    }

    private void save() {
        try {
            pageModel.save();
            doReturn(AccessRulesACLPage.class);
        } catch (Exception e) {
            error(e);
        }
    }
}
