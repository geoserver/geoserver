/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.support;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.notification.common.CustomSaslConfig;
import org.geotools.util.logging.Logging;

public class Receiver {

    protected static Logger LOGGER = Logging.getLogger(Receiver.class);

    private static String BROKER_URI = "amqp://localhost:4432";

    private static final String QUEUE_NAME = "jms/queue";

    private ReceiverService service;

    private Connection connection;

    private Channel channel;

    public Receiver() {}

    public Receiver(String username, String password) {
        if (username != null && !username.isEmpty() && password != null && !password.isEmpty()) {
            BROKER_URI = "amqp://" + username + ":" + password + "@localhost:4432";
        }
    }

    public void receive(ReceiverService service) throws Exception {
        // let's setup evrything and start listening
        this.service = service;
        ConnectionFactory factory = createConnectionFactory();
        factory.setSaslConfig(new CustomSaslConfig());
        connection = factory.newConnection();
        channel = connection.createChannel();
        channel.exchangeDeclare("testExchange", "fanout");
        channel.queueDeclare(QUEUE_NAME, false, true, false, null);
        channel.queueBind(QUEUE_NAME, "testExchange", "testRouting");
        channel.basicConsume(QUEUE_NAME, true, newConsumer(channel));
    }

    protected ConnectionFactory createConnectionFactory() throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setAutomaticRecoveryEnabled(false);
        factory.setUri(BROKER_URI);
        return factory;
    }

    private DefaultConsumer newConsumer(Channel channel) {
        return new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(
                    String consumerTag,
                    Envelope envelope,
                    AMQP.BasicProperties properties,
                    byte[] body)
                    throws IOException {
                service.manage(body);
            }
        };
    }

    public void close() {
        if (channel != null) {
            try {
                channel.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
        if (connection != null) {
            try {
                connection.close();
            } catch (Exception e) {
                LOGGER.log(Level.FINER, e.getMessage(), e);
            }
        }
    }
}
