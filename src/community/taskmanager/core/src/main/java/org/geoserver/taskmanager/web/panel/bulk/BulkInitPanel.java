/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel.bulk;

import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.util.ValidationError;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerDialog.DialogDelegate;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.logging.Logging;

public class BulkInitPanel extends Panel {

    private static final long serialVersionUID = -7787191736336649903L;

    private static final Logger LOGGER = Logging.getLogger(BulkInitPanel.class);

    private IModel<String> workspaceModel = new Model<>("%");

    private IModel<String> configurationModel = new Model<>("%");

    private List<Batch> batches;

    public BulkInitPanel(String id) {
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
        add(configuration.setRequired(true));

        NumberTextField<Integer> startDelay =
                new NumberTextField<>("startDelay", new Model<Integer>(0), Integer.class);
        startDelay.setMinimum(0);
        add(startDelay);

        NumberTextField<Integer> betweenDelay =
                new NumberTextField<>("betweenDelay", new Model<Integer>(0), Integer.class);
        betweenDelay.setMinimum(0);
        add(betweenDelay);

        Label configsFound =
                new Label(
                        "configsFound",
                        new ParamResourceModel(
                                "configsFound",
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
        add(configsFound.setOutputMarkupId(true));

        AjaxSubmitLink run =
                new AjaxSubmitLink("run") {
                    private static final long serialVersionUID = -3288982013478650146L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        if (batches.size() == 0) {
                            error(
                                    new StringResourceModel("noConfigs", BulkInitPanel.this)
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
                                                            "initConfigs",
                                                            BulkInitPanel.this,
                                                            Integer.toString(batches.size()),
                                                            Integer.toString(time)));
                                        }

                                        @Override
                                        protected boolean onSubmit(
                                                AjaxRequestTarget target, Component contents) {
                                            TaskManagerBeans beans = TaskManagerBeans.get();
                                            beans.getBjService()
                                                    .scheduleNow(
                                                            batches,
                                                            startDelay.getModelObject(),
                                                            betweenDelay.getModelObject(),
                                                            new Consumer<Batch>() {

                                                                @Override
                                                                public void accept(Batch batch) {
                                                                    tryToValidate(
                                                                            beans,
                                                                            batch
                                                                                    .getConfiguration());
                                                                }
                                                            });
                                            info(
                                                    new ParamResourceModel(
                                                                    "initializingConfigs",
                                                                    BulkInitPanel.this,
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
                        updateConfigs();
                        target.add(configsFound);
                        target.add(run);
                    }
                });
        configuration.add(
                new AjaxFormSubmitBehavior("change") {
                    private static final long serialVersionUID = 3397757222203749030L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        updateConfigs();
                        target.add(configsFound);
                        target.add(run);
                    }
                });

        updateConfigs();
    }

    private void tryToValidate(TaskManagerBeans beans, Configuration config) {
        config = beans.getDao().init(config);
        if (!beans.getInitConfigUtil().isInitConfig(config)) {
            List<ValidationError> errors = beans.getTaskUtil().validate(config);
            if (!errors.isEmpty()) {
                StringBuffer errorMessages = new StringBuffer();
                for (ValidationError error : errors) {
                    errorMessages.append("\n").append(error.toString());
                }
                LOGGER.log(
                        Level.WARNING,
                        "Validation for "
                                + config.getName()
                                + " failed: "
                                + errorMessages.toString());
                return;
            } else {
                config.setValidated(true);
            }
        }
    }

    private void updateConfigs() {
        batches =
                TaskManagerBeans.get()
                        .getDao()
                        .findInitBatches(
                                workspaceModel.getObject(), configurationModel.getObject());
    }
}
