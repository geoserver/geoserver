/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.hib;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;
import java.util.logging.Logger;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Utilities;
import org.geotools.util.logging.Logging;
import org.hibernate.HibernateException;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.opengis.geometry.BoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

/**
 * Hibernate user type for {@link BoundingBox}.
 *
 * <p>Copied from dbconfig module
 *
 * @author Justin Deoliveira, The Open Planing Project
 */
public class BoundingBoxType implements UserType, ParameterizedType {

    private static final Logger LOGGER = Logging.getLogger(BoundingBoxType.class);

    boolean storeCRSAsWKT = false;

    public void setParameterValues(Properties parameters) {
        if (parameters != null) {
            storeCRSAsWKT = "true".equals(parameters.getProperty("storeCRSAsWKT"));
        }
    }

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

        double minx = rs.getDouble(names[0]);
        double miny = rs.getDouble(names[1]);
        double maxx = rs.getDouble(names[2]);
        double maxy = rs.getDouble(names[3]);
        String s = rs.getString(names[4]);

        // Blob blob = rs.getBlob(names[4]);
        // if (blob != null) {
        CoordinateReferenceSystem crs = null;
        if (s != null) {
            try {
                if (storeCRSAsWKT) {
                    crs = CRS.parseWKT(s);
                } else {
                    crs = CRS.decode(s);
                }
            } catch (Exception e) {
                String msg = "Unable to create crs from wkt: " + s;
                throw new HibernateException(msg, e);
            }
            // String wkt = new String(blob.getBytes(1, (int) blob.length()));

            try {
                // crs = CRS.parseWKT(wkt);
            } catch (Exception e) {

            }
        }

        ReferencedEnvelope re = new ReferencedEnvelope(minx, maxx, miny, maxy, crs);
        if (minx > maxx) {
            return null;
            // re.setToNull();
        }
        return re;
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index)
            throws HibernateException, SQLException {

        BoundingBox box = (BoundingBox) value;
        if (box == null) {
            // set to null
            st.setDouble(index, 1);
            st.setDouble(index + 1, 1);
            st.setDouble(index + 2, -1);
            st.setDouble(index + 3, -1);
            st.setNull(index + 4, Types.VARCHAR);
            // st.setBlob(index + 4, (Blob) null);
            return;
        }

        // TODO: check for isNull() and set the minx,maxx accordingly
        st.setDouble(index, box.getMinX());
        st.setDouble(index + 1, box.getMinY());
        st.setDouble(index + 2, box.getMaxX());
        st.setDouble(index + 3, box.getMaxY());

        if (box.getCoordinateReferenceSystem() != null) {
            CoordinateReferenceSystem crs = box.getCoordinateReferenceSystem();
            if (storeCRSAsWKT) {
                st.setString(index + 4, crs.toWKT());
            } else {
                try {
                    st.setString(index + 4, "EPSG:" + CRS.lookupEpsgCode(crs, true));
                } catch (FactoryException e) {
                    throw new RuntimeException(e);
                }
            }
            // st.setBlob(index + 4, Hibernate.createBlob(crs.toWKT().getBytes()));
        } else {
            st.setNull(index + 4, Types.VARCHAR);
            // st.setBlob(index + 4, (Blob) null);
        }
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public Class<?> returnedClass() {
        return BoundingBox.class;
    }

    private static final int[] SQLTYPES =
            new int[] {Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.DOUBLE, Types.VARCHAR};

    public int[] sqlTypes() {
        return SQLTYPES;
    }
}
