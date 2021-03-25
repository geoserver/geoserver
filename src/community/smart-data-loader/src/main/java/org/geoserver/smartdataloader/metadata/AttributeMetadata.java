/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata;

import com.google.common.collect.ComparisonChain;

/** Class that represents metadata for entities' attributes on the underlying DataStore model. */
public abstract class AttributeMetadata implements Comparable<AttributeMetadata> {

    protected String name;
    protected EntityMetadata entity;
    protected String type;
    protected boolean externalReference;
    protected boolean identifier = false;

    public AttributeMetadata(
            EntityMetadata entity,
            String name,
            String type,
            boolean externalReference,
            boolean identifier) {
        this.name = name;
        this.entity = entity;
        this.setType(type);
        this.externalReference = externalReference;
        this.identifier = identifier;
    }

    public AttributeMetadata(
            EntityMetadata entity, String name, String type, boolean externalReference) {
        this.name = name;
        this.entity = entity;
        this.setType(type);
        this.externalReference = externalReference;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public EntityMetadata getEntity() {
        return entity;
    }

    public String getName() {
        return name;
    }

    public boolean isExternalReference() {
        return externalReference;
    }

    public boolean isIdentifier() {
        return identifier;
    }

    @Override
    public int compareTo(AttributeMetadata attributeMetadata) {
        if (attributeMetadata != null) {
            return ComparisonChain.start()
                    .compare(this.getEntity(), attributeMetadata.getEntity())
                    .compare(this.name, attributeMetadata.getName())
                    .result();
        }
        return 1;
    }
}
