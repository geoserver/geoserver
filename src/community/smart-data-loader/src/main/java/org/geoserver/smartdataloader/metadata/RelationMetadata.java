/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata;

import com.google.common.collect.ComparisonChain;
import org.geoserver.smartdataloader.domain.entities.DomainRelationType;

/**
 * Class that represents metadata for relations between entities on the underlying DataStore model.
 */
public abstract class RelationMetadata implements Comparable<RelationMetadata> {

    protected AttributeMetadata sourceAttribute;
    protected AttributeMetadata destinationAttribute;
    protected DomainRelationType type;

    public RelationMetadata(
            DomainRelationType type, AttributeMetadata source, AttributeMetadata destination) {
        this.type = type;
        this.sourceAttribute = source;
        this.destinationAttribute = destination;
    }

    public boolean participatesInRelation(String entityMetadataName) {
        if (sourceAttribute.getEntity().getName().equals(entityMetadataName)
                || destinationAttribute.getEntity().getName().equals(entityMetadataName))
            return true;
        return false;
    }

    public AttributeMetadata getSourceAttribute() {
        return this.sourceAttribute;
    }

    public AttributeMetadata getDestinationAttribute() {
        return this.destinationAttribute;
    }

    public DomainRelationType getRelationType() {
        return this.type;
    }

    @Override
    public int compareTo(RelationMetadata relation) {
        if (relation != null) {
            return ComparisonChain.start()
                    .compare(this.getSourceAttribute(), relation.getSourceAttribute())
                    .compare(this.getDestinationAttribute(), relation.getDestinationAttribute())
                    .compare(this.getRelationType(), relation.getRelationType())
                    .result();
        }
        return 1;
    }
}
