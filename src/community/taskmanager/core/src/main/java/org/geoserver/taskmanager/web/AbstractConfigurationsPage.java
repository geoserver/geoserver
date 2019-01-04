/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.taskmanager.data.BatchElement;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.web.model.ConfigurationsModel;
import org.geoserver.taskmanager.web.panel.DropDownPanel;
import org.geoserver.taskmanager.web.panel.MultiLabelCheckBoxPanel;
import org.geoserver.web.ComponentAuthorizer;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.util.logging.Logging;

public class AbstractConfigurationsPage extends GeoServerSecuredPage {

    private static final long serialVersionUID = -6780935404517755471L;

    private static final Logger LOGGER = Logging.getLogger(AbstractConfigurationsPage.class);

    private boolean templates;

    private AjaxLink<Object> remove;

    private AjaxLink<Object> copy;

    private GeoServerDialog dialog;

    private GeoServerTablePanel<Configuration> configurationsPanel;

    public AbstractConfigurationsPage(boolean templates) {
        this.templates = templates;
    }

    protected ComponentAuthorizer getPageAuthorizer() {
        return ComponentAuthorizer.AUTHENTICATED;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialHeight(150);
        ((ModalWindow) dialog.get("dialog")).showUnloadConfirmation(false);

        add(
                new AjaxLink<Object>("addNew") {
                    private static final long serialVersionUID = 3581476968062788921L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        if (templates
                                || TaskManagerBeans.get()
                                        .getDao()
                                        .getConfigurations(true)
                                        .isEmpty()) {
                            Configuration configuration =
                                    TaskManagerBeans.get().getFac().createConfiguration();
                            configuration.setTemplate(templates);

                            setResponsePage(new ConfigurationPage(configuration));

                        } else {
                            dialog.setTitle(
                                    new ParamResourceModel("addNewDialog.title", getPage()));

                            dialog.showOkCancel(
                                    target,
                                    new GeoServerDialog.DialogDelegate() {

                                        private static final long serialVersionUID =
                                                -5552087037163833563L;

                                        private DropDownPanel panel;

                                        @Override
                                        protected Component getContents(String id) {
                                            ArrayList<String> list = new ArrayList<String>();
                                            for (Configuration template :
                                                    TaskManagerBeans.get()
                                                            .getDao()
                                                            .getConfigurations(true)) {
                                                list.add(template.getName());
                                            }
                                            panel =
                                                    new DropDownPanel(
                                                            id,
                                                            new Model<String>(),
                                                            new Model<ArrayList<String>>(list),
                                                            new ParamResourceModel(
                                                                    "addNewDialog.chooseTemplate",
                                                                    getPage()),
                                                            true);
                                            return panel;
                                        }

                                        @Override
                                        protected boolean onSubmit(
                                                AjaxRequestTarget target, Component contents) {
                                            String choice = (String) panel.getDefaultModelObject();
                                            Configuration configuration;
                                            if (choice == null) {
                                                configuration =
                                                        TaskManagerBeans.get()
                                                                .getFac()
                                                                .createConfiguration();
                                            } else {
                                                configuration =
                                                        TaskManagerBeans.get()
                                                                .getDao()
                                                                .copyConfiguration(choice);
                                                configuration.setTemplate(false);
                                                configuration.setName(null);
                                            }

                                            setResponsePage(new ConfigurationPage(configuration));

                                            return true;
                                        }
                                    });
                        }
                    }
                });

        // the removal button
        add(
                remove =
                        new AjaxLink<Object>("removeSelected") {
                            private static final long serialVersionUID = 3581476968062788921L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                boolean someCant = false;
                                for (Configuration config : configurationsPanel.getSelection()) {
                                    BatchElement be =
                                            TaskManagerBeans.get()
                                                    .getDataUtil()
                                                    .taskInUseByExternalBatch(config);
                                    if (be != null) {
                                        error(
                                                new ParamResourceModel(
                                                                "taskInUse",
                                                                AbstractConfigurationsPage.this,
                                                                config.getName(),
                                                                be.getTask().getName(),
                                                                be.getBatch().getName())
                                                        .getString());
                                        someCant = true;
                                    } else if (!TaskManagerBeans.get()
                                            .getDataUtil()
                                            .isDeletable(config)) {
                                        error(
                                                new ParamResourceModel(
                                                                "stillRunning",
                                                                AbstractConfigurationsPage.this,
                                                                config.getName())
                                                        .getString());
                                        someCant = true;
                                    } else if (!TaskManagerBeans.get()
                                            .getSecUtil()
                                            .isAdminable(
                                                    AbstractConfigurationsPage.this
                                                            .getSession()
                                                            .getAuthentication(),
                                                    config)) {
                                        error(
                                                new ParamResourceModel(
                                                                "noDeleteRights",
                                                                AbstractConfigurationsPage.this,
                                                                config.getName())
                                                        .getString());
                                        someCant = true;
                                    }
                                }
                                if (someCant) {
                                    addFeedbackPanels(target);
                                } else {
                                    dialog.setTitle(
                                            new ParamResourceModel(
                                                    "confirmDeleteDialog.title", getPage()));
                                    dialog.showOkCancel(
                                            target,
                                            new GeoServerDialog.DialogDelegate() {

                                                private static final long serialVersionUID =
                                                        -5552087037163833563L;

                                                private String error = null;

                                                private IModel<Boolean> shouldCleanupModel =
                                                        new Model<Boolean>();

                                                @Override
                                                protected Component getContents(String id) {
                                                    StringBuilder sb = new StringBuilder();
                                                    sb.append(
                                                            new ParamResourceModel(
                                                                            "confirmDeleteDialog.content",
                                                                            getPage())
                                                                    .getString());
                                                    for (Configuration config :
                                                            configurationsPanel.getSelection()) {
                                                        sb.append("\n&nbsp;&nbsp;");
                                                        sb.append(
                                                                StringEscapeUtils.escapeHtml4(
                                                                        config.getName()));
                                                    }
                                                    return new MultiLabelCheckBoxPanel(
                                                            id,
                                                            sb.toString(),
                                                            new ParamResourceModel(
                                                                            "cleanUp", getPage())
                                                                    .getString(),
                                                            shouldCleanupModel);
                                                }

                                                @Override
                                                protected boolean onSubmit(
                                                        AjaxRequestTarget target,
                                                        Component contents) {
                                                    try {
                                                        for (Configuration config :
                                                                configurationsPanel
                                                                        .getSelection()) {
                                                            if (shouldCleanupModel.getObject()) {
                                                                config =
                                                                        TaskManagerBeans.get()
                                                                                .getDao()
                                                                                .init(config);
                                                                if (TaskManagerBeans.get()
                                                                        .getTaskUtil()
                                                                        .canCleanup(config)) {
                                                                    if (TaskManagerBeans.get()
                                                                            .getTaskUtil()
                                                                            .cleanup(config)) {
                                                                        info(
                                                                                new ParamResourceModel(
                                                                                                "cleanUp.success",
                                                                                                getPage(),
                                                                                                config
                                                                                                        .getName())
                                                                                        .getString());
                                                                    } else {
                                                                        error(
                                                                                new ParamResourceModel(
                                                                                                "cleanUp.failed",
                                                                                                getPage(),
                                                                                                config
                                                                                                        .getName())
                                                                                        .getString());
                                                                    }
                                                                } else {
                                                                    info(
                                                                            new ParamResourceModel(
                                                                                            "cleanUp.ignore",
                                                                                            getPage(),
                                                                                            config
                                                                                                    .getName())
                                                                                    .getString());
                                                                }
                                                            }
                                                            TaskManagerBeans.get()
                                                                    .getBjService()
                                                                    .remove(config);
                                                        }
                                                        configurationsPanel.clearSelection();
                                                        remove.setEnabled(false);
                                                    } catch (Exception e) {
                                                        LOGGER.log(
                                                                Level.WARNING, e.getMessage(), e);
                                                        Throwable rootCause =
                                                                ExceptionUtils.getRootCause(e);
                                                        error =
                                                                rootCause == null
                                                                        ? e.getLocalizedMessage()
                                                                        : rootCause
                                                                                .getLocalizedMessage();
                                                    }
                                                    return true;
                                                }

                                                @Override
                                                public void onClose(AjaxRequestTarget target) {
                                                    if (error != null) {
                                                        error(error);
                                                        addFeedbackPanels(target);
                                                    }
                                                    addFeedbackPanels(target);
                                                    target.add(configurationsPanel);
                                                    target.add(remove);
                                                }
                                            });
                                }
                            }
                        });
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);

        // the copy button
        add(
                copy =
                        new AjaxLink<Object>("copySelected") {
                            private static final long serialVersionUID = 3581476968062788921L;

                            @Override
                            public void onClick(AjaxRequestTarget target) {
                                Configuration copy =
                                        TaskManagerBeans.get()
                                                .getDao()
                                                .copyConfiguration(
                                                        configurationsPanel
                                                                .getSelection()
                                                                .get(0)
                                                                .getName());
                                // make sure we can't copy with workspace we don't have access to
                                WorkspaceInfo wi =
                                        GeoServerApplication.get()
                                                .getCatalog()
                                                .getWorkspaceByName(copy.getWorkspace());
                                if (wi == null
                                        || !TaskManagerBeans.get()
                                                .getSecUtil()
                                                .isAdminable(
                                                        AbstractConfigurationsPage.this
                                                                .getSession()
                                                                .getAuthentication(),
                                                        wi)) {
                                    copy.setWorkspace(null);
                                }
                                setResponsePage(new ConfigurationPage(copy));
                            }
                        });
        copy.setOutputMarkupId(true);
        copy.setEnabled(false);

        // the panel
        add(
                configurationsPanel =
                        new GeoServerTablePanel<Configuration>(
                                "configurationsPanel", new ConfigurationsModel(templates), true) {

                            private static final long serialVersionUID = -8943273843044917552L;

                            @Override
                            protected void onSelectionUpdate(AjaxRequestTarget target) {
                                remove.setEnabled(configurationsPanel.getSelection().size() > 0);
                                copy.setEnabled(configurationsPanel.getSelection().size() == 1);
                                target.add(remove);
                                target.add(copy);
                            }

                            @Override
                            public void onBeforeRender() {
                                ((ConfigurationsModel) getDataProvider()).reset();
                                super.onBeforeRender();
                            }

                            @SuppressWarnings("unchecked")
                            @Override
                            protected Component getComponentForProperty(
                                    String id,
                                    IModel<Configuration> itemModel,
                                    Property<Configuration> property) {
                                if (property.equals(ConfigurationsModel.NAME)) {
                                    SimpleAjaxLink<String> link =
                                            new SimpleAjaxLink<String>(
                                                    id,
                                                    (IModel<String>) property.getModel(itemModel)) {
                                                private static final long serialVersionUID =
                                                        -9184383036056499856L;

                                                @Override
                                                protected void onClick(AjaxRequestTarget target) {
                                                    setResponsePage(
                                                            new ConfigurationPage(
                                                                    TaskManagerBeans.get()
                                                                            .getDao()
                                                                            .init(
                                                                                    itemModel
                                                                                            .getObject())));
                                                }
                                            };
                                    if (!itemModel.getObject().isTemplate()
                                            && !itemModel.getObject().isValidated()) {
                                        link.add(
                                                new AttributeAppender(
                                                        "class", "notvalidated", " "));
                                    }
                                    return link;
                                }
                                return null;
                            }
                        });
        configurationsPanel.setOutputMarkupId(true);
    }
}
