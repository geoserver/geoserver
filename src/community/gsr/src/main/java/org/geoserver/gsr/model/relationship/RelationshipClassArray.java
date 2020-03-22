package org.geoserver.gsr.model.relationship;

import java.util.List;
import org.geoserver.gsr.model.GSRModel;

public class RelationshipClassArray implements GSRModel {
    public final List<RelationshipClass> relationshipClasses;

    public RelationshipClassArray(List<RelationshipClass> relationshipClasses) {
        this.relationshipClasses = relationshipClasses;
    }
}
