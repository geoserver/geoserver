/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata.jdbc;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import org.geoserver.smartdataloader.metadata.AttributeMetadata;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataConfig;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataImpl;
import org.geoserver.smartdataloader.metadata.EntityMetadata;
import org.geoserver.smartdataloader.metadata.RelationMetadata;

/**
 * Concrete class that implements access to JDBC dataStore metadata model, extending
 * DataStoreMetadataImpl.
 */
public class JdbcDataStoreMetadata extends DataStoreMetadataImpl {

    public JdbcDataStoreMetadata(DataStoreMetadataConfig config) {
        super(config);
    }

    @Override
    public void load() throws Exception {
        JdbcDataStoreMetadataConfig jdbcConfig = (JdbcDataStoreMetadataConfig) this.config;
        // load entities
        entities = new ArrayList<>();
        List<JdbcTableMetadata> tableList =
                JdbcHelper.getInstance()
                        .getSchemaTables(
                                jdbcConfig.getConnection().getMetaData(), jdbcConfig.getSchema());
        entities.addAll(tableList);
        // load attributes and relations for each entity
        relations = new ArrayList<>();
        Iterator<JdbcTableMetadata> iTables = tableList.iterator();
        while (iTables.hasNext()) {
            JdbcTableMetadata jTable = iTables.next();
            // load attributes
            List<AttributeMetadata> attributes =
                    JdbcHelper.getInstance()
                            .getColumnsByTable(jdbcConfig.getConnection().getMetaData(), jTable);
            attributes.forEach(
                    attributeMetadata -> {
                        jTable.addAttribute(attributeMetadata);
                    });
            // load relations
            List<RelationMetadata> tableRelations =
                    JdbcHelper.getInstance()
                            .getRelationsByTable(jdbcConfig.getConnection().getMetaData(), jTable);
            tableRelations.forEach(
                    relationMetadata -> {
                        jTable.addRelation(relationMetadata);
                        relations.add(relationMetadata);
                    });
        }
    }

    @Override
    public EntityMetadata getEntityMetadata(String name) {
        Iterator<EntityMetadata> ie = this.entities.iterator();
        while (ie.hasNext()) {
            EntityMetadata e = ie.next();
            if (e.getName().equals(name)) {
                return e;
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return config.getName();
    }
}
