/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.geoserver.security.config.SecurityAuthFilterConfig;
import org.geoserver.security.web.SecurityNamedServicePanelInfo;

/**
 * Extension point for authentication filter configuration panels.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AuthenticationFilterPanelInfo<
                C extends SecurityAuthFilterConfig, T extends AuthenticationFilterPanel<C>>
        extends SecurityNamedServicePanelInfo<C, T> {}
