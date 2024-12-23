package org.geoserver.wfs;

import java.sql.Connection;
import java.sql.SQLException;
import org.h2.api.Trigger;

/**
 * H2 trigger implementation that just throws exceptions for everything. Used for testing GeoServer conditional exposure
 * of underlying cause in WFS exceptions.
 */
public class ExceptionThrowingTrigger implements Trigger {

    public static final String STATIC_CAUSE = "Unit test expected: Trigger is not happy with data conditions";

    @Override
    public void init(Connection conn, String schemaName, String triggerName, String tableName, boolean before, int type)
            throws SQLException {
        // N/A
    }

    @Override
    public void fire(Connection conn, Object[] oldRow, Object[] newRow) throws SQLException {
        throw new SQLException(STATIC_CAUSE);
    }
}
