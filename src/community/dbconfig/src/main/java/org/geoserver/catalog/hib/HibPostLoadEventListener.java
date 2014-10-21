/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.hib;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.Wrapper;
import org.geoserver.catalog.impl.LayerGroupInfoImpl;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.hibernate.event.PostLoadEvent;
import org.hibernate.event.PostLoadEventListener;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;

/**
 * Event listener that sets the transient catalog reference on catalog objects.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class HibPostLoadEventListener implements PostLoadEventListener, ApplicationContextAware, 
    GeoServerInitializer {

    ApplicationContext appContext;
    GeoServer geoServer;
    Catalog catalog;
    
    boolean active = false;
    
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }
    
    public void initialize(GeoServer geoServer) throws Exception {
        this.geoServer = geoServer;
        
        //we need the original catalog, not a wrapper
        Catalog catalog = geoServer.getCatalog();
        if (catalog instanceof Wrapper) {
            catalog = ((Wrapper)catalog).unwrap(Catalog.class);
        }
        this.catalog = catalog;
        active = true;
    }
    
    public void onPostLoad(PostLoadEvent event) {
        if (!active) return;
        
        Object entity = event.getEntity();
        
        if (entity instanceof StoreInfoImpl) {
            ((StoreInfoImpl)entity).setCatalog(catalog);
        }
        else if (entity instanceof ResourceInfoImpl) {
            ((ResourceInfoImpl)entity).setCatalog(catalog);
        }
        else if (entity instanceof StyleInfoImpl) {
            ((StyleInfoImpl)entity).setCatalog(catalog);
        }
        else if (entity instanceof LayerGroupInfoImpl) {
            //hack to get around default styles being represented by null
            //TODO: see if we can coax the hibernate mappings into doing this for us
            LayerGroupInfoImpl lg = (LayerGroupInfoImpl) entity;
            if (lg.getStyles().isEmpty()) {
                for (LayerInfo l : lg.getLayers()) {
                    lg.getStyles().add(null);
                }
            }
        }
        else if (entity instanceof ServiceInfoImpl) {
            ((ServiceInfoImpl)entity).setGeoServer(geoServer);
        }
        else if (entity instanceof GeoServerInfoImpl) {
            //contact is mapped as a component... and hibernate assumes that all null values 
            // means a null object... i don't think this is configurable but coudl be wrong
            GeoServerInfoImpl global = (GeoServerInfoImpl) entity;
            if (global.getContact() == null) {
                global.setContact(geoServer.getFactory().createContact());
            }
           
        }
    }
}
