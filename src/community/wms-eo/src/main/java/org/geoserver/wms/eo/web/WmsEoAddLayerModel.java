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
    private String parameterLayerName;
    private String parameterUrl;
    private String bitmaskLayerName;
    private String bitmaskUrl;


    public LayerGroupInfo getGroup() {
        return eoGroup;
    }
    
    public void setGroup(LayerGroupInfo eoGroup) {
        this.eoGroup = eoGroup;
    }
    
    public String getParameterLayerName() {
        return parameterLayerName;
    }

    public void setParameterLayerName(String parameterLayerName) {
        this.parameterLayerName = parameterLayerName;
    }

    public String getBitmaskLayerName() {
        return bitmaskLayerName;
    }

    public void setBitmaskLayerName(String bitmaskLayerName) {
        this.bitmaskLayerName = bitmaskLayerName;
    }
    
    public String getParameterUrl() {
        return parameterUrl;
    }

    public void setParameterUrl(String parameterUrl) {
        this.parameterUrl = parameterUrl;
    }

    public String getBitmaskUrl() {
        return bitmaskUrl;
    }

    public void setBitmaskUrl(String bitmaskUrl) {
        this.bitmaskUrl = bitmaskUrl;
    }    
}