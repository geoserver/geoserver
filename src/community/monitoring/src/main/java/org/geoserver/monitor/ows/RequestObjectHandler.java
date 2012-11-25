/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.monitor.ows;

import java.util.List;

import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.opengis.geometry.BoundingBox;

/**
 * Class that extracts information from an ows request.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public abstract class RequestObjectHandler {

    String reqObjClassName;
    protected MonitorConfig monitorConfig;
    
    protected RequestObjectHandler(String reqObjClassName, MonitorConfig config) {
        this.reqObjClassName = reqObjClassName;
        this.monitorConfig = config;
    }
    
    public boolean canHandle(Object request) {
        Class<?> clazz;
        try {
            clazz = Class.forName(reqObjClassName);
        } 
        catch (ClassNotFoundException e) {
            return false;
        }
        
        return clazz.isInstance(request);
    }
    
    public void handle(Object request, RequestData data) {
        data.setResources(getLayers(request));
        if(monitorConfig.getBboxLogLevel()!=MonitorConfig.BBoxLogLevel.NONE){
            data.setBbox(getBBox(request));
        }
    }
    
    protected abstract List<String> getLayers(Object request);
    
    /**
     * Find a bounding box for the area covered by the request.
     */
    protected BoundingBox getBBox(Object request) {
        return null;
    }
}
