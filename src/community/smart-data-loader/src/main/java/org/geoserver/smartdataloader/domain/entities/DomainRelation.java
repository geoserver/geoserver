/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain.entities;

import org.geoserver.smartdataloader.domain.DomainModelVisitor;

/**
 * Represents a relation between two entities fo the domain model. Entities are rleated by a key
 * attribute and there is a containing and a destination entity.
 */
public final class DomainRelation {

    private DomainEntity containingEntity;
    private DomainEntity destinationEntity;
    private DomainEntitySimpleAttribute containingKeyAttribute;
    private DomainEntitySimpleAttribute destinationKeyAttribute;

    public DomainEntity getContainingEntity() {
        return containingEntity;
    }

    public void setContainingEntity(DomainEntity containingEntity) {
        this.containingEntity = containingEntity;
    }

    public DomainEntity getDestinationEntity() {
        return destinationEntity;
    }

    public void setDestinationEntity(DomainEntity destinationEntity) {
        this.destinationEntity = destinationEntity;
    }

    public DomainEntitySimpleAttribute getContainingKeyAttribute() {
        return containingKeyAttribute;
    }

    public void setContainingKeyAttribute(DomainEntitySimpleAttribute containingKeyAttribute) {
        this.containingKeyAttribute = containingKeyAttribute;
    }

    public DomainEntitySimpleAttribute getDestinationKeyAttribute() {
        return destinationKeyAttribute;
    }

    public void setDestinationKeyAttribute(DomainEntitySimpleAttribute destinationKeyAttribute) {
        this.destinationKeyAttribute = destinationKeyAttribute;
    }

    /** Will visit this domain relation with the provided visitor. */
    public void accept(DomainModelVisitor visitor) {
        visitor.visitDomainRelation(this);
        destinationEntity.accept(visitor, false);
    }
}
