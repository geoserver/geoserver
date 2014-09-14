/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.hib.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Properties;

import org.geotools.referencing.CRS;
import org.hibernate.HibernateException;
import org.hibernate.usertype.ParameterizedType;
import org.hibernate.usertype.UserType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class CRSType implements UserType, ParameterizedType {

    boolean storeAsWKT = false;
    
    public void setParameterValues(Properties parameters) {
        storeAsWKT = "true".equals(parameters.getProperty("storeAsWKT"));
    }
    
    public Class returnedClass() {
        return CoordinateReferenceSystem.class;
    }

    public int[] sqlTypes() {
        return new int[]{Types.VARCHAR};
    }
    
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }
    
    public Serializable disassemble(Object value) throws HibernateException {
        return null;
    }

    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }
        
    public boolean equals(Object x, Object y) throws HibernateException {
        return CRS.equalsIgnoreMetadata(x, y);
    }

    public int hashCode(Object x) throws HibernateException {
        return x.hashCode();
    }

    public boolean isMutable() {
        return false;
    }

    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
            throws HibernateException, SQLException {
        String s = rs.getString(names[0]);
        if (s == null) {
            return null;
        }
        
        try {
            if (storeAsWKT) {
                return CRS.parseWKT(s);
            }
            else {
                return CRS.decode(s);
            }
        }
        catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void nullSafeSet(PreparedStatement st, Object value, int index)
            throws HibernateException, SQLException {
        
        if (value == null) {
            st.setNull(index, Types.VARCHAR);
        }
        else {
            CoordinateReferenceSystem crs = (CoordinateReferenceSystem) value;
            try {
                st.setString(index, storeAsWKT ? crs.toWKT() : "EPSG:"+CRS.lookupEpsgCode(crs, true));
            } 
            catch(Exception e) {
                throw new RuntimeException(e);
            }
        }
        
    }

}
