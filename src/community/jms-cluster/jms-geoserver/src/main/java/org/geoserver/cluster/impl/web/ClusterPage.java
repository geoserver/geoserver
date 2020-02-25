/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.web;

import java.io.IOException;
import java.util.Properties;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxButton;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.geoserver.cluster.JMSFactory;
import org.geoserver.cluster.client.JMSContainer;
import org.geoserver.cluster.configuration.BrokerConfiguration;
import org.geoserver.cluster.configuration.ConnectionConfiguration;
import org.geoserver.cluster.configuration.ConnectionConfiguration.ConnectionConfigurationStatus;
import org.geoserver.cluster.configuration.EmbeddedBrokerConfiguration;
import org.geoserver.cluster.configuration.JMSConfiguration;
import org.geoserver.cluster.configuration.ReadOnlyConfiguration;
import org.geoserver.cluster.configuration.ToggleConfiguration;
import org.geoserver.cluster.configuration.TopicConfiguration;
import org.geoserver.cluster.events.ToggleEvent;
import org.geoserver.cluster.events.ToggleType;
import org.geoserver.config.ReadOnlyGeoServerLoader;
import org.geoserver.web.GeoServerSecuredPage;
import org.geotools.util.logging.Logging;
import org.springframework.context.ApplicationContext;

public class ClusterPage extends GeoServerSecuredPage {

    private static final java.util.logging.Logger LOGGER = Logging.getLogger(ClusterPage.class);

    public ClusterPage() {

        final FeedbackPanel fp = bottomFeedbackPanel;

        // setup the JMSContainer exception handler
        getJMSContainerExceptionHandler().setFeedbackPanel(fp);
        getJMSContainerExceptionHandler().setSession(fp.getSession());

        fp.setOutputMarkupId(true);

        // form and submit
        Properties configurations = getConfig().getConfigurations();
        final Form<Properties> form =
                new Form<Properties>("form", new CompoundPropertyModel<Properties>(configurations));

        // add broker URL setting
        final TextField<String> brokerURL =
                new TextField<String>(BrokerConfiguration.BROKER_URL_KEY, String.class);
        form.add(brokerURL);

        // add group name setting
        final TextField<String> instanceName =
                new TextField<String>(JMSConfiguration.INSTANCE_NAME_KEY, String.class);
        form.add(instanceName);

        // add instance name setting
        final TextField<String> group =
                new TextField<String>(JMSConfiguration.GROUP_KEY, String.class);
        form.add(group);

        // add topic name setting
        final TextField<String> topicName =
                new TextField<String>(TopicConfiguration.TOPIC_NAME_KEY, String.class);
        topicName.setType(String.class);
        form.add(topicName);

        // add connection status info
        final TextField<String> connectionInfo =
                new TextField<String>(ConnectionConfiguration.CONNECTION_KEY, String.class);

        // https://issues.apache.org/jira/browse/WICKET-2426
        connectionInfo.setType(String.class);

        connectionInfo.setOutputMarkupId(true);
        connectionInfo.setOutputMarkupPlaceholderTag(true);
        connectionInfo.setEnabled(false);
        form.add(connectionInfo);

        final AjaxButton connection =
                new AjaxButton("connectionB") {
                    /** serialVersionUID */
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(
                            AjaxRequestTarget target,
                            org.apache.wicket.markup.html.form.Form<?> form) {
                        // the container to use
                        final JMSContainer c = getJMSContainer();
                        if (c.isRunning()) {
                            fp.info("Disconnecting...");
                            if (c.disconnect()) {
                                fp.info("Succesfully un-registered from the destination topic");
                                fp.warn(
                                        "You will (probably) loose next incoming events from other instances!!! (depending on how you have configured the broker)");
                                connectionInfo
                                        .getModel()
                                        .setObject(
                                                ConnectionConfigurationStatus.disabled.toString());
                            } else {
                                fp.error("Disconnection error!");
                                connectionInfo
                                        .getModel()
                                        .setObject(
                                                ConnectionConfigurationStatus.enabled.toString());
                            }
                        } else {
                            fp.info("Connecting...");
                            if (c.connect()) {
                                fp.info("Now GeoServer is registered with the destination");
                                connectionInfo
                                        .getModel()
                                        .setObject(
                                                ConnectionConfigurationStatus.enabled.toString());
                            } else {
                                fp.error("Connection error!");
                                fp.error("Registration aborted due to a connection problem");
                                connectionInfo
                                        .getModel()
                                        .setObject(
                                                ConnectionConfigurationStatus.disabled.toString());
                            }
                        }
                        target.add(connectionInfo);
                        target.add(fp);
                    }
                };
        connection.setOutputMarkupId(true);
        connection.setOutputMarkupPlaceholderTag(true);
        form.add(connection);

        // add MASTER toggle
        addToggle(
                ToggleConfiguration.TOGGLE_MASTER_KEY,
                ToggleType.MASTER,
                ToggleConfiguration.TOGGLE_MASTER_KEY,
                "toggleMasterB",
                form,
                fp);

        // add SLAVE toggle
        addToggle(
                ToggleConfiguration.TOGGLE_SLAVE_KEY,
                ToggleType.SLAVE,
                ToggleConfiguration.TOGGLE_SLAVE_KEY,
                "toggleSlaveB",
                form,
                fp);

        // add Read Only switch
        final TextField<String> readOnlyInfo =
                new TextField<String>(ReadOnlyConfiguration.READ_ONLY_KEY);

        // https://issues.apache.org/jira/browse/WICKET-2426
        readOnlyInfo.setType(String.class);

        readOnlyInfo.setOutputMarkupId(true);
        readOnlyInfo.setOutputMarkupPlaceholderTag(true);
        readOnlyInfo.setEnabled(false);
        form.add(readOnlyInfo);

        final AjaxButton readOnly =
                new AjaxButton("readOnlyB") {
                    /** serialVersionUID */
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(
                            AjaxRequestTarget target,
                            org.apache.wicket.markup.html.form.Form<?> form) {
                        ReadOnlyGeoServerLoader loader = getReadOnlyGeoServerLoader();
                        if (loader.isEnabled()) {
                            readOnlyInfo.getModel().setObject("disabled");
                            loader.enable(false);
                        } else {
                            readOnlyInfo.getModel().setObject("enabled");
                            loader.enable(true);
                        }
                        target.add(this.getParent());
                    }
                };
        form.add(readOnly);

        final Button save =
                new Button("saveB") {

                    /** serialVersionUID */
                    private static final long serialVersionUID = 1L;

                    @Override
                    public void onSubmit() {
                        try {
                            getConfig().storeConfig();
                            fp.info("Configuration saved");
                        } catch (IOException e) {
                            if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                                LOGGER.severe(e.getLocalizedMessage());
                            fp.error(e.getLocalizedMessage());
                        }
                    }
                };
        form.add(save);

        // add Read Only switch
        final TextField<String> embeddedBrokerInfo =
                new TextField<String>(EmbeddedBrokerConfiguration.EMBEDDED_BROKER_KEY);

        // https://issues.apache.org/jira/browse/WICKET-2426
        embeddedBrokerInfo.setType(String.class);

        embeddedBrokerInfo.setOutputMarkupId(true);
        embeddedBrokerInfo.setOutputMarkupPlaceholderTag(true);
        embeddedBrokerInfo.setEnabled(false);
        form.add(embeddedBrokerInfo);

        final AjaxButton embeddedBroker =
                new AjaxButton("embeddedBrokerB") {
                    /** serialVersionUID */
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onSubmit(
                            AjaxRequestTarget target,
                            org.apache.wicket.markup.html.form.Form<?> form) {
                        JMSFactory factory = getJMSFactory();
                        if (!factory.isEmbeddedBrokerStarted()) {
                            try {
                                if (factory.startEmbeddedBroker(configurations)) {
                                    embeddedBrokerInfo.getModel().setObject("enabled");
                                }
                            } catch (Exception e) {
                                if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                                    LOGGER.severe(e.getLocalizedMessage());
                                fp.error(e.getLocalizedMessage());
                            }
                        } else {
                            try {
                                if (factory.stopEmbeddedBroker()) {
                                    embeddedBrokerInfo.getModel().setObject("disabled");
                                }
                            } catch (Exception e) {
                                if (LOGGER.isLoggable(java.util.logging.Level.SEVERE))
                                    LOGGER.severe(e.getLocalizedMessage());
                                fp.error(e.getLocalizedMessage());
                            }
                        }
                        target.add(this.getParent());
                    }
                };
        form.add(embeddedBroker);

        // add the form
        add(form);

        // make sure all text fields are not set to null in case of empty string, the
        // property model is based on HastTable, that cannot handle null values
        form.visitChildren(
                TextField.class,
                (component, visit) -> {
                    TextField tf = (TextField) component;
                    tf.setConvertEmptyInputStringToNull(false);
                });

        // add the status monitor
        add(fp);
    }

    // final JMSConfiguration config,
    private void addToggle(
            final String configKey,
            final ToggleType type,
            final String textFieldId,
            final String buttonId,
            final Form<?> form,
            final FeedbackPanel fp) {

        final TextField<String> toggleInfo = new TextField<String>(textFieldId, String.class);

        // https://issues.apache.org/jira/browse/WICKET-2426
        // toggleInfo.setType(String.class);

        toggleInfo.setOutputMarkupId(true);
        toggleInfo.setOutputMarkupPlaceholderTag(true);
        toggleInfo.setEnabled(false);
        form.add(toggleInfo);

        final AjaxButton toggle =
                new AjaxButton(buttonId) {
                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void onError(
                            AjaxRequestTarget target,
                            org.apache.wicket.markup.html.form.Form<?> form) {
                        fp.error("ERROR");

                        target.add(fp);
                    };

                    @Override
                    protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                        final boolean switchTo =
                                !Boolean.parseBoolean(toggleInfo.getModel().getObject());
                        final ApplicationContext ctx =
                                getGeoServerApplication().getApplicationContext();
                        ctx.publishEvent(new ToggleEvent(switchTo, type));
                        // getConfig().putConfiguration(configKey,
                        // Boolean.toString(switchTo));
                        if (switchTo) {
                            fp.info("The " + type + " toggle is now ENABLED");
                        } else {
                            fp.warn(
                                    "The "
                                            + type
                                            + " toggle is now DISABLED no event will be posted/received to/from the broker");
                            fp.info(
                                    "Note that the "
                                            + type
                                            + " is still registered to the topic destination");
                        }
                        toggleInfo.getModel().setObject(Boolean.toString(switchTo));
                        target.add(toggleInfo);
                        target.add(fp);
                    }
                };
        toggle.setRenderBodyOnly(false);

        form.add(toggle);

        // add(new Monitor(Duration.seconds(10)));
    }

    protected JMSConfiguration getConfig() {
        return getGeoServerApplication().getBeanOfType(JMSConfiguration.class);
    }

    protected JMSContainer getJMSContainer() {
        return getGeoServerApplication().getBeanOfType(JMSContainer.class);
    }

    protected JMSFactory getJMSFactory() {
        return getGeoServerApplication().getBeanOfType(JMSFactory.class);
    }

    protected ReadOnlyGeoServerLoader getReadOnlyGeoServerLoader() {
        return getGeoServerApplication().getBeanOfType(ReadOnlyGeoServerLoader.class);
    }

    protected JMSContainerHandlerExceptionListenerImpl getJMSContainerExceptionHandler() {
        return getGeoServerApplication()
                .getBeanOfType(JMSContainerHandlerExceptionListenerImpl.class);
    }
}
