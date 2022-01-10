/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common.sender;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import org.geoserver.notification.common.NotificationXStreamDefaultInitializer;

/**
 * Topic exchange sender implementation: routes messages to all of the queues that are bound to it
 * and the routing key is used by consumers to filter messages.
 *
 * <p>The broker connection parameters are populated by {@link XStream} deserialization, using the
 * configuration provided by {@link NotificationXStreamDefaultInitializer}
 *
 * <p>
 *
 * @param exchangeName the name of exchange to publish the message to
 * @param routingKey identify the queue to publish the message to
 * @author Xandros
 */
public class TopicRabbitMQSender extends RabbitMQSender {

    private static final long serialVersionUID = 8282122533228442676L;

    public static final String EXCHANGE_TYPE = "topic";

    protected String exchangeName;

    protected String routingKey;

    @Override
    public void sendMessage(byte[] payload) throws IOException {
        if (channel != null) {
            channel.exchangeDeclare(exchangeName, EXCHANGE_TYPE);
            channel.basicPublish(exchangeName, routingKey, null, payload);
        }
    }
}
