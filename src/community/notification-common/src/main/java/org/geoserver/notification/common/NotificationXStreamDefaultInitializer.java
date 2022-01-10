/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

import com.thoughtworks.xstream.XStream;
import org.geoserver.notification.common.sender.NotificationSender;

/**
 * Base class for notifier Xstream configuration mapper
 *
 * @author Xandros
 */
public class NotificationXStreamDefaultInitializer implements NotificationXStreamInitializer {

    @Override
    public void init(XStream xs) {
        xs.alias("notificationConfiguration", NotificationConfiguration.class);
        xs.alias("notificator", Notificator.class);
        xs.alias("genericProcessor", NotificationProcessor.class);
        xs.addDefaultImplementation(
                DefaultNotificationProcessor.class, NotificationProcessor.class);
        xs.addImplicitCollection(NotificationConfiguration.class, "notificators");
        xs.allowTypes(
                new Class[] {
                    NotificationConfiguration.class,
                    Notificator.class,
                    NotificationProcessor.class,
                    NotificationEncoder.class,
                    NotificationSender.class
                });
    }
}
