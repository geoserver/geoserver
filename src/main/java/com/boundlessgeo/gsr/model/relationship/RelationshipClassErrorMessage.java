package com.boundlessgeo.gsr.model.relationship;

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
