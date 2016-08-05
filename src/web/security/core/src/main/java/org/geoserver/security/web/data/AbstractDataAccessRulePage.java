/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.role.RuleRolesFormComponent;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Abstract page binding a {@link DataAccessRule}
 */
@SuppressWarnings("serial")
public abstract class AbstractDataAccessRulePage extends AbstractSecurityPage {

    static List<AccessMode> MODES = Arrays.asList(AccessMode.READ, AccessMode.WRITE, AccessMode.ADMIN);

    DropDownChoice<String> workspaceChoice, layerChoice;
    DropDownChoice<AccessMode> accessModeChoice;
    RuleRolesFormComponent rolesFormComponent;

    public AbstractDataAccessRulePage(final DataAccessRule rule) {
        // build the form
        Form form = new Form<DataAccessRule>("form", new CompoundPropertyModel(rule));
        add(form);

        form.add (new EmptyRolesValidator());
        form.add(workspaceChoice = new DropDownChoice<String>("workspace", getWorkspaceNames()));
        workspaceChoice.setRequired(true);
        workspaceChoice.add(new AjaxFormComponentUpdatingBehavior("change") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                layerChoice.setChoices(new Model<ArrayList<String>>(
                    getLayerNames(workspaceChoice.getConvertedInput())));
                layerChoice.modelChanged();
                target.add(layerChoice);
            }
        });

        form.add(layerChoice = new DropDownChoice<String>("layer", getLayerNames(rule.getWorkspace())));
        layerChoice.setRequired(true);
        layerChoice.setOutputMarkupId(true);

        form.add(accessModeChoice = 
            new DropDownChoice<AccessMode>("accessMode", MODES, new AccessModeRenderer()));
        accessModeChoice.setRequired(true);

        form.add(rolesFormComponent = new RuleRolesFormComponent("roles",
            new PropertyModel(rule, "roles")).setHasAnyRole(
                rule.getRoles().contains(GeoServerRole.ANY_ROLE.getAuthority())));

        // build the submit/cancel
        form.add(new SubmitLink("save") {
            @Override
            public void onSubmit() {
                DataAccessRule rule = (DataAccessRule) getForm().getModelObject();
                if (rolesFormComponent.isHasAnyRole()) {
                    rule.getRoles().clear();
                    rule.getRoles().add(GeoServerRole.ANY_ROLE.getAuthority());
                }
                onFormSubmit(rule);
            }
        });
        form.add(new BookmarkablePageLink<DataAccessRule>("cancel", DataSecurityPage.class));
    }

    /**
     * Implements the actual save action
     */
    protected abstract void onFormSubmit(DataAccessRule rule);

    /**
     * Returns a sorted list of workspace names
     */
    ArrayList<String> getWorkspaceNames() {
        ArrayList<String> result = new ArrayList<String>();
        for (WorkspaceInfo ws : getCatalog().getWorkspaces()) {
            result.add(ws.getName());
        }
        Collections.sort(result);
        result.add(0, "*");
        return result;
    }

    /**
     * Returns a sorted list of layer names in the specified workspace (or * if the workspace is *)
     */
    ArrayList<String> getLayerNames(String workspaceName) {
        ArrayList<String> result = new ArrayList<String>();
        if (!workspaceName.equals("*")) {
            for (ResourceInfo r : getCatalog().getResources(ResourceInfo.class)) {
                if (r.getStore().getWorkspace().getName().equals(workspaceName))
                    result.add(r.getName());
            }
            Collections.sort(result);
        }
        result.add(0, "*");
        return result;
    }

    /**
     * Makes sure we see translated text, by the raw name is used for the model
     */
    class AccessModeRenderer extends ChoiceRenderer<AccessMode> {

        public Object getDisplayValue(AccessMode object) {
            return (String) new ParamResourceModel( object.name(), getPage())
                    .getObject();
        }

        public String getIdValue(AccessMode object, int index) {
            return object.name();
        }

    }

    class EmptyRolesValidator extends AbstractFormValidator {

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
           return new FormComponent[] { 
               workspaceChoice, layerChoice, accessModeChoice, rolesFormComponent };
        }

        @Override
        public void validate(Form<?> form) {
            // only validate on final submit
            if (form.findSubmittingButton() != form.get("save")) { 
                return;
            }

            updateModels();
            String roleInputString = rolesFormComponent.getPalette().getRecorderComponent().getInput();
            if ((roleInputString == null || roleInputString.trim().isEmpty()) && !rolesFormComponent.isHasAnyRole()) {
                form.error(new ParamResourceModel("emptyRoles", getPage()).getString());
            }
        }
    }

    protected void updateModels() {
        workspaceChoice.updateModel();
        layerChoice.updateModel();
        accessModeChoice.updateModel();
        rolesFormComponent.updateModel();
    }
}
