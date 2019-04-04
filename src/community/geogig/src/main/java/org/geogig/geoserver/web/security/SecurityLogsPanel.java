/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.security;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.basic.MultiLineLabel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.apache.wicket.util.file.File;
import org.geogig.geoserver.config.LogEvent;
import org.geogig.geoserver.config.LogEvent.Severity;
import org.geogig.geoserver.config.LogStore;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.Icon;
import org.geoserver.web.wicket.SimpleAjaxLink;
import org.geotools.feature.type.DateUtil;

public class SecurityLogsPanel extends GeoServerTablePanel<LogEvent> {

    private static final long serialVersionUID = 5957961031378924960L;

    private static final EnumMap<Severity, PackageResourceReference> SEVERITY_ICONS =
            new EnumMap<>(Severity.class);

    static {
        final PackageResourceReference infoIcon =
                new PackageResourceReference(
                        GeoServerBasePage.class, "img/icons/silk/information.png");

        final PackageResourceReference successIcon =
                new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/accept.png");

        final PackageResourceReference errorIcon =
                new PackageResourceReference(GeoServerBasePage.class, "img/icons/silk/error.png");
        SEVERITY_ICONS.put(Severity.DEBUG, infoIcon);
        SEVERITY_ICONS.put(Severity.INFO, successIcon);
        SEVERITY_ICONS.put(Severity.ERROR, errorIcon);
    }

    private final ModalWindow popupWindow;

    /** Only to be used by {@link #logStore()} */
    private transient LogStore logStore;

    public SecurityLogsPanel(final String id) {
        super(id, new LogEventProvider(), false /* selectable */);
        super.setSelectable(false);
        super.setSortable(true);
        super.setFilterable(true);
        super.setFilterVisible(true);
        super.setPageable(true);
        popupWindow = new ModalWindow("popupWindow");
        add(popupWindow);
    }

    private LogStore logStore() {
        if (this.logStore == null) {
            this.logStore = GeoServerExtensions.bean(LogStore.class);
        }
        return this.logStore;
    }

    @Override
    protected Component getComponentForProperty(
            final String id,
            @SuppressWarnings("rawtypes") IModel<LogEvent> itemModel,
            Property<LogEvent> property) {

        LogEvent item = (LogEvent) itemModel.getObject();
        if (property == LogEventProvider.SEVERITY) {
            Severity severity = item.getSeverity();
            PackageResourceReference iconRef = SEVERITY_ICONS.get(severity);
            return new Icon(id, iconRef);
        }
        if (property == LogEventProvider.REPOSITORY) {
            return repositoryLink(id, item);
        }
        if (property == LogEventProvider.TIMESTAMP) {
            return new Label(id, DateUtil.serializeDateTime(item.getTimestamp()));
        }
        if (property == LogEventProvider.MESSAGE) {
            return messageLink(id, item);
        }
        return new Label(id, String.valueOf(property.getPropertyValue(item)));
    }

    private Component messageLink(String id, LogEvent item) {
        IModel<String> messageModel = new Model<>(item.getMessage());
        if (!item.getSeverity().equals(Severity.ERROR)) {
            return new Label(id, messageModel);
        }

        SimpleAjaxLink<LogEvent> link =
                new SimpleAjaxLink<LogEvent>(id, new Model<>(item), messageModel) {

                    private static final long serialVersionUID = 1242472443848716943L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        LogEvent event = getModelObject();
                        long eventId = event.getEventId();
                        LogStore logStore = logStore();
                        String stackTrace = logStore.getStackTrace(eventId);
                        popupWindow.setInitialHeight(525);
                        popupWindow.setInitialWidth(855);
                        popupWindow.setContent(
                                new StackTracePanel(popupWindow.getContentId(), stackTrace));
                        popupWindow.show(target);
                    }
                };
        return link;
    }

    public static class StackTracePanel extends Panel {
        private static final long serialVersionUID = 6428694990556424777L;

        public StackTracePanel(String id, String stackTrace) {
            super(id);

            MultiLineLabel stackTraceLabel = new MultiLineLabel("trace");

            add(stackTraceLabel);

            if (stackTrace != null) {
                stackTraceLabel.setDefaultModel(new Model<>(stackTrace));
            }
        }
    }

    private Component repositoryLink(String id, LogEvent item) {
        String repositoryURL = item.getRepositoryURL();
        String name = new File(repositoryURL).getName();
        Label label = new Label(id, name);
        label.add(AttributeModifier.replace("title", repositoryURL));
        return label;
    }

    static class LogEventProvider extends GeoServerDataProvider<LogEvent> {

        private static final long serialVersionUID = 4883560661021761394L;

        // static final Property<LogEvent> EVENT_ID = new BeanProperty<LogEvent>("eventId",
        // "eventId");

        static final Property<LogEvent> SEVERITY = new BeanProperty<>("severity", "severity");

        static final Property<LogEvent> TIMESTAMP = new BeanProperty<>("timestamp", "timestamp");

        static final Property<LogEvent> REPOSITORY =
                new BeanProperty<>("repository", "repositoryURL");

        static final Property<LogEvent> USER = new BeanProperty<>("user", "user");

        static final Property<LogEvent> MESSAGE = new BeanProperty<>("message", "message");

        final List<Property<LogEvent>> PROPERTIES =
                Arrays.asList(
                        /* EVENT_ID, */
                        SEVERITY, TIMESTAMP, REPOSITORY, USER, MESSAGE);

        // private transient List<LogEvent> items;

        private transient LogStore logStore;

        public LogEventProvider() {}

        private LogStore logStore() {
            if (logStore == null) {
                logStore = GeoServerExtensions.bean(LogStore.class);
            }
            return logStore;
        }

        @Override
        protected List<LogEvent> getItems() {
            // ensure logstore is set
            logStore();
            List<LogEvent> items = logStore.getLogEntries(0, Integer.MAX_VALUE);

            return items;
        }

        @Override
        protected List<Property<LogEvent>> getProperties() {
            return PROPERTIES;
        }

        @Override
        public IModel<LogEvent> newModel(LogEvent object) {
            return new Model<>(object);
        }

        @Override
        public int fullSize() {
            // ensure logstore is set
            logStore();
            return logStore.getFullSize();
        }
    }
}
