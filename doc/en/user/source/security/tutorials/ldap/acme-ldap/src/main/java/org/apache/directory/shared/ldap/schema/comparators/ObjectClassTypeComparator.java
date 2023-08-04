/**
 * 
 */
package org.apache.directory.shared.ldap.schema.comparators;


import org.apache.directory.shared.ldap.schema.LdapComparator;
import org.apache.directory.shared.ldap.util.StringTools;


/**
 * A comparator that compares the objectClass type with values: AUXILIARY,
 * ABSTRACT, and STRUCTURAL.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev$
 */
public class ObjectClassTypeComparator<T> extends LdapComparator<T> 
{
    private static final long serialVersionUID = 1L;

    
    public ObjectClassTypeComparator( String oid )
    {
        super( oid );
    }
    
    public int compare( T o1, T o2 )
    {
        String s1 = getString( o1 );
        String s2 = getString( o2 );
        
        if ( s1 == null && s2 == null )
        {
            return 0;
        }
        
        if ( s1 == null )
        {
            return -1;
        }
        
        if ( s2 == null )
        {
            return 1;
        }
        
        return s1.compareTo( s2 );
    }
    
    
    String getString( T obj )
    {
        String strValue;

        if ( obj == null )
        {
            return null;
        }
        
        if ( obj instanceof String )
        {
            strValue = ( String ) obj;
        }
        else if ( obj instanceof byte[] )
        {
            strValue = StringTools.utf8ToString( ( byte[] ) obj ); 
        }
        else
        {
            strValue = obj.toString();
        }

        return strValue;
    }
}
