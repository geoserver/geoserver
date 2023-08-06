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
package org.apache.directory.shared.ldap.util.tree;


import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.exception.LdapException;
import org.apache.directory.shared.ldap.exception.LdapUnwillingToPerformException;
import org.apache.directory.shared.ldap.message.ResultCodeEnum;
import org.apache.directory.shared.ldap.name.DN;
import org.apache.directory.shared.ldap.name.RDN;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * 
 * The Hierarchical Container holds elements ordered by their DN. 
 * <br/>
 * We can see them as directories, where the leaves are the files.
 * <br/>
 * This class is *not* thread safe
 *
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 */
public class DnBranchNode<N> implements DnNode<N>
{
    /** The logger for this class */
    private static final Logger LOG = LoggerFactory.getLogger( DnBranchNode.class );

    /** Stores the list of all the descendant */
    private Map<String, DnNode<N>> children;
    
    /** Stores the number of descendents */
    private int size;
    
    /**
     * Creates a new instance of a DnBranchNode.
     */
    public DnBranchNode()
    {
        children = new HashMap<String, DnNode<N>>(3);
        size = 0;
    }

    
    /**
     * @see DnNode#isLeaf()
     */
    public boolean isLeaf()
    {
        return false;
    }
    
    
    /**
     * Recursively adds new nodes to the element lookup tree data structure.  
     * When called it will add an element to the tree in the appropriate leaf 
     * node position based on the DN passed in as an argument.
     *
     * @param current The current node having an element added to it
     * @param dn The DN associated with the added element
     * @param index The index of the current RDN being processed 
     * @param element The associated element to add as a tree node
     * @return The modified tree structure.
     */
    private DnNode<N> recursivelyAddElement( DnBranchNode<N> current, DN dn, int index, N element ) throws LdapException
    {
        String rdnAtIndex = dn.getRdn( index ).getNormName();
        
        if ( index == dn.size() - 1 )
        {
            if ( !current.contains( rdnAtIndex ) )
            {
                return current.addNode( rdnAtIndex, new DnLeafNode<N>( element ) );
            }
            else
            {
                return null;
            }
        }
        else
        {
            DnNode<N> newNode = ((DnBranchNode<N>)current).getChild( rdnAtIndex );
            
            if ( newNode instanceof DnLeafNode )
            {
                String message = I18n.err( I18n.ERR_04334 );
                LOG.error( message );
                throw new LdapUnwillingToPerformException( ResultCodeEnum.UNWILLING_TO_PERFORM, message );
            }
        
            if ( newNode == null )
            {
                newNode = new DnBranchNode<N>();
            }

            DnNode<N> child = recursivelyAddElement( (DnBranchNode<N>)newNode, dn, index + 1, element );
            
            if ( child != null )
            {
                return current.addNode( rdnAtIndex, child );
            }
            else
            {
                return null;
            }
        }
    }
    
    
    /**
     * Directly adds a new child DnNode to the current DnBranchNode.
     *
     * @param rdn The rdn of the child node to add 
     * @param child The child node to add
     * @return The modified branch node after the insertion
     */
    public DnNode<N> addNode( String rdn, DnNode<N> child )
    {
        children.put( rdn, child );
        size++;
        return this;
    }
    
    
    /**
     * Tells if the current DnBranchNode contains another node associated 
     * with an rdn.
     *
     * @param rdn The name we are looking for
     * @return <code>true</code> if the tree instance contains this name
     */
    public boolean contains( String rdn )
    {
        return children.containsKey( rdn );
    }

    
    /**
     * Get's a child using an rdn string.
     * 
     * @param rdn the rdn to use as the node key
     * @return the child node corresponding to the rdn.
     */
    public DnNode<N> getChild( String rdn )
    {
        if ( children.containsKey( rdn ) )
        {
            return children.get( rdn );
        }

        return null;
    }
    
    
    /**
     * Get the parent of a given DN, if present in the tree. This parent should be a 
     * subset of the given dn.<br>
     * For instance, if we have stored dc=acme, dc=org into the tree, 
     * the DN: ou=example, dc=acme, dc=org will have a parent, and 
     * dc=acme, dc=org will be returned.
     * <br>For the DN ou=apache, dc=org, there is no parent, so null will be returned.
     *  
     *
     * @param dn the normalized distinguished name to resolve to a parent
     * @return the parent associated with the normalized dn
     */
    public N getParentElement( DN dn )
    {
        List<RDN> rdns = dn.getRdns();
        
        // This is synchronized so that we can't read the
        // partitionList when it is modified.
        synchronized ( this )
        {
            DnNode<N> currentNode = this;

            // Iterate through all the RDN until we find the associated partition
            for ( int i = rdns.size() - 1; i >= 0; i-- )
            {
                String rdnStr = rdns.get( i ).getNormName();

                if ( currentNode == null )
                {
                    break;
                }

                if ( currentNode instanceof DnLeafNode )
                {
                    return ( ( DnLeafNode<N> ) currentNode ).getElement();
                }

                DnBranchNode<N> currentBranch = ( DnBranchNode<N> ) currentNode;
                
                if ( currentBranch.contains( rdnStr ) )
                {
                    currentNode = currentBranch.getChild( rdnStr );
                    
                    if ( currentNode instanceof DnLeafNode )
                    {
                        return ( ( DnLeafNode<N> ) currentNode ).getElement();
                    }
                }
            }
        }
        
        return null;
    }

    
    /**
     * Tells if the DN contains a parent in the tree. This parent should be a 
     * subset of the given dn.<br>
     * For instance, if we have stored dc=acme, dc=org into the tree, 
     * the DN: ou=example, dc=acme, dc=org will have a parent. 
     *
     * @param dn the normalized distinguished name to resolve to a parent
     * @return the parent associated with the normalized dn
     */
    public boolean hasParentElement( DN dn )
    {
        List<RDN> rdns = dn.getRdns();
        
        // This is synchronized so that we can't read the
        // partitionList when it is modified.
        synchronized ( this )
        {
            DnNode<N> currentNode = this;

            // Iterate through all the RDN until we find the associated partition
            for ( int i = rdns.size() - 1; i >= 0; i-- )
            {
                String rdnStr = rdns.get( i ).getNormName();

                if ( currentNode == null )
                {
                    return false;
                }

                if ( currentNode instanceof DnLeafNode )
                {
                    return true;
                }

                DnBranchNode<N> currentBranch = ( DnBranchNode<N> ) currentNode;
                
                if ( currentBranch.contains( rdnStr ) )
                {
                    currentNode = currentBranch.getChild( rdnStr );
                    
                    if ( currentNode instanceof DnLeafNode )
                    {
                        return true;
                    }
                }
            }
        }
        
        return false;
    }
    
    
    /**
     * Tells if a branchNode has some children or not
     *
     * @return <code>true</code> if the node has some children
     */
    public boolean hasChildren()
    {
        return children.size() != 0;
    }
    
    
    /**
     * Removes an element from the tree.
     *
     * @param element The element to remove
     */
    private boolean recursivelyRemoveElement( DnBranchNode<N> currentNode, N element )
    {
        // It might be a leaf
        for ( String key: currentNode.children.keySet() )
        {
            DnNode<N> child = currentNode.children.get( key );
            
            if ( child instanceof DnLeafNode )
            {
                if ( ((DnLeafNode<N>)child).getElement().equals( element ) )
                {
                    // found ! Remove it from the children
                    currentNode.children.remove( key );
                    currentNode.size--;
                    return true;
                }
            }
            else
            {
                if ( recursivelyRemoveElement( (DnBranchNode<N>)child, element ) )
                {
                    if ( ((DnBranchNode<N>)child).children.size() == 0 )
                    {
                        // If there are no more children, we can remove the node
                        currentNode.children.remove( key );
                        currentNode.size--;
                    }
                    else
                    {
                        currentNode.size--;
                    }

                    return true;
                }
            }
        }
        
        
        return false;
    }

    
    /**
     * 
     * TODO add.
     *
     * @param dn
     * @param element
     * @throws NamingException
     */
    public void add( DN dn, N element ) throws LdapException
    {
        recursivelyAddElement( this, dn, 0, element );
    }
    
    
    /**
     * Removes an element from the tree.
     *
     * @param element The element to remove
     */
    public void remove( N element )
    {
        DnBranchNode<N> currentNode = this;
        
        if ( currentNode.hasChildren() )
        {
            recursivelyRemoveElement( currentNode, element );
        }
    }
    
    
    /**
     * {@inheritDoc}
     */
    public int size()
    {
        return size;
    }


    /**
     * @see Object#toString()
     */
    public String toString()
    {
        StringBuilder sb = new StringBuilder();
        sb.append( "{" );
        boolean isFirst = true;
        
        for ( String key:children.keySet() )
        {
            if ( isFirst )
            {
                isFirst = false;
            }
            else
            {
                sb.append(  ", " );
            }

            DnNode<N> child = children.get( key );
            
            if ( child instanceof DnBranchNode )
            {
                sb.append( "Branch[" ).append( key ).append( "]: ").append( child );
            }
            else
            {
                sb.append( "Leaf: " ).append( "'" ).append( child ).append( "'" );
            }
        }

        sb.append( "}" );
        return sb.toString();
    }
}
