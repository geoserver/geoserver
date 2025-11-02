/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data;

import org.springframework.context.ApplicationEvent;

public class DataModifiedEvent extends ApplicationEvent {

    public DataModifiedEvent(Object source) {
        super(source);
    }
}
