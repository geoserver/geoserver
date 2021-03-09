/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Abstract class that allows to get metadata information from a particular DataStore. Implements
 * some of the generic methods in the interface DataStoreMetadata, delegating some specific methods
 * to the concrete DataStores classes implementations (ie. JdbcDataStoreMetadata)
 */
public abstract class DataStoreMetadataImpl implements DataStoreMetadata {

    protected DataStoreMetadataConfig config;

    protected List<EntityMetadata> entities;
    protected List<RelationMetadata> relations;

    public DataStoreMetadataImpl(DataStoreMetadataConfig config) {
        this.entities = new ArrayList<>();
        this.relations = new ArrayList<>();
        this.config = config;
    }

    @Override
    public List<EntityMetadata> getDataStoreEntities() {
        return entities;
    }

    @Override
    public List<RelationMetadata> getEntityMetadataRelations(EntityMetadata entity) {
        List<RelationMetadata> output = new ArrayList<>();
        Iterator<RelationMetadata> ir = this.relations.iterator();
        while (ir.hasNext()) {
            RelationMetadata relation = ir.next();
            if (relation.participatesInRelation(entity.getName())) {
                output.add(relation);
            }
        }
        return output;
    }

    @Override
    public List<RelationMetadata> getDataStoreRelations() {
        return this.relations;
    }

    @Override
    public DataStoreMetadataConfig getDataStoreMetadataConfig() {
        return this.config;
    }

    @Override
    public void setDataStoreMetadataConfig(DataStoreMetadataConfig modelMetadataConfig) {
        this.config = modelMetadataConfig;
    }
}
