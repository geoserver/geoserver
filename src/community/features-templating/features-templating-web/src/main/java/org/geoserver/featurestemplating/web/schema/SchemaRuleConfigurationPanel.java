/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web.schema;

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
import org.geoserver.featurestemplating.configuration.schema.SchemaInfo;
import org.geoserver.featurestemplating.configuration.schema.SchemaInfoDAO;
import org.geoserver.featurestemplating.configuration.schema.SchemaRule;
import org.geoserver.featurestemplating.configuration.schema.SchemaRuleService;
import org.geoserver.featurestemplating.web.OutputFormatsDropDown;
import org.geoserver.util.XCQL;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.filter.text.cql2.CQLException;

// TODO WICKET8 - Verify this page works OK
public class SchemaRuleConfigurationPanel extends Panel {

    CompoundPropertyModel<SchemaRule> schemaRuleModel;
    SchemaRulesTablePanel tablePanel;
    Form<SchemaRule> theForm;
    NumberTextField<Integer> priorityField;
    DropDownChoice<SchemaInfo> schemaInfoDropDownChoice;
    OutputFormatsDropDown mimeTypeDropDown;
    TextArea<String> cqlFilterArea;
    TextArea<String> profileField;
    FeedbackPanel ruleFeedbackPanel;
    LayerInfo layer;
    Label submitLabel;
    Model<String> submitLabelModel;
    Label panelLabel;
    Model<String> panelLabelModel;

    public SchemaRuleConfigurationPanel(
            String id, CompoundPropertyModel<SchemaRule> model, boolean isUpdate, LayerInfo layer) {
        super(id, model);
        this.layer = layer;
        this.schemaRuleModel = model;
        initUI(schemaRuleModel, isUpdate);
    }

    private void initUI(CompoundPropertyModel<SchemaRule> model, boolean isUpdate) {
        panelLabel = new Label("schemaRuleConfigurationLabel", panelLabelModel = Model.of(getPanelLabelValue("add")));
        panelLabel.setOutputMarkupId(true);
        add(panelLabel);
        this.theForm = new Form<>("theForm", model);
        theForm.setOutputMarkupId(true);
        theForm.add(ruleFeedbackPanel = new FeedbackPanel("ruleFeedback"));
        ruleFeedbackPanel.setOutputMarkupId(true);
        add(theForm);

        priorityField = new NumberTextField<Integer>("priority", model.bind("priority"));
        theForm.add(priorityField);
        ChoiceRenderer<SchemaInfo> schemaInfoChoiceRenderer = new ChoiceRenderer<>("fullName", "identifier");
        schemaInfoDropDownChoice = new DropDownChoice<SchemaInfo>(
                "schemaIdentifier", model.bind("schemaInfo"), getSchemaInfoList(), schemaInfoChoiceRenderer);
        schemaInfoDropDownChoice.setOutputMarkupId(true);
        theForm.add(schemaInfoDropDownChoice);

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

        AjaxSubmitLink submitLink = new AjaxSubmitLink("save") {
            @Override
            protected void onSubmit(AjaxRequestTarget target) {
                super.onSubmit(target);
                cleanFeedbackPanel();
                SchemaRule rule = theForm.getModelObject();
                //                if (!validateAndReport(rule)) return;
                updateModelRules(rule);
                target.add(tablePanel);
                target.add(tablePanel.getTable());
                clearForm(target);
            }

            @Override
            protected void onAfterSubmit(AjaxRequestTarget target) {
                if (theForm.hasError()) target.add(ruleFeedbackPanel);
            }

            @Override
            protected void onError(AjaxRequestTarget target) {
                super.onError(target);
                if (theForm.hasError()) target.add(ruleFeedbackPanel);
            }
        };
        submitLabel = new Label("submitLabel", submitLabelModel = Model.of(getSubmitLabelValue("add")));
        submitLabel.setOutputMarkupId(true);
        submitLink.add(submitLabel);
        theForm.add(submitLink);
        theForm.add(new AjaxSubmitLink("cancel") {

            @Override
            public void onSubmit(AjaxRequestTarget target) {
                clearForm(target);
            }
        });
    }

    protected List<SchemaInfo> getSchemaInfoList() {
        ResourceInfo resourceInfo = layer.getResource();
        return SchemaInfoDAO.get().findByFeatureTypeInfo((FeatureTypeInfo) resourceInfo);
    }

    void setTemplateRuleTablePanel(SchemaRulesTablePanel panel) {
        this.tablePanel = panel;
    }

    private void updateModelRules(SchemaRule rule) {
        List<SchemaRule> rules = new ArrayList<>(tablePanel.getModel().getObject());
        Collections.sort(rules, new SchemaRule.SchemaRuleComparator());
        rules.removeIf(r -> r.getRuleId().equals(rule.getRuleId()));
        tablePanel.getModel().setObject(SchemaRuleService.updatePriorities(rules, rule));
        tablePanel.modelChanged();
        tablePanel.getTable().modelChanged();
    }

    private void clearForm(AjaxRequestTarget target) {
        theForm.clearInput();
        theForm.setModelObject(new SchemaRule());
        theForm.modelChanged();
        schemaInfoDropDownChoice.modelChanged();
        mimeTypeDropDown.modelChanged();
        cqlFilterArea.modelChanged();
        profileField.modelChanged();
        submitLabelModel.setObject(getSubmitLabelValue("add"));
        panelLabelModel.setObject(getPanelLabelValue("add"));
        target.add(theForm);
        target.add(schemaInfoDropDownChoice);
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
                getString("SchemaRuleConfigurationPanel." + label1),
                getString("SchemaRuleConfigurationPanel." + label2));
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
                            new ParamResourceModel("invalidCQL", SchemaRuleConfigurationPanel.this).getObject();
                    message += " " + e.getMessage();
                    error.setMessage(message);
                    iValidatable.error(error);
                }
            }
        };
    }
}
