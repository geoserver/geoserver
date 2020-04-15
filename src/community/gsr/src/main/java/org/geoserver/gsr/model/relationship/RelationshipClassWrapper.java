/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.relationship;

public class RelationshipClassWrapper implements RelationshipModel {
    public final RelationshipClass relationshipClass;

    public RelationshipClassWrapper(RelationshipClass relationshipClass) {
        this.relationshipClass = relationshipClass;
    }
}
