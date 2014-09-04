/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.hibernate;

import java.io.File;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerLoader;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.platform.ContextLoadedEvent;
import org.geoserver.platform.GeoServerResourceLoader;
import org.hibernate.SessionFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.context.ConfigurableApplicationContext;

public class HibGeoServerLoader extends GeoServerLoader {

    CatalogFacade catalogFacade;
    GeoServerFacade geoServerFacade;
    
    public HibGeoServerLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader);
    }

    public void setCatalogFacade(CatalogFacade catalogFacade) {
        this.catalogFacade = catalogFacade;
    }
    
    public void setGeoServerFacade(GeoServerFacade geoServerFacade) {
        this.geoServerFacade = geoServerFacade;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        super.setApplicationContext(applicationContext);
        
        //we create a session during initialization for code that access the catalog/config
        // during startup, once geoserver is startup we use a servlet filter to ensure there is an
        // active session
        final SessionFactory sessionFactory = 
            (SessionFactory) applicationContext.getBean("hibSessionFactory");
        HibUtil.setUpSession(sessionFactory);
        
        if (applicationContext instanceof ConfigurableApplicationContext) {
            ((ConfigurableApplicationContext)applicationContext).addApplicationListener(new ApplicationListener() {
                public void onApplicationEvent(ApplicationEvent event) {
                    if (event instanceof ContextLoadedEvent) {
                        HibUtil.tearDownSession(sessionFactory, null);
                    }
                }
            });
        }
    }
    
    @Override
    protected void loadCatalog(Catalog catalog, XStreamPersister xp) throws Exception {
        ((CatalogImpl)catalog).setFacade(catalogFacade);
        //clearCatalog(CatalogFacade); -- JD: only enabled when testing
        
        //if this is the first time loading up with hibernate configuration, migrate from old
        // file based structure
        File marker = resourceLoader.find("hibernate.marker");
        if (marker == null) {
            readCatalog(catalog, xp);
        }
        
    }

    void clearCatalog(CatalogFacade dao) {
        for (LayerGroupInfo lg : dao.getLayerGroups())  { dao.remove(lg); }
        for (LayerInfo l : dao.getLayers())  { dao.remove(l); }
        for (ResourceInfo r : dao.getResources(ResourceInfo.class))  { dao.remove(r); }
        for (StoreInfo s : dao.getStores(StoreInfo.class)) { dao.remove(s); }
        for (WorkspaceInfo ws : dao.getWorkspaces()){ dao.remove(ws); }
        for (NamespaceInfo ns : dao.getNamespaces()){ dao.remove(ns); }
        for (StyleInfo s : dao.getStyles()){ dao.remove(s); }

    }
    @Override
    protected void loadGeoServer(GeoServer geoServer, XStreamPersister xp) throws Exception {
        ((GeoServerImpl)geoServer).setFacade(geoServerFacade);
        
        //if this is the first time loading up with hibernate configuration, migrate from old
        // file based structure
        File marker = resourceLoader.find("hibernate.marker");
        if (marker == null) {
            readConfiguration(geoServer, xp);
            
            resourceLoader.createFile("hibernate.marker");
        }
        
        //do a post check to ensure things were loaded, for instance if we are starting from 
        // an empty data directory all objects will be empty
        // TODO: this should really be moved elsewhere
        if (geoServer.getGlobal() == null) {
            geoServer.setGlobal(geoServer.getFactory().createGlobal());
        }
        if (geoServer.getLogging() == null) {
            geoServer.setLogging(geoServer.getFactory().createLogging());
        }
    }
    
    @Override
    public void reload() throws Exception {
        //for testing, remove hibernate.marker file
        File f = resourceLoader.find("hibernate.marker");
        if (f != null) {
            f.delete();
        }
        
        super.reload();
    }

}
