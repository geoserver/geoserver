/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;

public class OwsMetadata implements Serializable {
    private static final long serialVersionUID = 1L;

    // <ows:AbstractMetaData>any content</ows:AbstractMetaData>
    private String abstractMetaData;

    public OwsMetadata() {}

    public String getAbstractMetaData() {
        return abstractMetaData;
    }

    public void setAbstractMetaData(String abstractMetaData) {
        this.abstractMetaData = abstractMetaData;
    }
}
