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

import org.geoserver.catalog.WorkspaceInfo;


/**
 * Model for wicket page WmsEoCreateGroupPage.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class WmsEoCreateGroupModel implements Serializable {

    private WorkspaceInfo workspace;
    private String name;
    private String outlineLayerName;
    private String productLayerName;
    private String productUrl;
    private String bandLayerName;
    private String bandUrl;
    private String parameterLayerName;
    private String parameterUrl;
    private String bitmaskLayerName;
    private String bitmaskUrl;


    public String getOutlineLayerName() {
        return outlineLayerName;
    }

    public void setOutlineLayerName(String outlineLayerName) {
        this.outlineLayerName = outlineLayerName;
    }

    public String getProductLayerName() {
        return productLayerName;
    }

    public void setProductLayerName(String productLayerName) {
        this.productLayerName = productLayerName;
    }

    public String getBandLayerName() {
        return bandLayerName;
    }

    public void setBandLayerName(String bandLayerName) {
        this.bandLayerName = bandLayerName;
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

    public void setBitMaskLayerName(String bitmaskLayerName) {
        this.bitmaskLayerName = bitmaskLayerName;
    }

    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public WorkspaceInfo getWorkspace() {
        return workspace;
    }
    
    public void setWorkspace(WorkspaceInfo workspace) {
        this.workspace = workspace;
    }
    
    public String getProductUrl() {
        return productUrl;
    }
    
    public void setProductUrl(String productUrl) {
        this.productUrl = productUrl;
    }
    
    public String getBandUrl() {
        return bandUrl;
    }
    
    public void setBandUrl(String bandUrl) {
        this.bandUrl = bandUrl;
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