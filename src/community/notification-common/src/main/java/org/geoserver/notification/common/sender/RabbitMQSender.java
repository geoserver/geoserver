/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common.sender;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.notification.common.CustomSaslConfig;
import org.geoserver.notification.common.NotificationXStreamDefaultInitializer;
import org.geotools.util.logging.Logging;

/**
 * Initialize the AMQP broker client connection and delegate to specific RabbitMQ client
 * implementation the dispatch of payload.
 *
 * <p>The broker connection parameters are populated by {@link XStream} deserialization, using the
 * configuration provided by {@link NotificationXStreamDefaultInitializer}
 *
 * <p>Anonymous connection is possible using {@link CustomSaslConfig}
 *
 * @param host the host to which the underlying TCP connection is made
 * @param port the port number to which the underlying TCP connection is made
 * @param virtualHost a path which acts as a namespace (optional)
 * @param username if present is used for SASL exchange (optional)
 * @param password if present is used for SASL exchange (optional)
 * @author Xandros
 * @see FanoutRabbitMQSender
 */
public abstract class RabbitMQSender implements NotificationSender, Serializable {

    private static Logger LOGGER = Logging.getLogger(RabbitMQSender.class);

    private static final long serialVersionUID = 1370640635300148935L;

    protected String host;

    protected String virtualHost;

    protected int port;

    protected String username;

    protected String password;

    protected String uri;

    protected Connection conn;

    protected Channel channel;

    public void initialize() throws Exception {
        if (uri == null) {
            if (this.username != null
                    && !this.username.isEmpty()
                    && this.password != null
                    && !this.password.isEmpty()) {
                this.uri =
                        "amqp://"
                                + this.username
                                + ":"
                                + this.password
                                + "@"
                                + this.host
                                + ":"
                                + this.port;
            } else {
                this.uri = "amqp://" + this.host + ":" + this.port;
            }
        }

        try {
            ConnectionFactory factory = new ConnectionFactory();
            factory.setUri(this.uri);
            String vHost =
                    (this.virtualHost != null && !this.virtualHost.isEmpty()
                            ? this.virtualHost
                            : "/");
            factory.setVirtualHost(vHost);
            factory.setSaslConfig(new CustomSaslConfig());
            conn = factory.newConnection();
            channel = conn.createChannel();
        } catch (Exception e) {
            LOGGER.log(
                    Level.WARNING, "Error while trying to initialize RabbitMQ Sender Connecton", e);
        }
    }

    public void close() throws Exception {
        if (this.channel != null) {
            this.channel.close();
        }

        if (this.conn != null) {
            this.conn.close();
        }
    }

    // Prepare Connection Channel
    public void send(byte[] payload) throws Exception {
        try {
            this.initialize();
            this.sendMessage(payload);
        } catch (Exception e) {
            LOGGER.log(Level.FINEST, e.getMessage(), e);
        } finally {
            this.close();
        }
    }

    // Send message to the Queue by using Channel
    public abstract void sendMessage(byte[] payload) throws IOException;
}
