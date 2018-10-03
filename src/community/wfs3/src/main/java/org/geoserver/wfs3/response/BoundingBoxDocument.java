/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;

public class BoundingBoxDocument {

    private String crs;
    private String lowerCorner;
    private String upperCorner;
    private String type = "BoundingBox";

    public BoundingBoxDocument() {}

    public BoundingBoxDocument(String crs, String lowerCorner, String upperCorner) {
        super();
        this.crs = crs;
        this.lowerCorner = lowerCorner;
        this.upperCorner = upperCorner;
    }

    @JacksonXmlProperty(localName = "crs")
    public String getCrs() {
        return crs;
    }

    public void setCrs(String crs) {
        this.crs = crs;
    }

    @JacksonXmlProperty(localName = "lowerCorner")
    public String getLowerCorner() {
        return lowerCorner;
    }

    public void setLowerCorner(String lowerCorner) {
        this.lowerCorner = lowerCorner;
    }

    @JacksonXmlProperty(localName = "upperCorner")
    public String getUpperCorner() {
        return upperCorner;
    }

    public void setUpperCorner(String upperCorner) {
        this.upperCorner = upperCorner;
    }

    @JacksonXmlProperty(localName = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
