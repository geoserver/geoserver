package org.geoserver.task.web;

import java.io.Serializable;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.time.Duration;
import org.geoserver.task.LongTask;
import org.geoserver.task.LongTask.Status;
import org.geoserver.task.LongTaskMonitor;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.IconWithLabel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.springframework.util.Assert;

/**
 * @author groldan
 * 
 */
public class LongTasksPanel extends Panel {

    private static final long serialVersionUID = 1L;

    private static TaskFilter FILTER_NONE = new TaskFilter() {
        private static final long serialVersionUID = 1L;

        public boolean accept(LongTask<?> task) {
            return true;
        }
    };

    private final LongTasksTablePanel longTasksTablePanel;

    private IModel<LongTaskMonitor> monitorModel;

    private boolean hideIfEmpty;

    private TaskFilter filterSubject = FILTER_NONE;

    private final TaskFilter filterProxy = new TaskFilter() {
        private static final long serialVersionUID = 1L;

        public boolean accept(LongTask<?> task) {
            return filterSubject.accept(task);
        }
    };

    public static interface TaskFilter extends Serializable {
        public boolean accept(LongTask<?> task);
    }

    public LongTasksPanel(final String id) {
        this(id, new LongTasksMonitorDetachableModel());
    }

    public LongTasksPanel(final String id, final IModel<LongTaskMonitor> monitorModel) {
        super(id);
        this.monitorModel = monitorModel;
        this.setOutputMarkupId(true);

        add(HeaderContributor.forCss(LongTasksPanel.class, "progressbar.css"));

        Label title = new Label("title", new ResourceModel("LongTasksPanel.runningTitle"));
        add(title);

        GeoServerLongTaskDataProvider dataProvider = new GeoServerLongTaskDataProvider(
                monitorModel, filterProxy);
        longTasksTablePanel = new LongTasksTablePanel("table", dataProvider);
        longTasksTablePanel.setItemsPerPage(5);
        longTasksTablePanel.setPageable(true);
        add(longTasksTablePanel);

        boolean visible = monitorModel.getObject().getTaskCount() > 0;
        longTasksTablePanel.setVisible(visible);

        // refresh the table every 1 second
        add(new AbstractAjaxTimerBehavior(Duration.seconds(2)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                boolean visible = hideIfEmpty ? monitorModel.getObject().getTaskCount() > 0 : true;
                longTasksTablePanel.setVisibilityAllowed(visible);
                target.addComponent(longTasksTablePanel);
                LongTasksPanel.this.onTimerInternal(target);
            }
        });
    }

    /**
     * Override if need to do something else besides updating this component
     */
    protected void onTimerInternal(final AjaxRequestTarget target) {
        // override if need to do something else besides updating this component
    }

    public void setHideIfEmpty(boolean hideIfEmpty) {
        this.hideIfEmpty = hideIfEmpty;
    }

    public void setFilter(final TaskFilter filter) {
        this.filterSubject = filter == null ? FILTER_NONE : filter;
    }

    public void setItemsPerPage(final int itemsPerPage) {
        Assert.isTrue(itemsPerPage > 0);
        this.longTasksTablePanel.setItemsPerPage(itemsPerPage);
    }

    /**
     * @author groldan
     * 
     */
    private static class GeoServerLongTaskDataProvider extends GeoServerDataProvider<LongTask<?>> {

        private static final long serialVersionUID = 1L;

        public static Property<LongTask<?>> STATUS = new BeanProperty<LongTask<?>>("status",
                "status");

        public static Property<LongTask<?>> TITLE = new BeanProperty<LongTask<?>>("title", "title");

        public static Property<LongTask<?>> DESCRIPTION = new BeanProperty<LongTask<?>>("status",
                "status");

        public static Property<LongTask<?>> PROGRESS = new BeanProperty<LongTask<?>>("progress",
                "progress");

        public static final Property<LongTask<?>> ACTION = new PropertyPlaceholder<LongTask<?>>("");

        private final IModel<LongTaskMonitor> monitorModel;

        private final TaskFilter filter;

        public GeoServerLongTaskDataProvider(final IModel<LongTaskMonitor> monitorModel,
                final TaskFilter filter) {
            this.monitorModel = monitorModel;
            this.filter = filter;
        }

        @SuppressWarnings("unchecked")
        @Override
        protected List<Property<LongTask<?>>> getProperties() {
            return Arrays.asList(STATUS, TITLE, PROGRESS, ACTION);
        }

        @Override
        protected List<LongTask<?>> getItems() {
            LongTaskMonitor longTaskMonitor = monitorModel.getObject();
            List<LongTask<?>> allTasks = longTaskMonitor.getAllTasks();
            List<LongTask<?>> filtered = new ArrayList<LongTask<?>>(allTasks.size());
            for (LongTask<?> task : allTasks) {
                if (this.filter.accept(task)) {
                    filtered.add(task);
                }
            }
            Collections.sort(filtered, new Comparator<LongTask<?>>() {

                public int compare(LongTask<?> t1, LongTask<?> t2) {
                    return t1.getStatus().compareTo(t2.getStatus());
                }
            });
            return filtered;
        }

        /**
         * @see org.geoserver.web.wicket.GeoServerDataProvider#newModel(java.lang.Object)
         */
        @Override
        public IModel<LongTask<?>> newModel(final Object task) {

            final Long taskId = monitorModel.getObject().getId((LongTask<?>) task);

            return new LoadableDetachableModel<LongTask<?>>() {
                private static final long serialVersionUID = 1L;

                @Override
                protected LongTask<?> load() {
                    return monitorModel.getObject().getTask(taskId);
                }
            };
        }

    }

    /**
     * @author groldan
     * 
     */
    private class LongTasksTablePanel extends GeoServerTablePanel<LongTask<?>> {

        private static final long serialVersionUID = 1L;

        private final NumberFormat PROGRESS_FORMAT;

        public LongTasksTablePanel(final String id,
                final GeoServerDataProvider<LongTask<?>> dataProvider) {

            super(id, dataProvider);
            super.setOutputMarkupId(true);
            super.setPageable(false);
            super.setFilterable(false);
            PROGRESS_FORMAT = NumberFormat.getPercentInstance(getLocale());
            PROGRESS_FORMAT.setMaximumFractionDigits(2);
        }

        @SuppressWarnings({ "rawtypes", "unchecked" })
        @Override
        protected Component getComponentForProperty(final String id,
                final IModel/* <LongTask> */itemModel, final Property<LongTask<?>> property) {

            final LongTask<?> task = (LongTask<?>) itemModel.getObject();
            if (GeoServerLongTaskDataProvider.STATUS.equals(property)) {

                return statusLink(id, task.getStatus());

            } else if (GeoServerLongTaskDataProvider.TITLE.equals(property)) {

                Label label = new Label(id, task.getTitle());
                label.add(new AttributeModifier("title", true, new Model<String>(task
                        .getDescription())));
                return label;
            } else if (GeoServerLongTaskDataProvider.PROGRESS.equals(property)) {

                IModel<Number> limitModel = new Model<Number>(Integer.valueOf(100));
                IModel<Number> progressModel = new PropertyModel<Number>(itemModel, "progress");
                IModel<String> progressMessageModel = new Model<String>(PROGRESS_FORMAT.format(task
                        .getProgress() / 100));

                String progressMessage = task.getProgressMessage();
                IModel<String> tooltipModel = new Model<String>(progressMessage);

                return new ProgressBar(id, limitModel, progressModel, progressMessageModel,
                        tooltipModel);

            } else if (GeoServerLongTaskDataProvider.ACTION.equals(property)) {
                return actionLink(id, itemModel);
            }
            return null;
        }

        private Component statusLink(final String id, final Status status) {
            String iconName;
            String labelKey;
            switch (status) {
            case ABORTED:
                iconName = "aborted.png";
                labelKey = "LongTasksPanel.aborted";
                break;
            case CANCELLED:
                iconName = "cancelled.gif";
                labelKey = "LongTasksPanel.cancelled";
                break;
            case CANCELLING:
                iconName = "cancelling.gif";
                labelKey = "LongTasksPanel.cancelling";
                break;
            case FINISHED:
                iconName = "finished.png";
                labelKey = "LongTasksPanel.finished";
                break;
            case QUEUED:
                iconName = "queued.gif";
                labelKey = "LongTasksPanel.queued";
                break;
            case RUNNING:
                iconName = "running.gif";
                labelKey = "LongTasksPanel.running";
                break;
            default:
                throw new IllegalArgumentException("Unknown status: " + status);
            }

            ResourceReference iconRef = new ResourceReference(LongTasksPanel.class, iconName);
            IconWithLabel icon = new IconWithLabel(id, iconRef, new ResourceModel(labelKey));
            return icon;
        }

        private Component actionLink(final String id, final IModel<LongTask<?>> itemModel) {
            final Status status = itemModel.getObject().getStatus();
            Component link;
            switch (status) {
            case QUEUED:
            case RUNNING: {
                ResourceReference icon = new ResourceReference(LongTasksPanel.class,
                        "terminate.gif");

                link = new ImageAjaxLink(id, icon) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        LongTask<?> task = itemModel.getObject();
                        if (task != null) {
                            task.cancel();
                        }
                        target.addComponent(LongTasksTablePanel.this);
                    }
                };
                link.add(new AttributeModifier("title", true, new ResourceModel(
                        "LongTasksPanel.cancel")));

            }
                break;
            case CANCELLING: {
                ResourceReference icon = new ResourceReference(LongTasksPanel.class, "waiting.gif");
                Fragment f = new Fragment(id, "iconFragment", LongTasksPanel.this);
                f.add(new Image("icon", icon));
                link = f;
            }
                break;

            default:
                ResourceReference icon = new ResourceReference(LongTasksPanel.class, "delete.png");

                link = new ImageAjaxLink(id, icon) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onClick(final AjaxRequestTarget target) {
                        LongTask<?> task = itemModel.getObject();
                        if (task != null) {
                            LongTasksPanel.this.monitorModel.getObject().removeTerminated(task);
                        }
                        target.addComponent(LongTasksTablePanel.this);
                    }
                };
                link.add(new AttributeModifier("title", true, new ResourceModel(
                        "LongTasksPanel.remove")));

                break;
            }

            return link;
        }
    }
}
