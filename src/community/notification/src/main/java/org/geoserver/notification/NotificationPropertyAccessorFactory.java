/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import org.geoserver.notification.common.Notification;
import org.geotools.filter.expression.PropertyAccessor;
import org.geotools.filter.expression.PropertyAccessorFactory;
import org.geotools.util.factory.Hints;

public class NotificationPropertyAccessorFactory implements PropertyAccessorFactory {

    private static final NotificationPropertyAccessor INSTANCE = new NotificationPropertyAccessor();

    @Override
    public PropertyAccessor createPropertyAccessor(
            Class<?> type, String xpath, Class<?> target, Hints hints) {
        if (Notification.class.isAssignableFrom(type)) {
            return INSTANCE;
        }
        return null;
    }
}
