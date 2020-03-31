package com.boundlessgeo.gsr.model.relationship;

import com.boundlessgeo.gsr.model.GSRModel;

import java.util.List;

public class RelationshipClassArray implements GSRModel {
    public final List<RelationshipClass> relationshipClasses;

    public RelationshipClassArray(List<RelationshipClass>relationshipClasses){
        this.relationshipClasses = relationshipClasses;
    }
}
