/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

public class Relationship {
    private String name;
    private String cardinality;
    private EntityRef source;
    private EntityRef target;

    public Relationship(String name, String cardinality, EntityRef source, EntityRef target) {
        this.name = name;
        this.cardinality = cardinality;
        this.source = source;
        this.target = target;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCardinality() {
        return cardinality;
    }

    public void setCardinality(String cardinality) {
        this.cardinality = cardinality;
    }

    public EntityRef getSource() {
        return source;
    }

    public void setSource(EntityRef source) {
        this.source = source;
    }

    public EntityRef getTarget() {
        return target;
    }

    public void setTarget(EntityRef target) {
        this.target = target;
    }
}
