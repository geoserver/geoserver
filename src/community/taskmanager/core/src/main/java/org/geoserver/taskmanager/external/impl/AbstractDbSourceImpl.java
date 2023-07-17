package org.geoserver.taskmanager.external.impl;

import org.geoserver.taskmanager.external.DbSource;
import org.geoserver.taskmanager.external.GeometryTable;
import org.geoserver.taskmanager.util.SecuredImpl;

public abstract class AbstractDbSourceImpl extends SecuredImpl implements DbSource {

    private GeometryTable rawGeometryTable;

    @Override
    public GeometryTable getRawGeometryTable() {
        return rawGeometryTable;
    }

    public void setRawGeometryTable(GeometryTable rawGeometryTable) {
        this.rawGeometryTable = rawGeometryTable;
    }
}
