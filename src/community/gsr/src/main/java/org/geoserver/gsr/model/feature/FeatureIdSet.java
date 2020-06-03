/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.model.feature;

import org.geoserver.gsr.model.GSRModel;

/** @author Juan Marin, OpenGeo */
public class FeatureIdSet implements GSRModel {

    private String objectIdFieldName;

    private long[] objectIds;

    public String getObjectIdFieldName() {
        return objectIdFieldName;
    }

    public void setObjectIdFieldName(String objectIdFieldName) {
        this.objectIdFieldName = objectIdFieldName;
    }

    public long[] getObjectIds() {
        return objectIds;
    }

    public void setObjectIds(long[] objectIds) {
        this.objectIds = objectIds;
    }

    public FeatureIdSet(String objectIdFieldName, long[] objectIds) {
        super();
        this.objectIdFieldName = objectIdFieldName;
        this.objectIds = objectIds;
    }
}
