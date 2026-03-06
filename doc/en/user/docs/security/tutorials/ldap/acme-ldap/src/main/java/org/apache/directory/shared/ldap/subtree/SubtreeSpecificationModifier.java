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
package org.apache.directory.shared.ldap.subtree;


import org.apache.directory.shared.i18n.I18n;
import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.DN;

import java.util.Set;
import java.util.Collections;


/**
 * SubtreeSpecification contains no setters so they must be built by a
 * modifiable object containing all the necessary parameters to build the base
 * object.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 919765 $
 */
public class SubtreeSpecificationModifier
{
    /** the subtree base relative to the administration point */
    private DN base = new DN();

    /** the set of subordinates entries and their subordinates to exclude */
    private Set<DN> chopBefore = Collections.EMPTY_SET;

    /** the set of subordinates entries whose subordinates are to be excluded */
    private Set<DN> chopAfter = Collections.EMPTY_SET;

    /** the minimum distance below base to start including entries */
    private int minBaseDistance = 0;

    /** the maximum distance from base past which entries are excluded */
    private int maxBaseDistance = SubtreeSpecification.UNBOUNDED_MAX;

    /**
     * a filter using only assertions on objectClass attributes for subtree
     * refinement
     */
    private ExprNode refinement = null;


    // -----------------------------------------------------------------------
    // F A C T O R Y M E T H O D
    // -----------------------------------------------------------------------

    /**
     * Creates a SubtreeSpecification using any of the default paramters that
     * may have been modified from their defaults.
     * 
     * @return the newly created subtree specification
     */
    public SubtreeSpecification getSubtreeSpecification()
    {

        return new BaseSubtreeSpecification( this.base, this.minBaseDistance, this.maxBaseDistance, this.chopAfter,
            this.chopBefore, this.refinement );
    }


    // -----------------------------------------------------------------------
    // M U T A T O R S
    // -----------------------------------------------------------------------

    /**
     * Sets the subtree base relative to the administration point.
     * 
     * @param base
     *            subtree base relative to the administration point
     */
    public void setBase( DN base )
    {
        this.base = base;
    }


    /**
     * Sets the set of subordinates entries and their subordinates to exclude.
     * 
     * @param chopBefore
     *            the set of subordinates entries and their subordinates to
     *            exclude
     */
    public void setChopBeforeExclusions( Set<DN> chopBefore )
    {
        this.chopBefore = chopBefore;
    }


    /**
     * Sets the set of subordinates entries whose subordinates are to be
     * excluded.
     * 
     * @param chopAfter
     *            the set of subordinates entries whose subordinates are to be
     *            excluded
     */
    public void setChopAfterExclusions( Set<DN> chopAfter )
    {
        this.chopAfter = chopAfter;
    }


    /**
     * Sets the minimum distance below base to start including entries.
     * 
     * @param minBaseDistance
     *            the minimum distance below base to start including entries
     */
    public void setMinBaseDistance( int minBaseDistance )
    {
        if ( minBaseDistance < 0 )
        {
            throw new IllegalArgumentException( I18n.err( I18n.ERR_04330 ) );
        }

        this.minBaseDistance = minBaseDistance;
    }


    /**
     * Sets the maximum distance from base past which entries are excluded.
     * 
     * @param maxBaseDistance
     *            the maximum distance from base past which entries are excluded
     */
    public void setMaxBaseDistance( int maxBaseDistance )
    {
        if ( maxBaseDistance < 0 )
        {
            this.maxBaseDistance = SubtreeSpecification.UNBOUNDED_MAX;
        }
        else
        {
            this.maxBaseDistance = maxBaseDistance;
        }
    }


    /**
     * Sets a filter using only assertions on objectClass attributes for subtree
     * refinement.
     * 
     * @param refinement
     *            a filter using only assertions on objectClass attributes for
     *            subtree refinement
     */
    public void setRefinement( ExprNode refinement )
    {
        this.refinement = refinement;
    }
}
