/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *  
 *    http://www.apache.org/licenses/LICENSE-2.0
 *  
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License. 
 *  
 */
package org.apache.directory.shared.ldap.filter;


import java.util.HashMap;
import java.util.Map;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.entry.BinaryValue;
import org.apache.directory.shared.ldap.entry.StringValue;
import org.apache.directory.shared.ldap.entry.Value;


/**
 * Abstract implementation of a expression node.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 928945 $
 */
public abstract class AbstractExprNode implements ExprNode
{
    /** The map of annotations */
    protected Map<String, Object> annotations;

    /** The node type */
    protected final AssertionType assertionType;
    
    
    /**
     * Creates a node by setting abstract node type.
     * 
     * @param assertionType The node's type
     */
    protected AbstractExprNode( AssertionType assertionType )
    {
        this.assertionType = assertionType;
    }


    /**
     * @see ExprNode#getAssertionType()
     * 
     * @return the node's type
     */
    public AssertionType getAssertionType()
    {
        return assertionType;
    }


    /**
     * Tests to see if this node is a leaf or branch node.
     * 
     * @return true if the node is a leaf,false otherwise
     */
    public abstract boolean isLeaf();

    
    /**
     * @see Object#equals(Object)
     *@return <code>true</code> if both objects are equal 
     */
    public boolean equals( Object o )
    {
        // Shortcut for equals object
        if ( this == o )
        {
            return true;
        }
        
        if ( !( o instanceof AbstractExprNode ) )
        {
            return false;
        }
        
        AbstractExprNode that = (AbstractExprNode)o;
        
        // Check the node type
        if ( this.assertionType != that.assertionType )
        {
            return false;
        }
        
        if ( annotations == null )
        {
            return that.annotations == null;
        }
        else if ( that.annotations == null )
        {
            return false;
        }
        
        // Check all the annotation
        for ( String key:annotations.keySet() )
        {
            if ( !that.annotations.containsKey( key ) )
            {
                return false;
            }
            
            Object thisAnnotation = annotations.get( key ); 
            Object thatAnnotation = that.annotations.get( key );
            
            if ( thisAnnotation == null )
            {
                if ( thatAnnotation != null )
                {
                    return false;
                }
            }
            else
            {
                if ( !thisAnnotation.equals( thatAnnotation ) )
                {
                    return false;
                }
            }
        }
        
        return true;
    }


    /**
     * Handles the escaping of special characters in LDAP search filter assertion values using the
     * &lt;valueencoding&gt; rule as described in
     * <a href="http://www.ietf.org/rfc/rfc4515.txt">RFC 4515</a>. Needed so that
     * {@link ExprNode#printToBuffer(StringBuffer)} results in a valid filter string that can be parsed
     * again (as a way of cloning filters).
     *
     * @param value Right hand side of "attrId=value" assertion occurring in an LDAP search filter.
     * @return Escaped version of <code>value</code>
     */
    protected static Value<?> escapeFilterValue( Value<?> value )
    {
        StringBuilder sb = null;
        String val;

        if ( value.isBinary() )
        {
            sb = new StringBuilder( ((BinaryValue)value).getReference().length * 3 );
            
            for ( byte b:((BinaryValue)value).getReference() )
            {
                if ( ( b < 0x7F ) && ( b >= 0 ) )
                {
                    switch ( b )
                    {
                        case '*' :
                            sb.append( "\\2A" );
                            break;
                            
                        case '(' :
                            sb.append( "\\28" );
                            break;
                            
                        case ')' :
                            sb.append( "\\29" );
                            break;
                            
                        case '\\' :
                            sb.append( "\\5C" );
                            break;
                            
                        case '\0' :
                            sb.append( "\\00" );
                            break;
                            
                        default :
                            sb.append( (char)b );
                    }
                }
                else
                {
                    sb.append( '\\' );
                    String digit = Integer.toHexString( ((byte)b) & 0x00FF );
                    
                    if ( digit.length() == 1 )
                    {
                        sb.append( '0' );
                    }

                    sb.append( digit.toUpperCase() );
                }
            }
            
            return new StringValue( sb.toString() );
        }

        val = ( ( StringValue ) value ).getString();
        
        for ( int i = 0; i < val.length(); i++ )
        {
            char ch = val.charAt( i );
            String replace = null;

            switch ( ch )
            {
                case '*':
                    replace = "\\2A";
                    break;
                    
                case '(':
                    replace = "\\28";
                    break;
                    
                case ')':
                    replace = "\\29";
                    break;
                    
                case '\\':
                    replace = "\\5C";
                    break;
                    
                case '\0':
                    replace = "\\00";
                    break;
            }
            
            if ( replace != null )
            {
                if ( sb == null )
                {
                    sb = new StringBuilder( val.length() * 2 );
                    sb.append( val.substring( 0, i ) );
                }
                sb.append( replace );
            }
            else if ( sb != null )
            {
                sb.append( ch );
            }
        }

        return ( sb == null ? value : new StringValue( sb.toString() ) );
    }


    /**
     * @see Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int h = 37;
        
        if ( annotations != null )
        {
            for ( String key:annotations.keySet() )
            {
                Object value = annotations.get( key );
                
                h = h*17 + key.hashCode();
                h = h*17 + ( value == null ? 0 : value.hashCode() );
            }
        }
        
        return h;
    }
    

    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#get(java.lang.Object)
     * 
     * @return the annotation value.
     */
    public Object get( Object key )
    {
        if ( null == annotations )
        {
            return null;
        }

        return annotations.get( key );
    }


    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#set(java.lang.Object,
     *      java.lang.Object)
     */
    public void set( String key, Object value )
    {
        if ( null == annotations )
        {
            annotations = new HashMap<String, Object>( 2 );
        }

        annotations.put( key, value );
    }


    /**
     * Gets the annotations as a Map.
     * 
     * @return the annotation map.
     */
    protected Map<String, Object> getAnnotations()
    {
        return annotations;
    }

    /**
     * Default implementation for this method : just throw an exception.
     * 
     * @param buf the buffer to append to.
     * @return The buffer in which the refinement has been appended
     * @throws UnsupportedOperationException if this node isn't a part of a refinement.
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf )
    {
        throw new UnsupportedOperationException( I18n.err( I18n.ERR_04144 ) );
    }
    
    
    /**
     * Clone the object
     */
    @Override public ExprNode clone()
    {
        try
        {
            ExprNode clone = (ExprNode)super.clone();
            
            if ( annotations != null )
            {
                for ( String key:annotations.keySet() )
                {
                    Object value = annotations.get( key );
                    
                    // Note : the value aren't cloned ! 
                    ((AbstractExprNode)clone).annotations.put( key, value );
                }
            }
            
            return clone;
        }
        catch ( CloneNotSupportedException cnse )
        {
            return null;
        }
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        if ( ( null != annotations ) && annotations.containsKey( "count" ) )
        {
            return ":[" + annotations.get( "count" ) + "]";
        }
        else 
        {
            return "";
        }
    }    
}
