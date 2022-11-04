/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import java.util.Properties;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.platform.GeoServerExtensions;
import org.geowebcache.GeoWebCacheEnvironment;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/** This class is responsible for synchronizing the GeoServer environment for GWC. */
public class GWCSynchEnv implements ApplicationContextAware {
    private GeoWebCacheEnvironment gwcEnvironment;

    GeoServerEnvironment gsEnvironment;

    private boolean forceSync = false;

    /**
     * Constructor to inject the GeoServerEnvironment.
     *
     * @param gsEnvironment the GeoServerEnvironment
     */
    public GWCSynchEnv(GeoServerEnvironment gsEnvironment) {
        this.gsEnvironment = gsEnvironment;
    }

    /**
     * Only for unit tests, this inserts a GeoServerEnvironment instance that is not a bean.
     *
     * @param gsEnvironment the GeoServerEnvironment instance to use
     */
    protected void setGsEnvironment(GeoServerEnvironment gsEnvironment) {
        this.gsEnvironment = gsEnvironment;
    }

    /**
     * Only for unit tests, this inserts a GeoWebCacheEnvironment instance that is not a bean.
     *
     * @param gwcEnvironment the GeoWebCacheEnvironment instance to use
     */
    protected void setGwcEnvironment(GeoWebCacheEnvironment gwcEnvironment) {
        this.gwcEnvironment = gwcEnvironment;
    }

    /**
     * Only for unit tests, this forces the synchronization of the GeoServer and GeoWebCache while
     * avoiding setting system properties.
     *
     * @param forceSync
     */
    protected void setForceSync(boolean forceSync) {
        this.forceSync = forceSync;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.gwcEnvironment = GeoServerExtensions.bean(GeoWebCacheEnvironment.class);
        syncEnv();
    }
    /**
     * Synchronizes environment properties between the {@link GeoServerEnvironment} and the {@link
     * GeoWebCacheEnvironment}. (GeoServer properties will override GeoWebCache properties)
     */
    public void syncEnv() throws IllegalArgumentException {
        if (needsSynchronization()) {
            synchronized (this) {
                if (needsSynchronization()) {
                    Properties gwcProps = gwcEnvironment.getProps();

                    if (gwcProps == null) {
                        gwcProps = new Properties();
                    }
                    gwcProps.putAll(gsEnvironment.getProps());

                    gwcEnvironment.setProps(gwcProps);
                }
            }
        }
    }

    private boolean needsSynchronization() {
        return (GeoServerEnvironment.allowEnvParametrization() || forceSync)
                && gsEnvironment != null
                && gsEnvironment.getProps() != null
                && gwcEnvironment != null
                && (gsEnvironment.isStale() || gwcEnvironment.getProps() == null);
    }
}
