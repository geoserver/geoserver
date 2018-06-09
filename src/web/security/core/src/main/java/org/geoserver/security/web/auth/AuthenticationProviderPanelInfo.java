/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.web.SecurityNamedServicePanelInfo;

/**
 * Extension point for authentication provider configuration panels.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AuthenticationProviderPanelInfo<
                C extends SecurityAuthProviderConfig, T extends AuthenticationProviderPanel<C>>
        extends SecurityNamedServicePanelInfo<C, T> {}
