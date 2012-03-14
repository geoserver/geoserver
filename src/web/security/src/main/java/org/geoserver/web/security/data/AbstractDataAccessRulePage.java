/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.data;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.security.RolesFormComponent;
import org.geoserver.web.wicket.ParamResourceModel;

/**
 * Abstract page binding a {@link DataAccessRule}
 */
@SuppressWarnings("serial")
public abstract class AbstractDataAccessRulePage extends GeoServerSecuredPage {

    List<AccessMode> MODES = Arrays.asList(AccessMode.READ, AccessMode.WRITE, AccessMode.ADMIN);

    DropDownChoice workspace;

    DropDownChoice layer;

    DropDownChoice accessMode;

    RolesFormComponent rolesForComponent;

    Form form;

    public AbstractDataAccessRulePage(DataAccessRule rule) {
        setDefaultModel(new CompoundPropertyModel(new DataAccessRule(rule)));

        // build the form
        form = new Form("ruleForm");
        add(form);
        form.add(workspace = new DropDownChoice("workspace", getWorkspaceNames()));
        workspace.add(new AjaxFormComponentUpdatingBehavior("onchange") {

            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                layer.setChoices(new Model(getLayerNames((String) workspace.getConvertedInput())));
                layer.modelChanged();
                target.addComponent(layer);
            }
        });
        setOutputMarkupId(true);
        form.add(layer = new DropDownChoice("layer", getLayerNames(rule.getWorkspace())));
        layer.setOutputMarkupId(true);
        form.add(accessMode = new DropDownChoice("accessMode", MODES, new AccessModeRenderer()));
        form.add(rolesForComponent = new RolesFormComponent("roles", new RolesModel(rule), form,
                true));

        // build the submit/cancel
        form.add(new BookmarkablePageLink("cancel", DataAccessRulePage.class));
        form.add(saveLink());

        // add the validators
        workspace.setRequired(true);
        layer.setRequired(true);
        accessMode.setRequired(true);
    }

    SubmitLink saveLink() {
        return new SubmitLink("save") {
            @Override
            public void onSubmit() {
                onFormSubmit();
            }
        };
    }

    /**
     * Implements the actual save action
     */
    protected abstract void onFormSubmit();

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
    class AccessModeRenderer implements IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return (String) new ParamResourceModel(((AccessMode) object).name(), getPage())
                    .getObject();
        }

        public String getIdValue(Object object, int index) {
            return ((AccessMode) object).name();
        }

    }

    /**
     * Bridge between Set and List
     */
    static class RolesModel implements IModel {

        DataAccessRule rule;

        RolesModel(DataAccessRule rule) {
            this.rule = rule;
        }

        public Object getObject() {
            return new ArrayList<String>(rule.getRoles());
        }

        public void setObject(Object object) {
            rule.getRoles().clear();
            rule.getRoles().addAll((List<String>) object);
        }

        public void detach() {
            // nothing to do

        }

    }

}
