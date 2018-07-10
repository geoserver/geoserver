/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.web;

import java.util.ArrayList;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.form.OnChangeAjaxBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.head.CssHeaderItem;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.html.IHeaderContributor;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.util.ListModel;
import org.geoserver.config.ServiceInfo;
import org.geoserver.qos.BaseConfigurationLoader;
import org.geoserver.qos.xml.OperatingInfo;
import org.geoserver.qos.xml.QosMainConfiguration;
import org.geoserver.qos.xml.QosMainMetadata;
import org.geoserver.qos.xml.QosRepresentativeOperation;
import org.geoserver.qos.xml.ReferenceType;
import org.geoserver.web.services.AdminPagePanel;

public abstract class QosAdminPanel extends AdminPagePanel implements IHeaderContributor {

    protected QosMainConfiguration config;
    protected ServiceInfo serviceInfo;

    protected WebMarkupContainer configs;
    protected IModel<ServiceInfo> mainModel;

    public QosAdminPanel(String id, IModel<ServiceInfo> model) {
        super(id, model);
        mainModel = model;
        serviceInfo = model.getObject();
        config = getLoader().getConfiguration(model.getObject());
    }

    @Override
    protected void onInitialize() {
        super.onInitialize();
        initCommonComponents();
    }

    protected void initCommonComponents() {
        /** CheckBox activatedCheck */
        final CheckBox activatedCheck =
                new CheckBox(
                        "createExtendedCapabilities",
                        new PropertyModel<Boolean>(config, "activated"));
        add(activatedCheck);

        final WebMarkupContainer container = new WebMarkupContainer("container");
        container.setOutputMarkupId(true);
        add(container);

        configs = new WebMarkupContainer("configs");
        configs.setOutputMarkupId(true);
        configs.setVisible(activatedCheck.getModelObject());
        container.add(configs);

        activatedCheck.add(
                new OnChangeAjaxBehavior() {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onUpdate(AjaxRequestTarget target) {
                        configs.setVisible(activatedCheck.getModelObject());
                        target.add(container);
                    }
                });

        if (config.getWmsQosMetadata() == null) config.setWmsQosMetadata(new QosMainMetadata());
        if (config.getWmsQosMetadata().getOperatingInfo() == null)
            config.getWmsQosMetadata().setOperatingInfo(new ArrayList<>());
        final ListView<OperatingInfo> opInfoListView =
                new ListView<OperatingInfo>(
                        "opInfoListView", config.getWmsQosMetadata().getOperatingInfo()) {
                    @Override
                    protected void populateItem(ListItem<OperatingInfo> item) {
                        OperatingInfoModalPanel opPanel =
                                new OperatingInfoModalPanel("opinfo", item.getModel());
                        // onDelete
                        opPanel.setOnDelete(
                                x -> {
                                    config.getWmsQosMetadata()
                                            .getOperatingInfo()
                                            .remove(x.getModel());
                                    x.getTarget().add(configs);
                                });
                        item.add(opPanel);
                    }
                };
        opInfoListView.setOutputMarkupId(true);
        configs.add(opInfoListView);

        final AjaxButton addOpInfoButton = new AjaxButton("addOpInfo") {};
        configs.add(addOpInfoButton);
        addOpInfoButton.add(
                new AjaxFormSubmitBehavior("click") {
                    @Override
                    protected void onAfterSubmit(final AjaxRequestTarget target) {
                        config.getWmsQosMetadata().getOperatingInfo().add(new OperatingInfo());
                        target.add(configs);
                    }
                });

        //
        if (config.getWmsQosMetadata().getOperationAnomalyFeed() == null) {
            config.getWmsQosMetadata().setOperationAnomalyFeed(new ArrayList<>());
        }
        final WebMarkupContainer anomalyDiv = new WebMarkupContainer("anomalyDiv");
        anomalyDiv.setOutputMarkupId(true);
        configs.add(anomalyDiv);

        final ListView<ReferenceType> anomalyListView =
                new ListView<ReferenceType>(
                        "anomalyListView", config.getWmsQosMetadata().getOperationAnomalyFeed()) {
                    @Override
                    protected void populateItem(ListItem<ReferenceType> item) {
                        OperationAnomalyFeedPanel anomalyPanel =
                                new OperationAnomalyFeedPanel("anomalyPanel", item.getModel());
                        // onDelete
                        anomalyPanel.setOnDelete(
                                x -> {
                                    config.getWmsQosMetadata()
                                            .getOperationAnomalyFeed()
                                            .remove(x.getModel());
                                    x.getTarget().add(anomalyDiv);
                                });
                        item.add(anomalyPanel);
                    }
                };
        anomalyDiv.add(anomalyListView);

        final AjaxButton anomalyButton = new AjaxButton("addAnomaly") {};
        anomalyDiv.add(anomalyButton);
        anomalyButton.add(
                new AjaxFormSubmitBehavior("click") {
                    @Override
                    protected void onAfterSubmit(final AjaxRequestTarget target) {
                        config.getWmsQosMetadata()
                                .getOperationAnomalyFeed()
                                .add(new ReferenceType());
                        target.add(anomalyDiv);
                    }
                });

        // Statements
        if (config.getWmsQosMetadata().getStatements() == null) {
            config.getWmsQosMetadata().setStatements(new ArrayList<>());
        }
        final QosStatementListPanel statPanel =
                new QosStatementListPanel(
                        "statemenstDiv",
                        new ListModel<>(config.getWmsQosMetadata().getStatements()));
        configs.add(statPanel);

        initRepresentativeOperations(mainModel);
    }

    protected abstract BaseConfigurationLoader<? extends ServiceInfo> getLoader();

    protected void initRepresentativeOperations(IModel<ServiceInfo> model) {

        final Form repOperationsForm = new Form("repOperationsDiv");
        repOperationsForm.setOutputMarkupId(true);
        configs.add(repOperationsForm);

        final ListView<QosRepresentativeOperation> repOpListView =
                new ListView<QosRepresentativeOperation>(
                        "repOperationsListView",
                        new PropertyModel<>(
                                config.getWmsQosMetadata(), "representativeOperations")) {
                    @Override
                    protected void populateItem(ListItem<QosRepresentativeOperation> item) {
                        RepresentativeOperationPanel panel =
                                buildRepOperationPanel("repOperationPanel", item.getModel());
                        // onDelete
                        panel.setOnDelete(
                                x -> {
                                    if (config.getWmsQosMetadata().getRepresentativeOperations()
                                            != null)
                                        config.getWmsQosMetadata()
                                                .getRepresentativeOperations()
                                                .remove(x.getModel());
                                    x.getTarget().add(repOperationsForm);
                                });
                        item.add(panel);
                    }
                };
        repOperationsForm.add(repOpListView);

        final AjaxButton addRepOpButton = new AjaxButton("addRepOper") {};
        repOperationsForm.add(addRepOpButton);
        addRepOpButton.add(
                new AjaxFormSubmitBehavior("click") {
                    @Override
                    protected void onAfterSubmit(final AjaxRequestTarget target) {
                        if (config.getWmsQosMetadata().getRepresentativeOperations() == null)
                            config.getWmsQosMetadata()
                                    .setRepresentativeOperations(new ArrayList<>());
                        config.getWmsQosMetadata()
                                .getRepresentativeOperations()
                                .add(newRepresentativeOperationInstance());
                        target.add(repOperationsForm);
                    }
                });
    }

    public IModel<ServiceInfo> getMainModel() {
        return mainModel;
    }

    protected QosRepresentativeOperation newRepresentativeOperationInstance() {
        QosRepresentativeOperation repOp = new QosRepresentativeOperation();
        return repOp;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        String css = ".qos-panel { " + "border: 1px solid #c6e09b; " + "padding: 5px; " + " }";
        response.render(CssHeaderItem.forCSS(css, "qosPanelCss"));
    }

    protected abstract RepresentativeOperationPanel buildRepOperationPanel(
            String id, IModel<QosRepresentativeOperation> model);
}
