/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.markup.html.repeater.data.sort.SortOrder;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.BatchRun;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.web.model.BatchRunsModel;
import org.geoserver.taskmanager.web.panel.SimpleAjaxSubmitLink;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class BatchRunsPage extends GeoServerSecuredPage {
    private static final long serialVersionUID = -5111795911981486778L;

    private IModel<Batch> batchModel;

    private GeoServerTablePanel<BatchRun> runsPanel;

    public BatchRunsPage(IModel<Batch> batchModel, Page parentPage) {
        this.batchModel = batchModel;
        setReturnPage(parentPage);
    }

    public BatchRunsPage(Batch batch, Page parentPage) {
        this(new Model<Batch>(batch), parentPage);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        add(
                new SimpleAjaxLink<String>(
                        "nameLink", new PropertyModel<String>(batchModel, "fullName")) {
                    private static final long serialVersionUID = -9184383036056499856L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        setResponsePage(new BatchPage(batchModel, getPage()));
                    }
                });

        add(
                new AjaxLink<Object>("refresh") {

                    private static final long serialVersionUID = 3905640474193868255L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        batchModel.setObject(
                                TaskManagerBeans.get()
                                        .getDao()
                                        .initHistory(batchModel.getObject()));
                        ((MarkupContainer) runsPanel.get("listContainer").get("items")).removeAll();
                        target.add(runsPanel);
                    }
                });

        // the tasks panel
        add(new Form<>("form").add(runsPanel = runPanel()));
        runsPanel.setOutputMarkupId(true);
        runsPanel.setSelectable(false);
        runsPanel.getDataProvider().setSort(BatchRunsModel.START.getName(), SortOrder.DESCENDING);

        add(
                new AjaxLink<Object>("close") {
                    private static final long serialVersionUID = -6892944747517089296L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        doReturn();
                    }
                });
    }

    protected GeoServerTablePanel<BatchRun> runPanel() {
        return new GeoServerTablePanel<BatchRun>(
                "runsPanel", new BatchRunsModel(batchModel), true) {

            private static final long serialVersionUID = -8943273843044917552L;

            @SuppressWarnings("unchecked")
            @Override
            protected Component getComponentForProperty(
                    String id, IModel<BatchRun> runModel, Property<BatchRun> property) {
                if (property.equals(BatchRunsModel.START)) {
                    return new SimpleAjaxLink<String>(
                            id, (IModel<String>) property.getModel(runModel)) {
                        private static final long serialVersionUID = -9184383036056499856L;

                        @Override
                        public void onClick(AjaxRequestTarget target) {
                            setResponsePage(new BatchRunPage(batchModel, runModel, getPage()));
                        }
                    };
                } else if (property.equals(BatchRunsModel.STOP)) {
                    if (runModel.getObject().getStatus().isClosed()
                            || !TaskManagerBeans.get()
                                    .getSecUtil()
                                    .isWritable(
                                            ((GeoServerSecuredPage) getPage())
                                                    .getSession()
                                                    .getAuthentication(),
                                            runModel.getObject().getBatch())) {
                        return new Label(id);
                    } else {
                        SimpleAjaxSubmitLink link =
                                new SimpleAjaxSubmitLink(id, null) {
                                    private static final long serialVersionUID =
                                            -9184383036056499856L;

                                    @Override
                                    protected void onSubmit(
                                            AjaxRequestTarget target, Form<?> form) {
                                        TaskManagerBeans.get()
                                                .getBjService()
                                                .interrupt(runModel.getObject());
                                        info(
                                                new ParamResourceModel(
                                                                "runInterrupted",
                                                                BatchRunsPage.this)
                                                        .getString());

                                        ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                                    }
                                };
                        link.getLink().add(new AttributeAppender("class", "stop-link", ","));
                        return link;
                    }
                }
                return null;
            }
        };
    }
}
