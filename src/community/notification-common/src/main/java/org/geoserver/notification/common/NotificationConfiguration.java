/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

import com.thoughtworks.xstream.XStream;
import java.io.Serializable;
import java.util.List;

/**
 * The root of notification configuration, populated by {@link XStream}
 *
 * @author Xandros
 */
public class NotificationConfiguration implements Serializable {

    private static final long serialVersionUID = 2029473095919663064L;

    /**
     * The size of main queue used to store {@link Notification} after events generate it, but
     * before the {@link MessageMultiplexer} elaborates it
     */
    private Long queueSize;

    /**
     * The configurations used by {@link MessageProcessor} used to store, filter and dispatch the
     * {@link Notification} using the right {@link NotificationProcessor}
     */
    private List<Notificator> notificators;

    public Long getQueueSize() {
        return queueSize;
    }

    public void setQueueSize(Long queueSize) {
        this.queueSize = queueSize;
    }

    public List<Notificator> getNotificators() {
        return notificators;
    }

    public void setNotificators(List<Notificator> notificators) {
        this.notificators = notificators;
    }
}
