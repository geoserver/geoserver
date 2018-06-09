/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import javax.annotation.Nullable;

public class OracleDialect extends Dialect {

    @Override
    public void applyOffsetLimit(
            StringBuilder sql, @Nullable Integer offset, @Nullable Integer limit) {
        // some db's require limit to be present of offset is
        if (offset != null && limit == null) {
            limit = Integer.MAX_VALUE; // ensure we don't wrap around
        }
        if (limit != null && offset == null) {
            offset = 0;
            limit += 1; // not zero-based
        }
        if (offset != null && limit != null) {
            sql.insert(0, "select * from ( select query.*, rownum rnum from (\n");
            sql.append(") query\n");
            if (limit != Integer.MAX_VALUE) {
                limit = offset + limit;
            }
            sql.append("where rownum <= ").append(limit).append(")\n");
            sql.append("where rnum > ").append(offset);
        }
    }

    @Override
    public String nextVal(String sequence) {
        return sequence + ".nextval";
    }
}
