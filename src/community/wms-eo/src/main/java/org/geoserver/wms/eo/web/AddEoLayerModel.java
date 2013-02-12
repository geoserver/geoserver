/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.eo.web;

import java.io.Serializable;

import org.geoserver.catalog.LayerGroupInfo;


/**
 * Model for wicket page WmsEoAddLayerPage.
 * 
 * @author Davide Savazzi - geo-solutions.it
 */
public class AddEoLayerModel implements Serializable {

    private LayerGroupInfo eoGroup; 
    private String parameterName;
    private String parameterUrl;
    private String bitmaskName;
    private String bitmaskUrl;


    public AddEoLayerModel(LayerGroupInfo eoGroup) {
        this.eoGroup = eoGroup;
    }

    
    public LayerGroupInfo getGroup() {
        return eoGroup;
    }
    
    public void setGroup(LayerGroupInfo eoGroup) {
        this.eoGroup = eoGroup;
    }
    
    public String getParameterName() {
        return parameterName;
    }

    public void setParameterLayerName(String parameterName) {
        this.parameterName = parameterName;
    }

    public String getBitmaskName() {
        return bitmaskName;
    }

    public void setBitmaskName(String bitmaskName) {
        this.bitmaskName = bitmaskName;
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