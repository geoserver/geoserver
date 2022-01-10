/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification.common;

import com.thoughtworks.xstream.XStream;

/**
 * Allows Xstream to map notification configuration objects
 *
 * @author Xandros
 */
public interface NotificationXStreamInitializer {

    /** Initialize notifier Xstream configuration mapper */
    public void init(XStream xs);
}
