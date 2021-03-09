/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata.jdbc;

import com.google.common.collect.SortedSetMultimap;
import com.google.common.collect.TreeMultimap;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.geoserver.smartdataloader.domain.entities.DomainRelationType;
import org.geoserver.smartdataloader.metadata.AttributeMetadata;
import org.geoserver.smartdataloader.metadata.EntityMetadata;
import org.geoserver.smartdataloader.metadata.RelationMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.constraint.JdbcForeignKeyConstraintMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.constraint.JdbcIndexConstraintMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.constraint.JdbcPrimaryKeyConstraintMetadata;
import org.geotools.jdbc.JDBCDataStore;

/**
 * JDBC utilities singleton class. It encapsulates a GeoTools JDBCDataStore instance in order to use
 * some useful methods and extends it based on JDBC API use.
 */
public class JdbcHelper {

    private static JdbcHelper single_instance = null;

    private JDBCDataStore jdbcDataStore;

    private JdbcHelper() {
        jdbcDataStore = new JDBCDataStore();
    }

    public static JdbcHelper getInstance() {
        if (single_instance == null) single_instance = new JdbcHelper();
        return single_instance;
    }

    private List<JdbcTableMetadata> getListOfTablesFromResultSet(
            DatabaseMetaData metaData, ResultSet tables) throws Exception {
        if (tables != null) {
            List<JdbcTableMetadata> tableList = new ArrayList<>();
            while (tables.next()) {
                if (tables.getString("TABLE_TYPE") != null
                        && tables.getString("TABLE_TYPE").equals("TABLE")) {
                    tableList.add(
                            new JdbcTableMetadata(
                                    metaData.getConnection(),
                                    tables.getString("TABLE_CAT"),
                                    tables.getString("TABLE_SCHEM"),
                                    tables.getString("TABLE_NAME")));
                }
            }
            return tableList;
        }
        return null;
    }

    public List<JdbcTableMetadata> getSchemaTables(DatabaseMetaData metaData, String schema)
            throws Exception {
        try (ResultSet tables =
                metaData.getTables(
                        null, jdbcDataStore.escapeNamePattern(metaData, schema), "%", null)) {
            return getListOfTablesFromResultSet(metaData, tables);
        }
    }

    public List<JdbcTableMetadata> getTables(DatabaseMetaData metaData) throws Exception {
        try (ResultSet tables = metaData.getTables(null, null, "%", null)) {
            return getListOfTablesFromResultSet(metaData, tables);
        }
    }

    public SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> getPrimaryKeyColumns(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables) throws Exception {
        if (tables != null) {
            SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> pkMap = new TreeMap<>();
            for (EntityMetadata table : tables) {
                JdbcPrimaryKeyConstraintMetadata primaryKey =
                        getPrimaryKeyColumnsByTable(metaData, (JdbcTableMetadata) table);
                if (primaryKey != null) {
                    pkMap.put(table, primaryKey);
                }
            }
            return pkMap;
        }
        return null;
    }

    public SortedMap<JdbcTableMetadata, List<AttributeMetadata>> getColumns(
            DatabaseMetaData metaData, List<JdbcTableMetadata> tables) throws Exception {
        if (tables != null) {
            SortedMap<JdbcTableMetadata, List<AttributeMetadata>> cMap = new TreeMap<>();
            for (JdbcTableMetadata table : tables) {
                List<AttributeMetadata> columnList = getColumnsByTable(metaData, table);
                if (columnList != null) {
                    cMap.put(table, columnList);
                }
            }
            return cMap;
        }
        return null;
    }

    public JdbcPrimaryKeyConstraintMetadata getPrimaryKeyColumnsByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception {
        ResultSet primaryKeyColumns =
                (table != null)
                        ? metaData.getPrimaryKeys(
                                table.getCatalog(), table.getSchema(), table.getName())
                        : null;
        if (primaryKeyColumns != null && primaryKeyColumns.next()) {
            JdbcTableMetadata pkTable =
                    new JdbcTableMetadata(
                            metaData.getConnection(),
                            primaryKeyColumns.getString("TABLE_CAT"),
                            primaryKeyColumns.getString("TABLE_SCHEM"),
                            primaryKeyColumns.getString("TABLE_NAME"));
            String pkConstraintName = primaryKeyColumns.getString("PK_NAME");
            List<String> pkColumnNames = new ArrayList<>();
            do {
                pkColumnNames.add(primaryKeyColumns.getString("COLUMN_NAME"));
            } while (primaryKeyColumns.next());
            JdbcPrimaryKeyConstraintMetadata primaryKey =
                    new JdbcPrimaryKeyConstraintMetadata(pkTable, pkConstraintName, pkColumnNames);
            return primaryKey;
        }
        return null;
    }

    public List<AttributeMetadata> getColumnsByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception {
        ResultSet columns =
                (table != null)
                        ? metaData.getColumns(
                                table.getCatalog(), table.getSchema(), table.getName(), "%")
                        : null;
        if (columns != null && columns.next()) {
            JdbcTableMetadata aTable =
                    new JdbcTableMetadata(
                            metaData.getConnection(),
                            columns.getString("TABLE_CAT"),
                            columns.getString("TABLE_SCHEM"),
                            columns.getString("TABLE_NAME"));
            List<AttributeMetadata> columnsList = new ArrayList<>();
            do {
                boolean isFK = isForeignKey(metaData, aTable, columns.getString("COLUMN_NAME"));
                boolean isPK = isPrimaryKey(metaData, aTable, columns.getString("COLUMN_NAME"));
                JdbcColumnMetadata aColumn =
                        new JdbcColumnMetadata(
                                aTable,
                                columns.getString("COLUMN_NAME"),
                                columns.getString("TYPE_NAME"),
                                isFK,
                                isPK);
                columnsList.add(aColumn);

            } while (columns.next());

            return columnsList;
        }
        return null;
    }

    public List<RelationMetadata> getRelationsByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table) throws Exception {
        ArrayList<RelationMetadata> relations = new ArrayList<>();
        // add all foreignkeys relations
        SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
                fkMap = JdbcHelper.getInstance().getForeignKeysByTable(metaData, table);
        if (fkMap != null) {
            Iterator<JdbcForeignKeyConstraintMetadata> iFkConstraint = fkMap.keySet().iterator();
            while (iFkConstraint.hasNext()) {
                JdbcForeignKeyConstraintMetadata key = iFkConstraint.next();
                Collection<JdbcForeignKeyColumnMetadata> fkColumns = fkMap.get(key);
                Iterator<JdbcForeignKeyColumnMetadata> iFkColumns = fkColumns.iterator();
                while (iFkColumns.hasNext()) {
                    JdbcForeignKeyColumnMetadata aFkColumn = iFkColumns.next();
                    DomainRelationType type = JdbcHelper.getInstance().getCardinality(table, key);
                    JdbcRelationMetadata relation =
                            new JdbcRelationMetadata(key.getName(), type, aFkColumn);
                    relations.add(relation);
                    table.addRelation(relation);
                }
            }
        }
        // add all inverted foreignkeys relations
        SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
                iFkMap = JdbcHelper.getInstance().getInversedForeignKeysByTable(metaData, table);
        if (iFkMap != null) {

            Iterator<JdbcForeignKeyConstraintMetadata> iFkConstraint = iFkMap.keySet().iterator();
            while (iFkConstraint.hasNext()) {
                JdbcForeignKeyConstraintMetadata key = iFkConstraint.next();
                Collection<JdbcForeignKeyColumnMetadata> fkColumns = iFkMap.get(key);
                Iterator<JdbcForeignKeyColumnMetadata> iFkColumns = fkColumns.iterator();
                while (iFkColumns.hasNext()) {
                    JdbcForeignKeyColumnMetadata aFkColumn = iFkColumns.next();
                    DomainRelationType type = DomainRelationType.ONEMANY;
                    JdbcRelationMetadata relation =
                            new JdbcRelationMetadata(key.getName(), type, aFkColumn);
                    relations.add(relation);
                    table.addRelation(relation);
                }
            }
        }
        return relations;
    }

    public boolean isForeignKey(
            DatabaseMetaData metaData, JdbcTableMetadata table, String columnName)
            throws Exception {
        ResultSet foreignKeys =
                (table != null)
                        ? metaData.getImportedKeys(
                                table.getCatalog(), table.getSchema(), table.getName())
                        : null;
        if (foreignKeys != null && foreignKeys.next()) {
            do {
                if (foreignKeys.getString("FKTABLE_SCHEM").equals(table.getSchema())
                        && foreignKeys.getString("FKTABLE_NAME").equals(table.getName())
                        && foreignKeys.getString("FKCOLUMN_NAME").equals(columnName)) {
                    return true;
                }
            } while (foreignKeys.next());
        }
        return false;
    }

    public boolean isPrimaryKey(
            DatabaseMetaData metaData, JdbcTableMetadata table, String columnName)
            throws Exception {
        ResultSet primaryKey =
                (table != null)
                        ? metaData.getPrimaryKeys(
                                table.getCatalog(), table.getSchema(), table.getName())
                        : null;
        if (primaryKey != null && primaryKey.next()) {
            do {
                if (primaryKey.getString("COLUMN_NAME").equals(columnName)) {
                    return true;
                }
            } while (primaryKey.next());
        }
        return false;
    }

    public AttributeMetadata getColumnFromTable(
            DatabaseMetaData metaData, JdbcTableMetadata table, String columnName)
            throws Exception {
        ResultSet columns =
                (table != null)
                        ? metaData.getColumns(
                                table.getCatalog(), table.getSchema(), table.getName(), columnName)
                        : null;
        if (columns != null && columns.next()) {
            JdbcTableMetadata aTable =
                    new JdbcTableMetadata(
                            metaData.getConnection(),
                            columns.getString("TABLE_CAT"),
                            columns.getString("TABLE_SCHEM"),
                            columns.getString("TABLE_NAME"));
            boolean isFK = isForeignKey(metaData, aTable, columnName);
            boolean isPK = isPrimaryKey(metaData, aTable, columnName);
            JdbcColumnMetadata aColumn =
                    new JdbcColumnMetadata(
                            aTable,
                            columns.getString("COLUMN_NAME"),
                            columns.getString("TYPE_NAME"),
                            isFK,
                            isPK);
            return aColumn;
        }
        return null;
    }

    public SortedMap<String, Collection<String>> getIndexColumns(
            DatabaseMetaData metaData,
            List<JdbcTableMetadata> tables,
            boolean unique,
            boolean approximate)
            throws Exception {
        if (tables != null) {
            SortedMap<String, Collection<String>> indexMap = new TreeMap<>();
            for (JdbcTableMetadata table : tables) {
                SortedMap<String, Collection<String>> tableIndexMap =
                        getIndexesByTable(metaData, table, unique, approximate);
                if (tableIndexMap != null) {
                    indexMap.putAll(tableIndexMap);
                }
            }
            return indexMap;
        }
        return null;
    }

    public SortedMap<String, Collection<String>> getIndexesByTable(
            DatabaseMetaData metaData, JdbcTableMetadata table, boolean unique, boolean approximate)
            throws Exception {
        ResultSet indexColumns =
                (table != null)
                        ? metaData.getIndexInfo(
                                table.getCatalog(),
                                table.getSchema(),
                                table.getName(),
                                unique,
                                approximate)
                        : null;
        if (indexColumns != null && indexColumns.next()) {
            SortedSetMultimap<String, String> indexMultimap = TreeMultimap.create();
            JdbcTableMetadata tableAux =
                    new JdbcTableMetadata(
                            metaData.getConnection(),
                            indexColumns.getString("TABLE_CAT"),
                            indexColumns.getString("TABLE_SCHEM"),
                            indexColumns.getString("TABLE_NAME"));
            do {

                String indexConstraintName = indexColumns.getString("INDEX_NAME");
                JdbcIndexConstraintMetadata indexConstraint =
                        new JdbcIndexConstraintMetadata(tableAux, indexConstraintName);
                String indexColumnName = indexColumns.getString("COLUMN_NAME");
                indexMultimap.put(indexConstraint.toString(), indexColumnName);
            } while (indexColumns.next());
            SortedMap<String, Collection<String>> indexMap = new TreeMap<>();
            indexMap.putAll(indexMultimap.asMap());
            return indexMap;
        }
        return null;
    }

    public SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
            getForeignKeys(DatabaseMetaData metaData, List<JdbcTableMetadata> tables)
                    throws Exception {
        if (tables != null) {
            SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
                    fkMap = new TreeMap<>();
            for (JdbcTableMetadata table : tables) {
                SortedMap<
                                JdbcForeignKeyConstraintMetadata,
                                Collection<JdbcForeignKeyColumnMetadata>>
                        tableFKMap = getForeignKeysByTable(metaData, table);
                if (tableFKMap != null) {
                    fkMap.putAll(tableFKMap);
                }
            }
            return fkMap;
        }
        return null;
    }

    public SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
            getForeignKeysByTable(DatabaseMetaData metaData, JdbcTableMetadata table)
                    throws Exception {
        ResultSet foreignKeys =
                (table != null)
                        ? metaData.getImportedKeys(
                                table.getCatalog(), table.getSchema(), table.getName())
                        : null;
        if (foreignKeys != null && foreignKeys.next()) {
            SortedSetMultimap<JdbcForeignKeyConstraintMetadata, JdbcForeignKeyColumnMetadata>
                    fkMultimap = TreeMultimap.create();
            JdbcTableMetadata fkTable =
                    new JdbcTableMetadata(
                            metaData.getConnection(),
                            foreignKeys.getString("FKTABLE_CAT"),
                            foreignKeys.getString("FKTABLE_SCHEM"),
                            foreignKeys.getString("FKTABLE_NAME"));
            //  get FKColumn from table just in order to get datatype
            AttributeMetadata aColumn =
                    this.getColumnFromTable(
                            metaData, table, foreignKeys.getString("FKCOLUMN_NAME"));
            String columnType = aColumn.getType();
            do {
                String fkConstraintName = foreignKeys.getString("FK_NAME");
                JdbcTableMetadata pkTable =
                        new JdbcTableMetadata(
                                metaData.getConnection(),
                                foreignKeys.getString("PKTABLE_CAT"),
                                foreignKeys.getString("PKTABLE_SCHEM"),
                                foreignKeys.getString("PKTABLE_NAME"));
                JdbcForeignKeyConstraintMetadata fkConstraint =
                        new JdbcForeignKeyConstraintMetadata(fkTable, fkConstraintName, pkTable);
                JdbcForeignKeyColumnMetadata fkColumn =
                        new JdbcForeignKeyColumnMetadata(
                                fkTable,
                                foreignKeys.getString("FKCOLUMN_NAME"),
                                columnType,
                                new JdbcColumnMetadata(
                                        pkTable,
                                        foreignKeys.getString("PKCOLUMN_NAME"),
                                        columnType,
                                        false));
                fkMultimap.put(fkConstraint, fkColumn);
            } while (foreignKeys.next());
            SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
                    fkMap = new TreeMap<>();
            fkMap.putAll(fkMultimap.asMap());
            return fkMap;
        }
        return null;
    }

    public DomainRelationType getCardinality(
            JdbcTableMetadata table, JdbcForeignKeyConstraintMetadata fkConstraint)
            throws Exception {
        DatabaseMetaData metaData = table.getConnection().getMetaData();
        SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
                fkMultimap = JdbcHelper.getInstance().getForeignKeysByTable(metaData, table);
        JdbcPrimaryKeyConstraintMetadata primaryKey =
                JdbcHelper.getInstance().getPrimaryKeyColumnsByTable(metaData, table);
        SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> pkMap = new TreeMap<>();
        pkMap.put(table, primaryKey);
        SortedMap<String, Collection<String>> uniqueIndexMultimap =
                JdbcHelper.getInstance().getIndexesByTable(metaData, table, true, true);
        if (fkMultimap != null) {
            for (JdbcForeignKeyConstraintMetadata aFkConstraint : fkMultimap.keySet()) {
                if (aFkConstraint.equals(fkConstraint)) {
                    Collection<JdbcForeignKeyColumnMetadata> fkColumnsList =
                            fkMultimap.get(aFkConstraint);
                    JdbcPrimaryKeyConstraintMetadata isPrimaryKey =
                            JdbcHelper.getInstance()
                                    .isPrimaryKey(aFkConstraint.getTable(), fkColumnsList, pkMap);
                    if (isPrimaryKey != null) {
                        return DomainRelationType.ONEONE;
                    } else {
                        String uniqueIndexConstraint =
                                JdbcHelper.getInstance()
                                        .isUniqueIndex(
                                                aFkConstraint.getTable(),
                                                fkColumnsList,
                                                uniqueIndexMultimap);
                        if (uniqueIndexConstraint != null) {
                            return DomainRelationType.ONEONE;
                        } else {
                            if (aFkConstraint.equals(fkConstraint))
                                return DomainRelationType.MANYONE;
                            else return DomainRelationType.ONEMANY;
                        }
                    }
                }
            }
        }
        return null;
    }

    public JdbcPrimaryKeyConstraintMetadata isPrimaryKey(
            JdbcTableMetadata table,
            Collection<JdbcForeignKeyColumnMetadata> fkColumnsList,
            SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> pkMap) {
        JdbcPrimaryKeyConstraintMetadata primaryKey = pkMap.get(table);
        if (primaryKey == null) {
            return null;
        }
        for (String columnName : primaryKey.getColumnNames()) {
            boolean containsPkColumnName = false;
            for (JdbcForeignKeyColumnMetadata fkColumns : fkColumnsList) {
                if (columnName.equals(fkColumns.getName())) {
                    containsPkColumnName = true;
                }
            }
            if (!containsPkColumnName) {
                return null;
            }
        }
        return primaryKey;
    }

    public String isUniqueIndex(
            JdbcTableMetadata table,
            Collection<JdbcForeignKeyColumnMetadata> fkColumnsList,
            SortedMap<String, Collection<String>> uniqueIndexMap) {
        if (uniqueIndexMap == null) {
            return null;
        }
        indexLoop:
        for (String uniqueIndexConstraint : uniqueIndexMap.keySet()) {
            if (uniqueIndexConstraint.startsWith(table.toString() + " - ")) {
                Collection<String> uniqueIndexColumns = uniqueIndexMap.get(uniqueIndexConstraint);
                for (String uniqueIndexColumn : uniqueIndexColumns) {
                    boolean containsUniqueIndexColumn = false;
                    for (JdbcForeignKeyColumnMetadata fkColumns : fkColumnsList) {
                        if (uniqueIndexColumn.equals(fkColumns.getName())) {
                            containsUniqueIndexColumn = true;
                        }
                    }
                    if (!containsUniqueIndexColumn) {
                        continue indexLoop;
                    }
                }
                return uniqueIndexConstraint;
            }
        }
        return null;
    }

    /**
     * @param metaData the Database metadata
     * @param table the table metadata to use to pick up the column metadata
     * @return a SortedMap mapping the foreignKeys metadata to the column metadata. Returns null if
     *     no column referencing the table are found
     * @throws Exception
     */
    public SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
            getInversedForeignKeysByTable(DatabaseMetaData metaData, JdbcTableMetadata table)
                    throws Exception {
        ResultSet foreignKeys =
                (table != null)
                        ? metaData.getExportedKeys(
                                table.getCatalog(), table.getSchema(), table.getName())
                        : null;
        if (foreignKeys != null && foreignKeys.next()) {
            SortedSetMultimap<JdbcForeignKeyConstraintMetadata, JdbcForeignKeyColumnMetadata>
                    inversedFkMultimap = TreeMultimap.create();
            do {
                JdbcTableMetadata pkTable =
                        new JdbcTableMetadata(
                                metaData.getConnection(),
                                foreignKeys.getString("PKTABLE_CAT"),
                                foreignKeys.getString("PKTABLE_SCHEM"),
                                foreignKeys.getString("PKTABLE_NAME"));
                String pkConstraintName = foreignKeys.getString("PK_NAME");
                JdbcTableMetadata fkTable =
                        new JdbcTableMetadata(
                                metaData.getConnection(),
                                foreignKeys.getString("FKTABLE_CAT"),
                                foreignKeys.getString("FKTABLE_SCHEM"),
                                foreignKeys.getString("FKTABLE_NAME"));
                JdbcForeignKeyConstraintMetadata pkConstraint =
                        new JdbcForeignKeyConstraintMetadata(pkTable, pkConstraintName, fkTable);

                // get FKColumn from table just in order to get datatype
                AttributeMetadata aColumn =
                        this.getColumnFromTable(
                                metaData, fkTable, foreignKeys.getString("FKCOLUMN_NAME"));
                String columnType = aColumn.getType();

                JdbcForeignKeyColumnMetadata fkColumns =
                        new JdbcForeignKeyColumnMetadata(
                                fkTable,
                                foreignKeys.getString("FKCOLUMN_NAME"),
                                columnType,
                                new JdbcColumnMetadata(
                                        pkTable,
                                        foreignKeys.getString("PKCOLUMN_NAME"),
                                        columnType,
                                        false));
                inversedFkMultimap.put(pkConstraint, fkColumns);
            } while (foreignKeys.next());
            SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
                    inversedFkMap = new TreeMap<>();
            inversedFkMap.putAll(inversedFkMultimap.asMap());
            return inversedFkMap;
        }
        return null;
    }
}
