/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2013, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.wms.eo.web;

import java.io.Serializable;

import org.geoserver.catalog.LayerGroupInfo;


/**
 * Model for wicket page WmsEoAddLayerPage.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class WmsEoAddLayerModel implements Serializable {

    private LayerGroupInfo eoGroup; 
    private String parametersLayerName;
    private String parametersUrl;
    private String masksLayerName;
    private String masksUrl;


    public LayerGroupInfo getGroup() {
        return eoGroup;
    }
    
    public void setGroup(LayerGroupInfo eoGroup) {
        this.eoGroup = eoGroup;
    }
    
    public String getParametersLayerName() {
        return parametersLayerName;
    }

    public void setParametersLayerName(String parametersLayerName) {
        this.parametersLayerName = parametersLayerName;
    }

    public String getMasksLayerName() {
        return masksLayerName;
    }

    public void setMasksLayerName(String masksLayerName) {
        this.masksLayerName = masksLayerName;
    }
    
    public String getParametersUrl() {
        return parametersUrl;
    }

    public void setParametersUrl(String parametersUrl) {
        this.parametersUrl = parametersUrl;
    }

    public String getMasksUrl() {
        return masksUrl;
    }

    public void setMasksUrl(String masksUrl) {
        this.masksUrl = masksUrl;
    }    
}