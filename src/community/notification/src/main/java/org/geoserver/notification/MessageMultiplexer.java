/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.notification.common.Notification;
import org.geoserver.notification.common.NotificationConfiguration;
import org.geoserver.notification.common.Notificator;
import org.geotools.util.logging.Logging;

public class MessageMultiplexer implements Runnable {

    protected static BlockingQueue<Notification> mainQueue;

    private static List<MessageProcessor> messageProcessors;

    protected static Logger LOGGER = Logging.getLogger(NotificationListener.class);

    public void addToMainQueue(Notification notification) {
        mainQueue.offer(notification);
    }

    public MessageMultiplexer(NotificationConfiguration notifierConfig) {
        mainQueue = new ArrayBlockingQueue<Notification>(notifierConfig.getQueueSize().intValue());
        messageProcessors =
                new ArrayList<MessageProcessor>(notifierConfig.getNotificators().size());
        // Create destination queue, and thread pools one for each processor
        for (Notificator notificator : notifierConfig.getNotificators()) {
            messageProcessors.add(
                    new MessageProcessor(
                            notificator.getQueueSize().intValue(),
                            notificator.getProcessorThreads().intValue(),
                            notificator.getMessageFilter(),
                            notificator.getGenericProcessor()));
        }
    }

    @Override
    public void run() {
        while (true) {
            try {
                consume(mainQueue.take());
            } catch (InterruptedException e) {
                LOGGER.log(Level.WARNING, e.getMessage(), e);
            }
        }
    }

    private void consume(Notification notification) {
        if (notification != null) {
            for (MessageProcessor messageProcessor : messageProcessors) {
                messageProcessor.process(notification);
            }
        }
    }
}
