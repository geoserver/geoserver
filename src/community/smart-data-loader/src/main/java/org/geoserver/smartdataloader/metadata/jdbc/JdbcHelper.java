package org.geoserver.smartdataloader.metadata.jdbc;

import java.sql.DatabaseMetaData;
import java.util.Collection;
import java.util.List;
import java.util.SortedMap;
import org.geoserver.smartdataloader.domain.entities.DomainRelationType;
import org.geoserver.smartdataloader.metadata.AttributeMetadata;
import org.geoserver.smartdataloader.metadata.EntityMetadata;
import org.geoserver.smartdataloader.metadata.RelationMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.constraint.JdbcForeignKeyConstraintMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.constraint.JdbcPrimaryKeyConstraintMetadata;

public interface JdbcHelper {
    List<JdbcTableMetadata> getSchemaTables(DatabaseMetaData metaData, String schema) throws Exception;

    List<JdbcTableMetadata> getTables(DatabaseMetaData metaData) throws Exception;

    SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> getPrimaryKeyColumns(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables) throws Exception;

    SortedMap<JdbcTableMetadata, List<AttributeMetadata>> getColumns(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables) throws Exception;

    JdbcPrimaryKeyConstraintMetadata getPrimaryKeyColumnsByTable(DatabaseMetaData metaData, JdbcTableMetadata table)
            throws Exception;

    List<AttributeMetadata> getColumnsByTable(DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception;

    List<RelationMetadata> getRelationsByTable(DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception;

    boolean isForeignKey(DatabaseMetaData metaData, JdbcTableMetadata table, String columnName) throws Exception;

    boolean isPrimaryKey(DatabaseMetaData metaData, JdbcTableMetadata table, String columnName) throws Exception;

    AttributeMetadata getColumnFromTable(DatabaseMetaData metaData, JdbcTableMetadata table, String columnName)
            throws Exception;

    SortedMap<String, Collection<String>> getIndexColumns(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables, boolean unique, boolean approximate)
            throws Exception;

    SortedMap<String, Collection<String>> getIndexesByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table, boolean unique, boolean approximate) throws Exception;

    SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>> getForeignKeys(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables) throws Exception;

    SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>> getForeignKeysByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception;

    DomainRelationType getCardinality(JdbcTableMetadata table, JdbcForeignKeyConstraintMetadata fkConstraint)
            throws Exception;

    JdbcPrimaryKeyConstraintMetadata isPrimaryKey(
            JdbcTableMetadata table,
            Collection<JdbcForeignKeyColumnMetadata> fkColumnsList,
            SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> pkMap);

    String isUniqueIndex(
            JdbcTableMetadata table,
            Collection<JdbcForeignKeyColumnMetadata> fkColumnsList,
            SortedMap<String, Collection<String>> uniqueIndexMap);

    SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>> getInversedForeignKeysByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception;
}
