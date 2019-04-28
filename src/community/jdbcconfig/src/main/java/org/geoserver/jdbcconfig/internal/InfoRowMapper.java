/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.io.ByteArrayInputStream;
import java.io.UnsupportedEncodingException;
import java.sql.ResultSet;
import java.sql.SQLException;
import org.geoserver.catalog.Info;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.lob.DefaultLobHandler;
import org.springframework.jdbc.support.lob.LobHandler;

public final class InfoRowMapper<T extends Info> implements RowMapper<T> {

    private final Class<T> type;

    private final int colNum;

    private final LobHandler lobHandler;

    private final XStreamInfoSerialBinding binding;

    public InfoRowMapper(final Class<T> type, final XStreamInfoSerialBinding binding) {
        this(type, binding, 1);
    }

    public InfoRowMapper(
            final Class<T> type, final XStreamInfoSerialBinding binding, final int colNum) {

        this.type = type;
        this.binding = binding;
        this.colNum = colNum;

        // TODO: be careful this may not work with
        // Oracle and need an OracleLobHandler
        this.lobHandler = new DefaultLobHandler();
    }

    @Override
    public T mapRow(final ResultSet rs, final int rowNum) throws SQLException {
        // InputStream binaryStream = lobHandler.getBlobAsBinaryStream(rs, colNum);
        String xml = rs.getString(colNum);
        ByteArrayInputStream in;
        try {
            byte[] bytes = xml.getBytes("UTF-8");
            in = new ByteArrayInputStream(bytes);
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
        return binding.entryToObject(in, type);
    }
}
