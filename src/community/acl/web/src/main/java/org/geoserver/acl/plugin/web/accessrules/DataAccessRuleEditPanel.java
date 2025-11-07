/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.acl.plugin.web.accessrules;

import java.util.Iterator;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormChoiceComponentUpdatingBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.event.Broadcast;
import org.apache.wicket.event.IEvent;
import org.apache.wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.Radio;
import org.apache.wicket.markup.html.form.RadioGroup;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.acl.domain.rules.CatalogMode;
import org.geoserver.acl.domain.rules.GrantType;
import org.geoserver.acl.plugin.web.accessrules.event.GrantTypeChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.event.LayerChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.event.PublishedInfoChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.event.WorkspaceChangeEvent;
import org.geoserver.acl.plugin.web.accessrules.layerdetails.LayerDetailsEditPanel;
import org.geoserver.acl.plugin.web.accessrules.model.DataAccessRuleEditModel;
import org.geoserver.acl.plugin.web.accessrules.model.LayerDetailsEditModel;
import org.geoserver.acl.plugin.web.accessrules.model.MutableLayerDetails;
import org.geoserver.acl.plugin.web.accessrules.model.MutableRule;
import org.geoserver.acl.plugin.web.accessrules.model.MutableRuleLimits;
import org.geoserver.acl.plugin.web.components.IPAddressRangeValidator;
import org.geoserver.acl.plugin.web.components.ModelUpdatingAutoCompleteTextField;
import org.geoserver.acl.plugin.web.components.PublishedInfoAutoCompleteTextField;
import org.geoserver.acl.plugin.web.support.SerializableFunction;

/**
 * @see RuleLimitsEditPanel
 * @see LayerDetailsEditPanel
 * @see ModelUpdatingAutoCompleteTextField
 * @see IPAddressRangeValidator
 */
@SuppressWarnings("serial")
class DataAccessRuleEditPanel extends FormComponentPanel<MutableRule> {

    protected final FormComponent<GrantType> grantType;

    protected final FormComponent<Long> priority;

    protected final FormComponent<String> role;
    protected final FormComponent<String> user;
    protected final FormComponent<String> addressRange;

    protected final DropDownChoice<String> service;
    protected final DropDownChoice<String> request;
    protected final Label subfieldLabel;
    protected final FormComponent<String> subfield;
    protected final FormComponent<String> workspace;
    protected final FormComponent<String> layer;

    /** Used to hide/show the catalog mode form component */
    protected final WebMarkupContainer catalogModeContainer;

    protected final FormComponent<CatalogMode> catalogMode;

    /** Used to hide/show the {@link MutableRuleLimits} form component */
    protected final WebMarkupContainer ruleLimitsContainer;

    /** Used to hide/show the {@link MutableLayerDetails} form component */
    protected final WebMarkupContainer layerDetailsContainer;

    private DataAccessRuleEditModel pageModel;

    public DataAccessRuleEditPanel(String id, DataAccessRuleEditModel pageModel) {
        super(id, pageModel.getModel());
        this.pageModel = pageModel;
        add(addressRange = addressRangeChoice());
        add(grantType = grantTypeChoice());

        catalogMode = catalogModeChoice();
        add(catalogModeContainer = catalogModeContainer());

        add(priority = priority());
        add(role = roleChoice());
        add(user = userChoice());

        add(service = serviceChoice());
        add(request = requestChoice());
        add(subfieldLabel = subfieldLabel());
        add(subfield = subfieldChoice());

        add(workspace = workspaceChoice());
        add(layer = layerChoice(workspace));

        FormComponent<MutableRuleLimits> ruleLimits = ruleLimits();
        add(ruleLimitsContainer = ruleLimitsContainer(ruleLimits));

        FormComponent<MutableLayerDetails> layerDetails = layerDetails();
        add(layerDetailsContainer = layerDetailsContainer(layerDetails));

        initVisibility();
    }

    private void initVisibility() {
        this.updateCatalogModeVisibility(null);
        this.updateRuleLimitsVisibility(null);
    }

    @Override
    public void convertInput() {
        MutableRule rule = getModel().getObject();
        setConvertedInput(rule);
    }

    private CompoundPropertyModel<MutableRule> model() {
        return (CompoundPropertyModel<MutableRule>) super.getModel();
    }

    private FormComponent<String> addressRangeChoice() {
        TextField<String> field = new TextField<>("addressRange");
        field.add(new IPAddressRangeValidator());
        return field;
    }

    private FormComponent<Long> priority() {
        NumberTextField<Long> p = new NumberTextField<>("priority");
        p.setRequired(true);
        p.setType(Long.class);
        return p;
    }

    private FormComponent<String> roleChoice() {
        return autoCompleteChoice("roleName", model().bind("roleName"), pageModel::getRoleChoices);
    }

    private FormComponent<String> userChoice() {
        return autoCompleteChoice("userName", model().bind("userName"), pageModel::getUserChoices);
    }

    private DropDownChoice<String> serviceChoice() {
        DropDownChoice<String> serviceChoice = new DropDownChoice<>("service", pageModel.findServiceNames());
        serviceChoice.add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                onServiceChange(target);
            }
        });
        serviceChoice.setNullValid(true);
        return serviceChoice;
    }

    private void onServiceChange(AjaxRequestTarget target) {
        String serviceName = service.getConvertedInput();
        request.setChoices(pageModel.findOperationNames(serviceName));
        request.getModel().setObject(null);
        request.modelChanged();
        target.add(request);

        boolean isWPS = "WPS".equalsIgnoreCase(serviceName);
        subfield.setVisible(isWPS);
        subfieldLabel.setVisible(isWPS);
        target.add(subfield);
        target.add(subfieldLabel);
    }

    private DropDownChoice<String> requestChoice() {
        List<String> choices = pageModel.findOperationNames(model().getObject().getService());
        CaseConversionRenderer renderer = new CaseConversionRenderer();

        DropDownChoice<String> requestChoice = new DropDownChoice<>("request", choices, renderer);
        requestChoice.setOutputMarkupId(true);
        requestChoice.setNullValid(true);
        return requestChoice;
    }

    private TextField<String> subfieldChoice() {
        AutoCompleteTextField<String> subfieldChoice =
                autoCompleteChoice("subfield", model().bind("subfield"), pageModel::getSubfieldChoices);

        subfieldChoice.setOutputMarkupId(true);
        subfieldChoice.setOutputMarkupPlaceholderTag(true);
        boolean isWPS = "WPS".equalsIgnoreCase(model().getObject().getService());
        subfieldChoice.setVisible(isWPS);

        return subfieldChoice;
    }

    private Label subfieldLabel() {
        Label l = new Label("subfieldLabel", new ResourceModel("subfield"));
        l.setOutputMarkupId(true);
        l.setOutputMarkupPlaceholderTag(true);
        boolean isWPS = "WPS".equalsIgnoreCase(model().getObject().getService());
        l.setVisible(isWPS);
        return l;
    }

    /**
     * A form component to select a workspace.
     *
     * <p>{@link #send sends} a {@link WorkspaceChangeEvent} event whenever the component's model value changes, even
     * with a partial workspace name that wouldn't match an actual catalog workspace
     */
    private FormComponent<String> workspaceChoice() {
        AutoCompleteTextField<String> choice =
                autoCompleteChoice("workspace", model().bind("workspace"), pageModel::getWorkspaceChoices);

        choice.add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                String workspaceName = choice.getConvertedInput();
                send(getPage(), Broadcast.BREADTH, new WorkspaceChangeEvent(workspaceName, target));
            }
        });

        return choice;
    }

    /**
     * A form component to select a layer.
     *
     * <p>{@link #send sends} a {@link LayerChangeEvent} event whenever the component's model value changes, even with a
     * partial layer name that wouldn't match an actual catalog layer
     */
    private FormComponent<String> layerChoice(final FormComponent<String> workspaceComponent) {

        final IModel<String> layerModel = model().bind("layer");
        PublishedInfoAutoCompleteTextField layerChoice =
                new PublishedInfoAutoCompleteTextField("layer", layerModel, pageModel::getLayerChoices);

        layerChoice.add(new OnChangeAjaxBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                String layer = layerChoice.getConvertedInput();
                send(getPage(), Broadcast.BREADTH, new LayerChangeEvent(layer, target));
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

    private FormComponent<MutableRuleLimits> ruleLimits() {
        IModel<MutableRuleLimits> model = model().bind("ruleLimits");
        RuleLimitsEditPanel panel = new RuleLimitsEditPanel("ruleLimits", model);
        panel.setRequired(true);
        panel.setOutputMarkupPlaceholderTag(true);
        return panel;
    }

    private WebMarkupContainer catalogModeContainer() {
        WebMarkupContainer container = new WebMarkupContainer("catalogModeContainer");
        container.setOutputMarkupPlaceholderTag(true);
        container.add(catalogMode);
        return container;
    }

    private WebMarkupContainer ruleLimitsContainer(FormComponent<MutableRuleLimits> ruleLimits) {
        WebMarkupContainer container = new WebMarkupContainer("ruleLimitsContainer");
        container.setOutputMarkupPlaceholderTag(true);
        container.add(ruleLimits);
        return container;
    }

    private FormComponent<MutableLayerDetails> layerDetails() {
        LayerDetailsEditModel layerDetailsEditModel = pageModel.layerDetails();
        LayerDetailsEditPanel layerDetails = new LayerDetailsEditPanel("layerDetails", layerDetailsEditModel);
        layerDetails.setOutputMarkupPlaceholderTag(true);
        return layerDetails;
    }

    private WebMarkupContainer layerDetailsContainer(FormComponent<MutableLayerDetails> layerDetails) {
        WebMarkupContainer container = new WebMarkupContainer("layerDetailsContainer");
        container.setOutputMarkupPlaceholderTag(true);
        container.add(layerDetails);
        return container;
    }

    private FormComponent<GrantType> grantTypeChoice() {
        RadioGroup<GrantType> grantType = new RadioGroup<>("grantType", model().bind("access"));
        for (GrantType grant : GrantType.values()) {
            grantType.add(new Radio<>(grant.toString(), Model.of(grant), grantType));
        }
        grantType.add(new AjaxFormChoiceComponentUpdatingBehavior() {
            protected @Override void onUpdate(AjaxRequestTarget target) {
                GrantType chosen = grantType.getModel().getObject();
                send(new GrantTypeChangeEvent(chosen, target));
            }
        });

        grantType.setRequired(true);
        return grantType;
    }

    private <T> void send(T payload) {
        send(getPage(), Broadcast.BREADTH, payload);
    }

    public @Override void onEvent(IEvent<?> event) {
        Object payload = event.getPayload();
        if (payload instanceof WorkspaceChangeEvent changeEvent2) {
            onWorkspaceChangeEvent(changeEvent2);
        } else if (payload instanceof LayerChangeEvent) {
            onLayerChangeEvent((LayerChangeEvent) event.getPayload());
        } else if (payload instanceof GrantTypeChangeEvent changeEvent1) {
            onGrantTypeChanged(changeEvent1);
        } else if (payload instanceof PublishedInfoChangeEvent changeEvent) {
            onPublishedInfoChangeEvent(changeEvent);
        }
    }

    private void onWorkspaceChangeEvent(WorkspaceChangeEvent event) {
        pageModel.workSpaceNameChanged(event.getWorkspace(), event.getTarget()).ifPresent(this::send);
    }

    private void onLayerChangeEvent(LayerChangeEvent event) {
        updateCatalogModeVisibility(event.getTarget());
        pageModel.layerNameChanged(event.getLayer(), event.getTarget()).ifPresent(this::send);
    }

    private void updateCatalogModeVisibility(AjaxRequestTarget target) {
        boolean catalogModeVisible = catalogModeContainer.isVisible();
        boolean showCatalogMode = pageModel.isShowCatalogMode();
        if (catalogModeVisible != showCatalogMode) {
            catalogModeContainer.setVisible(showCatalogMode);
            if (null != target) target.add(catalogModeContainer);
        }
    }

    private void onGrantTypeChanged(GrantTypeChangeEvent event) {
        updateCatalogModeVisibility(event.getTarget());
        updateRuleLimitsVisibility(event.getTarget());
    }

    private void updateRuleLimitsVisibility(AjaxRequestTarget target) {
        boolean visible = ruleLimitsContainer.isVisible();
        boolean showRuleLimits = pageModel.isShowRuleLimits();
        if (visible != showRuleLimits) {
            ruleLimitsContainer.setVisible(showRuleLimits);
            if (null != target) target.add(ruleLimitsContainer);
        }
    }

    private void onPublishedInfoChangeEvent(PublishedInfoChangeEvent payload) {}

    private FormComponent<CatalogMode> catalogModeChoice() {
        RadioGroup<CatalogMode> catalogMode = new RadioGroup<>("catalogMode");
        for (CatalogMode mode : CatalogMode.values()) {
            Radio<CatalogMode> radio = new Radio<>(mode.toString(), Model.of(mode), catalogMode);
            radio.setOutputMarkupPlaceholderTag(true);
            catalogMode.add(radio);
        }
        catalogMode.setRequired(true);
        return catalogMode;
    }

    /** Makes sure that while rendered in mixed case, is stored in uppercase */
    private static class CaseConversionRenderer extends ChoiceRenderer<String> {
        public @Override String getDisplayValue(String object) {
            return object;
        }

        public @Override String getIdValue(String object, int index) {
            return object.toUpperCase();
        }
    }
}
