/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.gsr.model.relationship;

import java.util.List;
import org.geoserver.gsr.model.GSRModel;

public class RelationshipClassArray implements GSRModel {
    public final List<RelationshipClass> relationshipClasses;

    public RelationshipClassArray(List<RelationshipClass> relationshipClasses) {
        this.relationshipClasses = relationshipClasses;
    }
}
