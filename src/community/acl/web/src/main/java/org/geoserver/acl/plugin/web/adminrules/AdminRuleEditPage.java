/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license (org.geoserver.geofence.server.web.GeofenceAdminRulePage)
 */
package org.geoserver.acl.plugin.web.adminrules;

import java.util.Iterator;
import lombok.NonNull;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.acl.domain.adminrules.AdminGrantType;
import org.geoserver.acl.plugin.web.adminrules.model.AdminRuleEditModel;
import org.geoserver.acl.plugin.web.adminrules.model.MutableAdminRule;
import org.geoserver.acl.plugin.web.components.ModelUpdatingAutoCompleteTextField;
import org.geoserver.acl.plugin.web.support.SerializableFunction;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;

@SuppressWarnings("serial")
public class AdminRuleEditPage extends GeoServerSecuredPage {

    private final Form<MutableAdminRule> form;

    protected FormComponent<Long> priority;
    protected FormComponent<String> roleChoice;
    protected FormComponent<String> userChoice;
    protected FormComponent<String> workspaceChoice;
    protected FormComponent<AdminGrantType> grantTypeChoice;

    private AdminRuleEditModel pageModel;
    private CompoundPropertyModel<MutableAdminRule> ruleModel;

    public AdminRuleEditPage(@NonNull AdminRuleEditModel pageModel) {
        this.pageModel = pageModel;

        ruleModel = pageModel.getModel();
        add(form = new Form<>("form", ruleModel));

        form.add(priority = priority());
        form.add(roleChoice = roleChoice());
        form.add(userChoice = userChoice());
        form.add(workspaceChoice = workspaceChoice());
        form.add(grantTypeChoice = grantTypeChoice());
        form.add(saveLink());
        form.add(new BookmarkablePageLink<>("cancel", AdminRulesACLPage.class));

        // feedback panel for error messages
        form.add(new FeedbackPanel("feedback"));
    }

    private FormComponent<String> roleChoice() {
        return autoCompleteChoice("roleName", ruleModel.bind("roleName"), pageModel::getRoleChoices);
    }

    private FormComponent<String> userChoice() {
        return autoCompleteChoice("userName", ruleModel.bind("userName"), pageModel::getUserChoices);
    }

    private FormComponent<String> workspaceChoice() {
        return autoCompleteChoice("workspace", ruleModel.bind("workspace"), pageModel::getWorkspaceChoices);
    }

    private AutoCompleteTextField<String> autoCompleteChoice(
            String id, IModel<String> model, SerializableFunction<String, Iterator<String>> choiceResolver) {

        AutoCompleteTextField<String> field = new ModelUpdatingAutoCompleteTextField<>(id, model, choiceResolver);
        field.setOutputMarkupId(true);
        field.setConvertEmptyInputStringToNull(true);
        return field;
    }

    private FormComponent<AdminGrantType> grantTypeChoice() {
        RadioGroup<AdminGrantType> group = new RadioGroup<>("access");
        group.add(new Radio<>("USER", Model.of(AdminGrantType.USER)));
        group.add(new Radio<>("ADMIN", Model.of(AdminGrantType.ADMIN)));
        return group;
    }

    private FormComponent<Long> priority() {
        return new TextField<Long>("priority").setRequired(true);
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
            doReturn(AdminRulesACLPage.class);
        } catch (Exception e) {
            error(e.getLocalizedMessage());
        }
    }

    protected GeoServerSecurityManager securityManager() {
        return GeoServerApplication.get().getSecurityManager();
    }
}
