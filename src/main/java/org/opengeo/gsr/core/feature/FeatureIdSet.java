/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.core.feature;

/**
 * 
 * @author Juan Marin, OpenGeo
 *
 */
public class FeatureIdSet {

    private String objectIdFieldName;

    private int[] objectIds;

    public String getObjectIdFieldName() {
        return objectIdFieldName;
    }

    public void setObjectIdFieldName(String objectIdFieldName) {
        this.objectIdFieldName = objectIdFieldName;
    }

    public int[] getObjectIds() {
        return objectIds;
    }

    public void setObjectIds(int[] objectIds) {
        this.objectIds = objectIds;
    }

    public FeatureIdSet(String objectIdFieldName, int[] objectIds) {
        super();
        this.objectIdFieldName = objectIdFieldName;
        this.objectIds = objectIds;
    }

}
