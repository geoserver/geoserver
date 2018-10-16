/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.web.panel;

import java.time.DayOfWeek;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.commons.text.StringEscapeUtils;
import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geoserver.taskmanager.data.Batch;
import org.geoserver.taskmanager.data.Configuration;
import org.geoserver.taskmanager.util.FrequencyUtil;
import org.geoserver.taskmanager.util.InitConfigUtil;
import org.geoserver.taskmanager.util.TaskManagerBeans;
import org.geoserver.taskmanager.web.BatchPage;
import org.geoserver.taskmanager.web.BatchRunsPage;
import org.geoserver.taskmanager.web.model.BatchesModel;
import org.geoserver.web.CatalogIconFactory;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.util.logging.Logging;

public class BatchesPanel extends Panel {
    private static final long serialVersionUID = 1297739738862860160L;

    private static final Logger LOGGER = Logging.getLogger(BatchesPanel.class);

    private IModel<Configuration> configurationModel;

    private AjaxLink<Object> remove;

    private GeoServerDialog dialog;

    private GeoServerTablePanel<Batch> batchesPanel;

    private List<Batch> removedBatches = new ArrayList<Batch>();

    private BatchesModel batchesModel;

    public BatchesPanel(String id) {
        super(id);
        batchesModel = new BatchesModel();
    }

    public BatchesPanel(String id, IModel<Configuration> configurationModel) {
        super(id);
        this.configurationModel = configurationModel;
        batchesModel = new BatchesModel(configurationModel);
    }

    public List<Batch> getRemovedBatches() {
        return removedBatches;
    }

    public BatchesModel getBatchesModel() {
        return batchesModel;
    }

    @Override
    public void onInitialize() {
        super.onInitialize();

        add(dialog = new GeoServerDialog("dialog"));
        dialog.setInitialHeight(100);
        ((ModalWindow) dialog.get("dialog")).showUnloadConfirmation(false);

        add(
                new AjaxLink<Object>("addNew") {
                    private static final long serialVersionUID = -9184383036056499856L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        Batch batch = TaskManagerBeans.get().getFac().createBatch();
                        if (configurationModel != null) {
                            batch.setConfiguration(configurationModel.getObject());
                        }
                        setResponsePage(new BatchPage(batch, getPage()));
                    }

                    @Override
                    public boolean isVisible() {
                        return configurationModel == null
                                || configurationModel.getObject().getId() != null;
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
                                for (Batch batch : batchesPanel.getSelection()) {
                                    if (!TaskManagerBeans.get().getDataUtil().isDeletable(batch)) {
                                        error(
                                                new ParamResourceModel(
                                                                "stillRunning",
                                                                BatchesPanel.this,
                                                                batch.getFullName())
                                                        .getString());
                                        someCant = true;
                                    } else if (!TaskManagerBeans.get()
                                            .getSecUtil()
                                            .isAdminable(
                                                    ((GeoServerBasePage) getPage())
                                                            .getSession()
                                                            .getAuthentication(),
                                                    batch)) {
                                        error(
                                                new ParamResourceModel(
                                                                "noDeleteRights",
                                                                BatchesPanel.this,
                                                                batch.getName())
                                                        .getString());
                                        someCant = true;
                                    }
                                }
                                if (someCant) {
                                    ((GeoServerBasePage) getPage()).addFeedbackPanels(target);
                                } else {

                                    dialog.setTitle(
                                            new ParamResourceModel(
                                                    "confirmDeleteBatchesDialog.title",
                                                    BatchesPanel.this));
                                    dialog.showOkCancel(
                                            target,
                                            new GeoServerDialog.DialogDelegate() {

                                                private static final long serialVersionUID =
                                                        -5552087037163833563L;

                                                private String error = null;

                                                @Override
                                                protected Component getContents(String id) {
                                                    StringBuilder sb = new StringBuilder();
                                                    sb.append(
                                                            new ParamResourceModel(
                                                                            "confirmDeleteBatchesDialog.content",
                                                                            BatchesPanel.this)
                                                                    .getString());
                                                    for (Batch batch :
                                                            batchesPanel.getSelection()) {
                                                        sb.append("\n&nbsp;&nbsp;");
                                                        sb.append(
                                                                StringEscapeUtils.escapeHtml4(
                                                                        batch.getFullName()));
                                                    }
                                                    return new MultiLineLabel(id, sb.toString())
                                                            .setEscapeModelStrings(false);
                                                }

                                                @Override
                                                protected boolean onSubmit(
                                                        AjaxRequestTarget target,
                                                        Component contents) {
                                                    try {
                                                        for (Batch batch :
                                                                batchesPanel.getSelection()) {
                                                            if (configurationModel != null) {
                                                                configurationModel
                                                                        .getObject()
                                                                        .getBatches()
                                                                        .remove(batch.getName());
                                                                if (batch.getId() != null) {
                                                                    removedBatches.add(batch);
                                                                }
                                                            } else {
                                                                TaskManagerBeans.get()
                                                                        .getBjService()
                                                                        .remove(batch);
                                                            }
                                                        }
                                                        batchesPanel.clearSelection();
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
                                                        target.add(remove);
                                                        ((GeoServerBasePage) getPage())
                                                                .addFeedbackPanels(target);
                                                    } else {
                                                        target.add(batchesPanel);
                                                    }
                                                }
                                            });
                                }
                            }
                        });
        remove.setOutputMarkupId(true);
        remove.setEnabled(false);

        add(
                new AjaxLink<Object>("refresh") {

                    private static final long serialVersionUID = 3905640474193868255L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        ((MarkupContainer) batchesPanel.get("listContainer").get("items"))
                                .removeAll();
                        target.add(batchesPanel);
                    }
                });

        // the panel
        add(
                new Form<>("form")
                        .add(
                                batchesPanel =
                                        new GeoServerTablePanel<Batch>(
                                                "batchesPanel", batchesModel, true) {
                                            private static final long serialVersionUID =
                                                    -8943273843044917552L;

                                            @Override
                                            protected void onSelectionUpdate(
                                                    AjaxRequestTarget target) {
                                                remove.setEnabled(
                                                        batchesPanel.getSelection().size() > 0);
                                                target.add(remove);
                                            }

                                            @Override
                                            public void onBeforeRender() {
                                                batchesModel.reset();
                                                super.onBeforeRender();
                                            }

                                            @SuppressWarnings("unchecked")
                                            @Override
                                            protected Component getComponentForProperty(
                                                    String id,
                                                    IModel<Batch> itemModel,
                                                    Property<Batch> property) {
                                                if ((property.equals(BatchesModel.NAME)
                                                                || property.equals(
                                                                        BatchesModel.FULL_NAME))
                                                        && itemModel.getObject().getId() != null) {
                                                    if (findParent(Form.class) == null) {
                                                        return new SimpleAjaxLink<String>(
                                                                id,
                                                                (IModel<String>)
                                                                        property.getModel(
                                                                                itemModel)) {
                                                            private static final long
                                                                    serialVersionUID =
                                                                            -9184383036056499856L;

                                                            @Override
                                                            protected void onClick(
                                                                    AjaxRequestTarget target) {
                                                                setResponsePage(
                                                                        new BatchPage(
                                                                                TaskManagerBeans
                                                                                        .get()
                                                                                        .getDao()
                                                                                        .init(
                                                                                                itemModel
                                                                                                        .getObject()),
                                                                                getPage()));
                                                            }
                                                        };
                                                    } else {
                                                        return new SimpleAjaxSubmitLink(
                                                                id,
                                                                (IModel<String>)
                                                                        property.getModel(
                                                                                itemModel)) {
                                                            private static final long
                                                                    serialVersionUID =
                                                                            -9184383036056499856L;

                                                            @Override
                                                            protected void onSubmit(
                                                                    AjaxRequestTarget target,
                                                                    Form<?> form) {
                                                                setResponsePage(
                                                                        new BatchPage(
                                                                                TaskManagerBeans
                                                                                        .get()
                                                                                        .getDao()
                                                                                        .init(
                                                                                                itemModel
                                                                                                        .getObject()),
                                                                                getPage()));
                                                            }
                                                        };
                                                    }

                                                } else if (property == BatchesModel.ENABLED) {
                                                    PackageResourceReference icon =
                                                            itemModel.getObject().isEnabled()
                                                                    ? CatalogIconFactory.get()
                                                                            .getEnabledIcon()
                                                                    : CatalogIconFactory.get()
                                                                            .getDisabledIcon();
                                                    Fragment f =
                                                            new Fragment(
                                                                    id,
                                                                    "iconFragment",
                                                                    BatchesPanel.this);
                                                    f.add(new Image("enabledIcon", icon));
                                                    return f;
                                                } else if (property == BatchesModel.FREQUENCY) {
                                                    return new Label(
                                                            id,
                                                            formatFrequency(
                                                                    itemModel
                                                                            .getObject()
                                                                            .getFrequency()));
                                                } else if (property == BatchesModel.STATUS) {
                                                    return new SimpleAjaxLink<String>(
                                                            id,
                                                            (IModel<String>)
                                                                    property.getModel(itemModel)) {
                                                        private static final long serialVersionUID =
                                                                -9184383036056499856L;

                                                        @Override
                                                        public void onClick(
                                                                AjaxRequestTarget target) {
                                                            setResponsePage(
                                                                    new BatchRunsPage(
                                                                            TaskManagerBeans.get()
                                                                                    .getDao()
                                                                                    .initHistory(
                                                                                            itemModel
                                                                                                    .getObject()),
                                                                            getPage()));
                                                        }
                                                    };
                                                } else if (property == BatchesModel.RUN) {
                                                    if (itemModel.getObject().getId() == null
                                                            || (configurationModel != null
                                                                    && configurationModel
                                                                            .getObject()
                                                                            .isTemplate())
                                                            || (configurationModel != null
                                                                    && !configurationModel
                                                                            .getObject()
                                                                            .isValidated()
                                                                    && !InitConfigUtil.isInitBatch(
                                                                            itemModel.getObject()))
                                                            || !TaskManagerBeans.get()
                                                                    .getSecUtil()
                                                                    .isWritable(
                                                                            ((GeoServerSecuredPage)
                                                                                            getPage())
                                                                                    .getSession()
                                                                                    .getAuthentication(),
                                                                            itemModel
                                                                                    .getObject())) {
                                                        return new Label(id);
                                                    } else {
                                                        SimpleAjaxSubmitLink link =
                                                                new SimpleAjaxSubmitLink(id, null) {
                                                                    private static final long
                                                                            serialVersionUID =
                                                                                    -9184383036056499856L;

                                                                    @Override
                                                                    protected void onSubmit(
                                                                            AjaxRequestTarget
                                                                                    target,
                                                                            Form<?> form) {
                                                                        TaskManagerBeans.get()
                                                                                .getBjService()
                                                                                .scheduleNow(
                                                                                        itemModel
                                                                                                .getObject());
                                                                        info(
                                                                                new ParamResourceModel(
                                                                                                "batchStarted",
                                                                                                BatchesPanel
                                                                                                        .this)
                                                                                        .getString());

                                                                        ((GeoServerBasePage)
                                                                                        getPage())
                                                                                .addFeedbackPanels(
                                                                                        target);
                                                                    }
                                                                };
                                                        link.getLink()
                                                                .add(
                                                                        new AttributeAppender(
                                                                                "class",
                                                                                "play-link",
                                                                                ","));
                                                        return link;
                                                    }
                                                } else {

                                                    return null;
                                                }
                                            }
                                        }));
        batchesPanel.setOutputMarkupId(true);
    }

    private String formatFrequency(String frequency) {
        if (frequency == null) {
            return null;
        }

        Matcher matcher = FrequencyUtil.DAILY_PATTERN.matcher(frequency);
        if (matcher.matches()) {
            int minutes = Integer.parseInt(matcher.group(1));
            int hour = Integer.parseInt(matcher.group(2));
            if (minutes <= 60 && hour < 24) {
                return new ParamResourceModel("Daily", this).getString()
                        + ", "
                        + String.format("%02d", hour)
                        + ":"
                        + String.format("%02d", minutes);
            }
        } else {
            matcher = FrequencyUtil.WEEKLY_PATTERN.matcher(frequency);
            if (matcher.matches()) {
                int minutes = Integer.parseInt(matcher.group(1));
                int hour = Integer.parseInt(matcher.group(2));
                DayOfWeek day = FrequencyUtil.findDayOfWeek(matcher.group(3));
                if (minutes <= 60 && hour < 24 && day != null) {
                    return new ParamResourceModel("Weekly", this).getString()
                            + ", "
                            + day.getDisplayName(TextStyle.FULL, getLocale())
                            + ", "
                            + String.format("%02d", hour)
                            + ":"
                            + String.format("%02d", minutes);
                }
            } else {
                matcher = FrequencyUtil.MONTHLY_PATTERN.matcher(frequency);
                if (matcher.matches()) {
                    int minutes = Integer.parseInt(matcher.group(1));
                    int hour = Integer.parseInt(matcher.group(2));
                    int day = Integer.parseInt(matcher.group(3));
                    if (minutes <= 60 && hour < 24 && day > 0 && day <= 28) {
                        return new ParamResourceModel("Monthly", this).getString()
                                + ", "
                                + new ParamResourceModel("Day", this).getString()
                                + " "
                                + day
                                + ", "
                                + String.format("%02d", hour)
                                + ":"
                                + String.format("%02d", minutes);
                    }
                }
            }
        }

        return new ParamResourceModel("Custom", this).getString() + ", " + frequency;
    }
}
