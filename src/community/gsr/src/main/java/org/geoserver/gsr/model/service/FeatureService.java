/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.service;

/**
 * Simple model of a feature service, for use in the list of services published by {@link
 * CatalogService}
 */
public class FeatureService implements AbstractService {
    private String name;
    private ServiceType type;

    public FeatureService(String name) {
        this.name = name;
        this.type = ServiceType.FeatureServer;
    }

    public String getName() {
        return this.name;
    }

    public ServiceType getType() {
        return this.type;
    }
}
