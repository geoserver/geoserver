/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.remote;

/**
 * Pojo to pass the attributes of a Web Mapping Context Output format.
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class WmcFeature {

    public WmcFeature() {}

    /** @return the type */
    public String getType() {
        return type;
    }

    /** @param type the type to set */
    public void setType(String type) {
        this.type = type;
    }

    String type;

    String name;

    String title;

    String description;

    String getMapBaseUrl;

    String lastUpdated;

    String workspace;

    String srs;

    String bbox;

    String latLonBbox;

    String layers;

    String styles;

    String geometryCoords;

    private String owcProperties;

    /** @return the srs */
    public String getSrs() {
        return srs;
    }

    /** @param srs the srs to set */
    public void setSrs(String srs) {
        this.srs = srs;
    }

    /** @return the bbox */
    public String getBbox() {
        return bbox;
    }

    /** @param bbox the bbox to set */
    public void setBbox(String bbox) {
        this.bbox = bbox;
    }

    /** @return the layers */
    public String getLayers() {
        return layers;
    }

    /** @param layers the layers to set */
    public void setLayers(String layers) {
        this.layers = layers;
    }

    /** @return the styles */
    public String getStyles() {
        return styles;
    }

    /** @param styles the styles to set */
    public void setStyles(String styles) {
        this.styles = styles;
    }

    /** @return the geometryCoords */
    public String getGeometryCoords() {
        return geometryCoords;
    }

    /** @param geometryCoords the geometryCoords to set */
    public void setGeometryCoords(String geometryCoords) {
        this.geometryCoords = geometryCoords;
    }

    /** @return the name */
    public String getName() {
        return name;
    }

    /** @param name the name to set */
    public void setName(String name) {
        this.name = name;
    }

    /** @return the title */
    public String getTitle() {
        return title;
    }

    /** @param title the title to set */
    public void setTitle(String title) {
        this.title = title;
    }

    /** @return the description */
    public String getDescription() {
        return description;
    }

    /** @param description the description to set */
    public void setDescription(String description) {
        this.description = description;
    }

    /** @return the getMapBaseUrl */
    public String getGetMapBaseUrl() {
        return getMapBaseUrl;
    }

    /** @param getMapBaseUrl the getMapBaseUrl to set */
    public void setGetMapBaseUrl(String getMapBaseUrl) {
        this.getMapBaseUrl = getMapBaseUrl;
    }

    /** @return the lastUpdated */
    public String getLastUpdated() {
        return lastUpdated;
    }

    /** @param lastUpdated the lastUpdated to set */
    public void setLastUpdated(String lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    /** @return the workspace */
    public String getWorkspace() {
        return workspace;
    }

    /** @param workspace the workspace to set */
    public void setWorkspace(String workspace) {
        this.workspace = workspace;
    }

    /** @return the latLonBbox */
    public String getLatLonBbox() {
        return latLonBbox;
    }

    /** @param latLonBbox the latLonBbox to set */
    public void setLatLonBbox(String latLonBbox) {
        this.latLonBbox = latLonBbox;
    }

    /** @return the owcProperties */
    public String getOwcProperties() {
        return owcProperties;
    }

    /** @param owcProperties the owcProperties to set */
    public void setOwcProperties(String owcProperties) {
        this.owcProperties = owcProperties;
    }
}
