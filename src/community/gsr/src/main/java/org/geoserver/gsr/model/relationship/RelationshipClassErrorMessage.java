/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.relationship;

public class RelationshipClassErrorMessage {
    private String relationshipClassString;
    private String errorMessage;

    public String getRelationshipClass() {
        return relationshipClassString;
    }

    public void setRelationshipClass(String relationshipClass) {
        this.relationshipClassString = relationshipClass;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
