/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;

public class OwsDCP implements Serializable {
    private static final long serialVersionUID = 1L;

    private OwsHttp http;

    public OwsDCP() {}

    public OwsHttp getHttp() {
        return http;
    }

    public void setHttp(OwsHttp http) {
        this.http = http;
    }
}
