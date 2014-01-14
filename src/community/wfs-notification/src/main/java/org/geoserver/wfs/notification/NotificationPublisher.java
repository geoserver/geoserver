/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.notification;

public interface NotificationPublisher {
    /**
     * Fires the given notification.
     * @param byteString A serialized notification.
     */
    void publish(String byteString);
    
    /**
     * Whether or not this publisher can accept notifications. If not, the whole notification process is short-
     * circuited.
     * @return {@code true} is this publisher is ready.
     */
    boolean isReady();
}