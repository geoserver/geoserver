/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.taskmanager.util;

public class NamedImpl implements Named {

    private String name;

    @Override
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        return o.getClass().equals(getClass()) && ((NamedImpl) o).getName().equals(getName());
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }
}
