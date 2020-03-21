package com.boundlessgeo.gsr.model.relationship;

import com.boundlessgeo.gsr.model.GSRModel;

import java.io.Serializable;
import java.util.Comparator;
import java.util.Objects;

public class RelationshipClass implements Comparable<RelationshipClass>, Serializable, RelationshipModel {
    private Long relationshipId;
    private String workspaceName;
    private String originTable;
    private String destinationTable;
    private RelationshipType relationshipType;
    private String forwardLabel;
    private String backwardLabel;
    private MessageDirection messageDirection;
    private Cardinality cardinality;
    private Boolean attributed;
    private String originPrimaryKey;
    private String originForeignKey;
    private String destinationPrimaryKey;
    private String destinationForeignKey;

    public Long getRelationshipId() {
        return relationshipId;
    }

    public void setRelationshipId(Long relationshipId) {
        this.relationshipId = relationshipId;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    public void setWorkspaceName(String workspaceName) {
        this.workspaceName = workspaceName;
    }

    public String getOriginTable() {
        return originTable;
    }

    public void setOriginTable(String originTable) {
        this.originTable = originTable;
    }

    public String getDestinationTable() {
        return destinationTable;
    }

    public void setDestinationTable(String destinationTable) {
        this.destinationTable = destinationTable;
    }

    public RelationshipType getRelationshipType() {
        return relationshipType;
    }

    public void setRelationshipType(RelationshipType relationshipType) {
        this.relationshipType = relationshipType;
    }

    public String getForwardLabel() {
        return forwardLabel;
    }

    public void setForwardLabel(String forwardLabel) {
        this.forwardLabel = forwardLabel;
    }

    public String getBackwardLabel() {
        return backwardLabel;
    }

    public void setBackwardLabel(String backwardLabel) {
        this.backwardLabel = backwardLabel;
    }

    public MessageDirection getMessageDirection() {
        return messageDirection;
    }

    public void setMessageDirection(MessageDirection messageDirection) {
        this.messageDirection = messageDirection;
    }

    public Cardinality getCardinality() {
        return cardinality;
    }

    public void setCardinality(Cardinality cardinality) {
        this.cardinality = cardinality;
    }

    public Boolean getAttributed() {
        return attributed;
    }

    public void setAttributed(Boolean attributed) {
        this.attributed = attributed;
    }

    public String getOriginPrimaryKey() {
        return originPrimaryKey;
    }

    public void setOriginPrimaryKey(String originPrimaryKey) {
        this.originPrimaryKey = originPrimaryKey;
    }

    public String getOriginForeignKey() {
        return originForeignKey;
    }

    public void setOriginForeignKey(String originForeignKey) {
        this.originForeignKey = originForeignKey;
    }

    public String getDestinationPrimaryKey() {
        return destinationPrimaryKey;
    }

    public void setDestinationPrimaryKey(String destinationPrimaryKey) {
        this.destinationPrimaryKey = destinationPrimaryKey;
    }

    public String getDestinationForeignKey() {
        return destinationForeignKey;
    }

    public void setDestinationForeignKey(String destinationForeignKey) {
        this.destinationForeignKey = destinationForeignKey;
    }

    /**
     * Returns a positive number if the RelationshipClass is less than this one, 0 if they are equal, and a negative number if it is less than.  In order to prevent more than one RelationshipClass from being defined for each set of source and destination fields this is defined using
     * (workspaceName, originTable, destinationTable,originPrimaryKey,originForeignKey) instead of the relationshipId
     * @param o
     * @return
     */
    @Override
    public int compareTo(RelationshipClass o) {
        return Comparator.comparing(RelationshipClass::getWorkspaceName)
                .thenComparing(RelationshipClass::getOriginTable)
                .thenComparing(RelationshipClass::getDestinationTable)
                .thenComparing(RelationshipClass::getOriginPrimaryKey)
                .thenComparing(RelationshipClass::getOriginForeignKey)
                .compare(this, o);
    }

    /**
     * Returns true if the RelationshipClass equals this one.  In order to prevent more than one RelationshipClass from being defined for each set of source and destination fields this is defined using
     * (workspaceName, originTable, destinationTable,originPrimaryKey,originForeignKey) instead of the relationshipId
     * @param obj
     * @return
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (!(obj instanceof RelationshipClass)) {
            return false;
        }
        RelationshipClass relationshipClass = (RelationshipClass) obj;
        return
                Objects.equals(workspaceName, relationshipClass.getWorkspaceName()) &&
                        Objects.equals(originTable, relationshipClass.getOriginTable()) &&
                        Objects.equals(destinationTable, relationshipClass.getDestinationTable()) &&
                        Objects.equals(originPrimaryKey,relationshipClass.getOriginPrimaryKey()) &&
                        Objects.equals(originForeignKey,relationshipClass.getOriginForeignKey());
    }

    /**
     * Returns a hashcode that allows for a simple way to determine if a RelationshipClass is unique.  In order to prevent more than one RelationshipClass from being defined for each set of source and destination fields this is defined using
     * (workspaceName, originTable, destinationTable,originPrimaryKey,originForeignKey) instead of the relationshipId
     * @return
     */
    @Override
    public int hashCode() {
        return Objects.hash(workspaceName, originTable, destinationTable,originPrimaryKey,originForeignKey);
    }

    @Override
    public String toString() {
        return "RelationshipClass{" +
                "workspaceName='" + workspaceName + '\'' +
                ", relationshipId='" + relationshipId + '\'' +
                ", originTable='" + originTable + '\'' +
                ", destinationTable='" + destinationTable + '\'' +
                ", relationshipType=" + relationshipType +
                ", forwardLabel='" + forwardLabel + '\'' +
                ", backwardLabel='" + backwardLabel + '\'' +
                ", messageDirection=" + messageDirection +
                ", cardinality=" + cardinality +
                ", attributed=" + attributed +
                ", originPrimaryKey='" + originPrimaryKey + '\'' +
                ", originForeignKey='" + originForeignKey + '\'' +
                ", destinationPrimaryKey='" + destinationPrimaryKey + '\'' +
                ", destinationForeignKey='" + destinationForeignKey + '\'' +
                '}';
    }
}
