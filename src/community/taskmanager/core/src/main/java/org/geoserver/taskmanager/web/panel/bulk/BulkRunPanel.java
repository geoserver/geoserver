/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel.bulk;

import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormSubmitBehavior;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.NumberTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;

public class BulkRunPanel extends Panel {

    private static final long serialVersionUID = -7787191736336649903L;

    private IModel<String> workspaceModel = new Model<>("%");

    private IModel<String> configurationModel = new Model<>("%");

    private IModel<String> nameModel = new Model<>("%");

    private List<Batch> batches;

    public BulkRunPanel(String id) {
        super(id);
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        GeoServerDialog dialog = new GeoServerDialog("dialog");
        add(dialog);
        dialog.setInitialHeight(100);

        TextField<String> workspace = new TextField<>("workspace", workspaceModel);
        add(workspace);

        TextField<String> configuration = new TextField<>("configuration", configurationModel);
        add(configuration);

        TextField<String> name = new TextField<>("name", nameModel);
        add(name.setRequired(true));

        NumberTextField<Integer> startDelay =
                new NumberTextField<>("startDelay", new Model<Integer>(0), Integer.class);
        startDelay.setMinimum(0);
        add(startDelay);

        NumberTextField<Integer> betweenDelay =
                new NumberTextField<>("betweenDelay", new Model<Integer>(0), Integer.class);
        betweenDelay.setMinimum(0);
        add(betweenDelay);

        Label batchesFound =
                new Label(
                        "batchesFound",
                        new ParamResourceModel(
                                "batchesFound",
                                this,
                                new IModel<String>() {
                                    private static final long serialVersionUID =
                                            -6328441242635771092L;

                                    @Override
                                    public String getObject() {
                                        return Integer.toString(batches.size());
                                    }

                                    @Override
                                    public void setObject(String object) {}

                                    @Override
                                    public void detach() {}
                                }));
        add(batchesFound.setOutputMarkupId(true));

        AjaxSubmitLink run =
                new AjaxSubmitLink("run") {
                    private static final long serialVersionUID = -3288982013478650146L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        if (batches.size() == 0) {
                            error(
                                    new StringResourceModel("noBatches", BulkRunPanel.this)
                                            .getString());
                            ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                        } else {
                            dialog.showOkCancel(
                                    target,
                                    new DialogDelegate() {
                                        private static final long serialVersionUID =
                                                -8203963847815744909L;

                                        @Override
                                        protected Component getContents(String id) {
                                            int time =
                                                    ((batches.size() - 1)
                                                                            * betweenDelay
                                                                                    .getModelObject()
                                                                    + startDelay.getModelObject())
                                                            / 60;
                                            return new Label(
                                                    id,
                                                    new ParamResourceModel(
                                                            "runBatches",
                                                            BulkRunPanel.this,
                                                            Integer.toString(batches.size()),
                                                            Integer.toString(time)));
                                        }

                                        @Override
                                        protected boolean onSubmit(
                                                AjaxRequestTarget target, Component contents) {
                                            TaskManagerBeans.get()
                                                    .getBjService()
                                                    .scheduleNow(
                                                            batches,
                                                            startDelay.getModelObject(),
                                                            betweenDelay.getModelObject());
                                            info(
                                                    new ParamResourceModel(
                                                                    "runningBatches",
                                                                    BulkRunPanel.this,
                                                                    Integer.toString(
                                                                            batches.size()))
                                                            .getString());
                                            ((GeoServerBasePage) getPage())
                                                    .addFeedbackPanels(target);
                                            return true;
                                        }
                                    });
                        }
                    }

                    @Override
                    protected void onError(AjaxRequestTarget target, Form<?> form) {
                        ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                    }
                };
        add(run);

        workspace.add(
                new AjaxFormSubmitBehavior("change") {
                    private static final long serialVersionUID = 3397757222203749030L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        updateBatches();
                        target.add(batchesFound);
                        target.add(run);
                    }
                });
        configuration.add(
                new AjaxFormSubmitBehavior("change") {
                    private static final long serialVersionUID = 3397757222203749030L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        updateBatches();
                        target.add(batchesFound);
                        target.add(run);
                    }
                });
        name.add(
                new AjaxFormSubmitBehavior("change") {
                    private static final long serialVersionUID = 3397757222203749030L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        updateBatches();
                        target.add(batchesFound);
                        target.add(run);
                    }
                });

        updateBatches();
    }

    private void updateBatches() {
        batches =
                TaskManagerBeans.get()
                        .getDao()
                        .findBatches(
                                workspaceModel.getObject(),
                                configurationModel.getObject(),
                                nameModel.getObject());
    }
}
