/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.hibernate.types;

import java.io.Serializable;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.usertype.UserType;

/**
* A userType for mapping enum costants.
*
* @author ETj <etj at geo-solutions.it>
*/
public class EnumUserType<E extends Enum<E>> implements UserType {

   private Class<E> clazz = null;
   protected EnumUserType(Class<E> c) {
       this.clazz = c;
   }

   private static final int[] SQL_TYPES = {Types.VARCHAR};
   public int[] sqlTypes() {
       return SQL_TYPES;
   }

   public Class returnedClass() {
       return clazz;
   }

   public Object nullSafeGet(ResultSet resultSet, String[] names, Object owner) throws HibernateException, SQLException {
       String name = resultSet.getString(names[0]);
       E result = null;
       if (!resultSet.wasNull()) {
  	   result = Enum.valueOf(clazz, name);
       }
       return result;
   }

   public void nullSafeSet(PreparedStatement preparedStatement, Object value, int index) throws HibernateException, SQLException {
       if (null == value) {
  	   preparedStatement.setNull(index, Types.VARCHAR);
       } else {
  	   preparedStatement.setString(index, ((Enum)value).name());
       }
   }

   public Object deepCopy(Object value) throws HibernateException{
       return value;
   }

   public boolean isMutable() {
       return false;
   }

   public Object assemble(Serializable cached, Object owner) throws HibernateException {
  	return cached;
   }
   public Serializable disassemble(Object value) throws HibernateException {
       return (Serializable)value;
   }

   public Object replace(Object original, Object target, Object owner) throws HibernateException {
       return original;
   }
   public int hashCode(Object x) throws HibernateException {
       return x.hashCode();
   }
   public boolean equals(Object x, Object y) throws HibernateException {
       if (x == y)
  	   return true;
       if (null == x || null == y)
  	   return false;
       return x.equals(y);
   }
}
