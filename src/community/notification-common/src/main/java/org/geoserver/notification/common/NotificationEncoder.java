/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

/** Encodes a notification into some payload format */
public interface NotificationEncoder {

    /** Transforms notification into byte stream payload */
    public byte[] encode(Notification notification) throws Exception;
}
