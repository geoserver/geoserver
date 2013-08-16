/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.passwd;

import org.geoserver.security.password.URLMasterPasswordProvider;
import org.geoserver.security.password.URLMasterPasswordProviderConfig;

public class URLMasterPasswordProviderPanelInfo 
    extends MasterPasswordProviderPanelInfo<URLMasterPasswordProviderConfig, URLMasterPasswordProviderPanel>{

     public URLMasterPasswordProviderPanelInfo() {
         setServiceClass(URLMasterPasswordProvider.class);
         setServiceConfigClass(URLMasterPasswordProviderConfig.class);
         setComponentClass(URLMasterPasswordProviderPanel.class);
     }
}
