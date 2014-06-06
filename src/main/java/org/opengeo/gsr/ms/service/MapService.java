/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.ms.service;

import org.opengeo.gsr.core.geometry.Envelope;
import org.opengeo.gsr.service.AbstractService;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * 
 * @author Juan Marin, OpenGeo
 * @author Brett Antonides, LMN Solutions
 * 
 */

@XStreamAlias("")
public class MapService implements AbstractService {

    private String mapName;
    
    private String serviceDescription;
    
    private String description;
    
    private String copyright;

    private double currentVersion;
    
    private Envelope fullExtent;
    
    private Envelope initialExtent;
    
    public Envelope getInitialExtent() {
		return initialExtent;
	}

	public void setInitialExtent(Envelope intialExtent) {
		this.initialExtent = intialExtent;
	}

	public double getCurrentVersion() {
		return currentVersion;
	}

	public void setCurrentVersion(double currentVersion) {
		this.currentVersion = currentVersion;
	}

	public String getMapName() {
        return mapName;
    }

	public void setMapName(String mapName) {
		this.mapName = mapName;
	}

	public String getServiceDescription() {
		return serviceDescription;
	}

	public void setServiceDescription(String serviceDescription) {
		this.serviceDescription = serviceDescription;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public String getCopyright() {
		return copyright;
	}

	public void setCopyright(String copyright) {
		this.copyright = copyright;
	}

	public Envelope getFullExtent() {
		return fullExtent;
	}

	public void setFullExtent(Envelope fullExtent) {
		this.fullExtent = fullExtent;
	}

    public MapService(String name, double currentVersion, Envelope extent) {
        mapName = name;
        this.currentVersion = currentVersion;
        this.fullExtent = extent;
        this.initialExtent = extent;
        serviceDescription = "N/A";
        description = "N/A";
        copyright = "N/A";
    }
    
    public MapService(String name, double currentVersion, Envelope fullExtent, Envelope initialExtent) {
        mapName = name;
        this.currentVersion = currentVersion;
        this.fullExtent = fullExtent;
        this.initialExtent = initialExtent;
        serviceDescription = "N/A";
        description = "N/A";
        copyright = "N/A";
    }
}
