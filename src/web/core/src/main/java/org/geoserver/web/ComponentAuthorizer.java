/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;
import org.springframework.security.core.Authentication;

/**
 * Controls access to a component.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public interface ComponentAuthorizer extends Serializable {

    /** authorizer that always allows access to the component */
    static ComponentAuthorizer ALLOW = new AllowComponentAuthorizer();

    /** authorizer that grants access if the user has admin credentials */
    static ComponentAuthorizer ADMIN = new AdminComponentAuthorizer();

    /** authorizer that grants access if the user has workspace admin credentials */
    static ComponentAuthorizer WORKSPACE_ADMIN = new WorkspaceAdminComponentAuthorizer();

    /** authorizer that grants access if the user has authenticated */
    static ComponentAuthorizer AUTHENTICATED = new AuthenticatedComponentAuthorizer();

    /** Determines if access is allowed to the component given the specified credentials. */
    boolean isAccessAllowed(Class<?> componentClass, Authentication authentication);
}
