/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.event.impl;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.catalog.event.CatalogModifyEvent;

public class CatalogModifyEventImpl extends CatalogEventImpl implements CatalogModifyEvent {

    List<String> propertyNames = new ArrayList<>();
    List<Object> oldValues = new ArrayList<>();
    List<Object> newValues = new ArrayList<>();

    @Override
    public List<String> getPropertyNames() {
        return propertyNames;
    }

    public void setPropertyNames(List<String> propertyNames) {
        this.propertyNames = propertyNames;
    }

    @Override
    public List<Object> getNewValues() {
        return newValues;
    }

    public void setNewValues(List<Object> newValues) {
        this.newValues = newValues;
    }

    @Override
    public List<Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(List<Object> oldValues) {
        this.oldValues = oldValues;
    }
}
