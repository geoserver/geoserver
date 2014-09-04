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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geotools.util.Utilities;
import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
 * Hibernate user type for {@link KeywordInfo}.
 */
public class KeywordInfoType implements UserType {

    @Override
    public Object assemble(Serializable cached, Object owner) throws HibernateException {
        return cached;
    }

    @Override
    public Object deepCopy(Object value) throws HibernateException {
        return value;
    }

    @Override
    public Serializable disassemble(Object value) throws HibernateException {
        return (Serializable) value;
    }

    @Override
    public boolean equals(Object x, Object y) throws HibernateException {
        return Utilities.equals(x, y);
    }

    @Override
    public int hashCode(Object x) throws HibernateException {
        return Utilities.deepHashCode(x);
    }

    @Override
    public boolean isMutable() {
        return false;
    }

    @Override
    public Object nullSafeGet(ResultSet rs, String[] names, Object owner)
            throws HibernateException, SQLException {

        String srsNameStyle = rs.getString(names[0]);
        KeywordInfo kwInfo = srsNameStyle == null ? null : fromString(srsNameStyle);
        return kwInfo;
    }

    @Override
    public void nullSafeSet(PreparedStatement st, Object value, int index)
            throws HibernateException, SQLException {

        KeywordInfo kwInfo = (KeywordInfo) value;

        if (kwInfo == null) {
            st.setNull(index, Types.VARCHAR);
        } else {
            String storedValue = toString(kwInfo);
            st.setString(index, storedValue);
        }
    }

    @Override
    public Object replace(Object original, Object target, Object owner) throws HibernateException {
        return original;
    }

    @Override
    public Class<?> returnedClass() {
        return Keyword.class;
    }

    private static final int[] SQLTYPES = new int[] { Types.VARCHAR };

    public int[] sqlTypes() {
        return SQLTYPES;
    }

    private static final Pattern RE = Pattern
            .compile("([^\\\\]+)(?:\\\\@language=([^\\\\]+)\\\\;)?(?:\\\\@vocabulary=([^\\\\]+)\\\\;)?");

    public KeywordInfo fromString(String str) {
        Matcher m = RE.matcher(str);
        if (!m.matches()) {
            throw new IllegalArgumentException(String.format(
                    "%s does not match regular expression: %s", str, RE));
        }

        KeywordInfo kw = new Keyword(m.group(1));
        if (m.group(2) != null) {
            kw.setLanguage(m.group(2));
        }
        if (m.group(3) != null) {
            kw.setVocabulary(m.group(3));
        }
        return kw;
    }

    public String toString(KeywordInfo kw) {
        StringBuilder sb = new StringBuilder();
        sb.append(kw.getValue());
        if (kw.getLanguage() != null) {
            sb.append("\\@language=").append(kw.getLanguage()).append("\\;");
        }
        if (kw.getVocabulary() != null) {
            sb.append("\\@vocabulary=").append(kw.getVocabulary()).append("\\;");
        }
        return sb.toString();
    }
}
