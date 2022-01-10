/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

import java.io.Serializable;
import org.geoserver.notification.common.sender.NotificationSender;

public class DefaultNotificationProcessor implements NotificationProcessor, Serializable {

    private static final long serialVersionUID = -981618390262055505L;

    private NotificationEncoder encoder;

    private NotificationSender sender;

    public DefaultNotificationProcessor() {
        super();
    }

    /**
     * Process {@link Notification} using an encoder to generate the payload and a sender to
     * delivery it to destination
     *
     * @param the encoder to transform {@link Notification} to payload
     * @param the sender to deliver the payload
     */
    public DefaultNotificationProcessor(NotificationEncoder encoder, NotificationSender sender) {
        super();
        this.encoder = encoder;
        this.sender = sender;
    }

    @Override
    public void process(Notification notification) throws Exception {
        byte[] payload = encoder.encode(notification);
        sender.send(payload);
    }

    public NotificationEncoder getEncoder() {
        return encoder;
    }

    public void setEncoder(NotificationEncoder encoder) {
        this.encoder = encoder;
    }

    public NotificationSender getSender() {
        return sender;
    }

    public void setSender(NotificationSender sender) {
        this.sender = sender;
    }
}
