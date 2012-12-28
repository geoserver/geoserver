package org.geoserver.gwc;

import org.springframework.beans.factory.InitializingBean;

public class DefaultQuotaStoreInitializer implements InitializingBean {

    @Override
    public void afterPropertiesSet() throws Exception {
        // setup GWC to use the H2 quota store by default
        if (System.getProperty("GEOWEBCACHE_DEFAULT_QUOTA_STORE") == null) {
            System.setProperty("GEOWEBCACHE_DEFAULT_QUOTA_STORE", "H2");
        }
    }

}
