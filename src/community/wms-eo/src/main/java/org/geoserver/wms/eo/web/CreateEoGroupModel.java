/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.io.Serializable;

import org.geoserver.catalog.WorkspaceInfo;


/**
 * Model for wicket page WmsEoCreateGroupPage.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class CreateEoGroupModel implements Serializable {

    private WorkspaceInfo workspace;
    private String name;
    private String title;
    private String browseImageUrl;
    private String bandUrl;
    private String parameterName;
    private String parameterUrl;
    private String bitmaskName;
    private String bitmaskUrl;


    public String getParameterName() {
        return parameterName;
    }

    public void setParameterName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getBitmaskName() {
        return bitmaskName;
    }

    public void setBitMaskName(String bitmaskName) {
        this.bitmaskName = bitmaskName;
    }

    public String getGroupName() {
        return name;
    }
    
    public void setGroupName(String name) {
        this.name = name;
    }

    public String getGroupTitle() {
        return title;
    }
    
    public void setGroupTitle(String title) {
        this.title = title;
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