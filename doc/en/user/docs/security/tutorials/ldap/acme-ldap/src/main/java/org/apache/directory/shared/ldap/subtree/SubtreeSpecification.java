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


import org.apache.directory.shared.ldap.filter.ExprNode;
import org.apache.directory.shared.ldap.name.DN;

import java.util.Set;


/**
 * <a href="http://www.faqs.org/rfcs/rfc3672.html">RFC 3672</a> defined a
 * subtree specification to be included within subentries.
 * 
 * @author <a href="mailto:dev@directory.apache.org">Apache Directory Project</a>
 * @version $Rev: 918756 $
 */
public interface SubtreeSpecification
{
    /** an unbounded maximum depth value in a subtree specification */
    int UNBOUNDED_MAX = -1;


    /**
     * Gets an RDN relative to the administrative context where the subtree
     * scope begins. All subentries containing these specifications are
     * immediate subordinates to the administrative point, and are considered to
     * be part of the same naming context. Hence the base for the subtree
     * specification of a subentry immediately subordinate to dc=apache,dc=org
     * would be relative to the dc=apache,dc=org context.
     * 
     * @return the RDN representing the base of the subtree, or the empty name
     *         if the base is the administrative point - note that this Name is
     *         not Normalized according to matchingRules.
     */
    DN getBase();


    /**
     * A set of RDNs relative to the base entry representing chopBefore
     * specificExclusions from the subtree. According to RFC 3672: "If the
     * chopBefore form is used then the specified entry and its subordinates are
     * excluded from the subtree or subtree refinement."
     * 
     * @return a set of relative {@link javax.naming.Name}s to the subtree base
     *         or the empty set
     */
    Set<DN> getChopBeforeExclusions();


    /**
     * A set of RDNs relative to the base entry representing chopAfter
     * specificExclusions from the subtree. According to RFC 3672: "If the
     * chopAfter form is used then only the subordinates of the specified entry
     * are excluded from the subtree or subtree refinement."
     * 
     * @return a set of relative {@link javax.naming.Name}s to the subtree base
     *         or the empty set
     */
    Set<DN> getChopAfterExclusions();


    /**
     * Gets the distance at which to start including entries in the subtree. All
     * entries whose RDN arcs relative to the base are less than the minimum are
     * excluded from the subtree or subtree refinement. The default is zero and
     * therefore excludes nothing.
     * 
     * @return the minimum number of RDN arcs relative to base for inclusion
     */
    int getMinBaseDistance();


    /**
     * Gets the distance after which to start excluding entries in the subtree
     * or subtree refinement. RFC 3672 Section 2.1.3 states: "Entries that are
     * more than the maximum number of RDN arcs below the base entry are
     * excluded from the subtree or subtree refinement. An absent maximum
     * component indicates that there is no upper limit on the number of RDN
     * arcs below the base entry for entries in the subtree or subtree
     * refinement." If the maximum is limitless a negative value should be used
     * to represent the maximum distance - which makes no sense other than to
     * denote the lack of an upper limit.
     * 
     * @see #UNBOUNDED_MAX
     * @return the number of arcs relative to the base after which entries are
     *         excluded
     */
    int getMaxBaseDistance();


    /**
     * A subtree refinement represents a non-contiguous selection of entries
     * using a limited filter expression where attribute assertions are based on
     * the objectClass of the entries.
     * 
     * @return a limited filter expression tree representing a subtree
     *         refinement or null if one does not exist for this subtree
     *         specification
     */
    ExprNode getRefinement();
    
    
    /**
     * Converts this item into its string representation as stored
     * in directory.
     *
     * @param buffer the string buffer
     */
    void printToBuffer( StringBuilder buffer );
    
}
