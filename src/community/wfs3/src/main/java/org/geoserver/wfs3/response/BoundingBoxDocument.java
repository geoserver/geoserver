/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;

public class BoundingBoxDocument {

    private String crs;
    private List<Double> lowerCorner;
    private List<Double> upperCorner;
    private String type = "BoundingBox";

    public BoundingBoxDocument() {}

    public BoundingBoxDocument(String crs, List<Double> lowerCorner, List<Double> upperCorner) {
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
    public List<Double> getLowerCorner() {
        return lowerCorner;
    }

    public void setLowerCorner(List<Double> lowerCorner) {
        this.lowerCorner = lowerCorner;
    }

    @JacksonXmlProperty(localName = "upperCorner")
    public List<Double> getUpperCorner() {
        return upperCorner;
    }

    public void setUpperCorner(List<Double> upperCorner) {
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
