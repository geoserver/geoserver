/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.hib.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import java.util.logging.Logger;
import org.geoserver.wms.WatermarkInfo.Position;
import org.geoserver.wms.WatermarkInfoImpl;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * Hibernate user type for {@link WatermarkInfoImpl}.
 * 
 * @author ETj
 */
public class WMSWatermarkType implements UserType {

    private static final Logger LOGGER = Logging.getLogger(WMSWatermarkType.class);

    public Object assemble(Serializable cached, Object owner) throws HibernateException {

        // String os = owner == null ? "null" : owner.getClass().getSimpleName();
        // String cs = cached == null ? "null" : cached.getClass().getSimpleName();
        // LOGGER.severe("ASSEMBLE " + cs + "(" + cached + ")" + os + "(" + owner + ")");

        return cached;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        // if (value == null) {
        // LOGGER.severe("DISASSEMBLE null");
        // } else {
        // LOGGER.severe("DISASSEMBLE " + value.getClass().getSimpleName() + " " + value);
        // }
        return (Serializable) value;
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        return Utilities.equals(x, y);
    }

    public int hashCode(Object x) throws HibernateException {
        return Utilities.deepHashCode(x);
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
            throws HibernateException, SQLException {

        boolean isnull = false;

        int enabled = rs.getInt(names[0]);
        isnull |= rs.wasNull();
        int position    = rs.getInt(names[1]);
        isnull |= rs.wasNull();
        int transpar    = rs.getInt(names[2]);
        isnull |= rs.wasNull();
        String url      = rs.getString(names[3]);
        isnull |= rs.wasNull();

        if(isnull)
            return null;
        else {
            WatermarkInfoImpl watermark = new WatermarkInfoImpl();
            watermark.setEnabled(enabled!=0);
            watermark.setPosition(Position.get(position));
            watermark.setTransparency(transpar);
            watermark.setURL(url);
            return watermark;
        }
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index)
            throws HibernateException, SQLException {

        WatermarkInfoImpl watermark = (WatermarkInfoImpl) value;
        if (watermark == null) {
            st.setNull(index, Types.INTEGER);
            st.setNull(index + 1, Types.INTEGER);
            st.setNull(index + 2, Types.INTEGER);
            st.setNull(index + 3, Types.VARCHAR);
        } else {
            st.setInt(index, watermark.isEnabled()?1:0);
            st.setInt(index + 1, watermark.getPosition().getCode());
            st.setInt(index + 2, watermark.getTransparency());
            st.setString(index + 3, watermark.getURL());
        }
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public Class<?> returnedClass() {
        return WatermarkInfoImpl.class;
    }

    private static final int[] SQLTYPES = new int[] { Types.INTEGER, Types.INTEGER, Types.INTEGER, Types.VARCHAR };
    public int[] sqlTypes() {
        return SQLTYPES;
    }
}
