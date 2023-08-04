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


import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.constants.SchemaConstants;
import org.apache.directory.shared.ldap.entry.Value;


/**
 * A simple assertion value node.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Revision: 912436 $
 */
public abstract class SimpleNode<T> extends LeafNode
{
    /** the value */
    protected Value<T> value;

    /* TODO - why are these here if not used? */
    /** Constants for comparisons : > */
    public static final boolean EVAL_GREATER = true;
    
    /* TODO - why are these here if not used? */
    /** Constants for comparisons : < */
    public static final boolean EVAL_LESSER = false;


    /**
     * Creates a new SimpleNode object.
     * 
     * @param attribute the attribute name
     * @param value the value to test for
     * @param assertionType the type of assertion represented by this ExprNode
     */
    protected SimpleNode( String attribute, Value<T> value, AssertionType assertionType )
    {
        super( attribute, assertionType );
        this.value = value;
    }

    
    /**
     * Clone the Node
     */
    @Override public ExprNode clone()
    {
        ExprNode clone = super.clone();
        
        // Clone the value
        ((SimpleNode<T>)clone).value = value.clone(); 
        
        return clone;
    }


    /**
     * Gets the value.
     * 
     * @return the value
     */
    public final Value<T> getValue()
    {
        return value;
    }

    /** 
     * @return representation of value, escaped for use in a filter if required 
     */
    public Value<?> getEscapedValue()
    {
        return AbstractExprNode.escapeFilterValue( value );
    }


    /**
     * Sets the value of this node.
     * 
     * @param value the value for this node
     */
    public void setValue( Value<T> value )
    {
        this.value = value;
    }


    /**
     * Pretty prints this expression node along with annotation information.
     *
     * TODO - perhaps this belong in some utility class?
     *
     * @param buf the buffer to print into
     * @return the same buf argument returned for call chaining
     */
    public StringBuilder printToBuffer( StringBuilder buf )
    {
        if ( ( null != getAnnotations() ) && getAnnotations().containsKey( "count" ) )
        {
            buf.append( ":[" );
            buf.append( getAnnotations().get( "count" ).toString() );
            buf.append( "] " );
        }

        buf.append( ')' );

        return buf;
    }


    /**
     * @see ExprNode#printRefinementToBuffer(StringBuilder)
     * @return The buffer in which the refinement has been appended
     * @throws UnsupportedOperationException if this node isn't a part of a refinement.
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf )
    {
        if ( getAttribute() == null || !SchemaConstants.OBJECT_CLASS_AT.equalsIgnoreCase( getAttribute() ) )
        {
            throw new UnsupportedOperationException( I18n.err( I18n.ERR_04162, getAttribute() ) );
        }

        buf.append( "item: " ).append( value );

        return buf;
    }


    /**
     * @see Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int h = 37;

        h = h * 17 + super.hashCode();
        h = h * 17 + ( value == null ? 0 : value.hashCode() );

        return h;
    }


    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals( Object other )
    {
        if ( this == other )
        {
            return true;
        }

        if ( !( other instanceof SimpleNode ) )
        {
            return false;
        }

        if ( other.getClass() != this.getClass() )
        {
            return false;
        }

        if ( !super.equals( other ) )
        {
            return false;
        }

        SimpleNode<?> otherNode = ( SimpleNode<?> ) other;

        if ( value == null )
        {
            return otherNode.value == null;
        }
        else
        {
            return value.equals( otherNode.value );
        }
    }
}
