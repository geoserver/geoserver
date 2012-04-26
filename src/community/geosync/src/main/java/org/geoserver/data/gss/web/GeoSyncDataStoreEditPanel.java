package org.geoserver.data.gss.web;

import static org.geoserver.data.gss.GeoSyncDataStoreFactory.GSS_ACTIVE_REPLICATION;
import static org.geoserver.data.gss.GeoSyncDataStoreFactory.GSS_CAPABILITIES_URL;
import static org.geoserver.data.gss.GeoSyncDataStoreFactory.GSS_PASSIVE_REPLICATION;
import static org.geoserver.data.gss.GeoSyncDataStoreFactory.GSS_PASSWORD;
import static org.geoserver.data.gss.GeoSyncDataStoreFactory.GSS_POLL_INTERVAL_SECS;
import static org.geoserver.data.gss.GeoSyncDataStoreFactory.GSS_REPLICATED_NAMESPACE;
import static org.geoserver.data.gss.GeoSyncDataStoreFactory.GSS_USER;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.IFormValidator;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.util.convert.IConverter;
import org.apache.wicket.util.time.Duration;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.data.gss.Capabilities;
import org.geoserver.data.gss.GeoSyncClient;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.task.LongTask;
import org.geoserver.task.LongTaskMonitor;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.NamespacePanel;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geoserver.web.util.MetadataMapModel;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;

import com.google.common.base.Preconditions;

@SuppressWarnings("unchecked")
public class GeoSyncDataStoreEditPanel extends StoreEditPanel {

    private static final long serialVersionUID = 1050076243346394589L;

    final NamespacePanel namespacePanel;

    final TextParamPanel capabilitiesUrl;

    final TextParamPanel user;

    final PasswordParamPanel password;

    final CheckBoxParamPanel usePassiveReplication;

    final CheckBoxParamPanel useActiveReplication;

    final TextParamPanel activeReplicationPollInterval;

    final TextField<Long> lastSynchronized;

    final Label connectMessage;

    private GeoServerAjaxFormLink connectButton;

    /**
     * Created by the {@link #connectButton} call, used the check the status of the getcapabilities
     * task from {@link LongTaskMonitor} by the {@link #checkCapabilities} timer, used to remove the
     * task from {@link LongTaskMonitor} by the {@link #formValidator} when everything is right.
     */
    private Long capabilitiesRequestFutureID;

    private AbstractAjaxTimerBehavior checkCapabilities;

    private boolean doCheckCapabilities = false;

    private IFormValidator formValidator;

    /**
     * Model to wrap and unwrap a {@link NamespaceInfo} to and from a String for the Datastore's
     * "namespace" parameter
     * 
     */
    private final class NamespaceParamModel extends MapModel {
        private NamespaceParamModel(IModel model, String expression) {
            super(model, expression);
        }

        @Override
        public Object getObject() {
            String nsUri = (String) super.getObject();
            NamespaceInfo namespaceInfo = getCatalog().getNamespaceByURI(nsUri);
            return namespaceInfo;
        }

        @Override
        public void setObject(Object object) {
            NamespaceInfo namespaceInfo = (NamespaceInfo) object;
            String nsUri = namespaceInfo.getURI();
            super.setObject(nsUri);
        }
    }

    public GeoSyncDataStoreEditPanel(final String componentId,
            final Form<DataStoreInfo> storeEditForm) {
        super(componentId, storeEditForm);

        final IModel<DataStoreInfo> model = storeEditForm.getModel();

        final IModel<Map<String, Serializable>> paramsModel;
        paramsModel = new PropertyModel<Map<String, Serializable>>(model, "connectionParameters");

        setDefaultModel(paramsModel);

        IModel namespaceModel = new NamespaceParamModel(paramsModel, GSS_REPLICATED_NAMESPACE.key);
        IModel paramLabelModel = new ResourceModel("namespace", "namespace");
        namespacePanel = new NamespacePanel("namespace", namespaceModel, paramLabelModel, true);
        add(namespacePanel);

        add(capabilitiesUrl = capabilitiesUrl());
        add(user = user());
        add(password = password());
        add(usePassiveReplication = usePassiveReplication());
        add(useActiveReplication = useActiveReplication());
        add(activeReplicationPollInterval = activeReplicationPollInterval());
        add(lastSynchronized = lastSynchronized(model));

        usePassiveReplication.setEnabled(false);
        useActiveReplication.setEnabled(false);
        activeReplicationPollInterval.setEnabled(false);

        connectMessage = new Label("connectMessage", new Model<String>());
        connectMessage.setOutputMarkupId(true);
        add(connectMessage);

        connectButton = new GeoServerAjaxFormLink("connect", storeEditForm) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onClick(AjaxRequestTarget target, Form form) {
                final LongTaskMonitor longTaskMonitor = LongTaskMonitor.get();
                if (capabilitiesRequestFutureID != null) {
                    final LongTask<Capabilities> task = (LongTask<Capabilities>) longTaskMonitor
                            .getTask(capabilitiesRequestFutureID);
                    if (task != null) {
                        if (!task.isDone()) {
                            connectMessage
                                    .setDefaultModelObject("Another request is in course, please wait...");
                            target.addComponent(connectMessage);
                            return;
                        } else {
                            longTaskMonitor.removeTerminated(task);
                        }
                    }
                }

                connectMessage.setDefaultModelObject("Retrieving GetCapabilities...");
                target.addComponent(connectMessage);

                final GeoSyncClient gssClient = GeoServerExtensions.bean(GeoSyncClient.class);
                Preconditions.checkState(gssClient != null, "GeoSyncClient not found");

                capabilitiesUrl.getFormComponent().processInput();
                user.getFormComponent().processInput();
                password.getFormComponent().processChildren();

                final String urlStr = String.valueOf(capabilitiesUrl.getFormComponent()
                        .getModelObject());
                final String userParam = (String) user.getFormComponent().getModelObject();
                final String passwordParam = password.getFormComponent().getModelObject();
                try {
                    LongTask<Capabilities> capabilitiesRequestFuture;
                    capabilitiesRequestFuture = gssClient.getCapabilities(urlStr, userParam,
                            passwordParam);
                    capabilitiesRequestFutureID = longTaskMonitor.getId(capabilitiesRequestFuture);
                } catch (Exception e) {
                    connectMessage.setDefaultModelObject("Error: " + e.getMessage());
                }
                target.addComponent(connectMessage);
                usePassiveReplication.setEnabled(false);
                useActiveReplication.setEnabled(false);
                activeReplicationPollInterval.setEnabled(false);
                target.addComponent(usePassiveReplication);
                target.addComponent(useActiveReplication);
                target.addComponent(activeReplicationPollInterval);
                doCheckCapabilities = true;
            }
        };

        // check if capabilities was obtained every 1 second
        checkCapabilities = new AbstractAjaxTimerBehavior(Duration.seconds(1)) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onTimer(final AjaxRequestTarget target) {
                if (!doCheckCapabilities) {
                    return;
                }
                if (capabilitiesRequestFutureID == null) {
                    return;
                }
                
                doCheckCapabilities = false;
                final LongTaskMonitor longTaskMonitor = LongTaskMonitor.get();
                final LongTask<Capabilities> task = (LongTask<Capabilities>) longTaskMonitor
                        .getTask(capabilitiesRequestFutureID);
                if (task == null) {
                    doCheckCapabilities = true;
                    return;
                }
                if (!task.isDone()) {
                    doCheckCapabilities = true;
                    return;
                }

                switch (task.getStatus()) {
                case ABORTED:
                    Exception e = task.getException();
                    connectMessage.setDefaultModelObject("Error: " + e.getMessage());
                    break;
                case CANCELLED:
                    connectMessage.setDefaultModelObject("Request cancelled, try again");
                    break;
                case FINISHED:
                    Capabilities capabilities = task.getResult();
                    String title = capabilities.getServiceTitle();
                    connectMessage.setDefaultModelObject("Connected to " + title);

                    usePassiveReplication.setEnabled(false);// should be true when we support
                                                            // subscription
                    useActiveReplication.setEnabled(true);
                    activeReplicationPollInterval.setEnabled(true);
                    target.addComponent(usePassiveReplication);
                    target.addComponent(useActiveReplication);
                    target.addComponent(activeReplicationPollInterval);
                    break;
                }

                target.addComponent(connectMessage);
            }
        };

        add(checkCapabilities);
        add(connectButton);

        storeEditForm.add(new IFormValidator() {
            private static final long serialVersionUID = 1L;

            @Override
            public FormComponent<?>[] getDependentFormComponents() {
                return new FormComponent<?>[] { capabilitiesUrl.getFormComponent() };
            }

            @Override
            public void validate(Form<?> form) {
                if (capabilitiesRequestFutureID == null) {
                    form.error("Press connect first to verify the server's capabilities.");
                    return;
                }
                final LongTaskMonitor longTaskMonitor = LongTaskMonitor.get();
                final LongTask<Capabilities> task = (LongTask<Capabilities>) longTaskMonitor
                        .getTask(capabilitiesRequestFutureID);
                if (task == null) {
                    form.error("Press connect first to verify the server's capabilities.");
                    return;
                }
                if (!task.isDone()) {
                    form.error("Waiting for GetCapabilities call to return...");
                    return;
                }
                if (!LongTask.Status.FINISHED.equals(task.getStatus())) {
                    form.error("GetCapabilities operation is " + task.getStatus() + ". Try again.");
                }
                // everything's allright
                longTaskMonitor.removeTerminated(task);
            }
        });
    }

    private TextField<Long> lastSynchronized(IModel<DataStoreInfo> model) {

        IModel<MetadataMap> metadata = new PropertyModel<MetadataMap>(model, "metadata");
        IModel<Long> timestampModel = new MetadataMapModel(metadata,
                GeoSyncClient.LAST_REPLICA_TIME, Long.class);

        @SuppressWarnings("serial")
        TextField<Long> lastSynchronized = new TextField<Long>("lastSynchronized", timestampModel) {
            @SuppressWarnings("serial")
            @Override
            public IConverter getConverter(Class<?> type) {
                return new IConverter() {
                    @Override
                    public String convertToString(Object value, Locale locale) {
                        if (value == null) {
                            return "--";
                        }
                        long timestamp = ((Long) value).longValue();
                        DateFormat format = SimpleDateFormat.getDateTimeInstance(
                                DateFormat.DEFAULT, DateFormat.DEFAULT, locale);
                        return format.format(new Date(timestamp));
                    }

                    @Override
                    public Object convertToObject(String value, Locale locale) {
                        if (value == null || value.length() == 0) {
                            return null;
                        }
                        DateFormat format = SimpleDateFormat.getDateTimeInstance(
                                DateFormat.DEFAULT, DateFormat.DEFAULT, locale);
                        try {
                            Date parsed = format.parse(value);
                            return Long.valueOf(parsed.getTime());
                        } catch (ParseException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            };
        };
        lastSynchronized.setOutputMarkupId(true);

        return lastSynchronized;
    }

    private TextParamPanel capabilitiesUrl() {
        IModel<String> model = new MapModel(getDefaultModel(), GSS_CAPABILITIES_URL.key);
        IModel<String> paramLabelModel = new ResourceModel(GSS_CAPABILITIES_URL.key);
        TextParamPanel capabilitiesUrl = new TextParamPanel("capabilitiesUrl", model,
                paramLabelModel, true);
        capabilitiesUrl.setOutputMarkupId(true);
        capabilitiesUrl.getFormComponent().add(
                new AttributeModifier("style", true, new Model<String>("width:90%;")));
        return capabilitiesUrl;
    }

    private TextParamPanel user() {
        IModel<String> model = new MapModel(getDefaultModel(), GSS_USER.key);
        IModel<String> paramLabelModel = new ResourceModel(GSS_USER.key);
        TextParamPanel user = new TextParamPanel("user", model, paramLabelModel, false);
        user.getFormComponent().add(
                new AttributeModifier("style", true, new Model<String>("width:150px;")));

        user.setOutputMarkupId(true);

        return user;
    }

    private PasswordParamPanel password() {
        IModel<String> model = new MapModel(getDefaultModel(), GSS_PASSWORD.key);
        IModel<String> paramLabelModel = new ResourceModel(GSS_PASSWORD.key);
        PasswordParamPanel password = new PasswordParamPanel("password", model, paramLabelModel,
                false);
        password.setOutputMarkupId(true);

        return password;
    }

    private CheckBoxParamPanel usePassiveReplication() {
        IModel<Boolean> model = new MapModel(getDefaultModel(), GSS_PASSIVE_REPLICATION.key);
        IModel<String> paramLabelModel = new ResourceModel(GSS_PASSIVE_REPLICATION.key);
        CheckBoxParamPanel passiveReplication = new CheckBoxParamPanel("usePassiveReplication",
                model, paramLabelModel);
        passiveReplication.setOutputMarkupId(true);

        return passiveReplication;
    }

    private CheckBoxParamPanel useActiveReplication() {
        IModel<Boolean> model = new MapModel(getDefaultModel(), GSS_ACTIVE_REPLICATION.key);
        IModel<String> paramLabelModel = new ResourceModel(GSS_ACTIVE_REPLICATION.key);
        CheckBoxParamPanel activeReplication = new CheckBoxParamPanel("useActiveReplication",
                model, paramLabelModel);
        activeReplication.setOutputMarkupId(true);

        return activeReplication;
    }

    private TextParamPanel activeReplicationPollInterval() {
        IModel<Integer> model = new MapModel(getDefaultModel(), GSS_POLL_INTERVAL_SECS.key);
        IModel<String> paramLabelModel = new ResourceModel(GSS_POLL_INTERVAL_SECS.key);
        TextParamPanel pollInterval = new TextParamPanel("activeReplicationPollInterval", model,
                paramLabelModel, false);
        pollInterval.setOutputMarkupId(true);
        pollInterval.getFormComponent().add(
                new AttributeModifier("style", true, new Model<String>("width:150px;")));

        return pollInterval;
    }

}
