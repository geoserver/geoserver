/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata.jdbc;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;
import org.geoserver.smartdataloader.data.store.virtualfk.EntityRef;
import org.geoserver.smartdataloader.data.store.virtualfk.Key;
import org.geoserver.smartdataloader.data.store.virtualfk.Relationship;
import org.geoserver.smartdataloader.data.store.virtualfk.Relationships;
import org.geoserver.smartdataloader.domain.entities.DomainRelationType;
import org.geoserver.smartdataloader.metadata.AttributeMetadata;
import org.geoserver.smartdataloader.metadata.EntityMetadata;
import org.geoserver.smartdataloader.metadata.RelationMetadata;
import org.geoserver.smartdataloader.metadata.VirtualRelationMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.constraint.JdbcForeignKeyConstraintMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.constraint.JdbcPrimaryKeyConstraintMetadata;
import org.junit.Test;

public class VirtualFkJdbcHelperTest {

    @Test
    public void generatesComplementaryRelation() throws Exception {
        StubJdbcHelper delegate = new StubJdbcHelper();
        TestJdbcTableMetadata sourceTable = new TestJdbcTableMetadata(delegate, "public", "observations_v");
        sourceTable.addAttribute(new JdbcColumnMetadata(sourceTable, "station_id", "integer", false));
        TestJdbcTableMetadata targetTable = new TestJdbcTableMetadata(delegate, "public", "stations");
        targetTable.addAttribute(new JdbcColumnMetadata(targetTable, "id", "integer", false));

        Relationships relationships = new Relationships();
        relationships.addRelationship(new Relationship(
                "obs_to_station",
                "1:n",
                new EntityRef("public", "observations_v", "VIEW", new Key("station_id")),
                new EntityRef("public", "stations", "TABLE", new Key("id"))));

        delegate.registerTables(sourceTable, targetTable);
        VirtualFkJdbcHelper helper = new VirtualFkJdbcHelper(delegate, relationships);

        helper.validateVirtualRelationships(null, "public");
        List<RelationMetadata> sourceRelations = helper.getRelationsByTable(null, sourceTable);
        assertEquals(1, sourceRelations.size());
        VirtualRelationMetadata forward = (VirtualRelationMetadata) sourceRelations.get(0);
        assertEquals(DomainRelationType.ONEMANY, forward.getRelationType());
        assertEquals("station_id", forward.getSourceAttribute().getName());
        assertEquals("id", forward.getDestinationAttribute().getName());

        List<RelationMetadata> targetRelations = helper.getRelationsByTable(null, targetTable);
        assertEquals(1, targetRelations.size());
        VirtualRelationMetadata inverse = (VirtualRelationMetadata) targetRelations.get(0);
        assertEquals(DomainRelationType.MANYONE, inverse.getRelationType());
        assertEquals("id", inverse.getSourceAttribute().getName());
        assertEquals("station_id", inverse.getDestinationAttribute().getName());
        assertNotNull(delegate.getSchemaTables(null, "public"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void validationFailsOnSchemaMismatch() throws Exception {
        StubJdbcHelper delegate = new StubJdbcHelper();
        TestJdbcTableMetadata sourceTable = new TestJdbcTableMetadata(delegate, "other", "observations_v");
        sourceTable.addAttribute(new JdbcColumnMetadata(sourceTable, "station_id", "integer", false));
        TestJdbcTableMetadata targetTable = new TestJdbcTableMetadata(delegate, "public", "stations");
        targetTable.addAttribute(new JdbcColumnMetadata(targetTable, "id", "integer", false));
        delegate.registerTables(sourceTable, targetTable);

        Relationships relationships = new Relationships();
        relationships.addRelationship(new Relationship(
                "obs_to_station",
                "1:n",
                new EntityRef("other", "observations_v", "VIEW", new Key("station_id")),
                new EntityRef("public", "stations", "TABLE", new Key("id"))));

        VirtualFkJdbcHelper helper = new VirtualFkJdbcHelper(delegate, relationships);
        helper.validateVirtualRelationships(null, "public");
    }

    @Test(expected = IllegalArgumentException.class)
    public void validationFailsOnMissingColumn() throws Exception {
        StubJdbcHelper delegate = new StubJdbcHelper();
        TestJdbcTableMetadata sourceTable = new TestJdbcTableMetadata(delegate, "public", "observations_v");
        sourceTable.addAttribute(new JdbcColumnMetadata(sourceTable, "id", "integer", false));
        delegate.registerTables(sourceTable);

        Relationships relationships = new Relationships();
        relationships.addRelationship(new Relationship(
                "obs_to_station",
                "1:n",
                new EntityRef("public", "observations_v", "VIEW", new Key("missing_col")),
                new EntityRef("public", "stations", "TABLE", new Key("id"))));

        VirtualFkJdbcHelper helper = new VirtualFkJdbcHelper(delegate, relationships);
        helper.validateVirtualRelationships(null, "public");
    }

    private static final class TestJdbcTableMetadata extends JdbcTableMetadata {
        private final List<AttributeMetadata> presetAttributes = new ArrayList<>();

        TestJdbcTableMetadata(JdbcHelper helper, String schema, String name) {
            super(null, null, schema, name, helper);
        }

        @Override
        public void addAttribute(AttributeMetadata attribute) {
            presetAttributes.add(attribute);
        }

        @Override
        public List<AttributeMetadata> getAttributes() {
            return presetAttributes;
        }
    }

    private static final class StubJdbcHelper implements JdbcHelper {
        private final List<JdbcTableMetadata> tables = new ArrayList<>();

        StubJdbcHelper(JdbcTableMetadata... tables) {
            Collections.addAll(this.tables, tables);
        }

        void registerTables(JdbcTableMetadata... tables) {
            Collections.addAll(this.tables, tables);
        }

        @Override
        public List<JdbcTableMetadata> getSchemaTables(DatabaseMetaData metaData, String schema) {
            List<JdbcTableMetadata> filtered = new ArrayList<>();
            for (JdbcTableMetadata table : tables) {
                if (table != null && (schema == null || schema.equals(table.getSchema()))) {
                    filtered.add(table);
                }
            }
            return filtered;
        }

        @Override
        public List<JdbcTableMetadata> getTables(DatabaseMetaData metaData) {
            return tables;
        }

        @Override
        public SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> getPrimaryKeyColumns(
                DatabaseMetaData metaData, List<JdbcTableMetadata> tables) {
            return new TreeMap<>();
        }

        @Override
        public SortedMap<JdbcTableMetadata, List<AttributeMetadata>> getColumns(
                DatabaseMetaData metaData, List<JdbcTableMetadata> tables) {
            return new TreeMap<>();
        }

        @Override
        public JdbcPrimaryKeyConstraintMetadata getPrimaryKeyColumnsByTable(
                DatabaseMetaData metaData, JdbcTableMetadata table) {
            return null;
        }

        @Override
        public List<AttributeMetadata> getColumnsByTable(DatabaseMetaData metaData, JdbcTableMetadata table) {
            return Collections.emptyList();
        }

        @Override
        public List<RelationMetadata> getRelationsByTable(DatabaseMetaData metaData, JdbcTableMetadata table) {
            return Collections.emptyList();
        }

        @Override
        public boolean isForeignKey(DatabaseMetaData metaData, JdbcTableMetadata table, String columnName) {
            return false;
        }

        @Override
        public boolean isPrimaryKey(DatabaseMetaData metaData, JdbcTableMetadata table, String columnName) {
            return false;
        }

        @Override
        public AttributeMetadata getColumnFromTable(
                DatabaseMetaData metaData, JdbcTableMetadata table, String columnName) {
            return null;
        }

        @Override
        public SortedMap<String, Collection<String>> getIndexColumns(
                DatabaseMetaData metaData, List<JdbcTableMetadata> tables, boolean unique, boolean approximate) {
            return new TreeMap<>();
        }

        @Override
        public SortedMap<String, Collection<String>> getIndexesByTable(
                DatabaseMetaData metaData, JdbcTableMetadata table, boolean unique, boolean approximate) {
            return new TreeMap<>();
        }

        @Override
        public SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>> getForeignKeys(
                DatabaseMetaData metaData, List<JdbcTableMetadata> tables) {
            return new TreeMap<>();
        }

        @Override
        public SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
                getForeignKeysByTable(DatabaseMetaData metaData, JdbcTableMetadata table) {
            return new TreeMap<>();
        }

        @Override
        public DomainRelationType getCardinality(
                JdbcTableMetadata table, JdbcForeignKeyConstraintMetadata fkConstraint) {
            return null;
        }

        @Override
        public JdbcPrimaryKeyConstraintMetadata isPrimaryKey(
                JdbcTableMetadata table,
                Collection<JdbcForeignKeyColumnMetadata> fkColumnsList,
                SortedMap<EntityMetadata, JdbcPrimaryKeyConstraintMetadata> pkMap) {
            return null;
        }

        @Override
        public String isUniqueIndex(
                JdbcTableMetadata table,
                Collection<JdbcForeignKeyColumnMetadata> fkColumnsList,
                SortedMap<String, Collection<String>> uniqueIndexMap) {
            return null;
        }

        @Override
        public SortedMap<JdbcForeignKeyConstraintMetadata, Collection<JdbcForeignKeyColumnMetadata>>
                getInversedForeignKeysByTable(DatabaseMetaData metaData, JdbcTableMetadata table) {
            return new TreeMap<>();
        }
    }
}
