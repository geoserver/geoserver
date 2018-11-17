/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3.response;

import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import java.util.List;

public class TileMatrixDocument {

    private Long matrixHeight;
    private Long matrixWidth;
    private Integer tileHeight;
    private Integer tileWidth;
    private String identifier;
    private Double scaleDenominator;
    private List<Double> topLeftCorner;
    private String type = "TileMatrix";

    public TileMatrixDocument() {}

    @JacksonXmlProperty(localName = "MatrixHeight")
    public Long getMatrixHeight() {
        return matrixHeight;
    }

    public void setMatrixHeight(Long matrixHeight) {
        this.matrixHeight = matrixHeight;
    }

    @JacksonXmlProperty(localName = "MatrixWidth")
    public Long getMatrixWidth() {
        return matrixWidth;
    }

    public void setMatrixWidth(Long matrixWidth) {
        this.matrixWidth = matrixWidth;
    }

    @JacksonXmlProperty(localName = "tileHeight")
    public Integer getTileHeight() {
        return tileHeight;
    }

    public void setTileHeight(Integer tileHeight) {
        this.tileHeight = tileHeight;
    }

    @JacksonXmlProperty(localName = "tileWidth")
    public Integer getTileWidth() {
        return tileWidth;
    }

    public void setTileWidth(Integer tileWidth) {
        this.tileWidth = tileWidth;
    }

    @JacksonXmlProperty(localName = "identifier")
    public String getIdentifier() {
        return identifier;
    }

    public void setIdentifier(String identifier) {
        this.identifier = identifier;
    }

    @JacksonXmlProperty(localName = "scaleDenominator")
    public Double getScaleDenominator() {
        return scaleDenominator;
    }

    public void setScaleDenominator(Double scaleDenominator) {
        this.scaleDenominator = scaleDenominator;
    }

    @JacksonXmlProperty(localName = "topLeftCorner")
    public List<Double> getTopLeftCorner() {
        return topLeftCorner;
    }

    public void setTopLeftCorner(List<Double> topLeftCorner) {
        this.topLeftCorner = topLeftCorner;
    }

    @JacksonXmlProperty(localName = "type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }
}
