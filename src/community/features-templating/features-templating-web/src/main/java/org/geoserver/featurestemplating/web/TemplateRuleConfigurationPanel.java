/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.featurestemplating.configuration.TemplateRule;
import org.geoserver.featurestemplating.configuration.TemplateRuleService;
import org.geoserver.util.XCQL;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.filter.text.cql2.CQLException;

public class TemplateRuleConfigurationPanel extends Panel {

    CompoundPropertyModel<TemplateRule> templateRuleModel;
    TemplateRulesTablePanel tablePanel;
    Form<TemplateRule> theForm;
    NumberTextField<Integer> priorityField;
    DropDownChoice<TemplateInfo> templateInfoDropDownChoice;
    OutputFormatsDropDown mimeTypeDropDown;
    TextArea<String> cqlFilterArea;
    TextArea<String> profileField;
    FeedbackPanel ruleFeedbackPanel;
    LayerInfo layer;
    Label submitLabel;
    Model<String> submitLabelModel;
    Label panelLabel;
    Model<String> panelLabelModel;

    public TemplateRuleConfigurationPanel(
            String id,
            CompoundPropertyModel<TemplateRule> model,
            boolean isUpdate,
            LayerInfo layer) {
        super(id, model);
        this.layer = layer;
        this.templateRuleModel = model;
        initUI(templateRuleModel, isUpdate);
    }

    private void initUI(CompoundPropertyModel<TemplateRule> model, boolean isUpdate) {
        panelLabel =
                new Label(
                        "ruleConfigurationLabel",
                        panelLabelModel = Model.of(getPanelLabelValue("add")));
        panelLabel.setOutputMarkupId(true);
        add(panelLabel);
        this.theForm = new Form<>("theForm", model);
        theForm.setOutputMarkupId(true);
        theForm.add(ruleFeedbackPanel = new FeedbackPanel("ruleFeedback"));
        ruleFeedbackPanel.setOutputMarkupId(true);
        add(theForm);

        priorityField = new NumberTextField<Integer>("priority", model.bind("priority"));
        theForm.add(priorityField);
        ChoiceRenderer<TemplateInfo> templateInfoChoicheRenderer =
                new ChoiceRenderer<>("fullName", "identifier");
        templateInfoDropDownChoice =
                new DropDownChoice<>(
                        "templateIdentifier",
                        model.bind("templateInfo"),
                        getTemplateInfoList(),
                        templateInfoChoicheRenderer);
        templateInfoDropDownChoice.setOutputMarkupId(true);
        theForm.add(templateInfoDropDownChoice);

        mimeTypeDropDown = new OutputFormatsDropDown("outputFormats", model.bind("outputFormat"));
        mimeTypeDropDown.setOutputMarkupId(true);
        theForm.add(mimeTypeDropDown);

        profileField = new TextArea<>("profileFilter", model.bind("profileFilter"));
        profileField.setOutputMarkupId(true);
        profileField.add(getCqlValidator());
        theForm.add(profileField);

        cqlFilterArea = new TextArea<>("cqlFilter", model.bind("cqlFilter"));
        cqlFilterArea.setOutputMarkupId(true);
        cqlFilterArea.add(getCqlValidator());
        theForm.add(cqlFilterArea);

        AjaxSubmitLink submitLink =
                new AjaxSubmitLink("save") {
                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onSubmit(target, form);
                        cleanFeedbackPanel();
                        TemplateRule rule = theForm.getModelObject();
                        if (!validateAndReport(rule)) return;
                        updateModelRules(rule);
                        target.add(tablePanel);
                        target.add(tablePanel.getTable());
                        clearForm(target);
                    }

                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        if (theForm.hasError()) target.add(ruleFeedbackPanel);
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        super.onError(target, form);
                        if (theForm.hasError()) target.add(ruleFeedbackPanel);
                    }
                };
        submitLabel =
                new Label("submitLabel", submitLabelModel = Model.of(getSubmitLabelValue("add")));
        submitLabel.setOutputMarkupId(true);
        submitLink.add(submitLabel);
        theForm.add(submitLink);
        theForm.add(
                new AjaxSubmitLink("cancel") {

                    @Override
                    public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        clearForm(target);
                    }
                });
    }

    private boolean validateAndReport(TemplateRule rule) {
        boolean result = true;
        try {
            TemplateModelsValidator validator = new TemplateModelsValidator();
            validator.validate(rule);
        } catch (TemplateConfigurationException e) {
            theForm.error(e.getMessage());
            result = false;
        }
        return result;
    }

    protected List<TemplateInfo> getTemplateInfoList() {
        ResourceInfo resourceInfo = layer.getResource();
        return TemplateInfoDAO.get().findByFeatureTypeInfo((FeatureTypeInfo) resourceInfo);
    }

    void setTemplateRuleTablePanel(TemplateRulesTablePanel panel) {
        this.tablePanel = panel;
    }

    private void updateModelRules(TemplateRule rule) {
        List<TemplateRule> rules = new ArrayList<>(tablePanel.getModel().getObject());
        Collections.sort(rules, new TemplateRule.TemplateRuleComparator());
        rules.removeIf(r -> r.getRuleId().equals(rule.getRuleId()));
        tablePanel.getModel().setObject(TemplateRuleService.updatePriorities(rules, rule));
        tablePanel.modelChanged();
        tablePanel.getTable().modelChanged();
    }

    private void clearForm(AjaxRequestTarget target) {
        theForm.clearInput();
        theForm.setModelObject(new TemplateRule());
        theForm.modelChanged();
        templateInfoDropDownChoice.modelChanged();
        mimeTypeDropDown.modelChanged();
        cqlFilterArea.modelChanged();
        profileField.modelChanged();
        submitLabelModel.setObject(getSubmitLabelValue("add"));
        panelLabelModel.setObject(getPanelLabelValue("add"));
        target.add(theForm);
        target.add(templateInfoDropDownChoice);
        target.add(mimeTypeDropDown);
        target.add(cqlFilterArea);
        target.add(profileField);
        target.add(submitLabel);
        target.add(panelLabel);
    }

    private void cleanFeedbackPanel() {
        ruleFeedbackPanel.getFeedbackMessages().clear();
    }

    String getPanelLabelValue(String labelType) {
        return getLabelValue("panel", labelType);
    }

    String getSubmitLabelValue(String labelType) {
        return getLabelValue("submit", labelType);
    }

    String getLabelValue(String label1, String label2) {
        return MessageFormat.format(
                getString("TemplateRuleConfigurationPanel." + label1),
                getString("TemplateRuleConfigurationPanel." + label2));
    }

    private IValidator<String> getCqlValidator() {
        return new IValidator<String>() {
            @Override
            public void validate(IValidatable<String> iValidatable) {
                try {
                    String value = iValidatable.getValue();
                    if (value != null && !"".equals(value)) XCQL.toFilter(iValidatable.getValue());
                } catch (CQLException e) {
                    ValidationError error = new ValidationError();
                    String message =
                            new ParamResourceModel(
                                            "invalidCQL", TemplateRuleConfigurationPanel.this)
                                    .getObject();
                    message += " " + e.getMessage();
                    error.setMessage(message);
                    iValidatable.error(error);
                }
            }
        };
    }
}
