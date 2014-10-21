/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.xacml.role;

import org.springframework.security.Authentication;
import org.springframework.security.userdetails.UserDetails;

/**
 * A RoleAssignmentAuthority is NOT responsible for assignment from roles to subjects
 * 
 * 
 * The purpose of this Authority is
 * 
 * 1) add needed role parameters 2) check against the XACML repository if the role is enabled (e.g
 * the role is enabled only between 8:00 and 16:00)
 * 
 * Some important notes about the XACML RBAC role specification
 * 
 * 1) Each role has a "role permission set". Roles for themselves are not hierarchical, but the
 * permission sets are. Permission sets can also use multiple inheritance.
 * 
 * 2) According to 1) if a parent role is disabled (e.g time constraints), the current role is not
 * 
 * 3) According to 1) role parameters are not inherited, you have to specify the whole set of role
 * parameters for each role
 * 
 * @author Christian Mueller
 * 
 */
public interface XACMLRoleAuthority {

    public void prepareRoles(Authentication auth);

    public <T extends UserDetails> void transformUserDetails(T details);

}
