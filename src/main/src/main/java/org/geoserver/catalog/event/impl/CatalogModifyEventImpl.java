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

    List propertyNames = new ArrayList();
    List oldValues = new ArrayList();
    List newValues = new ArrayList();

    public List getPropertyNames() {
        return propertyNames;
    }

    public void setPropertyNames(List propertyNames) {
        this.propertyNames = propertyNames;
    }

    public List getNewValues() {
        return newValues;
    }

    public void setNewValues(List newValues) {
        this.newValues = newValues;
    }

    public List getOldValues() {
        return oldValues;
    }

    public void setOldValues(List oldValues) {
        this.oldValues = oldValues;
    }
}
