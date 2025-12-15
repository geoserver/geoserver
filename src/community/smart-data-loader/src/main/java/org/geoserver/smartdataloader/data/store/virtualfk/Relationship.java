/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

/**
 * Represents a virtual relationship between two entities and exposes the metadata necessary to produce the virtual
 * foreign key.
 */
public class Relationship {
    private String name;
    private String cardinality;
    private EntityRef source;
    private EntityRef target;

    /**
     * @param name logical identifier that will appear in the generated mappings (must be unique)
     * @param cardinality textual cardinality (1:1, 1:n, n:1)
     * @param source descriptor of the relationship source entity/column (required)
     * @param target descriptor of the relationship target entity/column (required)
     */
    public Relationship(String name, String cardinality, EntityRef source, EntityRef target) {
        this.name = name;
        this.cardinality = cardinality;
        this.source = source;
        this.target = target;
    }

    /** Returns the logical name of this virtual relationship. */
    public String getName() {
        return name;
    }

    /** Sets the logical name of this virtual relationship. */
    public void setName(String name) {
        this.name = name;
    }

    /** Returns the textual representation of the relationship cardinality. */
    public String getCardinality() {
        return cardinality;
    }

    /** Updates the textual representation of the relationship cardinality. */
    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }

    /** Returns the source entity definition. */
    public EntityRef getSource() {
        return source;
    }

    /** Updates the source entity definition. */
    public void setSource(EntityRef source) {
        this.source = source;
    }

    /** Returns the target entity definition. */
    public EntityRef getTarget() {
        return target;
    }

    /** Updates the target entity definition. */
    public void setTarget(EntityRef target) {
        this.target = target;
    }
}
