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


import java.util.List;

/**
 * Node representing an AND connector in a filter operation
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 517453 $
 */
public class AndNode extends BranchNode
{
    /**
     * Creates a AndNode using a logical operator and a list of children.
     * 
     * @param childList the child nodes under this branch node.
     */
    public AndNode( List<ExprNode> childList )
    {
        super( AssertionType.AND, childList );
    }

    /**
     * Creates a AndNode using a logical operator and a list of children.
     * 
     * @param childList the child nodes under this branch node.
     */
    public AndNode( ExprNode... childList )
    {
        super( AssertionType.AND, childList );
    }

    
    /**
     * Clone the AndNode
     */
    @Override public ExprNode clone()
    {
        return super.clone();
    }


    /**
     * Creates an empty AndNode
     */
    public AndNode()
    {
        super( AssertionType.AND );
    }


    /**
     * Gets the operator for this branch node.
     * 
     * @return the operator constant.
     */
    public AssertionType getOperator()
    {
        return AssertionType.AND;
    }


    /**
     * Tests whether or not this node is a disjunction (a OR'ed branch).
     * 
     * @return true if the operation is a OR, false otherwise.
     */
    public boolean isDisjunction()
    {
        return false;
    }


    /**
     * Tests whether or not this node is a conjunction (a AND'ed branch).
     * 
     * @return true if the operation is a AND, false otherwise.
     */
    public boolean isConjunction()
    {
        return true;
    }


    /**
     * Tests whether or not this node is a negation (a NOT'ed branch).
     * 
     * @return true if the operation is a NOT, false otherwise.
     */
    public boolean isNegation()
    {
        return false;
    }

    
    /**
     * @see ExprNode#printRefinementToBuffer(StringBuffer)
     * 
     * @param buf the buffer to append to.
     * @return The buffer in which the refinement has been appended
     * @throws UnsupportedOperationException if this node isn't a part of a refinement.
     */
    public StringBuilder printRefinementToBuffer( StringBuilder buf )
    {
        buf.append( "and: {" );
        boolean isFirst = true;
        
        for ( ExprNode node:children )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                buf.append( ", " );
            }
            
            node.printRefinementToBuffer( buf );
        }
        
        buf.append( '}' );
        
        return buf;
    }

    /**
     * Gets the recursive prefix string represent of the filter from this node
     * down.
     * 
     * @see java.lang.Object#toString()
     * @return A string representing the AndNode
     */
    public String toString()
    {
        StringBuffer buf = new StringBuffer();
        buf.append( "(&" );

        buf.append( super.toString() );

        for ( ExprNode child:getChildren() )
        {
            buf.append( child );
        }
        
        buf.append( ')' );

        return buf.toString();
    }


    /**
     * @see Object#hashCode()
     * @return the instance's hash code 
     */
    public int hashCode()
    {
        int hash = 37;
        hash = hash*17 + AssertionType.AND.hashCode();
        hash = hash*17 + ( annotations == null ? 0 : annotations.hashCode() );
        return hash;
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

        if ( !( other instanceof AndNode ) )
        {
            return false;
        }

        AndNode otherExprNode = ( AndNode ) other;

        List<ExprNode> otherChildren = otherExprNode.getChildren();

        if ( otherChildren == children )
        {
            return true;
        }

        if ( children.size() != otherChildren.size() )
        {
            return false;
        }
        
        for ( int i = 0; i < children.size(); i++ )
        {
            ExprNode child = children.get( i );
            ExprNode otherChild = otherChildren.get( i );
            
            if ( !child.equals( otherChild ) )
            {
                return false;
            }
        }
        
        return true;
    }
}
