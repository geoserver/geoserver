/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javax.jms.JMSException;
import javax.jms.Message;
import javax.jms.ObjectMessage;
import javax.jms.Session;
import org.geoserver.cluster.events.ToggleType;
import org.springframework.jms.listener.SessionAwareMessageListener;

/** A simple JMS events listener for our tests. */
public final class JmsEventsListener extends JMSApplicationListener
        implements SessionAwareMessageListener<Message> {

    public enum Status {
        NO_STATUS,
        SELECT_CONTINUE,
        REJECT_CONTINUE,
        SELECT_STOP,
        REJECT_STOP
    }

    private static final List<Message> messages = new ArrayList<>();

    public JmsEventsListener() {
        super(ToggleType.SLAVE);
    }

    @Override
    public void onMessage(Message message, Session session) throws JMSException {
        synchronized (messages) {
            // we just need to store the received message
            messages.add(message);
        }
    }

    public static void clear() {
        synchronized (messages) {
            // clear the processing pending messages
            messages.clear();
        }
    }

    /**
     * Blocking helper method that allows us to wait for certain messages in a certain time. The
     * stop method will be used to check if we have all the messages we need. Only messages that
     * match one of the provided handlers keys will be selected.
     */
    public static List<Message> getMessagesByHandlerKey(
            int timeoutMs, Function<List<Message>, Boolean> stop, String... keys) {
        List<String> keysList = Arrays.asList(keys);
        return JmsEventsListener.getMessages(
                timeoutMs,
                stop,
                (message) -> {
                    try {
                        String handlerKey =
                                message.getStringProperty(JMSEventHandlerSPI.getKeyName());
                        if (keysList.contains(handlerKey)) {
                            // we want this message
                            return Status.SELECT_CONTINUE;
                        }
                    } catch (Exception exception) {
                        // we got an exception let's just ignore this message
                    }
                    // not the message we want
                    return Status.REJECT_CONTINUE;
                });
    }

    /**
     * Blocking helper method that allows us to wait for certain messages in a certain time. The
     * stop method will be used to check if we have all the messages we need. The selector method is
     * used to select only certain messages.
     */
    public static List<Message> getMessages(
            int timeoutMs,
            Function<List<Message>, Boolean> stop,
            Function<Message, Status> selector) {
        List<Message> selected = new ArrayList<>();
        Status status = Status.NO_STATUS;
        int max = (int) Math.ceil(timeoutMs / 10.0);
        int i = 0;
        while (i < max
                && status != Status.SELECT_STOP
                && status != Status.REJECT_STOP
                && !stop.apply(selected)) {
            try {
                // let's wait ten milliseconds
                Thread.sleep(10);
            } catch (InterruptedException exception) {
                // restore the interrupted status and return the current messages we have
                Thread.currentThread().interrupt();
                return selected;
            }
            i++;
            synchronized (messages) {
                for (Message message : messages) {
                    status = selector.apply(message);
                    if (status == Status.SELECT_CONTINUE || status == Status.SELECT_STOP) {
                        // we want this message
                        selected.add(message);
                    }
                    if (status == Status.SELECT_STOP || status == Status.REJECT_STOP) {
                        // we are done
                        break;
                    }
                }
                // clear all processed messages
                messages.clear();
            }
        }
        return selected;
    }

    /** Searches the events that match a certain handler and apply the handler to those elements. */
    public static <T> List<T> getMessagesForHandler(
            List<Message> messages, String handlerName, JMSEventHandler<String, T> handler) {
        List<T> found = new ArrayList<>();
        for (Message message : messages) {
            try {
                String handlerKey = message.getStringProperty(JMSEventHandlerSPI.getKeyName());
                if (handlerKey.equals(handlerName) && message instanceof ObjectMessage) {
                    // we found a message that match's the desired handler
                    String object = ((ObjectMessage) message).getObject().toString();
                    found.add(handler.deserialize(object));
                }
            } catch (Exception exception) {
                // we got an exception let's just ignore this message
            }
        }
        return found;
    }
}
