/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import java.util.logging.Logger;
import org.geoserver.notification.common.Notification;
import org.geotools.util.logging.Logging;

public class NotificationListener {

    protected static Logger LOGGER = Logging.getLogger(NotificationListener.class);

    protected MessageMultiplexer messageMultiplexer;

    protected void notify(Notification notification) {
        this.messageMultiplexer.addToMainQueue(notification);
    }
}
