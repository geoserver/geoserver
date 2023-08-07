package org.geoserver.taskmanager.external;

public interface GeometryTable {

    public enum Type {
        WKT,
        WKB,
        WKB_HEX
    };

    String getNameTable();

    String getAttributeNameTable();

    String getAttributeNameGeometry();

    String getAttributeNameType();

    String getAttributeNameSrid();

    Type getType();
}
