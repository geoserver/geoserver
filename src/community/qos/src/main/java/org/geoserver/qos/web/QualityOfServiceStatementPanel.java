/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.qos.xml.Metric;
import org.geoserver.qos.xml.MetricsFactory;
import org.geoserver.qos.xml.QualityOfServiceStatement;
import org.geoserver.qos.xml.QualityOfServiceStatement.ValueType;

public class QualityOfServiceStatementPanel extends Panel {

    protected SerializableConsumer<AjaxTargetAndModel<QualityOfServiceStatement>> onDelete;
    protected IModel<QualityOfServiceStatement> statementModel;

    protected WebMarkupContainer div;

    protected WebMarkupContainer uomDiv;
    protected DropDownChoice<String> uomSelector;
    protected DropDownChoice<ValueType> typeSelector;
    protected IModel<List<String>> uomChoices;

    public QualityOfServiceStatementPanel(String id, IModel<QualityOfServiceStatement> model) {
        super(id, model);
        initComponents(model);
    }

    protected void initComponents(IModel<QualityOfServiceStatement> model) {
        statementModel = model;
        if (statementModel.getObject().getMetric() == null)
            statementModel.getObject().setMetric(new Metric());
        initUomChoices();

        div = new WebMarkupContainer("div");
        div.setOutputMarkupId(true);
        add(div);

        final DropDownChoice<String> metricSelector =
                new DropDownChoice<String>(
                        "metricSelector",
                        new PropertyModel<String>(model, "metric.href"),
                        Metric.values().stream().map(Metric::getHref).collect(Collectors.toList()),
                        new ChoiceRenderer<String>() {
                            @Override
                            public String getDisplayValue(String object) {
                                String[] parts = object.split("#");
                                return parts[1];
                            }
                        });
        div.add(metricSelector);
        metricSelector.add(
                new AjaxFormSubmitBehavior("change") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target) {
                        target.add(uomDiv);
                    }
                });

        final TextField<String> metricTitleText =
                new TextField<>("metricTitle", new PropertyModel<String>(model, "metric.title"));
        div.add(metricTitleText);

        typeSelector =
                new DropDownChoice<ValueType>(
                        "typeSelector",
                        new PropertyModel<ValueType>(model, "valueType"),
                        Arrays.asList(ValueType.values()));
        div.add(typeSelector);

        uomDiv = new WebMarkupContainer("uomDiv");
        uomDiv.setOutputMarkupId(true);
        div.add(uomDiv);
        uomSelector =
                new DropDownChoice<String>(
                        "uomSelector", new PropertyModel<>(model, "meassure.uom"), uomChoices);
        uomSelector.setOutputMarkupId(true);
        uomDiv.add(uomSelector);

        final TextField<String> valueText =
                new TextField<>("valueText", new PropertyModel<String>(model, "meassure.value"));
        div.add(valueText);

        final AjaxSubmitLink deleteLink =
                new AjaxSubmitLink("deleteLink") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target, Form<?> form) {
                        super.onAfterSubmit(target, form);
                        delete(target);
                    }
                };
        div.add(deleteLink);

        typeSelector.add(
                new AjaxFormSubmitBehavior("change") {
                    @Override
                    protected void onAfterSubmit(AjaxRequestTarget target) {
                        updateUomState();
                        target.add(div);
                    }
                });
    }

    protected void updateUomState() {
        // if type is value only, no uom
        if (ValueType.value.equals(statementModel.getObject().getValueType())) {
            uomSelector.setEnabled(false);
            statementModel.getObject().getMeassure().setUom(null);
        } else {
            uomSelector.setEnabled(true);
        }
    }

    protected void initUomChoices() {
        uomChoices =
                new AbstractReadOnlyModel<List<String>>() {
                    @Override
                    public List<String> getObject() {
                        return getMetrics()
                                .getUom(statementModel.getObject().getMetric().getTitle());
                    }
                };
    }

    public void delete(AjaxRequestTarget target) {
        if (onDelete == null) return;
        AjaxTargetAndModel<QualityOfServiceStatement> chain =
                new AjaxTargetAndModel<QualityOfServiceStatement>(
                        (QualityOfServiceStatement) this.getDefaultModelObject(), target);
        onDelete.accept(chain);
    }

    public void setOnDelete(
            SerializableConsumer<AjaxTargetAndModel<QualityOfServiceStatement>> onDelete) {
        this.onDelete = onDelete;
    }

    protected MetricsFactory getMetrics() {
        return MetricsFactory.getInstance();
    }

    protected void dataInit() {
        // set available metrics for choice:
        uomChoices = Model.ofList(getMetrics().getCodes());
    }
}
