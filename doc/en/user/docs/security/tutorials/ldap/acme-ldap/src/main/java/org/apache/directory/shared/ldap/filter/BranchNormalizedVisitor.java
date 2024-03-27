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


import java.text.ParseException;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;


/**
 * Visitor which traverses a filter tree while normalizing the branch node
 * order. Filter expressions can change the order of expressions in branch nodes
 * without effecting the logical meaning of the expression. This visitor orders
 * the children of expression tree branch nodes consistantly. It is really
 * useful for comparing expression trees which may be altered for performance or
 * altered because of codec idiosyncracies: for example the SNACC4J codec uses a
 * hashmap to store expressions in a sequence which rearranges the order of
 * children based on object hashcodes. We need this visitor to remove such
 * inconsitancies in order hence normalizing the branch node's child order.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 746607 $
 */
public class BranchNormalizedVisitor implements FilterVisitor
{
    public Object visit( ExprNode node )
    {
        if ( !( node instanceof BranchNode ) )
        {
            return null;
        }

        BranchNode branch = ( BranchNode ) node;

        Comparator<ExprNode> nodeComparator = new NodeComparator();

        Set<ExprNode> set = new TreeSet<ExprNode>( nodeComparator );

        List<ExprNode> children = branch.getChildren();

        for ( ExprNode child:branch.getChildren() )
        {
            if ( !child.isLeaf() )
            {
                ExprNode newChild = (ExprNode)visit( child );
                
                if ( newChild != null )
                {
                    set.add( newChild );
                }
            }
            else
            {
                set.add( child );
            }
        }

        children.clear();

        children.addAll( set );
        
        return branch;
    }


    public boolean canVisit( ExprNode node )
    {
        if ( node instanceof BranchNode )
        {
            return true;
        }

        return false;
    }


    public boolean isPrefix()
    {
        return false;
    }


    public List<ExprNode> getOrder( BranchNode node, List<ExprNode> children )
    {
        return children;
    }


    /**
     * Normalizes a filter expression to a canonical representation while
     * retaining logical meaning of the expression.
     * 
     * @param filter
     *            the filter to normalize
     * @return the normalized version of the filter
     * @throws java.text.ParseException
     *             if the filter is malformed
     */
    public static String getNormalizedFilter( String filter ) throws ParseException
    {
        ExprNode originalNode = FilterParser.parse( filter );

        return getNormalizedFilter( originalNode );
    }


    /**
     * Normalizes a filter expression to a canonical representation while
     * retaining logical meaning of the expression.
     * 
     * @param filter
     *            the filter to normalize
     * @return the normalized String version of the filter
     */
    public static String getNormalizedFilter( ExprNode filter )
    {
        BranchNormalizedVisitor visitor = new BranchNormalizedVisitor();

        ExprNode result = (ExprNode)visitor.visit( filter );

        return result.toString().trim();
    }

    class NodeComparator implements Comparator<ExprNode>
    {
        public int compare( ExprNode o1, ExprNode o2 )
        {
            StringBuilder buf = new StringBuilder();

            buf.setLength( 0 );

            String s1 = null;

            buf.append( o1.toString() );

            s1 = buf.toString();

            buf.setLength( 0 );

            String s2 = null;

            buf.append( o2.toString() );

            s2 = buf.toString();

            return s1.compareTo( s2 );
        }
    }
}
