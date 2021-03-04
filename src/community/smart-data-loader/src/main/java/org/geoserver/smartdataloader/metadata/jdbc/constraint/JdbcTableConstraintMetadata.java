/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.metadata.jdbc.constraint;

import com.google.common.collect.ComparisonChain;
import java.util.Objects;
import org.geoserver.smartdataloader.metadata.ConstraintMetadata;
import org.geoserver.smartdataloader.metadata.jdbc.JdbcTableMetadata;

/**
 * Super class representing metadata for constraints related to tables (Entities) in a JDBC
 * DataStore.
 *
 * @author Jose Macchi - Geosolutions
 */
public class JdbcTableConstraintMetadata extends ConstraintMetadata {
    private final JdbcTableMetadata table;

    public JdbcTableConstraintMetadata(JdbcTableMetadata table, String name) {
        super(name);
        this.table = table;
    }

    @Override
    public String toString() {
        StringBuilder stringBuilder = new StringBuilder(this.table.toString());
        stringBuilder.append(" - ");
        stringBuilder.append(this.getName());
        return stringBuilder.toString();
    }

    @Override
    public boolean equals(Object object) {
        if (object == null || !(object instanceof JdbcTableConstraintMetadata)) {
            return false;
        }
        JdbcTableConstraintMetadata tableConstaint = (JdbcTableConstraintMetadata) object;
        return this.compareTo(tableConstaint) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(this.table, this.getName());
    }

    public JdbcTableMetadata getTable() {
        return table;
    }

    @Override
    public int compareTo(ConstraintMetadata tableConstraint) {
        if (tableConstraint != null) {
            JdbcTableConstraintMetadata tc = (JdbcTableConstraintMetadata) tableConstraint;
            return ComparisonChain.start()
                    .compare(this.table, tc.getTable())
                    .compare(this.getName(), tc.getName())
                    .result();
        }
        return 1;
    }
}
