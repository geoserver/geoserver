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
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.form.validation.AbstractFormValidator;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.security.AccessMode;
import org.geoserver.security.impl.DataAccessRule;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.web.AbstractSecurityPage;
import org.geoserver.security.web.role.RuleRolesFormComponent;
import org.geoserver.web.wicket.ParamResourceModel;
import org.opengis.filter.Filter;

/** Abstract page binding a {@link DataAccessRule} */
@SuppressWarnings("serial")
public abstract class AbstractDataAccessRulePage extends AbstractSecurityPage {

    public class RootLabelModel extends LoadableDetachableModel<String> {

        @Override
        protected String load() {
            if (globalGroupRule.getModelObject()) {
                return new ParamResourceModel("globalGroup", AbstractDataAccessRulePage.this)
                        .getString();
            } else {
                return new ParamResourceModel("workspace", AbstractDataAccessRulePage.this)
                        .getString();
            }
        }
    }

    public class RootsModel extends LoadableDetachableModel<List<String>> {

        @Override
        protected List<String> load() {
            if (globalGroupRule.getModelObject()) {
                return getGlobalLayerGroupNames();
            } else {
                return getWorkspaceNames();
            }
        }

        /** Returns a sorted list of global layer group names */
        List<String> getGlobalLayerGroupNames() {
            Stream<String> names =
                    getCatalog()
                            .getLayerGroupsByWorkspace(CatalogFacade.NO_WORKSPACE)
                            .stream()
                            .map(lg -> lg.getName())
                            .sorted();
            return Stream.concat(Stream.of("*"), names).collect(Collectors.toList());
        }

        /** Returns a sorted list of workspace names */
        List<String> getWorkspaceNames() {
            Stream<String> names =
                    getCatalog().getWorkspaces().stream().map(ws -> ws.getName()).sorted();
            return Stream.concat(Stream.of("*"), names).collect(Collectors.toList());
        }
    }

    static List<AccessMode> MODES =
            Arrays.asList(AccessMode.READ, AccessMode.WRITE, AccessMode.ADMIN);

    DropDownChoice<String> rootChoice, layerChoice;
    DropDownChoice<AccessMode> accessModeChoice;
    RuleRolesFormComponent rolesFormComponent;
    CheckBox globalGroupRule;

    WebMarkupContainer layerContainer;

    Label rootLabel;

    WebMarkupContainer layerAndLabel;

    public AbstractDataAccessRulePage(final DataAccessRule rule) {
        // build the form
        Form form = new Form<DataAccessRule>("form", new CompoundPropertyModel(rule));
        add(form);
        form.add(new EmptyRolesValidator());

        form.add(globalGroupRule = new CheckBox("globalGroupRule"));
        globalGroupRule.setOutputMarkupId(true);
        globalGroupRule.add(
                new OnChangeAjaxBehavior() {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        rootChoice.getModel().detach();
                        target.add(rootChoice);
                        layerAndLabel.setVisible(!globalGroupRule.getModelObject());
                        target.add(layerContainer);
                        rootLabel.getDefaultModel().detach();
                        target.add(rootLabel);
                    }
                });

        form.add(rootLabel = new Label("rootLabel", new RootLabelModel()));
        rootLabel.setOutputMarkupId(true);
        form.add(rootChoice = new DropDownChoice<String>("root", new RootsModel()));
        rootChoice.setRequired(true);
        rootChoice.setOutputMarkupId(true);
        rootChoice.add(
                new AjaxFormComponentUpdatingBehavior("change") {
                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        layerChoice.setChoices(
                                new Model<ArrayList<String>>(
                                        getLayerNames(rootChoice.getConvertedInput())));
                        layerChoice.modelChanged();
                        target.add(layerChoice);
                    }
                });

        form.add(layerContainer = new WebMarkupContainer("layerContainer"));
        layerContainer.setOutputMarkupId(true);
        layerContainer.add(layerAndLabel = new WebMarkupContainer("layerAndLabel"));
        layerAndLabel.add(
                layerChoice = new DropDownChoice<String>("layer", getLayerNames(rule.getRoot())));
        layerAndLabel.setVisible(!rule.isGlobalGroupRule());
        layerChoice.setRequired(true);
        layerChoice.setOutputMarkupId(true);

        form.add(
                accessModeChoice =
                        new DropDownChoice<AccessMode>(
                                "accessMode", MODES, new AccessModeRenderer()));
        accessModeChoice.setRequired(true);

        form.add(
                rolesFormComponent =
                        new RuleRolesFormComponent("roles", new PropertyModel(rule, "roles"))
                                .setHasAnyRole(
                                        rule.getRoles()
                                                .contains(GeoServerRole.ANY_ROLE.getAuthority())));

        // build the submit/cancel
        form.add(
                new SubmitLink("save") {
                    @Override
                    public void onSubmit() {
                        DataAccessRule rule = (DataAccessRule) getForm().getModelObject();
                        if (rolesFormComponent.isHasAnyRole()) {
                            rule.getRoles().clear();
                            rule.getRoles().add(GeoServerRole.ANY_ROLE.getAuthority());
                        }
                        if (globalGroupRule.getModelObject()) {
                            // just to be on the safe side
                            rule.setLayer(null);
                        }
                        onFormSubmit(rule);
                    }
                });
        form.add(new BookmarkablePageLink<DataAccessRule>("cancel", DataSecurityPage.class));
    }

    /** Implements the actual save action */
    protected abstract void onFormSubmit(DataAccessRule rule);

    /**
     * Returns a sorted list of layer names in the specified workspace (or * if the workspace is *)
     */
    ArrayList<String> getLayerNames(String rootName) {
        ArrayList<String> result = new ArrayList<String>();
        if (!rootName.equals("*")) {
            Filter wsResources = Predicates.equal("store.workspace.name", rootName);
            try (CloseableIterator<ResourceInfo> it =
                    getCatalog().list(ResourceInfo.class, wsResources)) {
                while (it.hasNext()) {
                    result.add(it.next().getName());
                }
            }
            // collect also layer groups
            getCatalog()
                    .getLayerGroupsByWorkspace(rootName)
                    .stream()
                    .map(lg -> lg.getName())
                    .forEach(
                            name -> {
                                if (!result.contains(name)) {
                                    result.add(name);
                                }
                            });
            Collections.sort(result);
        }
        result.add(0, "*");
        return result;
    }

    /** Makes sure we see translated text, by the raw name is used for the model */
    class AccessModeRenderer extends ChoiceRenderer<AccessMode> {

        public Object getDisplayValue(AccessMode object) {
            return (String) new ParamResourceModel(object.name(), getPage()).getObject();
        }

        public String getIdValue(AccessMode object, int index) {
            return object.name();
        }
    }

    class EmptyRolesValidator extends AbstractFormValidator {

        @Override
        public FormComponent<?>[] getDependentFormComponents() {
            return new FormComponent[] {
                rootChoice, layerChoice, accessModeChoice, rolesFormComponent
            };
        }

        @Override
        public void validate(Form<?> form) {
            // only validate on final submit
            if (form.findSubmittingButton() != form.get("save")) {
                return;
            }

            updateModels();
            String roleInputString =
                    rolesFormComponent.getPalette().getRecorderComponent().getInput();
            if ((roleInputString == null || roleInputString.trim().isEmpty())
                    && !rolesFormComponent.isHasAnyRole()) {
                form.error(new ParamResourceModel("emptyRoles", getPage()).getString());
            }
        }
    }

    protected void updateModels() {
        rootChoice.updateModel();
        layerChoice.updateModel();
        accessModeChoice.updateModel();
        rolesFormComponent.updateModel();
    }
}
