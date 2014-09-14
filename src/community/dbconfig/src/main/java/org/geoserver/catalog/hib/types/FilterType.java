/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.hib.types;

import java.io.ByteArrayOutputStream;
import java.io.Serializable;
import java.io.StringReader;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.geotools.filter.v1_1.OGC;
import org.geotools.filter.v1_1.OGCConfiguration;
import org.geotools.util.Utilities;
import org.geotools.xml.Encoder;
import org.geotools.xml.Parser;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;
import org.opengis.filter.Filter;

/**
 * Hibernate user type for {@link Filter}.
 * 
 * @author This class persists a filter as a string of xml.
 * 
 */
public class FilterType implements UserType {

    /**
     * xml configuration for parsing / encoding filters.
     */
    static OGCConfiguration ogc = new OGCConfiguration();

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return null;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return null;
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        return Utilities.equals(x, y);
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
            throws HibernateException, SQLException {

        String xml = rs.getString(names[0]);
        if (xml == null) {
            return null;
        }

        Parser parser = new Parser(ogc);
        try {
            Filter filter = (Filter) parser.parse(new StringReader(xml));
            return filter;
        } catch (Exception e) {
            String msg = "Could not decode filter: " + xml;
            throw new HibernateException(msg, e);
        }
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index)
            throws HibernateException, SQLException {

        Filter filter = (Filter) value;
        if (filter == null) {
            st.setString(index, null);
            return;
        }

        Encoder encoder = new Encoder(ogc);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            encoder.encode(filter, OGC.Filter, output);
            st.setString(index, new String(output.toByteArray()));
        } catch (Exception e) {
            String msg = "Could not encode filter: " + filter;
            throw new HibernateException(msg, e);
        }

    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    public Class<Filter> returnedClass() {
        return Filter.class;
    }

    private static final int[] SQLTYPES = new int[] { Types.VARCHAR};
    public int[] sqlTypes() {
        return SQLTYPES;
    }

}
