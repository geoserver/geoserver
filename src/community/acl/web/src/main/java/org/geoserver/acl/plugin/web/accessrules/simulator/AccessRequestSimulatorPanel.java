/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules.simulator;

import com.google.common.collect.Streams;
import java.util.Iterator;
import java.util.Set;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.acl.authorization.AccessInfo;
import org.geoserver.acl.plugin.web.accessrules.event.LayerChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.event.WorkspaceChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.model.AccessRequestSimulatorModel;
import org.geoserver.acl.plugin.web.accessrules.model.MutableAccessRequest;
import org.geoserver.acl.plugin.web.accessrules.model.MutableRule;
import org.geoserver.acl.plugin.web.components.ModelUpdatingAutoCompleteTextField;
import org.geoserver.acl.plugin.web.components.PublishedInfoAutoCompleteTextField;
import org.geoserver.acl.plugin.web.components.RulesTablePanel;
import org.geoserver.acl.plugin.web.components.Select2SetMultiChoice;
import org.geoserver.acl.plugin.web.support.SerializableFunction;
import org.geoserver.acl.plugin.web.support.SerializablePredicate;
import org.wicketstuff.select2.ChoiceProvider;
import org.wicketstuff.select2.Response;
import org.wicketstuff.select2.StringTextChoiceProvider;

@SuppressWarnings("serial")
public class AccessRequestSimulatorPanel extends Panel {

    private AccessRequestSimulatorModel panelModel;
    private RulesTablePanel<MutableRule> rulesTable;

    protected final FormComponent<String> user;
    protected final FormComponent<Set<String>> roles;
    protected final FormComponent<String> ipAddress;

    protected final FormComponent<String> service;
    protected final FormComponent<String> request;
    protected final FormComponent<String> subfield;
    protected final Label subfieldLabel;

    protected final FormComponent<String> workspace;
    protected final FormComponent<String> layer;

    private AccessInfoPanel accessInfo;
    private WebMarkupContainer noMatchingRulesDiv;

    public AccessRequestSimulatorPanel(String id, RulesTablePanel<MutableRule> rulesTable) {
        super(id);
        this.rulesTable = rulesTable;
        this.panelModel = new AccessRequestSimulatorModel();
        this.setOutputMarkupPlaceholderTag(true);

        add(user = userChoice());
        add(roles = rolesChoice());
        add(ipAddress = ipAddressChoice());

        add(service = serviceChoice());
        add(request = requestChoice());
        add(subfield = subfieldChoice());
        add(subfieldLabel = subfieldLabel());

        add(workspace = workspaceChoice());
        add(layer = layerChoice(workspace));

        add(clearButton());
        add(closeButton());
        add(accessInfo = accessInfoPanel());
        add(noMatchingRulesDiv = noMatchingRulesDiv());

        syncPanelModel();
    }

    private Component clearButton() {
        AjaxLink<Object> link = new AjaxLink<>("clearSimulator") {
            public @Override void onClick(AjaxRequestTarget target) {
                clear(target);
            }
        };
        link.setOutputMarkupId(true);
        return link;
    }

    private Component closeButton() {
        AjaxLink<Object> link = new AjaxLink<>("closeSimulator") {
            public @Override void onClick(AjaxRequestTarget target) {
                close(target);
            }
        };
        link.setOutputMarkupId(true);
        return link;
    }

    protected @Override void onBeforeRender() {
        setNoMatchingRulesVisibility();
        super.onBeforeRender();
    }

    private WebMarkupContainer noMatchingRulesDiv() {
        WebMarkupContainer noMatchingRules = new WebMarkupContainer("noMatchingRules");
        noMatchingRules.setOutputMarkupPlaceholderTag(true);
        return noMatchingRules;
    }

    private AccessInfoPanel accessInfoPanel() {
        IModel<AccessInfo> model = panelModel.getAccessInfoModel();
        AccessInfoPanel panel = new AccessInfoPanel("accessInfo", model);
        panel.setOutputMarkupPlaceholderTag(true);
        return panel;
    }

    private void syncPanelModel() {
        this.setDefaultModel(panelModel.getModel());
        this.user.add(new ModelChangeNotifier());
        this.roles.add(new ModelChangeNotifier());
        this.ipAddress.add(new ModelChangeNotifier());
        this.service.add(new ModelChangeNotifier());
        this.request.add(new ModelChangeNotifier());
        this.subfield.add(new ModelChangeNotifier());
        this.workspace.add(new ModelChangeNotifier());
        this.layer.add(new ModelChangeNotifier());
    }

    private void clear(AjaxRequestTarget target) {
        panelModel.clear();
        user.modelChanged();
        roles.modelChanged();
        ipAddress.modelChanged();
        service.modelChanged();
        request.modelChanged();
        subfield.modelChanged();
        workspace.modelChanged();
        layer.modelChanged();
        this.modelChanged();
        target.add(user);
        target.add(roles);
        target.add(ipAddress);
        target.add(service);
        target.add(request);
        target.add(subfield);
        target.add(workspace);
        target.add(layer);
        target.add(this);
        onModelChanged(target);
    }

    private class ModelChangeNotifier extends OnChangeAjaxBehavior {
        protected @Override void onUpdate(AjaxRequestTarget target) {
            onModelChanged(target);
        }
    }

    void onModelChanged(AjaxRequestTarget target) {
        if (this.isVisible()) {
            boolean resultChanged = panelModel.runSimulation();
            if (resultChanged) {
                setNoMatchingRulesVisibility();
                target.add(noMatchingRulesDiv);

                rulesTable.modelChanged();
                target.add(rulesTable);

                accessInfo.modelChanged();
                target.add(accessInfo);
            }
        }
    }

    private void setNoMatchingRulesVisibility() {
        noMatchingRulesDiv.setVisible(!panelModel.isMatchingRules());
    }

    public void open(AjaxRequestTarget target) {
        SerializablePredicate<MutableRule> filter = panelModel.getMatchingRulesFilter();
        setOpen(true, filter);
        target.add(this);
        target.add(rulesTable);
    }

    public void close(AjaxRequestTarget target) {
        setOpen(false, null);
        target.add(this);
        target.add(rulesTable);
    }

    private void setOpen(boolean open, SerializablePredicate<MutableRule> filter) {
        rulesTable.getDataProvider().setFilter(filter);
        rulesTable.setFilteredMode(open);
        this.setVisible(open);
    }

    private CompoundPropertyModel<MutableAccessRequest> model() {
        return panelModel.getModel();
    }

    private FormComponent<Set<String>> rolesChoice() {
        IModel<Set<String>> model = model().bind("roles");
        ChoiceProvider<String> choiceProvider = new RolesChoiceProvider();
        Select2SetMultiChoice<String> roles;
        roles = new Select2SetMultiChoice<>("roles", model, choiceProvider);
        roles.setOutputMarkupId(true);
        roles.getSettings().setPlaceholder("*");
        return roles;
    }

    private class RolesChoiceProvider extends StringTextChoiceProvider {
        public @Override void query(String term, int page, Response<String> response) {
            Streams.stream(panelModel.getRoleChoices(term)).forEach(response::add);
        }
    }

    private FormComponent<String> userChoice() {
        IModel<String> model = model().bind("user");
        AutoCompleteTextField<String> user = autoCompleteChoice("userName", model, panelModel::getUserChoices);
        user.add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                Set<String> roleNames = panelModel.findUserRoles();
                if (!roleNames.isEmpty()) {
                    roles.setModelObject(roleNames);
                    target.add(roles);
                }
            }
        });
        return user;
    }

    private FormComponent<String> ipAddressChoice() {
        IModel<String> model = model().bind("sourceAddress");
        return new TextField<>("sourceAddress", model);
    }

    private FormComponent<String> serviceChoice() {
        IModel<String> model = model().bind("service");
        AutoCompleteTextField<String> serviceChoice =
                autoCompleteChoice("service", model, panelModel::getServiceChoices);
        serviceChoice.add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                onServiceChange(target);
            }
        });
        return serviceChoice;
    }

    private void onServiceChange(AjaxRequestTarget target) {
        String serviceName = service.getConvertedInput();
        request.setModelObject(null);
        target.add(request);

        subfield.setModelObject(null);
        boolean isWPS = "WPS".equalsIgnoreCase(serviceName);
        subfield.setVisible(isWPS);
        subfieldLabel.setVisible(isWPS);
        target.add(subfield);
        target.add(subfieldLabel);
    }

    private FormComponent<String> requestChoice() {
        IModel<String> model = model().bind("request");
        AutoCompleteTextField<String> requestChoice;
        requestChoice = autoCompleteChoice("request", model, panelModel::getRequestChoices);
        requestChoice.setOutputMarkupId(true);
        return requestChoice;
    }

    private TextField<String> subfieldChoice() {
        IModel<String> model = model().bind("subfield");
        AutoCompleteTextField<String> subfieldChoice;
        subfieldChoice = autoCompleteChoice("subfield", model, panelModel::getSubfieldChoices);

        subfieldChoice.setOutputMarkupPlaceholderTag(true);
        boolean isWPS = "WPS".equalsIgnoreCase(model().getObject().getService());
        subfieldChoice.setVisible(isWPS);
        return subfieldChoice;
    }

    private Label subfieldLabel() {
        Label label = new Label("subfieldLabel", new ResourceModel("subfield"));
        label.setOutputMarkupPlaceholderTag(true);
        boolean isWPS = "WPS".equalsIgnoreCase(model().getObject().getService());
        label.setVisible(isWPS);
        return label;
    }

    /**
     * A form component to select a workspace.
     *
     * <p>{@link #send sends} a {@link WorkspaceChangeEvent} event whenever the component's model value changes, even
     * with a partial workspace name that wouldn't match an actual catalog workspace
     */
    private FormComponent<String> workspaceChoice() {
        IModel<String> model = model().bind("workspace");
        AutoCompleteTextField<String> choice;
        choice = autoCompleteChoice("workspace", model, panelModel::getWorkspaceChoices);

        choice.add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                String workspaceName = choice.getConvertedInput();
                send(
                        AccessRequestSimulatorPanel.this,
                        Broadcast.BREADTH,
                        new WorkspaceChangeEvent(workspaceName, target));
            }
        });

        return choice;
    }

    /**
     * A form component to select a layer from the selected workspace.
     *
     * <p>{@link #send sends} a {@link LayerChangeEvent} event whenever the component's model value changes, even with a
     * partial layer name that wouldn't match an actual catalog layer
     */
    private FormComponent<String> layerChoice(final FormComponent<String> workspaceComponent) {

        final IModel<String> layerModel = model().bind("layer");
        PublishedInfoAutoCompleteTextField layerChoice =
                new PublishedInfoAutoCompleteTextField("layer", layerModel, panelModel::getLayerChoices);

        layerChoice.add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                String layer = layerChoice.getConvertedInput();
                send(AccessRequestSimulatorPanel.this, Broadcast.BREADTH, new LayerChangeEvent(layer, target));
            }
        });
        return layerChoice;
    }

    private AutoCompleteTextField<String> autoCompleteChoice(
            String id, IModel<String> model, SerializableFunction<String, Iterator<String>> choiceResolver) {

        AutoCompleteTextField<String> field;
        field = new ModelUpdatingAutoCompleteTextField<>(id, model, choiceResolver);
        field.setOutputMarkupId(true);
        field.setConvertEmptyInputStringToNull(true);
        return field;
    }
}
