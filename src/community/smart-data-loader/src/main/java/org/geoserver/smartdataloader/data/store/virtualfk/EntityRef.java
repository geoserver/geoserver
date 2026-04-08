/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.data.store.virtualfk;

/**
 * Identifies an entity participating in a virtual relationship, including its schema, type (table/view) and the key
 * column involved in the join.
 */
public class EntityRef {
    private String schema;
    private String entity;
    private String kind;
    private Key key;

    /**
     * @param schema optional schema/catalog that qualifies the entity
     * @param entity logical name of the table or view
     * @param kind textual description of the entity type (e.g. table, view)
     * @param key key metadata describing the column used in the relationship (required)
     */
    public EntityRef(String schema, String entity, String kind, Key key) {
        this.schema = schema;
        this.entity = entity;
        this.kind = kind;
        this.key = key;
    }

    /** Schema that contains the referenced entity (may be {@code null}). */
    public String getSchema() {
        return schema;
    }

    /** Updates the schema that contains the referenced entity. */
    public void setSchema(String schema) {
        this.schema = schema;
    }

    /** Returns the table/view name that participates in the relationship. */
    public String getEntity() {
        return entity;
    }

    /** Sets the table/view name that participates in the relationship. */
    public void setEntity(String entity) {
        this.entity = entity;
    }

    /** Returns the textual type of the entity (table or view). */
    public String getKind() {
        return kind;
    }

    /** Sets the textual type of the entity (table or view). */
    public void setKind(String kind) {
        this.kind = kind;
    }

    /** Returns the key metadata describing the join column. */
    public Key getKey() {
        return key;
    }

    /** Sets the key metadata describing the join column. */
    public void setKey(Key key) {
        this.key = key;
    }
}
