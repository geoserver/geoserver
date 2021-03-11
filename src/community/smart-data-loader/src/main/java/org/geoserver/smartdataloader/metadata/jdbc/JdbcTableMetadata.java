/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata.jdbc;

import com.google.common.base.Strings;
import com.google.common.collect.ComparisonChain;
import com.google.common.util.concurrent.UncheckedExecutionException;
import java.sql.Connection;
import java.util.List;
import java.util.Objects;
import org.geoserver.smartdataloader.metadata.AttributeMetadata;
import org.geoserver.smartdataloader.metadata.EntityMetadata;
import org.geoserver.smartdataloader.metadata.RelationMetadata;

/**
 * Class representing metadata for a table (EntityMetadata) in a JDBC DataStore.
 *
 * @author Jose Macchi - Geosolutions
 */
public class JdbcTableMetadata extends EntityMetadata implements JdbcConnectable {
    private final Connection connection;
    private final String catalog;
    private final String schema;

    public JdbcTableMetadata(Connection connection, String catalog, String schema, String name) {
        super(name);
        this.connection = connection;
        this.catalog = catalog;
        this.schema = schema;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder();
        if (!Strings.isNullOrEmpty(catalog)) {
            stringBuilder.append(catalog);
            stringBuilder.append(".");
        }
        if (!Strings.isNullOrEmpty(schema)) {
            stringBuilder.append(schema);
            stringBuilder.append(".");
        }
        if (!Strings.isNullOrEmpty(name)) stringBuilder.append(this.name);
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof JdbcTableMetadata)) {
            return false;
        }
        if (!super.equals(object)) return false;
        JdbcTableMetadata table = (JdbcTableMetadata) object;
        return this.compareTo(table) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.catalog, this.schema, this.getName());
    }

    @Override
    public int compareTo(EntityMetadata table) {
        if (table != null) {
            JdbcTableMetadata t = (JdbcTableMetadata) table;
            if (this.catalog != null && t.getCatalog() != null) {
                return ComparisonChain.start()
                        .compare(this.catalog, t.getCatalog())
                        .compare(this.schema, t.getSchema())
                        .compare(this.name, t.getName())
                        .result();
            } else if (this.catalog == null && t.getCatalog() == null) {
                return ComparisonChain.start()
                        .compare(this.schema, t.getSchema())
                        .compare(this.name, t.getName())
                        .result();
            }
        }
        return 1;
    }

    @Override
    public List<AttributeMetadata> getAttributes() {
        try {
            // Lazy load in case not loaded before
            if (attributes.isEmpty()) {
                attributes.addAll(
                        JdbcHelper.getInstance().getColumnsByTable(connection.getMetaData(), this));
            }
            return attributes;
        } catch (Exception e) {
            throw new UncheckedExecutionException("Cannot get attributes from DatabaseMetadata", e);
        }
    }

    public String getCatalog() {
        return catalog;
    }

    public String getSchema() {
        return schema;
    }

    @Override
    public Connection getConnection() {
        return connection;
    }

    @Override
    public List<RelationMetadata> getRelations() {
        try {
            if (relations.isEmpty()) {
                relations.addAll(
                        JdbcHelper.getInstance()
                                .getRelationsByTable(connection.getMetaData(), this));
            }
            return relations;
        } catch (Exception e) {
            throw new UncheckedExecutionException("Cannot get relations from DatabaseMetadata", e);
        }
    }
}
