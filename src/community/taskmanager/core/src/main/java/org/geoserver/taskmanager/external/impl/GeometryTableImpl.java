package org.geoserver.taskmanager.external.impl;

import org.geoserver.taskmanager.external.GeometryTable;

public class GeometryTableImpl implements GeometryTable {

    private String nameTable;

    private String attributeNameTable;

    private String attributeNameGeometry;

    private String attributeNameType;

    private String attributeNameSrid;

    private Type type;

    private GeometryTableImpl() {}

    @Override
    public String getNameTable() {
        return nameTable;
    }

    public void setNameTable(String nameTable) {
        this.nameTable = nameTable;
    }

    @Override
    public String getAttributeNameTable() {
        return attributeNameTable;
    }

    public void setAttributeNameTable(String attributeNameTable) {
        this.attributeNameTable = attributeNameTable;
    }

    @Override
    public String getAttributeNameGeometry() {
        return attributeNameGeometry;
    }

    public void setAttributeNameGeometry(String attributeNameGeometry) {
        this.attributeNameGeometry = attributeNameGeometry;
    }

    @Override
    public String getAttributeNameType() {
        return attributeNameType;
    }

    public void setAttributeNameType(String attributeNameType) {
        this.attributeNameType = attributeNameType;
    }

    @Override
    public String getAttributeNameSrid() {
        return attributeNameSrid;
    }

    public void setAttributeNameSrid(String attributeNameSrid) {
        this.attributeNameSrid = attributeNameSrid;
    }

    @Override
    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }
}
