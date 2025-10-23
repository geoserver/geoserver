/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata.jdbc;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;
import org.geoserver.smartdataloader.data.store.virtualfk.EntityRef;
import org.geoserver.smartdataloader.data.store.virtualfk.Relationship;
import org.geoserver.smartdataloader.data.store.virtualfk.Relationships;
import org.geoserver.smartdataloader.data.store.virtualfk.RelationshipsXmlParser;
import org.geoserver.smartdataloader.domain.entities.DomainRelationType;
import org.geoserver.smartdataloader.metadata.AttributeMetadata;
import org.geoserver.smartdataloader.metadata.EntityMetadata;
import org.geoserver.smartdataloader.metadata.RelationMetadata;
import org.geoserver.smartdataloader.metadata.VirtualRelationMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.constraint.JdbcForeignKeyConstraintMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.constraint.JdbcPrimaryKeyConstraintMetadata;

public class VirtualFkJdbcHelper implements JdbcHelper {

    private final JdbcHelper delegate;
    private final Relationships relationships;

    public VirtualFkJdbcHelper(Relationships relationships) {
        this(new DefaultJdbcHelper(), relationships);
    }

    /** Allow injecting a different delegate (useful for testing or custom implementations). */
    public VirtualFkJdbcHelper(JdbcHelper delegate, Relationships relationships) {
        this.delegate = (delegate != null) ? delegate : new DefaultJdbcHelper();
        this.relationships = (relationships != null) ? relationships : new Relationships();
    }

    @Override
    public List<JdbcTableMetadata> getSchemaTables(DatabaseMetaData metaData, String schema) throws Exception {
        List<JdbcTableMetadata> delegateTables = delegate.getSchemaTables(metaData, schema);
        List<JdbcTableMetadata> tables = (delegateTables != null) ? new ArrayList<>(delegateTables) : new ArrayList<>();
        applyVirtualHelper(tables);
        for (Relationship relationship : relationships.getRelationships()) {
            includeRelationshipEndpoint(metaData, tables, relationship.getSource());
            includeRelationshipEndpoint(metaData, tables, relationship.getTarget());
        }
        return tables;
    }

    @Override
    public List<JdbcTableMetadata> getTables(DatabaseMetaData metaData) throws Exception {
        List<JdbcTableMetadata> tables = delegate.getTables(metaData);
        applyVirtualHelper(tables);
        return tables;
    }

    @Override
    public SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> getPrimaryKeyColumns(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables) throws Exception {
        return delegate.getPrimaryKeyColumns(metaData, tables);
    }

    @Override
    public SortedMap<JdbcTableMetadata, List<AttributeMetadata>> getColumns(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables) throws Exception {
        return delegate.getColumns(metaData, tables);
    }

    @Override
    public JdbcPrimaryKeyConstraintMetadata getPrimaryKeyColumnsByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception {
        return delegate.getPrimaryKeyColumnsByTable(metaData, table);
    }

    @Override
    public List<AttributeMetadata> getColumnsByTable(DatabaseMetaData metaData, JdbcTableMetadata table)
            throws Exception {
        return delegate.getColumnsByTable(metaData, table);
    }

    @Override
    public List<RelationMetadata> getRelationsByTable(DatabaseMetaData metaData, JdbcTableMetadata table)
            throws Exception {
        if (table != null) {
            table.setJdbcHelper(this);
        }
        List<RelationMetadata> relations = new ArrayList<>();
        for (Relationship relationship : relationships.getRelationships()) {
            DomainRelationType cardinality = RelationshipsXmlParser.resolveCardinality(relationship.getCardinality());
            boolean matchesSource = Objects.equals(relationship.getSource().getEntity(), table.getName())
                    && Objects.equals(relationship.getSource().getSchema(), table.getSchema());
            boolean matchesTarget = Objects.equals(relationship.getTarget().getEntity(), table.getName())
                    && Objects.equals(relationship.getTarget().getSchema(), table.getSchema());

            if (matchesSource) {
                AttributeMetadata sourceAttr =
                        findAttribute(table, relationship.getSource().getKey().getColumn());
                JdbcTableMetadata targetTableMetadata = findTableMetadata(
                        metaData,
                        relationship.getTarget().getSchema(),
                        relationship.getTarget().getEntity());
                AttributeMetadata targetAttr = findAttribute(
                        targetTableMetadata, relationship.getTarget().getKey().getColumn());
                if (sourceAttr != null && targetAttr != null) {
                    RelationMetadata virtualRelation =
                            new VirtualRelationMetadata(cardinality, sourceAttr, targetAttr, relationship.getName());
                    relations.add(virtualRelation);
                }
            }

            if (matchesTarget) {
                AttributeMetadata targetAttr =
                        findAttribute(table, relationship.getTarget().getKey().getColumn());
                JdbcTableMetadata sourceTableMetadata = findTableMetadata(
                        metaData,
                        relationship.getSource().getSchema(),
                        relationship.getSource().getEntity());
                AttributeMetadata sourceAttr = findAttribute(
                        sourceTableMetadata, relationship.getSource().getKey().getColumn());
                if (targetAttr != null && sourceAttr != null) {
                    DomainRelationType inverseCardinality = invertCardinality(cardinality);
                    RelationMetadata inverseRelation = new VirtualRelationMetadata(
                            inverseCardinality, targetAttr, sourceAttr, relationship.getName());
                    relations.add(inverseRelation);
                }
            }
        }
        relations.addAll(delegate.getRelationsByTable(metaData, table));
        return relations;
    }

    @Override
    public boolean isForeignKey(DatabaseMetaData metaData, JdbcTableMetadata table, String columnName)
            throws Exception {
        return delegate.isForeignKey(metaData, table, columnName) || isVirtualForeignKey(table, columnName);
    }

    private boolean isVirtualForeignKey(JdbcTableMetadata table, String columnName) {
        // check if column matches any source in relationships
        return relationships.getRelationships().stream()
                .anyMatch(rel -> rel.getSource().getEntity().equals(table.getName())
                        && rel.getSource().getKey().getColumn().equals(columnName)
                        && rel.getSource().getSchema().equals(table.getSchema()));
    }

    @Override
    public boolean isPrimaryKey(DatabaseMetaData metaData, JdbcTableMetadata table, String columnName)
            throws Exception {
        return delegate.isPrimaryKey(metaData, table, columnName);
    }

    @Override
    public AttributeMetadata getColumnFromTable(DatabaseMetaData metaData, JdbcTableMetadata table, String columnName)
            throws Exception {
        return delegate.getColumnFromTable(metaData, table, columnName);
    }

    @Override
    public SortedMap<String, Collection<String>> getIndexColumns(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables, boolean unique, boolean approximate)
            throws Exception {
        return delegate.getIndexColumns(metaData, tables, unique, approximate);
    }

    @Override
    public SortedMap<String, Collection<String>> getIndexesByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table, boolean unique, boolean approximate) throws Exception {
        return delegate.getIndexesByTable(metaData, table, unique, approximate);
    }

    @Override
    public SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>> getForeignKeys(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables) throws Exception {
        return delegate.getForeignKeys(metaData, tables);
    }

    @Override
    public SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>> getForeignKeysByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception {
        return delegate.getForeignKeysByTable(metaData, table);
    }

    @Override
    public DomainRelationType getCardinality(JdbcTableMetadata table, JdbcForeignKeyConstraintMetadata fkConstraint)
            throws Exception {
        return delegate.getCardinality(table, fkConstraint);
    }

    @Override
    public JdbcPrimaryKeyConstraintMetadata isPrimaryKey(
            JdbcTableMetadata table,
            Collection<JdbcForeignKeyColumnMetadata> fkColumnsList,
            SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> pkMap) {
        return delegate.isPrimaryKey(table, fkColumnsList, pkMap);
    }

    @Override
    public String isUniqueIndex(
            JdbcTableMetadata table,
            Collection<JdbcForeignKeyColumnMetadata> fkColumnsList,
            SortedMap<String, Collection<String>> uniqueIndexMap) {
        return delegate.isUniqueIndex(table, fkColumnsList, uniqueIndexMap);
    }

    @Override
    public SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
            getInversedForeignKeysByTable(DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception {
        return delegate.getInversedForeignKeysByTable(metaData, table);
    }

    private void applyVirtualHelper(List<JdbcTableMetadata> tables) {
        if (tables == null) {
            return;
        }
        for (JdbcTableMetadata table : tables) {
            if (table != null) {
                table.setJdbcHelper(this);
            }
        }
    }

    private void includeRelationshipEndpoint(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables, EntityRef endpoint) throws Exception {
        if (endpoint == null || endpoint.getEntity() == null) {
            return;
        }
        boolean alreadyPresent = tables.stream()
                .anyMatch(t -> Objects.equals(t.getName(), endpoint.getEntity())
                        && Objects.equals(t.getSchema(), endpoint.getSchema()));
        if (alreadyPresent) {
            return;
        }
        JdbcTableMetadata tableMetadata = findTableMetadata(metaData, endpoint.getSchema(), endpoint.getEntity());
        if (tableMetadata != null) {
            tables.add(tableMetadata);
        }
    }

    private JdbcTableMetadata findTableMetadata(DatabaseMetaData metaData, String schema, String tableName)
            throws Exception {
        if (tableName == null) {
            return null;
        }
        List<JdbcTableMetadata> schemaTables = delegate.getSchemaTables(metaData, schema);
        applyVirtualHelper(schemaTables);
        if (schemaTables == null) {
            return null;
        }
        for (JdbcTableMetadata table : schemaTables) {
            if (table != null && Objects.equals(table.getName(), tableName)) {
                return table;
            }
        }
        return null;
    }

    private AttributeMetadata findAttribute(JdbcTableMetadata tableMetadata, String columnName) {
        if (tableMetadata == null || columnName == null) {
            return null;
        }
        if (tableMetadata.getAttributes() == null) {
            return null;
        }
        return tableMetadata.getAttributes().stream()
                .filter(attr -> Objects.equals(attr.getName(), columnName))
                .findFirst()
                .orElse(null);
    }

    private DomainRelationType invertCardinality(DomainRelationType cardinality) {
        if (cardinality == null) {
            return null;
        }
        switch (cardinality) {
            case ONEMANY:
                return DomainRelationType.MANYONE;
            case MANYONE:
                return DomainRelationType.ONEMANY;
            case ONEONE:
                return DomainRelationType.ONEONE;
            case MANYMANY:
                return DomainRelationType.MANYMANY;
            default:
                return cardinality;
        }
    }
}
