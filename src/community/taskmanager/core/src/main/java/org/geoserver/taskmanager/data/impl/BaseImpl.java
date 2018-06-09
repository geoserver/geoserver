/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.data.impl;

import org.geoserver.taskmanager.data.Identifiable;

public abstract class BaseImpl implements Identifiable {

    @Override
    public boolean equals(Object o) {
        if (getId() == null) {
            return super.equals(o);
        } else {
            return o.getClass().equals(getClass()) && getId().equals(((Identifiable) o).getId());
        }
    }

    @Override
    public int hashCode() {
        if (getId() == null) {
            return super.hashCode();
        } else {
            return getId().hashCode();
        }
    }
}
