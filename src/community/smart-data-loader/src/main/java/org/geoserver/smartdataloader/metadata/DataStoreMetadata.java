/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata;

import java.util.List;

/**
 * Interface that provides access to metadata from a particular DataStore. It identifies different
 * objects that defines the abstract model. Those are Entities, Relations and Attributes.
 */
public interface DataStoreMetadata {

    public String getName();

    public DataStoreMetadataConfig getDataStoreMetadataConfig();

    public void setDataStoreMetadataConfig(DataStoreMetadataConfig modelMetadataConfig);

    public List<EntityMetadata> getDataStoreEntities();

    public List<RelationMetadata> getDataStoreRelations();

    public List<RelationMetadata> getEntityMetadataRelations(EntityMetadata entity);

    public EntityMetadata getEntityMetadata(String name);

    public void load() throws Exception;
}
