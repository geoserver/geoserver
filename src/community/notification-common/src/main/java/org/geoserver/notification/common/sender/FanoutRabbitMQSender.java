/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common.sender;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import org.geoserver.notification.common.NotificationXStreamDefaultInitializer;

/**
 * Fanout exchange sender implementation: routes messages to all of the queues that are bound to it
 * and the routing key is ignore (ideal for the broadcast routing of messages)
 *
 * <p>The broker connection parameters are populated by {@link XStream} deserialization, using the
 * configuration provided by {@link NotificationXStreamDefaultInitializer}
 *
 * <p>
 *
 * @param exchangeName the name of exchange to publish the message to
 * @param routingKey identify the queue to publish the message to (ignored by fanout type)
 * @author Xandros
 */
public class FanoutRabbitMQSender extends RabbitMQSender {

    private static final long serialVersionUID = -1947966245086626842L;

    public static final String EXCHANGE_TYPE = "fanout";

    protected String exchangeName;

    protected String routingKey;

    @Override
    public void sendMessage(byte[] payload) throws IOException {
        channel.exchangeDeclare(exchangeName, EXCHANGE_TYPE);
        channel.basicPublish(exchangeName, routingKey, null, payload);
    }
}
