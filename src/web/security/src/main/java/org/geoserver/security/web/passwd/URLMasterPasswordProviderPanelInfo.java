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
