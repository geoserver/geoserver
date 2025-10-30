/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata;

import java.util.Objects;
import org.geoserver.smartdataloader.domain.entities.DomainRelationType;

/**
 * Metadata representation for a virtual relation between entities.
 *
 * <p>A virtual relation extends {@link RelationMetadata} with a name and an optional expression that can be used to
 * describe or compute the virtual mapping.
 */
public class VirtualRelationMetadata extends RelationMetadata {

    private final String name;

    public VirtualRelationMetadata(
            DomainRelationType type, AttributeMetadata source, AttributeMetadata destination, String name) {
        super(type, source, destination);
        this.name = name;
    }

    public String getName() {
        return name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        VirtualRelationMetadata that = (VirtualRelationMetadata) o;
        return Objects.equals(getSourceAttribute(), that.getSourceAttribute())
                && Objects.equals(getDestinationAttribute(), that.getDestinationAttribute())
                && Objects.equals(getRelationType(), that.getRelationType())
                && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getSourceAttribute(), getDestinationAttribute(), getRelationType(), name);
    }

    @Override
    public String toString() {
        return "VirtualRelationMetadata{"
                + "source=" + getSourceAttribute()
                + ", destination=" + getDestinationAttribute()
                + ", type=" + getRelationType()
                + ", name='" + name + '\''
                + '}';
    }
}
