/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.hib;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.sql.Blob;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import org.geoserver.platform.GeoServerExtensions;
import org.hibernate.Hibernate;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

public class ErrorUserType implements UserType {

    /**
     * flag that determines if we should use the hibernate BlobImpl class when writing to the
     * database, since it does not work with oracle.
     *
     * <p>http://opensource.atlassian.com/projects/hibernate/browse/EJB-24
     */
    public static String USE_HIBERNATE_BLOB = "USE_HIBERNATE_BLOB";

    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Serializable disassemble(Object value) throws HibernateException {
        return (Throwable) value;
    }

    public boolean equals(Object x, Object y) throws HibernateException {
        return x == y;
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
            throws HibernateException, SQLException {
        Blob blob = rs.getBlob(names[0]);
        if (blob == null) {
            return null;
        }
        //        byte[] bytes = rs.getBytes(names[0]);
        //        if (bytes == null) {
        //            return null;
        //        }

        ObjectInputStream in = null;
        try {
            // in = new ObjectInputStream(new ByteArrayInputStream(bytes));
            in = new ObjectInputStream(blob.getBinaryStream());
            return in.readObject();
        } catch (IOException e) {
            throw new HibernateException(e);
        } catch (ClassNotFoundException e) {
            throw new HibernateException(e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                }
            }
        }
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index)
            throws HibernateException, SQLException {
        if (value != null) {
            try {
                ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ObjectOutputStream out = new ObjectOutputStream(bytes);
                out.writeObject(value);
                out.flush();

                if (useHibernateBlob()) {
                    st.setBlob(index, Hibernate.createBlob(bytes.toByteArray()));
                } else {
                    st.setBytes(index, bytes.toByteArray());
                }

                out.close();
            } catch (IOException e) {
                throw new HibernateException(e);
            }
        } else {
            st.setNull(index, Types.BLOB);
            // st.setBytes(index, null);
        }
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return target;
    }

    public Class returnedClass() {
        return Throwable.class;
    }

    public int[] sqlTypes() {
        return new int[] {Types.BLOB};
    }

    boolean useHibernateBlob() {
        String prop = GeoServerExtensions.getProperty(USE_HIBERNATE_BLOB);
        return !("no".equals(prop) || "false".equals(prop));
    }
}
