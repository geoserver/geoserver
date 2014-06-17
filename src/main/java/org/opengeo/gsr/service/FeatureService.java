/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.service;

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
