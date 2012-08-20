package org.opengeo.gsr.core.geometry;

/**
 * 
 * @author Juan Marin - OpenGeo
 *
 */
public enum GeometryType {

    POINT("GeometryPoint"), MULTIPOINT("GeometryMultiPoint"), POLYLINE("GeometryPolyline"), POLYGON(
            "GeometryPolygon"), ENVELOPE("GeometryEnvelope");
    private String geometryType;

    public String getGeometryType() {
        return geometryType;
    }

    private GeometryType(String geomType) {
        this.geometryType = geomType;
    }

};