/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.service;

import com.thoughtworks.xstream.annotations.XStreamAlias;

/**
 * Simple model of a geometry service, for use in the list of services published by {@link
 * CatalogService}
 *
 * @author Juan Marin, OpenGeo
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
