/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import org.geoserver.security.password.MasterPasswordProviderConfig;
import org.geoserver.security.web.SecurityNamedServicePanelInfo;

/**
 * Extension point for master password provider configuration panels.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class MasterPasswordProviderPanelInfo<
                C extends MasterPasswordProviderConfig, T extends MasterPasswordProviderPanel<C>>
        extends SecurityNamedServicePanelInfo<C, T> {}
