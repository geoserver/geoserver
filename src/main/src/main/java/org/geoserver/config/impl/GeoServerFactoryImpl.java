/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.impl;

import java.util.Collection;
import java.util.Iterator;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.impl.MetadataLinkInfoImpl;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFactory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

public class GeoServerFactoryImpl implements GeoServerFactory, ApplicationContextAware {

    GeoServer gs;
    ApplicationContext applicationContext;

    public GeoServerFactoryImpl(GeoServer gs) {
        this.gs = gs;
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    public GeoServerInfo createGlobal() {
        return new GeoServerInfoImpl(gs);
    }

    public SettingsInfo createSettings() {
        return new SettingsInfoImpl();
    }

    public ContactInfo createContact() {
        return new ContactInfoImpl();
    }

    public JAIInfo createJAI() {
        return new JAIInfoImpl();
    }

    public MetadataLinkInfo createMetadataLink() {
        return new MetadataLinkInfoImpl();
    }

    public ServiceInfo createService() {
        return new ServiceInfoImpl();
    }

    public LoggingInfo createLogging() {
        return new LoggingInfoImpl();
    }

    public Object create(Class clazz) {
        if (applicationContext != null) {
            Collection extensions =
                    applicationContext.getBeansOfType(GeoServerFactory.Extension.class).values();
            for (Iterator e = extensions.iterator(); e.hasNext(); ) {
                Extension extension = (Extension) e.next();
                if (extension.canCreate(clazz)) {
                    return extension.create(clazz);
                }
            }
        }

        return null;
    }
}
