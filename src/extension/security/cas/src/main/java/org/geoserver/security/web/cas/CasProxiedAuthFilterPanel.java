/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.cas;

import org.apache.wicket.model.IModel;
import org.geoserver.security.cas.CasProxiedAuthenticationFilterConfig;
import org.geoserver.security.cas.GeoServerCasProxiedAuthenticationFilter;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel;

/**
 * Configuration panel for {@link GeoServerCasProxiedAuthenticationFilter}.
 * 
 * @author mcr
 */
public class CasProxiedAuthFilterPanel 
    extends PreAuthenticatedUserNameFilterPanel<CasProxiedAuthenticationFilterConfig>  {

    
    private static final long serialVersionUID = 1;
    

    public CasProxiedAuthFilterPanel(String id, IModel<CasProxiedAuthenticationFilterConfig> model) {
        super(id, model);
        add (new CasConnectionPanel<CasProxiedAuthenticationFilterConfig>("cas",model)) ;
    }

}
