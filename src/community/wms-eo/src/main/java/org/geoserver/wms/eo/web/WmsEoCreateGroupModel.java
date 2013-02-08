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
 * Model for wicket page WmsEoCreatePage.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class WmsEoCreateGroupModel implements Serializable {

    private String name;
    private WorkspaceInfo workspace;
    private String browseImageUrl;
    private String bandsUrl;
    private String parametersUrl;
    private String masksUrl;


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
    
    public String getBrowseImageUrl() {
        return browseImageUrl;
    }
    
    public void setBrowseImageUrl(String browseImageUrl) {
        this.browseImageUrl = browseImageUrl;
    }
    
    public String getBandsUrl() {
        return bandsUrl;
    }
    
    public void setBandsUrl(String bandsUrl) {
        this.bandsUrl = bandsUrl;
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