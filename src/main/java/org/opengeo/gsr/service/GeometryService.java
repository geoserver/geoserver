/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.service;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * 
 * @author Juan Marin, OpenGeo
 * 
 */
@XStreamAlias("GeometryService")
public class GeometryService implements AbstractService {

    private String name;

    private ServiceType type;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ServiceType getType() {
        return type;
    }

    public void setType(ServiceType serviceType) {
        this.type = serviceType;
    }

    public GeometryService(String name) {
        this.name = name;
        this.type = ServiceType.GeometryServer;
    }
}
