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


/**
 * Abstract base class for leaf nodes within the expression filter tree.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 746607 $
 */
public class LeafNode extends AbstractExprNode
{
    /** attribute on which this leaf is based */
    private String attribute;


    /**
     * Creates a leaf node.
     * 
     * @param attribute the attribute this node is based on
     * @param assertionType the type of this leaf node
     */
    protected LeafNode( String attribute, AssertionType assertionType )
    {
        super( assertionType );
        this.attribute = attribute;
    }
    
    /**
     * Makes a full clone in new memory space of the current node and children
     * 
     * @return the clone
     */
    @Override public ExprNode clone()
    {
        return super.clone();
    }
    

    /**
     * Gets whether this node is a leaf - the answer is always true here.
     * 
     * @return true always
     */
    public final boolean isLeaf()
    {
        return true;
    }


    /**
     * Gets the attribute this leaf node is based on.
     * 
     * @return the attribute asserted
     */
    public final String getAttribute()
    {
        return attribute;
    }
    
    
    /**
     * Sets the attribute this leaf node is based on.
     * 
     * @param attribute the attribute that is asserted by this filter node
     */
    public void setAttribute( String attribute )
    {
        this.attribute = attribute;
    }

    
    /**
     * @see org.apache.directory.shared.ldap.filter.ExprNode#accept(
     *      org.apache.directory.shared.ldap.filter.FilterVisitor)
     * 
     * @param visitor the filter expression tree structure visitor
     * @return The modified element
     */
    public final Object accept( FilterVisitor visitor )
    {
        if ( visitor.canVisit( this ) )
        {
            return visitor.visit( this );
        }
        else
        {
            return null;
        }
    }


    /**
     * @see Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int h = 37;
        
        h = h*17 + super.hashCode();
        h = h*17 + attribute.hashCode();
        
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

        if ( !( other instanceof LeafNode ) )
        {
            return false;
        }

        //noinspection SimplifiableIfStatement
        if ( other.getClass() != this.getClass() )
        {
            return false;
        }
            
        return attribute.equals( ( ( LeafNode ) other ).getAttribute() );
    }
}
